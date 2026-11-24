"""Loader operations on top of loader.py: scan, install into the *found* profile, connect over the real IPC, list modules."""
from __future__ import annotations

import json
import socket
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any, Callable

from . import loader as L
from .core import CancelToken, atomic_write, log, read_json
from .jarfetch import JarUnavailable, fetch_release_jar
from .jarvis.ipc import IPC_FILE, BridgeError, BridgeUnavailable, JarvisClient

if TYPE_CHECKING:
    from .service import Launcher


@dataclass
class Scan:
    state: L.State
    instance: L.GameInstance | None
    jar_in_mods: bool
    mod_loaded: bool
    text: str
    restart: bool


def port_open(game_dir: Path | None) -> bool:
    """The in-game mod wrote jarvis-ipc.json *and* its loopback port accepts connections (no auth attempted here)."""
    if not game_dir:
        return False
    info = read_json(game_dir / IPC_FILE, None)
    if not info or "port" not in info:
        return False
    try:
        with socket.create_connection(("127.0.0.1", int(info["port"])), timeout=1.0):
            return True
    except (OSError, ValueError):
        return False


class LoaderService:
    def __init__(self, launcher: "Launcher", finder: Callable[[], list[L.GameInstance]] = L.find_running):
        self.l = launcher
        self.finder = finder
        self.client: JarvisClient | None = None
        self.connected_dir: Path | None = None

    # ------------------------------------------------------------------ scan
    def instances(self) -> list[L.GameInstance]:
        try:
            return self.finder()
        except Exception as e:                                   # a scan must never crash the UI
            log.warning("loader scan failed: %s", e)
            return []

    def evaluate(self, inst: L.GameInstance | None, *, failed: bool = False) -> Scan:
        jar = bool(inst and L.mod_jars(inst.mods_dir))
        loaded = bool(inst and port_open(inst.game_dir))
        conn = bool(inst and self.client and self.client.connected() and self.connected_dir == inst.game_dir)
        st = L.evaluate(inst, jar_in_mods=jar, mod_loaded=loaded, connected=conn, failed=failed)
        return Scan(st, inst, jar, loaded, self._text(st, inst), L.needs_restart(st))

    def scan(self) -> list[Scan]:
        found = self.instances()
        return [self.evaluate(i) for i in found] or [self.evaluate(None)]

    @staticmethod
    def _text(st: L.State, inst: L.GameInstance | None) -> str:
        base = L.TEXT_RU[st]
        if inst and st is L.State.WRONG_VERSION:
            base += f" Найдена: {inst.mc_version}."
        if inst and st is L.State.RIMVISUALS_MISSING:
            base += " Нажмите «Установить»: JAR будет проверен по SHA-256 и скопирован в mods; игру нужно перезапустить."
        return base

    # ------------------------------------------------------------------ profiles already on disk (game not running)
    def installed(self):
        from .launchers import discover
        return discover(extra=[Path(x) for x in self.l.settings["extra_game_dirs"]])

    def add_folder(self, folder: str) -> None:
        cur = list(self.l.settings["extra_game_dirs"])
        if folder not in cur:
            self.l.settings.set("extra_game_dirs", cur + [folder])

    def install_profile(self, prof, cancel: CancelToken | None = None, progress=None) -> dict[str, Any]:
        """Put the verified mod into an existing profile. No Minecraft/Fabric download: the profile already has them."""
        inst = L.GameInstance(0, prof.game_dir, prof.mc_version, bool(prof.fabric_loader), prof.fabric_loader)
        r = self.install(inst, cancel, progress)
        r["needs_restart"] = False
        r["message"] = "Мод установлен в профиль. Запустите этот профиль в вашем лаунчере, затем откройте Loader и нажмите CONNECT."
        return r

    # ------------------------------------------------------------------ install
    def ensure_jar(self, cancel: CancelToken | None = None, progress=None):
        """bundled → user-picked → official release (SHA-256 verified). Raises JarUnavailable with the real reason."""
        try:
            src = self.l.mod_jar_source()
        except ValueError as e:
            raise JarUnavailable(str(e)) from None
        if src:
            return src
        return fetch_release_jar(self.l.paths, self.l.installer.ep, cancel, progress)

    def install(self, inst: L.GameInstance, cancel: CancelToken | None = None, progress=None) -> dict[str, Any]:
        if inst.game_dir is None:
            raise RuntimeError("Не удалось определить папку игры из командной строки процесса")
        if inst.mc_version and inst.mc_version != L.SUPPORTED_MC:
            raise RuntimeError(f"Версия {inst.mc_version} не поддерживается (нужна {L.SUPPORTED_MC})")
        if not inst.fabric:
            raise RuntimeError("В запущенной игре нет Fabric: создайте профиль Fabric Loader 0.19.3 для 1.21.11 в вашем лаунчере и перезапустите игру")
        src = self.ensure_jar(cancel, progress)
        gd = inst.game_dir
        if not self.l.mods.fabric_api_present(gd):
            self.l.mods.install_fabric_api(gd, cancel)
        self.l.mods.install_mod(gd, src.path)
        self._enable_bridge(gd)
        loaded = port_open(gd)
        return {"installed": src.version, "sha256": src.sha256, "game_dir": str(gd),
                "needs_restart": not loaded,
                "message": ("Мод установлен. Он уже загружен в запущенную игру." if loaded else
                            "Мод установлен в mods. Fabric загружает моды только при старте: перезапустите Minecraft, затем нажмите CONNECT.")}

    def _enable_bridge(self, game_dir: Path) -> None:
        p = game_dir / "config" / "rimvisuals" / "launcher.json"
        cur = read_json(p, {})
        if not isinstance(cur, dict):
            cur = {}
        cur.setdefault("schemaVersion", 1)
        cur["jarvis"] = {"enabled": True}
        atomic_write(p, json.dumps(cur, indent=2))

    # ------------------------------------------------------------------ connect
    def connect(self, inst: L.GameInstance) -> dict[str, Any]:
        """Real handshake: token from jarvis-ipc.json, `auth`, then `ping` and `status` answered by the game thread."""
        s = self.evaluate(inst)
        if s.state in (L.State.NOT_FOUND, L.State.WRONG_VERSION, L.State.FABRIC_MISSING, L.State.RIMVISUALS_MISSING, L.State.INSTALL_REQUIRED):
            return {"ok": False, "state": s.state.value, "message": s.text, "needs_restart": s.restart}
        cl = JarvisClient(inst.game_dir)
        try:
            cl.connect()
            pong = cl.call("ping")
            if not pong.get("ok"):
                raise BridgeError(pong.get("message", "ping отклонён"))
            st = cl.call("status")
        except (BridgeUnavailable, BridgeError) as e:
            cl.close()
            return {"ok": False, "state": L.State.FAILED.value,
                    "message": f"Подключение не удалось: {e}. Нажмите «Диагностика IPC» — она покажет, какой шаг не прошёл.",
                    "diagnosis": self.diagnose_ipc(inst)}
        if self.client:
            self.client.close()
        self.client, self.connected_dir = cl, inst.game_dir
        self.l.set_jarvis_dir(inst.game_dir, cl)
        return {"ok": True, "state": L.State.CONNECTED.value, "message": "Подключено к игре через IPC (токен сессии, только 127.0.0.1).", "status": st}

    def diagnose_ipc(self, inst: L.GameInstance | None) -> str:
        from .ipcdiag import diagnose, format_steps
        return format_steps(diagnose(inst.game_dir if inst else None))

    def disconnect(self) -> None:
        if self.client:
            self.client.close()
        self.client, self.connected_dir = None, None

    # ------------------------------------------------------------------ catalog (real modules, from the running mod)
    def modules(self) -> list[dict[str, Any]]:
        if not (self.client and self.client.connected()):
            raise BridgeUnavailable("Нет подключения к игре")
        r = self.client.call("list_modules")
        if not r.get("ok"):
            raise BridgeError(r.get("message", "list_modules"))
        return r["modules"]

    def module(self, mid: str) -> dict[str, Any]:
        r = self.client.call("get_module", id=mid)
        if not r.get("ok"):
            raise BridgeError(r.get("message", "get_module"))
        return r

    def toggle(self, mid: str, enabled: bool) -> bool:
        """True only if the game confirmed the new state."""
        r = self.client.call("set_module", module=mid, enabled=enabled)
        return bool(r.get("ok") and r.get("enabled") == enabled)

    def set_setting(self, mid: str, key: str, value: Any) -> dict[str, Any]:
        return self.client.call("set_setting", module=mid, setting=key, value=value)
