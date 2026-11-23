"""AI Mod Manager back end: legal source (Modrinth), compatibility + dependency checks, SHA-512 verification, backups,
review-before-install plans, conflict detection and crash-log analysis. Nothing is installed or removed without a plan the
user confirmed; nothing outside the profile's mods folder is touched; only .jar files from cdn.modrinth.com are accepted."""
from __future__ import annotations

import json
import re
import shutil
import time
import urllib.parse
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from ..core import TARGET_MC, CancelToken, HttpError, Paths, download, http_json, is_safe_url, log, read_json, safe_join, write_json
from ..minecraft import Endpoints

LEDGER = ".rimvisuals-modmgr.json"
ALLOWED_HOSTS = ("https://cdn.modrinth.com/", "http://127.0.0.1", "http://localhost")   # loopback only for tests


@dataclass
class PlanItem:
    slug: str
    title: str
    version: str
    filename: str
    url: str
    sha512: str
    size: int
    role: str = "mod"                 # mod | dependency
    action: str = "install"           # install | update | keep


@dataclass
class Plan:
    items: list[PlanItem] = field(default_factory=list)
    problems: list[str] = field(default_factory=list)

    def describe(self) -> str:
        lines = [f"{'+' if i.action != 'keep' else '='} {i.title} {i.version} ({i.filename}, {i.size // 1024} KB){' [зависимость]' if i.role == 'dependency' else ''}" for i in self.items]
        return "\n".join(lines + [f"! {p}" for p in self.problems])


def search(query: str, ep: Endpoints | None = None, limit: int = 5) -> list[dict[str, Any]]:
    ep = ep or Endpoints()
    facets = json.dumps([["project_type:mod"], ["categories:fabric"], [f"versions:{TARGET_MC}"]])
    q = urllib.parse.urlencode({"query": query[:60], "facets": facets, "limit": limit})
    data = http_json(f"{ep.modrinth}/v2/search?{q}")
    return [{"slug": h["slug"], "title": h["title"], "description": h.get("description", ""), "downloads": h.get("downloads", 0)}
            for h in data.get("hits", [])]


def _versions(ep: Endpoints, project: str) -> list[dict[str, Any]]:
    q = urllib.parse.urlencode({"game_versions": json.dumps([TARGET_MC]), "loaders": json.dumps(["fabric"])})
    return http_json(f"{ep.modrinth}/v2/project/{urllib.parse.quote(project, safe='')}/version?{q}")


def _pick(versions: list[dict[str, Any]]) -> dict[str, Any] | None:
    rel = [v for v in versions if v.get("version_type") == "release"] or versions
    return rel[0] if rel else None


def _item(slug: str, v: dict[str, Any], role: str) -> PlanItem:
    files = v.get("files") or []
    f = next((x for x in files if x.get("primary")), files[0] if files else None)
    if not f or not str(f.get("filename", "")).endswith(".jar"):
        raise ValueError(f"{slug}: у версии нет .jar файла")
    if not f.get("hashes", {}).get("sha512"):
        raise ValueError(f"{slug}: источник не дал SHA-512 — не устанавливаю без проверки")
    if not str(f["url"]).startswith(ALLOWED_HOSTS):
        raise ValueError(f"{slug}: файл не с cdn.modrinth.com")
    name = Path(f["filename"]).name
    return PlanItem(slug, v.get("name") or slug, v.get("version_number", "?"), name, f["url"], f["hashes"]["sha512"], int(f.get("size") or 0), role)


def plan_install(slug: str, mods_dir: Path, ep: Endpoints | None = None) -> Plan:
    ep = ep or Endpoints()
    plan = Plan()
    seen: set[str] = set()

    def add(s: str, role: str, depth: int) -> None:
        if s in seen or depth > 2:
            return
        seen.add(s)
        v = _pick(_versions(ep, s))
        if not v:
            plan.problems.append(f"{s}: нет версии для Minecraft {TARGET_MC} + Fabric")
            return
        it = _item(s, v, role)
        have = mods_dir / it.filename
        it.action = "keep" if have.exists() else "install"
        plan.items.append(it)
        for d in v.get("dependencies", []):
            if d.get("dependency_type") == "required" and d.get("project_id"):
                add(d["project_id"], "dependency", depth + 1)
            if d.get("dependency_type") == "incompatible" and d.get("project_id"):
                plan.problems.append(f"несовместим с проектом {d['project_id']} — проверьте установленные моды")

    add(slug, "mod", 0)
    return plan


def apply_plan(plan: Plan, mods_dir: Path, paths: Paths, cancel: CancelToken | None = None) -> list[str]:
    """Download + SHA-512 verify. Existing different files with the same slug are moved to backups/, never deleted."""
    mods_dir.mkdir(parents=True, exist_ok=True)
    ledger = read_json(mods_dir / LEDGER, {"installed": {}})
    done: list[str] = []
    for it in plan.items:
        if it.action == "keep":
            continue
        dest = safe_join(mods_dir, it.filename)
        old = ledger["installed"].get(it.slug)
        if old and old != it.filename and (mods_dir / old).exists():
            bak = paths.backups / "mods" / time.strftime("%Y%m%d-%H%M%S")
            bak.mkdir(parents=True, exist_ok=True)
            shutil.move(str(mods_dir / old), str(bak / old))
        download(it.url, dest, sha512=it.sha512, size=it.size or None, cancel=cancel)
        ledger["installed"][it.slug] = it.filename
        done.append(it.filename)
    write_json(mods_dir / LEDGER, ledger)
    log.info("Installed mods: %s", ", ".join(done) or "none")
    return done


def remove_mod(filename: str, mods_dir: Path, paths: Paths) -> Path:
    """'Delete' = move to backups/. The caller must have asked the user first."""
    src = safe_join(mods_dir, filename)
    if not src.is_file() or src.suffix != ".jar":
        raise ValueError("нет такого .jar")
    bak = paths.backups / "mods" / time.strftime("%Y%m%d-%H%M%S")
    bak.mkdir(parents=True, exist_ok=True)
    dest = bak / src.name
    shutil.move(str(src), str(dest))
    return dest


# ------------------------------------------------------------------ analysis
def read_mods(mods_dir: Path) -> list[dict[str, Any]]:
    out = []
    for jar in sorted(mods_dir.glob("*.jar")) if mods_dir.exists() else []:
        try:
            with zipfile.ZipFile(jar) as z:
                m = json.loads(z.read("fabric.mod.json").decode("utf-8"))
            out.append({"file": jar.name, "id": str(m.get("id", "")), "version": str(m.get("version", "?")), "depends": m.get("depends") or {},
                        "breaks": m.get("breaks") or {}, "conflicts": m.get("conflicts") or {}})
        except (OSError, KeyError, ValueError, zipfile.BadZipFile):
            out.append({"file": jar.name, "id": "", "version": "?", "depends": {}, "breaks": {}, "conflicts": {}, "broken": True})
    return out


BUILTIN = {"minecraft", "java", "fabricloader", "fabric", "fabric-api"}


def find_conflicts(mods_dir: Path) -> list[str]:
    mods = read_mods(mods_dir)
    ids: dict[str, list[str]] = {}
    for m in mods:
        if m.get("broken"):
            continue
        ids.setdefault(m["id"], []).append(m["file"])
    problems = [f"{m['file']}: не читается fabric.mod.json (повреждён или не Fabric-мод)" for m in mods if m.get("broken")]
    for i, files in ids.items():
        if len(files) > 1:
            problems.append(f"дубликат мода «{i}»: {', '.join(files)}")
    have = set(ids)
    if "fabric-api" in have:
        have.add("fabric")
    for m in mods:
        for dep in m["depends"]:
            if dep not in have and dep not in BUILTIN:
                problems.append(f"{m['file']} требует «{dep}», его нет в mods")
        for dep in list(m["breaks"]) + list(m["conflicts"]):
            if dep in have:
                problems.append(f"{m['file']} несовместим с «{dep}»")
        mc = str(m["depends"].get("minecraft", ""))
        if mc and not re.search(r"1\.21(\.11)?|\*|>=1\.21", mc):
            problems.append(f"{m['file']} рассчитан на Minecraft {mc}, а не {TARGET_MC}")
    return problems


CRASH_RULES: list[tuple[str, str]] = [
    (r"requires (?:any )?version .* of (?:mod )?(\S+)|Mod '([^']+)'.*requires", "Не хватает или не подходит зависимость: {0}. Установите нужную версию (Fabric API и др.)."),
    (r"OutOfMemoryError|GC overhead limit", "Не хватает памяти (OutOfMemoryError). Увеличьте RAM профиля в «Конфигурации» (6–8 ГБ для 1.21)."),
    (r"UnsupportedClassVersionError|class file version 6[0-9]", "Неверная версия Java: для 1.21.11 нужна Java 21."),
    (r"Mixin apply failed|MixinApplyError|InvalidInjectionException", "Конфликт Mixin: мод несовместим с версией игры или с другим модом. Уберите недавно добавленные моды и проверьте обновления."),
    (r"Duplicate mod|Duplicate ID", "Два JAR одного мода в mods. Оставьте один."),
    (r"Incompatible mod set|Unmet dependency", "Неподходящий набор модов: загрузчик перечислил непройденные зависимости выше по логу."),
    (r"NoSuchMethodError|NoSuchFieldError|ClassNotFoundException", "Мод собран под другую версию игры или другого мода (отсутствует класс/метод). Проверьте версии."),
    (r"GLFW error|OpenGL|GL_INVALID|Failed to create window", "Проблема с графикой/драйвером или шейдер-модом. Обновите драйвер видеокарты и отключите шейдеры."),
    (r"Could not (?:read|load) (?:launcher|config)", "Файл настроек повреждён — восстановите резервную копию CFG во вкладке «Конфигурации»."),
]


def analyze_crash(text: str) -> list[str]:
    from ..core import redact
    text = redact(text)[-200_000:]
    out: list[str] = []
    for pattern, msg in CRASH_RULES:
        m = re.search(pattern, text, re.I)
        if m:
            g = next((x for x in m.groups() if x), "?") if m.groups() else "?"
            out.append(msg.replace("{0}", g))
    first = re.search(r"^(?:Caused by: |Exception in thread .*?: )?([\w.$]+(?:Exception|Error)[^\n]{0,160})", text, re.M)
    if first and not out:
        out.append(f"Первая ошибка в логе: {first.group(1)}")
    return out or ["Известных причин не найдено. Сохраните диагностику (Логи) и посмотрите первую строку Caused by."]
