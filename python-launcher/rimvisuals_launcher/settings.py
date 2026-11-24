"""Launcher settings and game profiles (separate folders, per-profile RAM / Java / visuals)."""
from __future__ import annotations

import json
import re
import time
import uuid
import zipfile
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Any

from .core import Paths, atomic_write, clean_name, read_json, write_json, safe_join, log

DEFAULT_SETTINGS: dict[str, Any] = {
    "schema": 1,
    "language": "ru",
    "ram_mb": 4096,
    "java_path": "",
    "jvm_args": "",
    "ms_client_id": "",
    "last_profile": "default",
    "check_updates": True,
    "close_launcher_on_start": False,
    # RIM JARVIS
    "jarvis_bridge": True,        # the mod opens its loopback IPC (127.0.0.1 + session token)
    "jarvis_voice": False,        # spoken replies (local Windows speech)
    "jarvis_listen": "off",       # off | ptt (push-to-talk) | wake
    "jarvis_mic": "",             # "" = Windows default microphone
    "jarvis_speaker": "",         # "" = Windows default speakers
    "jarvis_ptt_key": "F8",
    "jarvis_volume": 80,
    "jarvis_rate": 0,
    "jarvis_ai": False,           # optional external AI, off unless the user adds an endpoint and key
    "jarvis_ai_url": "",
    "jarvis_ai_model": "",
}

DEFAULT_VISUALS: dict[str, Any] = {
    "pulseGlow": True, "particles": True, "watermark": True, "infoHud": True,
    "accent": "#8B5CF6", "hudScale": 100,
}

RAM_MIN, RAM_MAX = 1024, 32768


def clamp_ram(value: Any, default: int = 4096) -> int:
    try:
        v = int(value)
    except (TypeError, ValueError):
        return default
    return max(RAM_MIN, min(RAM_MAX, v // 256 * 256))


class Settings:
    def __init__(self, paths: Paths):
        self.paths = paths
        self.data: dict[str, Any] = dict(DEFAULT_SETTINGS)
        self.load()

    def load(self) -> None:
        raw = read_json(self.paths.settings_file, {})
        if isinstance(raw, dict):
            for k, v in raw.items():
                if k in DEFAULT_SETTINGS and isinstance(v, type(DEFAULT_SETTINGS[k])):
                    self.data[k] = v
        self.data["ram_mb"] = clamp_ram(self.data["ram_mb"])
        if self.data["language"] not in ("ru", "en"):
            self.data["language"] = "ru"

    def save(self) -> None:
        write_json(self.paths.settings_file, self.data)

    def __getitem__(self, k: str) -> Any:
        return self.data[k]

    def set(self, k: str, v: Any) -> None:
        if k not in DEFAULT_SETTINGS:
            raise KeyError(k)
        if k == "ram_mb":
            v = clamp_ram(v)
        if k == "ms_client_id":
            v = str(v).strip()
            if v and not re.fullmatch(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", v):
                raise ValueError("Client ID должен быть GUID")
        if k == "jarvis_listen" and v not in ("off", "ptt", "wake"):
            raise ValueError("jarvis_listen")
        if k == "jarvis_ptt_key":
            from .jarvis.voice import VK
            if v not in VK:
                raise ValueError("jarvis_ptt_key")
        if k in ("jarvis_mic", "jarvis_speaker"):
            v = str(v)[:120]
        if k in ("jarvis_volume",):
            v = max(0, min(100, int(v)))
        if k in ("jarvis_rate",):
            v = max(-5, min(5, int(v)))
        if k == "jarvis_ai_url" and v and not str(v).lower().startswith(("https://", "http://127.0.0.1", "http://localhost")):
            raise ValueError("AI endpoint must be https")
        self.data[k] = v
        self.save()


@dataclass
class Profile:
    id: str
    name: str
    ram_mb: int = 4096
    java_path: str = ""
    visuals: dict[str, Any] = field(default_factory=lambda: dict(DEFAULT_VISUALS))
    created: float = field(default_factory=time.time)


class Profiles:
    jarvis_enabled = True

    def __init__(self, paths: Paths, default_ram: int = 4096):
        self.paths = paths
        self.items: dict[str, Profile] = {}
        self.default_ram = default_ram
        self.load()
        if not self.items:
            self.create("RIMVISUALS", pid="default")

    def load(self) -> None:
        raw = read_json(self.paths.profiles_file, {})
        for p in raw.get("profiles", []) if isinstance(raw, dict) else []:
            try:
                pid = re.sub(r"[^a-z0-9\-]", "", str(p["id"]).lower())[:40]
                if not pid:
                    continue
                vis = dict(DEFAULT_VISUALS)
                for k, v in (p.get("visuals") or {}).items():
                    if k in DEFAULT_VISUALS and isinstance(v, type(DEFAULT_VISUALS[k])):
                        vis[k] = v
                self.items[pid] = Profile(pid, clean_name(p.get("name", pid)), clamp_ram(p.get("ram_mb", 4096)),
                                          str(p.get("java_path", "")), vis, float(p.get("created", time.time())))
            except (KeyError, TypeError, ValueError):
                continue

    def save(self) -> None:
        write_json(self.paths.profiles_file, {"schema": 1, "profiles": [asdict(p) for p in self.items.values()]})

    def game_dir(self, pid: str) -> Path:
        return safe_join(self.paths.profiles_dir, pid)

    def create(self, name: str, *, pid: str | None = None, copy_from: str | None = None) -> Profile:
        name = clean_name(name, "Profile")
        pid = pid or re.sub(r"[^a-z0-9\-]", "", name.lower().replace(" ", "-")) or "p"
        base, n = pid, 2
        while pid in self.items:
            pid = f"{base}-{n}"
            n += 1
        prof = Profile(pid, name, self.default_ram)
        if copy_from and copy_from in self.items:
            src = self.items[copy_from]
            prof.ram_mb, prof.java_path, prof.visuals = src.ram_mb, src.java_path, dict(src.visuals)
        self.items[pid] = prof
        self.game_dir(pid).mkdir(parents=True, exist_ok=True)
        self.save()
        return prof

    def rename(self, pid: str, name: str) -> None:
        self.items[pid].name = clean_name(name, self.items[pid].name)
        self.save()

    def delete(self, pid: str) -> None:
        if len(self.items) <= 1:
            raise ValueError("Нельзя удалить последний профиль")
        del self.items[pid]          # game files stay on disk: nothing is erased automatically
        self.save()

    def update(self, pid: str, **kw: Any) -> None:
        p = self.items[pid]
        if "ram_mb" in kw:
            p.ram_mb = clamp_ram(kw["ram_mb"])
        if "java_path" in kw:
            p.java_path = str(kw["java_path"])
        if "visuals" in kw:
            for k, v in kw["visuals"].items():
                if k not in DEFAULT_VISUALS or not isinstance(v, type(DEFAULT_VISUALS[k])):
                    raise ValueError(f"unknown visual setting {k}")
                if k == "accent" and not re.fullmatch(r"#[0-9a-fA-F]{6}", v):
                    raise ValueError("accent must be #RRGGBB")
                if k == "hudScale":
                    v = max(60, min(200, int(v)))
                p.visuals[k] = v
        self.save()

    # ---- bridge to the mod (LauncherSettings.java reads config/rimvisuals/launcher.json, schema 1)
    def write_bridge(self, pid: str) -> Path:
        p = self.items[pid]
        out = self.game_dir(pid) / "config" / "rimvisuals" / "launcher.json"
        atomic_write(out, json.dumps({"schemaVersion": 1, "visuals": p.visuals,
                                      "jarvis": {"enabled": bool(self.jarvis_enabled)}}, indent=2))
        return out

    # ---- import / export (no paths, no secrets)
    def export_json(self, pid: str) -> str:
        p = self.items[pid]
        return json.dumps({"rimvisualsProfile": 1, "name": p.name, "ram_mb": p.ram_mb, "visuals": p.visuals},
                          ensure_ascii=False, indent=2)

    def import_json(self, text: str) -> Profile:
        d = json.loads(text)
        if not isinstance(d, dict) or d.get("rimvisualsProfile") != 1:
            raise ValueError("Это не профиль RIMVISUALS")
        prof = self.create(str(d.get("name", "Imported")))
        self.update(prof.id, ram_mb=d.get("ram_mb", 4096),
                    visuals={k: v for k, v in (d.get("visuals") or {}).items() if k in DEFAULT_VISUALS and isinstance(v, type(DEFAULT_VISUALS[k]))})
        return prof


# ------------------------------------------------------------------------- mod CFG files
SECRET_NAMES = {"mod-session.json"}


class CfgManager:
    """Backups / export / import of config/rimvisuals/ of one profile. Never includes the login session."""

    def __init__(self, paths: Paths, profiles: Profiles):
        self.paths, self.profiles = paths, profiles

    def cfg_dir(self, pid: str) -> Path:
        return self.profiles.game_dir(pid) / "config" / "rimvisuals"

    def files(self, pid: str) -> list[Path]:
        d = self.cfg_dir(pid)
        if not d.exists():
            return []
        return sorted(p for p in d.rglob("*") if p.is_file() and p.name not in SECRET_NAMES
                      and not p.name.endswith((".tmp", ".bak")))

    def export_zip(self, pid: str, dest: Path) -> int:
        n = 0
        with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
            for f in self.files(pid):
                z.write(f, f.relative_to(self.cfg_dir(pid)).as_posix())
                n += 1
        return n

    def backup(self, pid: str) -> Path:
        stamp = time.strftime("%Y%m%d-%H%M%S")
        dest = self.paths.backups / f"{pid}-cfg-{stamp}.zip"
        dest.parent.mkdir(parents=True, exist_ok=True)
        self.export_zip(pid, dest)
        return dest

    def list_backups(self, pid: str) -> list[Path]:
        return sorted(self.paths.backups.glob(f"{pid}-cfg-*.zip"), reverse=True)

    def import_zip(self, pid: str, src: Path) -> int:
        """Restore a zip into the profile. Makes an automatic backup first; refuses unsafe paths and secrets."""
        target = self.cfg_dir(pid)
        with zipfile.ZipFile(src) as z:
            names = [i for i in z.infolist() if not i.is_dir()]
            total = sum(i.file_size for i in names)
            if total > 64 << 20 or len(names) > 500:
                raise ValueError("Архив слишком большой для конфигурации")
            plan = []
            for i in names:
                out = safe_join(target, *i.filename.replace("\\", "/").split("/"))   # raises on ../
                if out.name in SECRET_NAMES:
                    continue
                plan.append((i, out))
            if target.exists():
                self.backup(pid)
            for i, out in plan:
                atomic_write(out, z.read(i))
        return len(plan)
