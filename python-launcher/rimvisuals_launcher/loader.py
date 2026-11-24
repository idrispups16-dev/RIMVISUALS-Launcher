"""RIMVISUALS Loader core: finds a Minecraft started by *any* launcher and reports an honest connection state.

There is no hot-injection here. A Fabric mod is loaded by Knot at JVM start; adding it to a running JVM through the
Attach API is not reliable (mixins cannot be applied to already loaded classes, entrypoints never run). So the Loader
(1) finds the running game and its game directory, (2) checks version / Fabric / mod, (3) installs the verified JAR into
that instance's ``mods`` folder when needed, (4) connects over the existing authenticated loopback IPC once the mod is
actually loaded, and (5) asks for a restart only when one is really required.
"""
from __future__ import annotations

import re
import subprocess
import sys
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path

SUPPORTED_MC = "1.21.11"
MOD_FILE_RE = re.compile(r"^rimvisuals.*\.jar$", re.I)


class State(str, Enum):
    NOT_FOUND = "minecraft_not_found"
    DETECTED = "minecraft_detected"
    WRONG_VERSION = "wrong_version"
    FABRIC_MISSING = "fabric_missing"
    RIMVISUALS_MISSING = "rimvisuals_missing"
    INSTALL_REQUIRED = "installation_required"
    READY = "ready_to_connect"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    FAILED = "connection_failed"


TEXT_RU = {
    State.NOT_FOUND: "Minecraft не найден. Запустите игру через любой лаунчер.",
    State.DETECTED: "Minecraft обнаружен.",
    State.WRONG_VERSION: "Версия игры не поддерживается (нужна " + SUPPORTED_MC + ").",
    State.FABRIC_MISSING: "В запущенной игре нет Fabric. Нужен профиль Fabric и перезапуск игры.",
    State.RIMVISUALS_MISSING: "RIMVISUALS не установлен в этот профиль.",
    State.INSTALL_REQUIRED: "Мод установлен, но игра запущена без него. Перезапустите Minecraft.",
    State.READY: "Готово к подключению.",
    State.CONNECTING: "Подключение…",
    State.CONNECTED: "Подключено.",
    State.FAILED: "Не удалось подключиться.",
}


def split_windows_cmdline(line: str) -> list[str]:
    """Minimal CommandLineToArgvW-style splitter (double quotes, backslash before quote)."""
    out, cur, inq, i, n, have = [], [], False, 0, len(line), False
    while i < n:
        c = line[i]
        if c == "\\":
            j = i
            while j < n and line[j] == "\\":
                j += 1
            k = j - i
            if j < n and line[j] == '"':
                cur.append("\\" * (k // 2)); have = True
                if k % 2:
                    cur.append('"'); i = j + 1
                else:
                    i = j
            else:
                cur.append("\\" * k); have = True; i = j
            continue
        if c == '"':
            inq = not inq; have = True
        elif c in " \t" and not inq:
            if have:
                out.append("".join(cur)); cur, have = [], False
        else:
            cur.append(c); have = True
        i += 1
    if have:
        out.append("".join(cur))
    return out


@dataclass
class GameInstance:
    pid: int
    game_dir: Path | None
    mc_version: str | None
    fabric: bool
    fabric_loader: str | None = None
    cmdline: list[str] = field(default_factory=list, repr=False)

    @property
    def mods_dir(self) -> Path | None:
        return self.game_dir / "mods" if self.game_dir else None


def _arg(args: list[str], name: str) -> str | None:
    for i, a in enumerate(args):
        if a == name and i + 1 < len(args):
            return args[i + 1]
        if a.startswith(name + "="):
            return a.split("=", 1)[1]
    return None


def parse_instance(pid: int, cmdline: str) -> GameInstance | None:
    """Returns None for a Java process that is not a Minecraft client."""
    args = split_windows_cmdline(cmdline)
    low = " ".join(args).lower()
    if "net.minecraft.client.main.main" not in low and "knotclient" not in low and "net.minecraft.client" not in low:
        return None
    fabric = "knotclient" in low or "fabric-loader" in low or "net.fabricmc.loader" in low
    gd = _arg(args, "--gameDir")
    ver = _arg(args, "--version")
    loader = None
    if ver:
        m = re.match(r"fabric-loader-([\d.]+)-(.+)$", ver, re.I)
        if m:
            fabric, loader, ver = True, m.group(1), m.group(2)
    m = re.search(r"fabric-loader-([\d.]+)\.jar", low)
    if m and not loader:
        loader = m.group(1)
    return GameInstance(pid, Path(gd) if gd else None, ver, fabric, loader, args)


def find_running() -> list[GameInstance]:
    """Running Minecraft clients (Windows). Never raises."""
    if sys.platform != "win32":
        return []
    try:
        r = subprocess.run(["powershell", "-NoProfile", "-Command",
                            "Get-CimInstance Win32_Process -Filter \"name='javaw.exe' or name='java.exe'\" | "
                            "ForEach-Object { $_.ProcessId.ToString() + '|' + $_.CommandLine }"],
                           capture_output=True, text=True, timeout=15, creationflags=0x08000000)
    except (OSError, subprocess.SubprocessError):
        return []
    res = []
    for ln in r.stdout.splitlines():
        pid, _, cmd = ln.partition("|")
        if pid.strip().isdigit() and cmd:
            inst = parse_instance(int(pid), cmd)
            if inst:
                res.append(inst)
    return res


def mod_jars(mods_dir: Path | None) -> list[Path]:
    if not mods_dir or not mods_dir.is_dir():
        return []
    return [p for p in mods_dir.iterdir() if p.is_file() and MOD_FILE_RE.match(p.name)]


def evaluate(inst: GameInstance | None, *, jar_in_mods: bool, mod_loaded: bool,
             connected: bool = False, failed: bool = False) -> State:
    """Pure decision. ``mod_loaded`` means the in-game mod answered (its IPC file exists and the port is open)."""
    if inst is None:
        return State.NOT_FOUND
    if inst.mc_version and inst.mc_version != SUPPORTED_MC:
        return State.WRONG_VERSION
    if not inst.fabric:
        return State.FABRIC_MISSING
    if connected:
        return State.CONNECTED
    if failed:
        return State.FAILED
    if mod_loaded:
        return State.READY
    if jar_in_mods:
        return State.INSTALL_REQUIRED     # jar is there but this JVM started without it -> restart needed
    return State.RIMVISUALS_MISSING


def needs_restart(state: State) -> bool:
    return state in (State.FABRIC_MISSING, State.INSTALL_REQUIRED)
