"""Step-by-step diagnosis of the Loader <-> game IPC, with a plain-language fix for the first failing step."""
from __future__ import annotations

import json
import socket
from dataclasses import dataclass
from pathlib import Path

from .core import read_json
from .jarvis.ipc import IPC_FILE, BridgeError, BridgeUnavailable, JarvisClient
from .loader import mod_jars


@dataclass
class Step:
    name: str
    ok: bool
    detail: str
    fix: str = ""


def diagnose(game_dir: Path | None) -> list[Step]:
    """Never raises. Stops at the first failing step (later steps would only repeat the same cause)."""
    out: list[Step] = []

    def add(name, ok, detail, fix=""):
        out.append(Step(name, ok, detail, fix))
        return ok

    if not game_dir:
        add("Папка игры", False, "не определена", "Игра запущена нестандартно: укажите папку профиля вручную или перезапустите игру через обычный лаунчер.")
        return out
    gd = Path(game_dir)
    if not add("Папка игры", gd.is_dir(), str(gd), "Папка не существует: профиль удалён или перемещён."):
        return out
    jars = mod_jars(gd / "mods")
    if not add("JAR мода в mods", bool(jars), ", ".join(j.name for j in jars) or "нет",
               "Нажмите «Установить мод» и перезапустите Minecraft."):
        return out
    lj = read_json(gd / "config" / "rimvisuals" / "launcher.json", None)
    enabled = bool(lj and lj.get("jarvis", {}).get("enabled"))
    if not add("Мост выключен/включён в launcher.json", enabled, "включён" if enabled else "выключен или файла нет",
               "Нажмите «Установить мод» (включит мост) и перезапустите игру: мод читает настройку при старте."):
        return out
    p = gd / IPC_FILE
    info = read_json(p, None)
    if not add("Файл jarvis-ipc.json", bool(info), str(p) if info else "не найден",
               "Мод ещё не загрузился или игра закрыта. Запустите Minecraft с Fabric и дождитесь главного меню. Если уже в игре — проверьте logs/latest.log на ошибки RIMVISUALS."):
        return out
    if not add("Формат файла", isinstance(info.get("port"), int) and isinstance(info.get("token"), str) and len(info["token"]) >= 32,
               "port и token есть" if isinstance(info.get("port"), int) else "повреждён",
               "Закройте игру и запустите заново: файл перезаписывается при старте."):
        return out
    try:
        with socket.create_connection(("127.0.0.1", info["port"]), timeout=1.5):
            pass
        add("Порт 127.0.0.1:%d" % info["port"], True, "принимает соединения")
    except OSError as e:
        add("Порт 127.0.0.1:%d" % info["port"], False, str(e),
            "Файл остался от прошлого запуска (игра закрыта), либо порт блокирует брандмауэр/антивирус для локальных соединений.")
        return out
    cl = JarvisClient(gd, timeout=3.0)
    try:
        cl.connect()
        add("Авторизация токеном", True, "принята")
        pong = cl.call("ping")
        if not add("Ответ игрового потока (ping)", bool(pong.get("ok")), json.dumps(pong, ensure_ascii=False)[:120],
                   "Игра зависла или загружается. Подождите и повторите."):
            return out
        st = cl.call("status")
        add("Статус мода", bool(st.get("ok")), json.dumps(st, ensure_ascii=False)[:160])
    except BridgeError as e:
        add("Авторизация токеном", False, str(e),
            "Токен устарел: запущены две копии игры или файл от прошлой сессии. Закройте лишние Minecraft и повторите.")
    except BridgeUnavailable as e:
        add("Соединение", False, str(e), "Игра закрыта или мост выключен.")
    except OSError as e:
        add("Соединение", False, str(e), "Сетевая ошибка на loopback: проверьте антивирус/брандмауэр.")
    finally:
        cl.close()
    return out


def format_steps(steps: list[Step]) -> str:
    lines = []
    for s in steps:
        lines.append(f"[{'OK' if s.ok else '!!'}] {s.name}: {s.detail}")
        if not s.ok and s.fix:
            lines.append(f"     → {s.fix}")
    return "\n".join(lines)
