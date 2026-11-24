"""State of the black Loader home screen (no tkinter): versions, what «Играть» does, honest status line."""
from __future__ import annotations

from dataclasses import dataclass

from .loader import SUPPORTED_MC

VERSIONS = [SUPPORTED_MC, "1.21.4"]          # 1.20.1 is intentionally absent
BUILT_FOR = {SUPPORTED_MC}                   # versions for which a RIMVISUALS build exists


@dataclass
class GameLink:
    connected: bool = False
    pid: int = 0
    minecraft: str = ""
    player: str = ""
    lost: bool = False                       # connection existed and then dropped
    error: str = ""
    connecting: bool = False


def default_selection(link: GameLink) -> str:
    return link.minecraft if link.connected and link.minecraft in VERSIONS else SUPPORTED_MC


def status_line(link: GameLink, selected: str) -> str:
    if link.connecting:
        return "Подключение к игре…"
    if link.lost:
        return "Соединение с игрой потеряно. Нажмите CONNECT, чтобы повторить."
    if link.error:
        return f"Не удалось подключиться: {link.error}"
    if link.connected:
        if selected == link.minecraft:
            return f"RIMVISUALS подключён к Minecraft {link.minecraft}."
        if selected in BUILT_FOR:
            return f"Подключена игра {link.minecraft}. Для {selected} откройте клиент {selected} и нажмите CONNECT."
    if selected not in BUILT_FOR:
        return f"Сборка RIMVISUALS для {selected} пока недоступна (мод собран для {SUPPORTED_MC})."
    return "Игра не подключена. Запустите Minecraft и нажмите CONNECT."


def play_action(link: GameLink, selected: str) -> tuple[str, str]:
    """('focus', pid) | ('message', text). «Играть» never launches Minecraft: the launcher of the user does that."""
    if link.connected and selected == link.minecraft and link.pid:
        return "focus", str(link.pid)
    if link.connected:
        if selected in BUILT_FOR:
            return "message", f"Сейчас подключена игра {link.minecraft}. Для {selected} откройте этот клиент и нажмите CONNECT."
        return "message", status_line(link, selected)
    if selected not in BUILT_FOR:
        return "message", status_line(link, selected)
    return "message", f"Запустите Minecraft {selected} с Fabric через ваш лаунчер, затем нажмите CONNECT."
