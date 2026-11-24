"""Bring the window of a process to the foreground (Windows). Returns what really happened, never a guess."""
from __future__ import annotations

import sys


def focus_pid(pid: int) -> tuple[bool, str]:
    if sys.platform != "win32":
        return False, "Фокус окна поддерживается только в Windows."
    try:
        import ctypes
        from ctypes import wintypes
        u = ctypes.windll.user32
        found: list[int] = []
        proc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

        def cb(h, _):
            p = wintypes.DWORD()
            u.GetWindowThreadProcessId(h, ctypes.byref(p))
            if p.value == pid and u.IsWindowVisible(h) and u.GetWindowTextLengthW(h) > 0:
                found.append(h)
            return True
        u.EnumWindows(proc(cb), 0)
        if not found:
            return False, "Окно Minecraft не найдено."
        h = found[0]
        if u.IsIconic(h):
            u.ShowWindow(h, 9)                              # SW_RESTORE
        u.keybd_event(0x12, 0, 0, 0)                        # Alt tap: lets SetForegroundWindow through the foreground lock
        u.keybd_event(0x12, 0, 2, 0)
        ok = bool(u.SetForegroundWindow(h))
        return ok, "Окно Minecraft активировано." if ok else "Windows не разрешила переключить фокус."
    except Exception as e:
        return False, f"Не удалось переключить фокус: {e}"
