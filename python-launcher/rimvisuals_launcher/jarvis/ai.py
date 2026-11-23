"""Optional external AI for RIM JARVIS ("AI Mode"). Off by default; no paid API is preconfigured.

The user supplies an OpenAI-compatible chat endpoint (https) and a key. The key lives in the DPAPI secret store.
Only the user's text (and, if they ask to explain a crash, the redacted log tail) is sent, and only after they turned
AI Mode on. The reply is shown as text. It is NEVER executed: game actions only come from the offline parser + allowlist.
"""
from __future__ import annotations

from typing import Any, Callable

from ..core import HttpError, http_json, redact

SYSTEM = ("You are RIM JARVIS, a calm, concise assistant inside the RIMVISUALS Minecraft launcher (Fabric 1.21.11). "
          "Answer in the user's language. You cannot press buttons; suggest what the user can say or do.")


class AiClient:
    def __init__(self, url: str, model: str, get_key: Callable[[], str | None]):
        self.url, self.model, self.get_key = url.strip(), model.strip(), get_key

    @property
    def configured(self) -> bool:
        return bool(self.url and self.model and self.get_key())

    def ask(self, text: str, context: str = "") -> str:
        key = self.get_key()
        if not (self.url and self.model and key):
            raise RuntimeError("AI Mode не настроен (адрес, модель и ключ)")
        msgs: list[dict[str, Any]] = [{"role": "system", "content": SYSTEM}]
        if context:
            msgs.append({"role": "user", "content": "Context (redacted log):\n" + redact(context)[-6000:]})
        msgs.append({"role": "user", "content": text[:2000]})
        try:
            r = http_json(self.url, payload={"model": self.model, "messages": msgs, "max_tokens": 400},
                          headers={"Authorization": "Bearer " + key}, timeout=40)
            return str(r["choices"][0]["message"]["content"]).strip()
        except (HttpError, KeyError, IndexError, TypeError) as e:
            raise RuntimeError(f"AI не ответил ({e.__class__.__name__})") from None
