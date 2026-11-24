"""Find Minecraft profiles that ALREADY exist on disk (any launcher), so nothing is downloaded again.

Recognised by on-disk layout, not by brand: official launcher folder (versions/fabric-loader-*), Prism/MultiMC-style instances
(instance.cfg + mmc-pack.json), ATLauncher-style instances (instance.json). A layout is only *recognised*; a launcher is never
labelled supported here — that needs a real test with that launcher (``verified`` stays False until then).
"""
from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass
from pathlib import Path

from .loader import MOD_FILE_RE, SUPPORTED_MC

FABRIC_ID = re.compile(r"^fabric-loader-([\d.]+)-(.+)$", re.I)


@dataclass
class Profile:
    launcher: str
    name: str
    game_dir: Path
    mc_version: str | None
    fabric_loader: str | None
    verified: bool = False            # True only after a documented real-launcher test

    @property
    def compatible(self) -> bool:
        return self.mc_version == SUPPORTED_MC and bool(self.fabric_loader)

    @property
    def has_rimvisuals(self) -> bool:
        m = self.game_dir / "mods"
        return m.is_dir() and any(MOD_FILE_RE.match(p.name) for p in m.iterdir())

    @property
    def note(self) -> str:
        if self.compatible:
            return "подходит: Minecraft 1.21.11 + Fabric" + (" (мод уже установлен)" if self.has_rimvisuals else "")
        if self.mc_version and self.mc_version != SUPPORTED_MC:
            return f"версия {self.mc_version}: нужна {SUPPORTED_MC}"
        if not self.fabric_loader:
            return "нет Fabric Loader: создайте профиль Fabric 0.19.3 для 1.21.11"
        return "версия не определена"


def _read(p: Path):
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def _ini(p: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    try:
        for ln in p.read_text(encoding="utf-8", errors="replace").splitlines():
            if "=" in ln and not ln.startswith(("[", "#", ";")):
                k, _, v = ln.partition("=")
                out[k.strip()] = v.strip()
    except OSError:
        pass
    return out


def scan_official(root: Path, label: str = "Official/.minecraft") -> list[Profile]:
    """A .minecraft-style folder. Profiles come from launcher_profiles.json; versions from versions/*."""
    res: list[Profile] = []
    vers = root / "versions"
    if not vers.is_dir():
        return res
    lp = _read(root / "launcher_profiles.json") or {}
    seen: set[str] = set()
    for pid, pr in (lp.get("profiles") or {}).items():
        vid = str(pr.get("lastVersionId", ""))
        m = FABRIC_ID.match(vid)
        gd = Path(pr["gameDir"]) if pr.get("gameDir") else root
        if m:
            res.append(Profile(label, str(pr.get("name") or pid), gd, m.group(2), m.group(1)))
            seen.add(vid)
    for d in sorted(vers.iterdir()):
        m = FABRIC_ID.match(d.name)
        if m and d.name not in seen and (d / f"{d.name}.json").is_file():
            res.append(Profile(label, d.name, root, m.group(2), m.group(1)))
    return res


def scan_mmc_style(root: Path, label: str) -> list[Profile]:
    """Prism Launcher / MultiMC: <root>/instances/<id>/{instance.cfg, mmc-pack.json, (.minecraft|minecraft)/}."""
    res: list[Profile] = []
    inst = root / "instances"
    if not inst.is_dir():
        return res
    for d in sorted(inst.iterdir()):
        if not (d / "instance.cfg").is_file():
            continue
        pack = _read(d / "mmc-pack.json") or {}
        comp = {c.get("uid"): c.get("version") for c in pack.get("components", []) if isinstance(c, dict)}
        gd = next((d / n for n in (".minecraft", "minecraft") if (d / n).is_dir()), d / ".minecraft")
        res.append(Profile(label, _ini(d / "instance.cfg").get("name", d.name), gd, comp.get("net.minecraft"), comp.get("net.fabricmc.fabric-loader")))
    return res


def scan_atlauncher(root: Path) -> list[Profile]:
    res: list[Profile] = []
    inst = root / "instances"
    if not inst.is_dir():
        return res
    for d in sorted(inst.iterdir()):
        j = _read(d / "instance.json")
        if not isinstance(j, dict):
            continue
        launcher = j.get("launcher") or {}
        loader = launcher.get("loaderVersion") or {}
        fab = loader.get("version") if "fabric" in str(loader.get("type", "") or launcher.get("loaderType", "")).lower() else None
        res.append(Profile("ATLauncher", str(launcher.get("name") or d.name), d, str(j.get("id") or "") or None, str(fab) if fab else None))
    return res


def discover(env: dict[str, str] | None = None, extra: list[Path] | None = None) -> list[Profile]:
    e = env if env is not None else dict(os.environ)
    appdata, local, home = e.get("APPDATA", ""), e.get("LOCALAPPDATA", ""), e.get("USERPROFILE") or e.get("HOME", "")
    out: list[Profile] = []
    seen: set[Path] = set()

    def add(ps: list[Profile]) -> None:
        for p in ps:
            key = p.game_dir.resolve() if p.game_dir.exists() else p.game_dir
            if (key, p.name) not in seen:
                seen.add((key, p.name))
                out.append(p)
    if appdata:
        add(scan_official(Path(appdata) / ".minecraft"))
        for nm in ("PrismLauncher", "MultiMC", "PolyMC"):
            add(scan_mmc_style(Path(appdata) / nm, nm))
        add(scan_atlauncher(Path(appdata) / "ATLauncher"))
    if local:
        for nm in ("PrismLauncher", "MultiMC", "Programs/PrismLauncher"):
            add(scan_mmc_style(Path(local) / nm, nm.split("/")[-1]))
    if home:
        for nm in ("MultiMC", "PrismLauncher", "ATLauncher"):
            add(scan_mmc_style(Path(home) / nm, nm) if nm != "ATLauncher" else scan_atlauncher(Path(home) / nm))
    for x in extra or []:                       # user-chosen folder: any of the layouts
        add(scan_official(x, f"Папка {x.name}") + scan_mmc_style(x, f"Папка {x.name}") + scan_atlauncher(x))
    return out
