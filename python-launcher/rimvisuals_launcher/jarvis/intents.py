"""Offline, deterministic command understanding for RIM JARVIS (Russian + English). No network, no ML model."""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

WAKE = ("джарвис", "жарвис", "джарвиз", "jarvis", "rim jarvis", "рим джарвис")

# module id -> alias stems (already normalised: lower-case, ё→е). Longest matching alias wins.
ALIASES: dict[str, tuple[str, ...]] = {
    "crystal_fade": ("crystal fade", "кристалл фейд", "фейд кристалл", "затухание кристалл", "прозрачность кристалл"),
    "crystal_scale": ("crystal scale", "размер кристалл", "кристалл скейл", "масштаб кристалл"),
    "anchor_fade": ("anchor fade", "anchor", "якор", "анкор"),
    "totem_fade": ("totem fade", "тотем фейд", "фейд тотем", "затухание тотем"),
    "totem_scale": ("totem scale", "размер тотем", "тотем скейл", "масштаб тотем"),
    "hit_particles": ("hit particles", "частицы удар", "частицы попадан", "хит частиц"),
    "crit_particles": ("critical particles", "crit particles", "крит частиц", "критическ"),
    "damage_particles": ("damage particles", "частицы урон"),
    "sharpness_particles": ("sharpness particles", "острот"),
    "kill_effects": ("kill effects", "эффекты убийств", "килл эффект"),
    "hit_marker": ("hit marker", "хитмаркер", "хит маркер"),
    "hit_color": ("hit color", "цвет удар"),
    "damage_tint": ("damage tint", "оттенок урон", "красный экран"),
    "target_hud": ("target hud", "таргет худ", "худ цел"),
    "target_highlight": ("target highlight", "подсветк цел", "подсветка цели"),
    "visual_hitbox": ("visual hitbox", "хитбокс"),
    "sword_trails": ("sword trails", "следы меч", "трейл меч"),
    "pearl_trails": ("pearl trails", "следы жемчуг", "трейл жемчуг", "трейл перл"),
    "projectile_trails": ("projectile trails", "следы снаряд"),
    "swing": ("swing animations", "swing", "свинг", "анимаци удар", "анимаци взмах"),
    "viewmodel": ("viewmodel", "вьюмодел", "вьюмодель", "положение рук"),
    "hand_glow": ("hand glow", "свечение рук"),
    "hand_color": ("hand color", "цвет рук"),
    "hand_lighting": ("hand lighting", "освещение рук"),
    "custom_fog": ("custom fog", "туман"),
    "custom_sky": ("custom sky", "небо"),
    "atmosphere": ("atmosphere", "атмосфер"),
    "china_hat": ("china hat", "китайск шляп", "чайна хет", "шляп"),
    "block_overlay": ("block overlay", "оверлей блок", "подсветка блок"),
    "crosshair": ("crosshair", "прицел", "кроссхейр"),
    "info_hud": ("info hud", "инфо худ", "информационн"),
    "armor_hud": ("armor hud", "худ брон", "броня худ"),
    "potion_hud": ("potion hud", "худ зелий", "зелья худ"),
    "cps_hud": ("cps", "сps", "цпс"),
    "keystrokes_hud": ("keystrokes", "кейстрокс", "нажатия клавиш"),
    "cooldowns_hud": ("cooldowns hud", "кулдауны худ"),
    "performance_hud": ("performance monitor", "монитор производительности"),
    "watermark": ("watermark", "водяной знак", "вотермарк"),
    "pulse_glow": ("pulse glow", "пульс"),
    "particles": ("particle effects", "particles", "частицы экран", "оверлей частиц"),
    "fullbright": ("fullbright", "фулбрайт", "полная яркость"),
    "zoom": ("zoom", "зум"),
    "hurt_camera": ("hurt camera", "камера урон"),
    "water_color": ("water color", "цвет воды"),
    "lava_color": ("lava color", "цвет лавы"),
    "weather_color": ("weather color", "цвет погоды"),
    "explosion_effects": ("explosion effects", "взрыв"),
    "golden_apple_glow": ("golden apple glow", "золотое яблоко"),
    "item_physics": ("item physics", "физика предмет"),
    "hw_helper": ("hw helper", "холи хелпер", "холиворлд хелпер", "hw помощник"),
    "ft_helper": ("ft helper", "фантайм хелпер", "ft помощник"),
}
PARTICLE_GROUP = ("hit_particles", "crit_particles", "damage_particles", "sharpness_particles")

COLORS: dict[str, str] = {
    "фиолет": "#8B5CF6", "violet": "#8B5CF6", "purple": "#A855F7", "пурпур": "#A855F7", "сирен": "#C084FC",
    "красн": "#EF4444", "red": "#EF4444", "син": "#3B82F6", "blue": "#3B82F6", "голуб": "#7DD3FC", "cyan": "#22D3EE", "циан": "#22D3EE",
    "зелен": "#22C55E", "green": "#22C55E", "желт": "#FACC15", "yellow": "#FACC15", "оранж": "#F97316", "orange": "#F97316",
    "розов": "#EC4899", "pink": "#EC4899", "бел": "#FFFFFF", "white": "#FFFFFF", "черн": "#111111", "black": "#111111",
}
PERF_MODES = {"performance": "Performance", "производительн": "Performance", "balanced": "Balanced", "баланс": "Balanced",
              "ultra": "Ultra", "ультра": "Ultra"}
PRESETS = {"pvp clean": "PvP_Clean", "pvp_clean": "PvP_Clean", "чистый пвп": "PvP_Clean", "minimal": "Minimal", "минимал": "Minimal",
           "crystal pvp": "Crystal_PvP", "кристалл пвп": "Crystal_PvP", "sword pvp": "Sword_PvP", "меч пвп": "Sword_PvP"}

ON = ("включи", "включить", "включай", "активируй", "enable", "turn on", "switch on", "enable the")
OFF = ("выключи", "выключить", "отключи", "отключить", "выруби", "disable", "turn off", "switch off")
OPEN = ("открой", "открыть", "покажи меню", "show", "open")
SAVE = ("сохрани", "сохранить", "save")
LOAD = ("загрузи", "загрузить", "load", "применить конфиг", "примени конфиг")
CONFIG_WORDS = ("конфиг", "cfg", "config", "configuration")


@dataclass
class Intent:
    kind: str                       # game | local | unknown
    name: str                       # enable | disable | color | open_gui | save_config | load_config | performance | preset | events | cooldown | fps | help | updates | status
    params: dict[str, Any] = field(default_factory=dict)
    lang: str = "ru"
    raw: str = ""


def norm(text: str) -> str:
    t = text.lower().replace("ё", "е")
    t = re.sub(r"[^\w\s#_\-]", " ", t, flags=re.UNICODE)
    return re.sub(r"\s+", " ", t).strip()


def strip_wake(t: str) -> str:
    for w in sorted(WAKE, key=len, reverse=True):
        if t.startswith(w):
            return t[len(w):].strip(" ,")
    return t


def has(t: str, words: tuple[str, ...]) -> str | None:
    for w in sorted(words, key=len, reverse=True):
        if re.search(r"(?<!\w)" + re.escape(w), t):
            return w
    return None


def find_module(t: str) -> str | None:
    best, best_len = None, 0
    for mid, aliases in ALIASES.items():
        for a in aliases:
            a = norm(a)
            if a and re.search(r"(?<!\w)" + re.escape(a), t) and len(a) > best_len:
                best, best_len = mid, len(a)
    return best


def find_color(t: str) -> str | None:
    m = re.search(r"#([0-9a-f]{6})\b", t)
    if m:
        return "#" + m.group(1).upper()
    for stem, hexv in COLORS.items():
        if re.search(r"(?<!\w)" + re.escape(stem), t):
            return hexv
    return None


def parse(text: str) -> Intent:
    lang = "ru" if re.search(r"[а-яё]", text.lower()) else "en"
    t = strip_wake(norm(text))
    I = lambda kind, name, **p: Intent(kind, name, p, lang, text)  # noqa: E731

    if not t:
        return I("unknown", "empty")
    if has(t, ("помощь", "что ты умеешь", "что умеешь", "help", "what can you do")):
        return I("local", "help")
    if has(t, ("обновлен", "update")) and has(t, ("мод", "mods")) and has(t, ("проверь", "check", "есть")):
        return I("mods", "mod_updates")
    if has(t, ("проверь обновлен", "есть обновлен", "check for updates", "check updates")):
        return I("local", "updates")
    if has(t, ("конфликт", "conflict", "несовместим")) and has(t, ("мод", "mods")):
        return I("mods", "conflicts")
    if has(t, ("crash", "краш", "вылет", "лог")) and has(t, ("объясни", "разбери", "проанализируй", "почему", "explain", "analyze", "why")):
        return I("mods", "crash")
    if has(t, ("сборк", "modpack", "pack")) and has(t, ("создай", "собери", "сделай", "build", "create", "make")) and has(t, ("pvp", "пвп")):
        return I("mods", "pvp_pack")
    m = re.search(r"(?:установи|установить|скачай|install|download)\s+(?:мод\s+|mod\s+)?([a-z0-9][a-z0-9_\-]{1,40})", t)
    if m and not has(t, CONFIG_WORDS):
        return I("mods", "install_mod", slug=m.group(1))
    if has(t, ("диагностик", "статус", "состояние", "diagnostics", "status", "health")) and not has(t, ("фпс", "fps")):
        return I("local", "status")

    # questions about the live game
    if has(t, ("фпс", "fps", "кадр")) and has(t, ("упал", "упад", "просел", "низк", "лаг", "тормоз", "почему", "why", "drop", "low", "slow", "stutter")):
        return I("game", "fps")
    if has(t, ("кд", "кулдаун", "cooldown", "осталось", "left")) and has(t, ("трапк", "trap", "пласт", "plast", "cooldown", "кулдаун", "кд")):
        which = "Plast" if has(t, ("пласт", "plast")) else "Trapka" if has(t, ("трапк", "trap")) else ""
        return I("game", "cooldown", which=which)
    if has(t, ("ивент", "event", "события")):
        return I("game", "events")

    # configs
    if has(t, CONFIG_WORDS):
        name = _after_config_word(text)
        if has(t, SAVE):
            return I("game", "save_config", config=name)
        if has(t, LOAD):
            return I("game", "load_config", config=name)
    # presets & performance
    for key, preset in PRESETS.items():
        if key in t and has(t, ("пресет", "preset", "примени", "apply", "включи", "enable")):
            return I("game", "preset", preset=preset)
    if has(t, ("performance mode", "режим производительности", "режим perf", "balanced mode", "баланс", "ultra mode", "ультра режим", "режим ultra")) \
            or (has(t, ON + ("режим", "mode")) and has(t, tuple(PERF_MODES)) and not find_module(t)):
        for k, v in PERF_MODES.items():
            if k in t:
                return I("game", "performance", mode=v)
    # open
    if has(t, OPEN):
        if has(t, ("hud editor", "редактор худ", "редактор hud", "редактор интерфейса")):
            return I("game", "open_gui", target="hud_editor")
        if has(t, ("clickgui", "click gui", "клик гуи", "кликгуи", "меню", "menu", "настройки")):
            return I("game", "open_gui", target="clickgui")
    # colour
    col = find_color(t)
    if col and has(t, ("поставь", "сделай", "измени", "поменяй", "цвет", "color", "colour", "set", "make", "change")):
        if has(t, ("частиц", "particle")) and not find_module(t):
            return I("game", "color", modules=list(PARTICLE_GROUP), color=col)
        mid = find_module(t)
        if mid:
            return I("game", "color", modules=[mid], color=col)
    # on/off
    mid = find_module(t)
    if mid:
        if has(t, OFF):
            return I("game", "disable", module=mid)
        if has(t, ON):
            return I("game", "enable", module=mid)
    return I("unknown", "unknown")


def _after_config_word(text: str) -> str:
    """Config name with the user's own capitalisation (PvP stays PvP)."""
    t = re.sub(r"[^\w\s\-]", " ", text, flags=re.UNICODE)
    t = re.sub(r"\s+", " ", t).strip()
    for w in sorted(WAKE, key=len, reverse=True):
        t = re.sub(r"^" + re.escape(w) + r"\s*", "", t, flags=re.I)
    m = re.search(r"(?:конфиг\w*|cfg|config\w*|configuration)\s+(.+)$", t, re.I)
    name = m.group(1) if m else ""
    if not name:
        m = re.search(r"(?:сохрани\w*|save|загрузи\w*|load)\s+(.+?)\s+(?:конфиг\w*|cfg|config\w*)", t, re.I)
        name = m.group(1) if m else ""
    name = re.sub(r"\b(пожалуйста|please|named|под названием|с названием)\b", "", name, flags=re.I).strip()
    return re.sub(r"\s+", "_", name)[:40]
