"""Install Minecraft 1.21.11 + Fabric Loader into RIMVISUALS' own folder and build the launch command.

Nothing here reads or writes the user's regular .minecraft.
"""
from __future__ import annotations

import os
import re
import shlex
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable

from .core import (APP_NAME, APP_VERSION, TARGET_FABRIC, TARGET_MC, CancelToken, Paths, download, file_hash,
                   http_json, log, parallel_map, read_json, safe_join, write_json)


@dataclass
class Endpoints:
    manifest: str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    resources: str = "https://resources.download.minecraft.net"
    fabric_meta: str = "https://meta.fabricmc.net"
    modrinth: str = "https://api.modrinth.com"


Progress = Callable[[str, float], None]     # (stage text, 0..1)


def fabric_version_id(mc: str = TARGET_MC, loader: str = TARGET_FABRIC) -> str:
    return f"fabric-loader-{loader}-{mc}"


# ------------------------------------------------------------------ rules / libraries
def os_name() -> str:
    return {"win32": "windows", "darwin": "osx"}.get(sys.platform, "linux")


def rules_allow(rules: list[dict[str, Any]] | None, features: dict[str, bool] | None = None) -> bool:
    if not rules:
        return True
    features = features or {}
    allowed = False
    for r in rules:
        match = True
        osr = r.get("os")
        if osr:
            if osr.get("name") and osr["name"] != os_name():
                match = False
            if osr.get("arch") and osr["arch"] not in ("x86_64", "amd64", "x64") and sys.maxsize > 2**32:
                match = False
        feat = r.get("features")
        if feat:
            for k, v in feat.items():
                if bool(features.get(k, False)) != bool(v):
                    match = False
        if match:
            allowed = r.get("action") == "allow"
    return allowed


def maven_path(name: str) -> str:
    """group:artifact:version[:classifier][@ext] -> repo path."""
    ext = "jar"
    if "@" in name:
        name, ext = name.split("@", 1)
    parts = name.split(":")
    group, art, ver = parts[0], parts[1], parts[2]
    cls = f"-{parts[3]}" if len(parts) > 3 else ""
    return f"{group.replace('.', '/')}/{art}/{ver}/{art}-{ver}{cls}.{ext}"


@dataclass
class Artifact:
    path: str           # relative to libraries/
    url: str
    sha1: str | None = None
    sha256: str | None = None
    sha512: str | None = None
    size: int | None = None


def library_artifacts(libs: list[dict[str, Any]]) -> list[Artifact]:
    out: dict[str, Artifact] = {}
    for lib in libs:
        if not rules_allow(lib.get("rules")):
            continue
        art = (lib.get("downloads") or {}).get("artifact")
        if art and art.get("url"):
            out[art["path"]] = Artifact(art["path"], art["url"], art.get("sha1"), None, None, art.get("size"))
            continue
        if "name" in lib and lib.get("url"):               # Fabric / Maven style
            p = maven_path(lib["name"])
            out[p] = Artifact(p, lib["url"].rstrip("/") + "/" + p, lib.get("sha1"), lib.get("sha256"),
                              lib.get("sha512"), lib.get("size"))
    return list(out.values())


def merge_profiles(vanilla: dict[str, Any], fabric: dict[str, Any], new_id: str) -> dict[str, Any]:
    merged = dict(vanilla)
    merged["id"] = new_id
    merged["mainClass"] = fabric["mainClass"]
    merged["libraries"] = list(fabric.get("libraries", [])) + list(vanilla.get("libraries", []))
    args = {"jvm": [], "game": []}
    for src in (vanilla, fabric):
        for k in ("jvm", "game"):
            args[k] += list((src.get("arguments") or {}).get(k, []))
    merged["arguments"] = args
    merged["inheritsFrom"] = vanilla["id"]
    return merged


# ------------------------------------------------------------------ install
class MinecraftInstaller:
    def __init__(self, paths: Paths, ep: Endpoints | None = None):
        self.paths, self.ep = paths, ep or Endpoints()

    @property
    def ledger_file(self) -> Path:
        return self.paths.minecraft / "install-ledger.json"

    def installed_id(self) -> str:
        return fabric_version_id()

    def fetch_metadata(self) -> tuple[dict[str, Any], dict[str, Any]]:
        man = http_json(self.ep.manifest)
        entry = next((v for v in man.get("versions", []) if v.get("id") == TARGET_MC), None)
        if not entry:
            raise RuntimeError(f"Minecraft {TARGET_MC} не найден в манифесте Mojang")
        from .core import http_request
        import hashlib
        raw = http_request(entry["url"], max_bytes=8 << 20)
        if entry.get("sha1") and hashlib.sha1(raw).hexdigest() != entry["sha1"]:
            raise RuntimeError("Контрольная сумма version.json не совпала")
        import json
        vanilla = json.loads(raw.decode("utf-8"))
        fabric = http_json(f"{self.ep.fabric_meta}/v2/versions/loader/{TARGET_MC}/{TARGET_FABRIC}/profile/json")
        if fabric.get("id") and TARGET_FABRIC not in fabric["id"]:
            raise RuntimeError("Fabric вернул неожиданную версию загрузчика")
        return vanilla, fabric

    def install(self, progress: Progress | None = None, cancel: CancelToken | None = None, workers: int = 8) -> dict[str, Any]:
        p = progress or (lambda s, f: None)
        self.paths.ensure()
        p("Метаданные Mojang и Fabric", 0.02)
        vanilla, fabric = self.fetch_metadata()
        vid = fabric_version_id()
        merged = merge_profiles(vanilla, fabric, vid)
        files: list[dict[str, Any]] = []

        # client jar
        client = vanilla["downloads"]["client"]
        cjar = self.paths.versions / TARGET_MC / f"{TARGET_MC}.jar"
        p("Клиент Minecraft", 0.05)
        download(client["url"], cjar, sha1=client["sha1"], size=client["size"], cancel=cancel)
        files.append({"path": str(cjar.relative_to(self.paths.minecraft)), "sha1": client["sha1"], "size": client["size"]})

        # libraries
        arts = library_artifacts(merged["libraries"])
        done = {"n": 0}

        def get_lib(a: Artifact) -> dict[str, Any]:
            dest = safe_join(self.paths.libraries, *a.path.split("/"))
            download(a.url, dest, sha1=a.sha1, sha256=a.sha256, sha512=a.sha512, size=a.size, cancel=cancel)
            done["n"] += 1
            p(f"Библиотеки {done['n']}/{len(arts)}", 0.1 + 0.3 * done["n"] / max(1, len(arts)))
            return {"path": "libraries/" + a.path, "sha1": a.sha1, "sha256": a.sha256, "size": dest.stat().st_size}

        files += parallel_map(get_lib, arts, workers, cancel)

        # assets
        ai = vanilla["assetIndex"]
        idx_path = self.paths.assets / "indexes" / f"{ai['id']}.json"
        download(ai["url"], idx_path, sha1=ai["sha1"], size=ai.get("size"), cancel=cancel)
        index = read_json(idx_path, {})
        objs = list((index.get("objects") or {}).values())
        seen = set()
        uniq = [o for o in objs if not (o["hash"] in seen or seen.add(o["hash"]))]
        n_assets = {"n": 0}

        def get_asset(o: dict[str, Any]) -> None:
            h = o["hash"]
            dest = self.paths.assets / "objects" / h[:2] / h
            download(f"{self.ep.resources}/{h[:2]}/{h}", dest, sha1=h, size=o.get("size"), cancel=cancel)
            n_assets["n"] += 1
            if n_assets["n"] % 50 == 0:
                p(f"Ресурсы {n_assets['n']}/{len(uniq)}", 0.4 + 0.5 * n_assets["n"] / max(1, len(uniq)))

        parallel_map(get_asset, uniq, workers, cancel)
        files.append({"path": f"assets/indexes/{ai['id']}.json", "sha1": ai["sha1"], "size": ai.get("size")})

        # logging config (optional)
        lc = (vanilla.get("logging") or {}).get("client", {}).get("file")
        if lc and lc.get("url"):
            lp = self.paths.assets / "log_configs" / lc["id"]
            download(lc["url"], lp, sha1=lc.get("sha1"), size=lc.get("size"), cancel=cancel)

        vdir = self.paths.versions / vid
        write_json(vdir / f"{vid}.json", merged)
        write_json(self.ledger_file, {"schema": 1, "version": vid, "minecraft": TARGET_MC, "fabric": TARGET_FABRIC,
                                      "assetObjects": len(uniq), "files": files})
        p("Готово", 1.0)
        log.info("Installed %s (%d libraries, %d assets)", vid, len(arts), len(uniq))
        return merged

    # ---- integrity
    def verify(self, full: bool = False) -> list[str]:
        """Return problems found; empty list means files match the ledger. `full` re-hashes every file."""
        led = read_json(self.ledger_file, None)
        if not led:
            return ["Minecraft/Fabric ещё не установлены"]
        problems: list[str] = []
        base = self.paths.minecraft
        for f in led.get("files", []):
            fp = base / f["path"]
            if not fp.exists():
                problems.append(f"нет файла {f['path']}")
                continue
            if f.get("size") and fp.stat().st_size != f["size"]:
                problems.append(f"размер не совпал: {f['path']}")
                continue
            if full:
                for algo in ("sha1", "sha256"):
                    if f.get(algo) and file_hash(fp, algo) != f[algo].lower():
                        problems.append(f"хэш {algo} не совпал: {f['path']}")
        vjson = self.paths.versions / led.get("version", "") / f"{led.get('version', '')}.json"
        if not vjson.exists():
            problems.append("нет version.json")
        return problems

    def is_installed(self) -> bool:
        return not self.verify(False)

    def load_version(self) -> dict[str, Any]:
        vid = fabric_version_id()
        d = read_json(self.paths.versions / vid / f"{vid}.json", None)
        if not d:
            raise RuntimeError("Minecraft/Fabric не установлены: нажмите Установить / Проверить")
        return d


# ------------------------------------------------------------------ launch command
@dataclass
class Account:
    name: str
    uuid: str
    access_token: str
    user_type: str = "msa"        # "msa" or "demo"
    demo: bool = False


def _subst(arg: str, table: dict[str, str]) -> str:
    return re.sub(r"\$\{([a-z_]+)\}", lambda m: table.get(m.group(1), m.group(0)), arg)


def _expand(args: list[Any], table: dict[str, str], features: dict[str, bool]) -> list[str]:
    out: list[str] = []
    for a in args:
        if isinstance(a, str):
            out.append(_subst(a, table))
        elif isinstance(a, dict) and rules_allow(a.get("rules"), features):
            v = a.get("value")
            out += [_subst(x, table) for x in (v if isinstance(v, list) else [v])]
    return out


def safe_extra_jvm_args(text: str) -> list[str]:
    out = []
    for tok in shlex.split(text, posix=False) if text.strip() else []:
        tok = tok.strip('"')
        if not tok.startswith("-"):
            raise ValueError(f"JVM-аргумент должен начинаться с '-': {tok}")
        if tok.lower().startswith(("-javaagent", "-agentlib", "-agentpath", "-xbootclasspath")):
            raise ValueError(f"Аргумент запрещён в лаунчере: {tok.split('=')[0]}")
        out.append(tok)
    return out


@dataclass
class LaunchSpec:
    cmd: list[str]
    cwd: Path
    classpath_missing: list[str] = field(default_factory=list)

    def printable(self) -> str:
        parts, hide = [], False
        for c in self.cmd:
            parts.append("<redacted>" if hide else c)
            hide = c == "--accessToken"
        return " ".join(shlex.quote(x) for x in parts)


def build_launch(paths: Paths, version: dict[str, Any], java: Path, account: Account, game_dir: Path,
                 ram_mb: int, extra_jvm: str = "") -> LaunchSpec:
    libs = library_artifacts(version["libraries"])
    cp_items = [paths.libraries.joinpath(*a.path.split("/")) for a in libs]
    client_jar = paths.versions / TARGET_MC / f"{TARGET_MC}.jar"
    cp_items.append(client_jar)
    missing = [str(c.name) for c in cp_items if not c.exists()]
    sep = os.pathsep
    natives = paths.versions / version["id"] / "natives"
    natives.mkdir(parents=True, exist_ok=True)
    table = {
        "auth_player_name": account.name, "auth_uuid": account.uuid, "auth_access_token": account.access_token,
        "auth_xuid": "0", "clientid": "0", "user_type": account.user_type, "version_name": version["id"],
        "version_type": "release", "game_directory": str(game_dir), "assets_root": str(paths.assets),
        "assets_index_name": version["assetIndex"]["id"], "natives_directory": str(natives),
        "launcher_name": APP_NAME, "launcher_version": APP_VERSION, "classpath": sep.join(str(c) for c in cp_items),
        "library_directory": str(paths.libraries), "classpath_separator": sep, "user_properties": "{}",
    }
    features = {"is_demo_user": account.demo, "has_custom_resolution": False, "has_quick_plays_support": False,
                "is_quick_play_singleplayer": False, "is_quick_play_multiplayer": False, "is_quick_play_realms": False}
    jvm = _expand((version.get("arguments") or {}).get("jvm", []), table, features)
    game = _expand((version.get("arguments") or {}).get("game", []), table, features)
    if "-cp" not in jvm and "-classpath" not in jvm:
        jvm += ["-cp", table["classpath"]]
    heap = [f"-Xmx{int(ram_mb)}M", f"-Xms{min(int(ram_mb), 1024)}M"]
    cmd = [str(java)] + heap + safe_extra_jvm_args(extra_jvm) + jvm + [version["mainClass"]] + game
    if account.demo:
        for flag in ("--demo", "--disableMultiplayer", "--disableChat"):
            if flag not in cmd:
                cmd.append(flag)
    game_dir.mkdir(parents=True, exist_ok=True)
    return LaunchSpec(cmd, game_dir, missing)
