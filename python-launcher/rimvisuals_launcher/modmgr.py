"""Install / update / verify the RIMVISUALS Fabric mod (+ Fabric API) inside a profile's mods folder."""
from __future__ import annotations

import json
import os
import re
import shutil
import sys
import time
import urllib.parse
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .core import (is_safe_url, TARGET_FABRIC_API, TARGET_MC, CancelToken, HttpError, Paths, atomic_write, download, http_json,
                   log, read_json, sha256_file, write_json)
from .minecraft import Endpoints

LEDGER = ".rimvisuals-install.json"


@dataclass
class ModJar:
    path: Path
    version: str
    sha256: str
    minecraft: str
    mod_id: str

    @property
    def targets_current(self) -> bool:
        return bool(re.search(r"1\.21(\.11)?", self.minecraft)) and self.mod_id == "rimvisuals"


def read_jar_meta(jar: Path) -> ModJar:
    try:
        with zipfile.ZipFile(jar) as z:
            meta = json.loads(z.read("fabric.mod.json").decode("utf-8"))
    except (OSError, KeyError, ValueError, zipfile.BadZipFile) as e:
        raise ValueError(f"{jar.name}: не похоже на Fabric-мод ({e.__class__.__name__})") from None
    deps = meta.get("depends") or {}
    return ModJar(jar, str(meta.get("version", "?")), sha256_file(jar), str(deps.get("minecraft", "")), str(meta.get("id", "")))


def bundled_dirs() -> list[Path]:
    out = []
    if getattr(sys, "_MEIPASS", None):
        out.append(Path(sys._MEIPASS) / "assets")
    here = Path(__file__).resolve().parent
    out += [here.parent / "assets", here / "assets"]
    exe = Path(sys.executable).parent
    out += [exe / "assets"]
    env = os.environ.get("RIMVISUALS_ASSETS")
    if env:
        out.insert(0, Path(env))
    return out


def find_bundled_jar() -> ModJar | None:
    """The mod JAR shipped with the launcher (assets/rimvisuals-*.jar) with its manifest hash checked."""
    for d in bundled_dirs():
        if not d.is_dir():
            continue
        jars = sorted(j for j in d.glob("rimvisuals-*.jar") if not j.name.endswith(("-sources.jar", "-dev.jar")))
        if not jars:
            continue
        jar = jars[-1]
        info = read_jar_meta(jar)
        mf = read_json(d / "rimvisuals-mod.json", None)
        if mf and mf.get("sha256") and mf["sha256"].lower() != info.sha256:
            raise ValueError(f"Контрольная сумма {jar.name} не совпала с манифестом сборки")
        return info
    return None


class ModManager:
    def __init__(self, paths: Paths, ep: Endpoints | None = None):
        self.paths, self.ep = paths, ep or Endpoints()

    def mods_dir(self, game_dir: Path) -> Path:
        return game_dir / "mods"

    def ledger(self, game_dir: Path) -> dict[str, Any]:
        return read_json(self.mods_dir(game_dir) / LEDGER, {})

    def installed(self, game_dir: Path) -> list[Path]:
        d = self.mods_dir(game_dir)
        return sorted(d.glob("rimvisuals-*.jar")) if d.exists() else []

    # ---- install RIMVISUALS jar
    def install_mod(self, game_dir: Path, jar: Path) -> ModJar:
        info = read_jar_meta(jar)
        if info.mod_id != "rimvisuals":
            raise ValueError("Выбранный JAR — не RIMVISUALS")
        if not info.targets_current:
            raise ValueError(f"JAR рассчитан на Minecraft {info.minecraft!r}, нужен {TARGET_MC}")
        md = self.mods_dir(game_dir)
        md.mkdir(parents=True, exist_ok=True)
        bak = self.paths.backups / "mods" / time.strftime("%Y%m%d-%H%M%S")
        for old in self.installed(game_dir):          # replace only our own jars, keep the user's other mods
            if old.name != jar.name:
                bak.mkdir(parents=True, exist_ok=True)
                shutil.move(str(old), str(bak / old.name))
        dest = md / jar.name
        if not (dest.exists() and sha256_file(dest) == info.sha256):
            tmp = dest.with_name(dest.name + ".tmp")
            shutil.copyfile(jar, tmp)
            os.replace(tmp, dest)
        led = self.ledger(game_dir)
        led.update({"schema": 1, "rimvisuals": {"file": dest.name, "sha256": info.sha256, "version": info.version}})
        write_json(md / LEDGER, led)
        log.info("Mod %s installed (%s)", info.version, dest.name)
        return read_jar_meta(dest)

    # ---- Fabric API (required dependency)
    def latest_fabric_api(self) -> dict[str, Any]:
        q = urllib.parse.urlencode({"game_versions": json.dumps([TARGET_MC]), "loaders": json.dumps(["fabric"])})
        versions = http_json(f"{self.ep.modrinth}/v2/project/fabric-api/version?{q}")
        for v in versions:
            if v.get("version_number") == TARGET_FABRIC_API:
                files = v.get("files") or []
                f = next((x for x in files if x.get("primary")), files[0] if files else None)
                if f and f.get("hashes", {}).get("sha512") and is_safe_url(f.get("url", "")):
                    return {"filename": f["filename"], "url": f["url"], "sha512": f["hashes"]["sha512"], "size": f.get("size"),
                            "version": v["version_number"]}
        raise RuntimeError(f"Fabric API {TARGET_FABRIC_API} не найден на Modrinth")

    def install_fabric_api(self, game_dir: Path, cancel: CancelToken | None = None) -> Path:
        md = self.mods_dir(game_dir)
        md.mkdir(parents=True, exist_ok=True)
        info = self.latest_fabric_api()
        dest = md / info["filename"]
        for old in md.glob("fabric-api-*.jar"):
            if old.name != dest.name:
                old.rename(old.with_name(old.name + ".old"))
        download(info["url"], dest, sha512=info["sha512"], size=info["size"], cancel=cancel)
        led = self.ledger(game_dir)
        led["fabric_api"] = {"file": dest.name, "version": info["version"]}
        write_json(md / LEDGER, led)
        return dest

    def fabric_api_present(self, game_dir: Path) -> bool:
        d = self.mods_dir(game_dir)
        return d.exists() and any(d.glob("fabric-api-*.jar"))

    # ---- integrity
    def verify(self, game_dir: Path) -> list[str]:
        problems: list[str] = []
        jars = self.installed(game_dir)
        led = self.ledger(game_dir).get("rimvisuals")
        if not jars:
            return ["Мод RIMVISUALS не установлен"]
        if len(jars) > 1:
            problems.append("в mods несколько RIMVISUALS JAR: " + ", ".join(j.name for j in jars))
        if not led:
            problems.append("нет записи об установке (JAR положен вручную)")
        else:
            j = self.mods_dir(game_dir) / led["file"]
            if not j.exists():
                problems.append(f"нет файла {led['file']}")
            elif sha256_file(j) != led["sha256"]:
                problems.append(f"{led['file']} изменён после установки (SHA-256 не совпал)")
        if not self.fabric_api_present(game_dir):
            problems.append("не установлен Fabric API")
        return problems

    def status(self, game_dir: Path) -> dict[str, Any]:
        jars = self.installed(game_dir)
        bundled = None
        try:
            bundled = find_bundled_jar()
        except ValueError as e:
            log.warning("%s", e)
        cur = read_jar_meta(jars[-1]) if jars else None
        return {"installed": cur.version if cur else None, "bundled": bundled.version if bundled else None,
                "update_available": bool(bundled and cur and bundled.sha256 != cur.sha256),
                "problems": self.verify(game_dir)}
