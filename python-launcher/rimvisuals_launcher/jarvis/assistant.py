"""RIM JARVIS: understands a command, runs it through the mod's allowlisted IPC, and reports what REALLY happened."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable

from ..core import log
from .intents import ALIASES, Intent, parse
from .ipc import BridgeError, BridgeUnavailable, JarvisClient


@dataclass
class Reply:
    text: str
    ok: bool = True
    executed: bool = False                     # True only if the game confirmed a change
    pending: dict[str, Any] | None = None      # dangerous action waiting for the user's "yes"
    details: dict[str, Any] = field(default_factory=dict)


def _t(lang: str, ru: str, en: str) -> str:
    return ru if lang == "ru" else en


NOT_RUNNING = ("Minecraft не запущен или мост JARVIS выключен (Настройки → JARVIS). Запустите игру через INJECT.",
               "Minecraft is not running or the JARVIS bridge is off (Settings → JARVIS). Start the game with INJECT.")


class Assistant:
    def __init__(self, client: JarvisClient, local: dict[str, Callable[[], str]] | None = None,
                 ai: "Callable[[str], str] | None" = None, mods: Any = None):
        self.client = client
        self.mods = mods                        # ModOps (see service.ModOps) — plan/apply/conflicts/crash/updates
        self.local = local or {}                # launcher-side handlers: help/status/updates
        self.ai = ai                            # optional external AI (explicit opt-in)
        self.pending: dict[str, Any] | None = None
        self.history: list[tuple[str, str]] = []

    # ----------------------------------------------------------------- public
    def ask(self, text: str) -> Reply:
        low = text.strip().lower()
        if self.pending and low in ("да", "подтверждаю", "yes", "confirm", "ок", "ok", "yes please", "да, подтверждаю"):
            return self.confirm()
        if self.pending and low in ("нет", "отмена", "no", "cancel", "отменить"):
            self.pending = None
            return Reply("Отменено." if any("а" <= c <= "я" for c in low) else "Cancelled.")
        it = parse(text)
        r = self._run(it)
        self.history.append((text, r.text))
        del self.history[:-50]
        return r

    def confirm(self) -> Reply:
        p, self.pending = self.pending, None
        if not p:
            return Reply("Нечего подтверждать.", ok=False)
        if p.get("do"):
            try:
                return p["do"]()
            except Exception as e:  # shown to the user
                log.warning("confirmed mod action failed: %s", e)
                return Reply(str(e), ok=False)
        return self._exec(p["action"], p["params"], p["lang"], p["say"], confirmed=True)

    # ----------------------------------------------------------------- dispatch
    def _run(self, it: Intent) -> Reply:
        L = it.lang
        if it.kind == "local":
            fn = self.local.get(it.name)
            return Reply(fn() if fn else _t(L, "Эта функция недоступна здесь.", "Not available here."), ok=bool(fn))
        if it.kind == "mods":
            return self._mods(it)
        if it.kind == "unknown":
            if self.ai:
                return self._ai(it)
            return Reply(_t(L, "Не понял команду. Скажите «помощь», чтобы увидеть, что я умею.",
                            "I did not understand. Say “help” to see what I can do."), ok=False)
        n, p = it.name, it.params
        if n in ("enable", "disable"):
            on = n == "enable"
            name = self._name(p["module"])
            return self._exec("set_module", {"module": p["module"], "enabled": on}, L,
                              lambda r: _t(L, f"{name} {'включён' if on else 'выключен'}.", f"{name} {'enabled' if on else 'disabled'}."))
        if n == "performance":
            return self._exec("performance_mode", {"mode": p["mode"]}, L,
                              lambda r: _t(L, f"Режим {r['mode']} включён. Лимит частиц: {r['particleLimit']}.", f"{r['mode']} mode on. Particle limit: {r['particleLimit']}."))
        if n == "open_gui":
            return self._exec("open_gui", {"target": p["target"]}, L, lambda r: _t(L, "Открываю.", "Opening."))
        if n == "save_config":
            if not p["config"]:
                return Reply(_t(L, "Как назвать конфиг? Например: «сохрани конфиг PvP».", "Name the config, e.g. “save config PvP”."), ok=False)
            return self._exec("save_config", {"name": p["config"]}, L, lambda r: _t(L, f"Конфиг «{r['name']}» сохранён.", f"Config “{r['name']}” saved."))
        if n == "load_config":
            if not p["config"]:
                return Reply(_t(L, "Какой конфиг загрузить?", "Which config?"), ok=False)
            return self._exec("load_config", {"name": p["config"]}, L, lambda r: _t(L, f"Конфиг «{r['name']}» загружен.", f"Config “{r['name']}” loaded."))
        if n == "preset":
            return self._exec("apply_preset", {"name": p["preset"]}, L, lambda r: _t(L, f"Пресет {r['name']} применён.", f"Preset {r['name']} applied."))
        if n == "color":
            return self._color(p["modules"], p["color"], L)
        if n == "events":
            return self._events(L)
        if n == "cooldown":
            return self._cooldown(p.get("which", ""), L)
        if n == "fps":
            return self._fps(L)
        return Reply("?", ok=False)

    # ----------------------------------------------------------------- AI mod manager
    def _mods(self, it: Intent) -> Reply:
        L, n = it.lang, it.name
        if not self.mods:
            return Reply(_t(L, "Менеджер модов недоступен.", "Mod manager unavailable."), ok=False)
        try:
            if n == "conflicts":
                pr = self.mods.conflicts()
                return Reply(_t(L, "Конфликтов в mods не найдено.", "No conflicts found in mods.") if not pr else
                             _t(L, "Нашёл: ", "Found: ") + "; ".join(pr[:6]), details={"problems": pr})
            if n == "crash":
                txt = self.mods.crash_text()
                if not txt:
                    return Reply(_t(L, "Нет логов игры для разбора.", "No game logs to analyse."), ok=False)
                return Reply("\n".join(self.mods.analyze(txt)))
            if n == "mod_updates":
                return Reply(self.mods.updates())
            if n in ("install_mod", "pvp_pack"):
                slugs = [it.params["slug"]] if n == "install_mod" else self.mods.PVP_PACK
                plan = self.mods.plan(slugs)
                if not plan.items:
                    return Reply(_t(L, "Не нашёл подходящей версии: ", "No compatible version: ") + "; ".join(plan.problems), ok=False)
                text = _t(L, "Изменения перед установкой:\n", "Changes before install:\n") + plan.describe()
                if all(i.action == "keep" for i in plan.items):
                    return Reply(text + _t(L, "\nВсё уже установлено.", "\nAlready installed."))
                self.pending = {"do": lambda: Reply(_t(L, "Установлено (SHA-512 проверен): ", "Installed (SHA-512 verified): ") + ", ".join(self.mods.apply(plan)) +
                                                    _t(L, ". Перезапустите игру.", ". Restart the game."), executed=True), "lang": L}
                return Reply(text + _t(L, "\nУстановить? «да» / «нет». Совместимость с RIMVISUALS в игре не проверена.", "\nInstall? “yes” / “no”. Compatibility with RIMVISUALS in game is not verified."),
                             pending={"action": "install_mods", "params": {"slugs": slugs}})
        except (BridgeError, RuntimeError, ValueError, OSError) as e:
            return Reply(f"{e}", ok=False)
        except Exception as e:  # network errors etc.
            return Reply(_t(L, "Источник модов недоступен: ", "Mod source unavailable: ") + e.__class__.__name__, ok=False)
        return Reply("?", ok=False)

    # ----------------------------------------------------------------- game calls
    def _call(self, action: str, **params: Any) -> dict[str, Any]:
        return self.client.call(action, **params)

    def _exec(self, action: str, params: dict[str, Any], lang: str, say: Callable[[dict], str], confirmed: bool = False) -> Reply:
        extra = {"confirmed": True} if confirmed else {}
        try:
            r = self._call(action, **params, **extra)
        except BridgeUnavailable:
            return Reply(_t(lang, *NOT_RUNNING), ok=False)
        except BridgeError as e:
            return Reply(str(e), ok=False)
        if r.get("ok"):
            return Reply(say(r), executed=True, details=r)
        if r.get("error") == "confirmation_required":
            self.pending = {"action": action, "params": params, "lang": lang, "say": say}
            what = f"{action} {params}"
            return Reply(_t(lang, f"Это изменит сразу несколько настроек ({what}). Подтвердите: «да» или «нет».",
                            f"This changes several settings ({what}). Confirm with “yes” or “no”."), pending={"action": action, "params": params})
        return Reply(self._explain(r, lang), ok=False, details=r)

    @staticmethod
    def _explain(r: dict[str, Any], lang: str) -> str:
        m = {"access_denied": ("Нужен вход в аккаунт RIMVISUALS (вкладка «Аккаунт»).", "Sign in to your RIMVISUALS account first."),
             "no_module": ("Такого модуля нет в этой версии мода.", "No such module in this mod version."),
             "no_setting": ("У модуля нет такой настройки.", "That module has no such setting."),
             "no_config": ("Такого конфига нет.", "No such config."),
             "no_preset": ("Такого пресета нет.", "No such preset."),
             "not_toggleable": ("Этот модуль нельзя включать и выключать.", "That module cannot be switched.")}
        if r.get("error") in m:
            return _t(lang, *m[r["error"]])
        return _t(lang, "Игра отклонила команду: ", "The game refused: ") + str(r.get("message") or r.get("error"))

    def _name(self, mid: str) -> str:
        try:
            for m in self._call("list_modules").get("modules", []):
                if m["id"] == mid:
                    return m["name"]
        except BridgeError:
            pass
        return mid

    def _color(self, modules: list[str], color: str, lang: str) -> Reply:
        done, failed = [], []
        try:
            for mid in modules:
                info = self._call("get_module", id=mid)
                if not info.get("ok"):
                    failed.append(mid)
                    continue
                cs = [s for s in info["settings"] if s["type"] == "color"]
                target = next((s for s in cs if s["key"] == "color"), cs[0] if cs else None)
                if not target:
                    failed.append(mid)
                    continue
                r = self._call("set_setting", module=mid, setting=target["key"], value=color)
                (done if r.get("ok") else failed).append(mid)
        except BridgeUnavailable:
            return Reply(_t(lang, *NOT_RUNNING), ok=False)
        if not done:
            return Reply(_t(lang, "Не удалось изменить цвет: у модуля нет настройки цвета.", "Could not change the colour: no colour setting."), ok=False)
        return Reply(_t(lang, f"Цвет {color} установлен: {', '.join(done)}.", f"Colour {color} set on: {', '.join(done)}."),
                     executed=True, details={"modules": done, "failed": failed})

    def _events(self, lang: str) -> Reply:
        try:
            r = self._call("query", what="events")
        except BridgeUnavailable:
            return Reply(_t(lang, *NOT_RUNNING), ok=False)
        rows = [x for x in r.get("events", []) if x and not x.lower().startswith("no received")]
        if not rows:
            return Reply(_t(lang, "Сейчас нет полученных данных об ивентах. HW Helper показывает только то, что пришло с сервера. Статус: ",
                            "No event data received right now. HW Helper only shows what the server sent. Status: ") + str(r.get("status", "?")), ok=True)
        return Reply(_t(lang, "Данные HW Helper: ", "HW Helper data: ") + "; ".join(rows[:5]))

    def _cooldown(self, which: str, lang: str) -> Reply:
        try:
            r = self._call("query", what="cooldowns")
        except BridgeUnavailable:
            return Reply(_t(lang, *NOT_RUNNING), ok=False)
        rows = r.get("cooldowns", [])
        pick = [c for c in rows if not which or which.lower() in c["text"].lower()]
        if not pick:
            return Reply(_t(lang, "Активного КД не наблюдаю: его нет, или сервер не присылал время (задайте префиксы в HW Helper).",
                            "No active cooldown observed: none, or the server sent no timer (set the prefixes in HW Helper)."))
        return Reply(_t(lang, "Cooldowns HUD: ", "Cooldowns HUD: ") + "; ".join(c["text"] for c in pick))

    def _fps(self, lang: str) -> Reply:
        try:
            r = self._call("query", what="fps")
        except BridgeUnavailable:
            return Reply(_t(lang, *NOT_RUNNING), ok=False)
        fps, target = r.get("fps", 0), r.get("targetFps", 60)
        heavy = r.get("heavyEnabled", [])
        parts = [_t(lang, f"FPS сейчас {fps} (цель {target}), режим {r.get('preset')}.", f"FPS is {fps} (target {target}), mode {r.get('preset')}.")]
        if r.get("topCpu"):
            parts.append(_t(lang, "Самые дорогие по CPU: ", "Heaviest by CPU: ") + ", ".join(r["topCpu"]) + ".")
        elif not r.get("monitorOn"):
            parts.append(_t(lang, "Мониторинг модулей выключен — включите «Performance monitor» для точных цифр.", "Module monitor is off — enable “Performance monitor” for exact numbers."))
        if fps and fps < target:
            if r.get("preset") != "Performance":
                parts.append(_t(lang, "Совет: скажите «включи Performance Mode».", "Tip: say “enable Performance Mode”."))
            if heavy:
                parts.append(_t(lang, f"Включено эффектов: {len(heavy)} ({', '.join(heavy[:6])}).", f"Effects enabled: {len(heavy)} ({', '.join(heavy[:6])})."))
        elif fps:
            parts.append(_t(lang, "Просадки по модулям не видны; причина может быть вне мода (шейдеры, сервер, драйвер).", "No module-level drops visible; the cause may be outside the mod (shaders, server, driver)."))
        return Reply(" ".join(parts), details=r)

    def _ai(self, it: Intent) -> Reply:
        try:
            return Reply(self.ai(it.raw))
        except Exception as e:  # network/AI errors are reported, never fatal
            log.warning("AI mode failed: %s", e)
            return Reply(f"AI: {e}", ok=False)
