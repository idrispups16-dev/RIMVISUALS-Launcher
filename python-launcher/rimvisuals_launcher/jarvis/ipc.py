"""Client for the Fabric mod's local JARVIS bridge (127.0.0.1 + session token; see JarvisServer.java)."""
from __future__ import annotations

import json
import socket
import threading
from pathlib import Path
from typing import Any

from ..core import log, read_json

IPC_FILE = Path("config") / "rimvisuals" / "jarvis-ipc.json"


class BridgeError(RuntimeError):
    pass


class BridgeUnavailable(BridgeError):
    """The game is not running, or the mod's JARVIS bridge is switched off."""


class JarvisClient:
    def __init__(self, game_dir: Path, timeout: float = 5.0):
        self.game_dir, self.timeout = Path(game_dir), timeout
        self._sock: socket.socket | None = None
        self._rf = None
        self._lock = threading.Lock()

    @property
    def ipc_path(self) -> Path:
        return self.game_dir / IPC_FILE

    def connected(self) -> bool:
        return self._sock is not None

    def connect(self) -> None:
        info = read_json(self.ipc_path, None)
        if not info or "port" not in info or "token" not in info:
            raise BridgeUnavailable("мост JARVIS не найден: игра не запущена или он выключен в настройках")
        port = int(info["port"])
        try:
            s = socket.create_connection(("127.0.0.1", port), timeout=self.timeout)   # loopback only, never a configured host
        except OSError:
            raise BridgeUnavailable("мост JARVIS не отвечает (игра закрыта?)") from None
        s.settimeout(self.timeout)
        rf = s.makefile("r", encoding="utf-8", newline="\n")
        s.sendall((json.dumps({"auth": info["token"]}) + "\n").encode())
        hello = rf.readline()
        if not hello or not json.loads(hello).get("ok"):
            s.close()
            raise BridgeError("мост отклонил токен")
        self._sock, self._rf = s, rf

    def close(self) -> None:
        with self._lock:
            if self._sock:
                try:
                    self._sock.close()
                except OSError:
                    pass
            self._sock = self._rf = None

    def call(self, action: str, **params: Any) -> dict[str, Any]:
        """One allowlisted request. Reconnects once if the previous connection died."""
        with self._lock:
            for attempt in (0, 1):
                try:
                    if not self._sock:
                        self._locked_connect()
                    self._sock.sendall((json.dumps({"action": action, **params}) + "\n").encode())
                    line = self._rf.readline()
                    if not line:
                        raise OSError("closed")
                    res = json.loads(line)
                    log.info("JARVIS %s %s -> %s", action, {k: v for k, v in params.items() if k != "token"}, "ok" if res.get("ok") else res.get("error"))
                    return res
                except (OSError, ValueError):
                    self._sock = self._rf = None
                    if attempt:
                        raise BridgeUnavailable("связь с игрой потеряна") from None
        raise BridgeUnavailable("нет связи")

    def _locked_connect(self) -> None:
        # connect() does not take the lock itself
        self.connect()
