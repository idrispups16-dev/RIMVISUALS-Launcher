"""Assistant chat history and the daily request counter.

The 50-requests-per-day quota is meant to be kept on the server; until the Supabase endpoint exists this counter is LOCAL and the UI
says so. Rules implemented here already: confirming/declining a card or re-sending the same request id never costs a unit.
"""
from __future__ import annotations

import json
import time
import uuid
from pathlib import Path
from typing import Any

from ..core import atomic_write, read_json

DAILY = 50
MAX_CHATS, MAX_MSGS = 50, 400


class ChatStore:
    def __init__(self, path: Path, clock=time.time):
        self.path, self.clock = path, clock
        d = read_json(path, {})
        self.d: dict[str, Any] = d if isinstance(d, dict) else {}
        self.d.setdefault("chats", [])
        self.d.setdefault("quota", {"day": "", "ids": []})

    def _save(self) -> None:
        atomic_write(self.path, json.dumps(self.d, ensure_ascii=False))

    # ---- quota
    def _day(self) -> str:
        return time.strftime("%Y-%m-%d", time.localtime(self.clock()))

    def _quota(self) -> dict[str, Any]:
        q = self.d["quota"]
        if q.get("day") != self._day():
            q.update(day=self._day(), ids=[])
        return q

    def remaining(self) -> int:
        return max(0, DAILY - len(self._quota()["ids"]))

    def next_reset(self) -> str:
        t = time.localtime(self.clock())
        nxt = time.mktime((t.tm_year, t.tm_mon, t.tm_mday + 1, 0, 0, 0, 0, 0, -1))
        return time.strftime("%d.%m %H:%M", time.localtime(nxt))

    def charge(self, request_id: str) -> bool:
        """True if the request may run. A known id is free (redelivery)."""
        q = self._quota()
        if request_id in q["ids"]:
            return True
        if len(q["ids"]) >= DAILY:
            return False
        q["ids"].append(request_id)
        self._save()
        return True

    # ---- chats
    def new_chat(self, title: str = "Новый чат") -> dict[str, Any]:
        c = {"id": uuid.uuid4().hex[:12], "title": title, "msgs": []}
        self.d["chats"].insert(0, c)
        del self.d["chats"][MAX_CHATS:]
        self._save()
        return c

    def chats(self) -> list[dict[str, Any]]:
        return self.d["chats"]

    def add(self, chat: dict[str, Any], role: str, text: str, **extra: Any) -> None:
        chat["msgs"].append({"role": role, "text": text, "t": int(self.clock()), **extra})
        del chat["msgs"][:-MAX_MSGS]
        if role == "me" and chat["title"] == "Новый чат":
            chat["title"] = text[:32]
        self._save()
