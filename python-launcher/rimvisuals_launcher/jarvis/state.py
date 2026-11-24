"""RIM JARVIS indicator state machine (UI-independent, testable)."""
from __future__ import annotations

import threading
from typing import Callable

STATES = ("idle", "listening", "processing", "speaking", "success", "error")
LABEL_RU = {"idle": "Неактивен", "listening": "Слушаю", "processing": "Обрабатываю", "speaking": "Отвечаю", "success": "Выполнено", "error": "Ошибка"}
TRANSIENT = {"success": 2.5, "error": 4.0}          # return to idle after this many seconds


class StateTracker:
    def __init__(self, on_change: Callable[[str, str], None] | None = None,
                 timer: Callable[[float, Callable[[], None]], object] | None = None):
        self.state, self.detail = "idle", ""
        self._cb = on_change or (lambda s, d: None)
        self._timer = timer or self._thread_timer
        self._gen = 0
        self._lock = threading.Lock()

    @staticmethod
    def _thread_timer(sec: float, fn: Callable[[], None]):
        t = threading.Timer(sec, fn)
        t.daemon = True
        t.start()
        return t

    def set(self, state: str, detail: str = "") -> None:
        if state not in STATES:
            raise ValueError(state)
        with self._lock:
            self._gen += 1
            gen = self._gen
            self.state, self.detail = state, detail
        self._cb(state, detail)
        if state in TRANSIENT:
            self._timer(TRANSIENT[state], lambda: self._expire(gen))

    def _expire(self, gen: int) -> None:
        with self._lock:
            if gen != self._gen:                   # a newer state arrived; do not clobber it
                return
            self._gen += 1
            self.state, self.detail = "idle", ""
        self._cb("idle", "")

    @property
    def text(self) -> str:
        return LABEL_RU[self.state] + (f": {self.detail}" if self.detail else "")
