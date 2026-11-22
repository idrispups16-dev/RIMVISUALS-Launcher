"""Orchestration: diagnose, prepare (install), INJECT / PLAY, logs, updates."""
from __future__ import annotations

import os
import re
import subprocess
import sys
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable

from . import java_rt
from .accounts import MicrosoftAccount, SecretStore, SiteAccount, demo_account
from .core import (APP_VERSION, TARGET_FABRIC, TARGET_FABRIC_API, TARGET_JAVA, TARGET_MC, CancelToken, HttpError, Paths,
                   http_json, log, redact, setup_logging)
from .core import read_json
from .minecraft import Account, Endpoints, MinecraftInstaller, build_launch
from .modmgr import ModManager, find_bundled_jar, read_jar_meta
from .settings import CfgManager, Profiles, Settings

CREATE_NO_WINDOW = 0x08000000 if sys.platform == "win32" else 0
Progress = Callable[[str, float], None]


@dataclass
class Check:
    key: str
    ok: bool
    title: str
    detail: str = ""
    fixable: bool = False


@dataclass
class RunState:
    proc: subprocess.Popen | None = None
    profile: str = ""
    started: float = 0.0
    fabric_seen: bool = False
    mod_seen: bool = False
    exit_code: int | None = None
    tail: list[str] = field(default_factory=list)

    @property
    def running(self) -> bool:
        return self.proc is not None and self.proc.poll() is None


class ModOps:
    """What the assistant's mod manager may do, bound to one profile. Plans are shown to the user before apply()."""
    PVP_PACK = ["sodium", "lithium", "ferrite-core", "immediatelyfast"]

    def __init__(self, launcher: "Launcher"):
        self.L = launcher

    @property
    def mods_dir(self) -> Path:
        return self.L.profiles.game_dir(self.L.pid) / "mods"

    def plan(self, slugs: list[str]):
        from .jarvis import modtools
        total = modtools.Plan()
        for sl in slugs:
            p = modtools.plan_install(sl, self.mods_dir, self.L.mods.ep)
            for it in p.items:
                if all(x.filename != it.filename for x in total.items):
                    total.items.append(it)
            total.problems += p.problems
        return total

    def apply(self, plan) -> list[str]:
        from .jarvis import modtools
        return modtools.apply_plan(plan, self.mods_dir, self.L.paths)

    def conflicts(self) -> list[str]:
        from .jarvis import modtools
        return modtools.find_conflicts(self.mods_dir)

    def crash_text(self) -> str:
        gd = self.L.profiles.game_dir(self.L.pid)
        cands = sorted((gd / "crash-reports").glob("*.txt"), key=lambda p: p.stat().st_mtime) if (gd / "crash-reports").exists() else []
        cands += sorted(self.L.paths.logs.glob("game-*.log"), key=lambda p: p.stat().st_mtime)
        for f in reversed(cands[-3:]):
            try:
                return f.read_text(encoding="utf-8", errors="replace")[-200_000:]
            except OSError:
                continue
        return ""

    def analyze(self, text: str) -> list[str]:
        from .jarvis import modtools
        return modtools.analyze_crash(text)

    def updates(self) -> str:
        from .jarvis import modtools
        led = read_json(self.mods_dir / modtools.LEDGER, {"installed": {}})["installed"]
        if not led:
            return "Через менеджер модов ничего не установлено — проверять нечего."
        out = []
        for slug, fn in led.items():
            v = modtools._pick(modtools._versions(self.L.mods.ep, slug))
            if v:
                new = modtools._item(slug, v, "mod").filename
                out.append(f"{slug}: " + ("актуален" if new == fn else f"доступно {new}"))
        return "; ".join(out) or "обновлений нет"


class Launcher:
    def __init__(self, root: Path | None = None, ep: Endpoints | None = None):
        self.paths = Paths(root)
        self.paths.ensure()
        setup_logging(self.paths)
        self.settings = Settings(self.paths)
        self.profiles = Profiles(self.paths, self.settings["ram_mb"])
        self.profiles.jarvis_enabled = bool(self.settings["jarvis_bridge"])
        self.installer = MinecraftInstaller(self.paths, ep)
        self.mods = ModManager(self.paths, ep)
        self.cfg = CfgManager(self.paths, self.profiles)
        self.secrets = SecretStore(self.paths)
        self.site = SiteAccount(self.secrets)
        self.ms = MicrosoftAccount(self.secrets, self.settings["ms_client_id"])
        self.run = RunState()
        self.selected_jar: Path | None = None       # user-chosen JAR (overrides the bundled one)

    # ------------------------------------------------------------- state helpers
    @property
    def pid(self) -> str:
        p = self.settings["last_profile"]
        return p if p in self.profiles.items else next(iter(self.profiles.items))

    def select_profile(self, pid: str) -> None:
        if pid in self.profiles.items:
            self.settings.set("last_profile", pid)

    def reload_ms(self) -> None:
        self.ms.client_id = self.settings["ms_client_id"].strip()

    # ------------------------------------------------------------- diagnostics
    def official_minecraft_notes(self) -> str:
        """Read-only look at the regular .minecraft: reported, never modified."""
        base = Path(os.environ.get("APPDATA", "")) / ".minecraft" if sys.platform == "win32" else Path.home() / ".minecraft"
        if not base.exists():
            return "обычный .minecraft не найден (не требуется)"
        have = (base / "versions" / TARGET_MC).exists()
        return f"найден {base}; версия {TARGET_MC} {'есть' if have else 'нет'} — RIMVISUALS его не использует и не изменяет"

    def mod_jar_source(self):
        if self.selected_jar:
            return read_jar_meta(self.selected_jar)
        return find_bundled_jar()

    def diagnose(self) -> list[Check]:
        pr = self.profiles.items[self.pid]
        gd = self.profiles.game_dir(pr.id)
        out: list[Check] = []
        issues = self.installer.verify(False)
        out.append(Check("minecraft", not issues, f"Minecraft {TARGET_MC} + Fabric {TARGET_FABRIC}",
                         "установлено" if not issues else "; ".join(issues[:3]), True))
        java = java_rt.find_java(self.paths, pr.java_path or self.settings["java_path"])
        out.append(Check("java", bool(java and java.ok), f"Java {TARGET_JAVA} (x64)",
                         f"{java.path} (v{java.major}, {java.arch})" if java else "не найдена — будет скачана Temurin 21", True))
        try:
            src = self.mod_jar_source()
        except ValueError as e:
            src = None
            out.append(Check("jar_src", False, "JAR мода RIMVISUALS", str(e), False))
        else:
            out.append(Check("jar_src", src is not None, "JAR мода RIMVISUALS",
                             f"{src.path.name} v{src.version}" if src else "нет встроенного JAR — выберите файл вручную", False))
        st = self.mods.status(gd)
        out.append(Check("mod", not st["problems"], "Мод в профиле «%s»" % pr.name,
                         (f"v{st['installed']}" if st["installed"] else "") + (" · " + "; ".join(st["problems"]) if st["problems"] else " установлен, целостность OK"),
                         True))
        out.append(Check("account", True, "Microsoft-вход",
                         "настроен" if self.ms.configured else "Client ID не задан → PLAY запустит только Demo", False))
        out.append(Check("dot_minecraft", True, "Обычный .minecraft", self.official_minecraft_notes(), False))
        return out

    # ------------------------------------------------------------- prepare / install
    def prepare(self, progress: Progress | None = None, cancel: CancelToken | None = None) -> Path:
        """Make everything ready: Java, Minecraft+Fabric, Fabric API, mod JAR, mod bridge. Returns Java path."""
        p = progress or (lambda s, f: None)
        pr = self.profiles.items[self.pid]
        gd = self.profiles.game_dir(pr.id)
        p("Проверка Java", 0.02)
        java = java_rt.find_java(self.paths, pr.java_path or self.settings["java_path"])
        if not java or not java.ok:
            if pr.java_path or self.settings["java_path"]:
                raise RuntimeError(f"Выбранная Java не подходит (нужна {TARGET_JAVA} x64): {java.path if java else 'не запускается'}")
            p("Загрузка Java 21", 0.05)
            java = java_rt.install_temurin(self.paths, cancel, lambda a, b: p("Загрузка Java 21", 0.05 + 0.1 * (a / b if b else 0)))
        if not self.installer.is_installed():
            self.installer.install(lambda s, f: p(s, 0.15 + 0.6 * f), cancel)
        p("Fabric API", 0.78)
        if not self.mods.fabric_api_present(gd):
            self.mods.install_fabric_api(gd, cancel)
        p("Мод RIMVISUALS", 0.88)
        src = self.mod_jar_source()
        if src is None:
            raise RuntimeError("Нет JAR мода RIMVISUALS: выберите файл на странице «Визуалы»")
        if self.mods.verify(gd) or (self.mods.ledger(gd).get("rimvisuals", {}).get("sha256") != src.sha256):
            self.mods.install_mod(gd, src.path)
        self.profiles.write_bridge(pr.id)
        self.site.share_with_mod(gd)
        p("Готово", 1.0)
        return java.path

    # ------------------------------------------------------------- INJECT / PLAY
    def find_external_minecraft(self) -> list[str]:
        """Command lines of already running Minecraft clients that this launcher did not start (Windows only)."""
        if sys.platform != "win32":
            return []
        try:
            r = subprocess.run(["powershell", "-NoProfile", "-Command",
                                "Get-CimInstance Win32_Process -Filter \"name='javaw.exe' or name='java.exe'\" | "
                                "ForEach-Object { $_.ProcessId.ToString() + ' ' + $_.CommandLine }"],
                               capture_output=True, text=True, timeout=15, creationflags=CREATE_NO_WINDOW)
        except (OSError, subprocess.SubprocessError):
            return []
        mine = str(self.run.proc.pid) if self.run.running else None
        return [ln for ln in r.stdout.splitlines()
                if ("net.minecraft" in ln or "KnotClient" in ln) and not (mine and ln.startswith(mine + " "))]

    def inject(self, progress: Progress | None = None, cancel: CancelToken | None = None) -> dict[str, Any]:
        """INJECT = install/verify, then start Minecraft *with* Fabric + RIMVISUALS.

        Loading Fabric mods into an already running JVM would need Java instrumentation and is not supported:
        mods are only loaded at game start. If a foreign Minecraft is running we say so instead of pretending.
        """
        if self.run.running:
            return {"status": "already", "message": "Minecraft уже запущен этим лаунчером с RIMVISUALS."}
        foreign = self.find_external_minecraft()
        if foreign:
            return {"status": "needs_restart",
                    "message": "Запущен другой Minecraft. Fabric-моды загружаются только при старте игры, "
                               "подключить визуалы к работающей JVM нельзя. Закройте игру и нажмите INJECT снова."}
        return self.play(progress, cancel)

    def pick_account(self) -> tuple[Account, str]:
        ses = self.ms.refresh() if self.ms.configured else None
        if ses:
            return Account(ses.name, ses.uuid, ses.access_token, "msa", False), f"Microsoft: {ses.name}"
        return demo_account(), "Demo (без Microsoft-входа: игра запустится в демо-режиме без мультиплеера)"

    def play(self, progress: Progress | None = None, cancel: CancelToken | None = None) -> dict[str, Any]:
        p = progress or (lambda s, f: None)
        java = self.prepare(progress, cancel)
        pr = self.profiles.items[self.pid]
        gd = self.profiles.game_dir(pr.id)
        acct, label = self.pick_account()
        version = self.installer.load_version()
        spec = build_launch(self.paths, version, java, acct, gd, pr.ram_mb or self.settings["ram_mb"], self.settings["jvm_args"])
        if spec.classpath_missing:
            raise RuntimeError("Не хватает файлов: " + ", ".join(spec.classpath_missing[:5]) + ". Нажмите «Проверить».")
        log.info("Launching with %s", label)
        log.debug("cmd: %s", redact(spec.printable()))
        self.paths.logs.mkdir(parents=True, exist_ok=True)
        logf = self.paths.logs / f"game-{time.strftime('%Y%m%d-%H%M%S')}.log"
        self.run = RunState(profile=pr.id, started=time.time())
        proc = subprocess.Popen(spec.cmd, cwd=str(spec.cwd), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, encoding="utf-8", errors="replace", creationflags=CREATE_NO_WINDOW)
        self.run.proc = proc
        threading.Thread(target=self._pump, args=(proc, logf, self.run), daemon=True, name="game-log").start()
        p("Minecraft запускается", 1.0)
        return {"status": "started", "message": f"Minecraft запущен ({label}). Лог: {logf.name}", "pid": proc.pid, "log": str(logf)}

    def _pump(self, proc: subprocess.Popen, logf: Path, st: RunState) -> None:
        with open(logf, "w", encoding="utf-8") as f:
            for line in proc.stdout or []:
                line = redact(line.rstrip("\n"))
                f.write(line + "\n")
                st.tail.append(line)
                del st.tail[:-200]
                if "Loading Minecraft" in line and "Fabric Loader" in line:
                    st.fabric_seen = True
                if re.search(r"rimvisuals|RIMVISUALS", line) and ("Initializ" in line or "loaded" in line.lower() or "Applied" in line):
                    st.mod_seen = True
                if re.search(r"\b(ERROR|FATAL|Exception)\b", line):
                    log.warning("[game] %s", line[:300])
        st.exit_code = proc.wait()
        log.info("Minecraft exited with code %s", st.exit_code)

    def stop(self) -> None:
        if self.run.running:
            self.run.proc.terminate()

    def crash_hint(self) -> str:
        st = self.run
        if st.exit_code in (None, 0):
            return ""
        text = "\n".join(st.tail[-80:])
        if "UnsupportedClassVersionError" in text:
            return "Нужна Java 21 — выбранная Java слишком старая."
        if "OutOfMemoryError" in text:
            return "Не хватает памяти: увеличьте RAM в профиле."
        if "Mod resolution failed" in text or "requires" in text and "fabric" in text.lower():
            return "Fabric не смог разрешить зависимости мода (проверьте Fabric API и версию Minecraft)."
        return f"Игра завершилась с кодом {st.exit_code}. Подробности — на странице «Логи»."

    # ------------------------------------------------------------- updates / report
    def check_updates(self) -> dict[str, Any]:
        out: dict[str, Any] = {"launcher": APP_VERSION, "latest": None, "mod": self.mods.status(self.profiles.game_dir(self.pid))}
        try:
            rel = http_json("https://api.github.com/repos/idrispups16-dev/RIMVISUALS-Launcher/releases/latest",
                            headers={"Accept": "application/vnd.github+json"})
            out["latest"] = rel.get("tag_name")
            out["url"] = rel.get("html_url")
        except HttpError as e:
            out["error"] = "Опубликованных релизов нет или нет связи: " + str(e)
        return out

    # ------------------------------------------------------------- RIM JARVIS
    HELP_RU = ("Я умею: включать/выключать модули («включи Crystal Fade»), менять цвет («поставь фиолетовые частицы»), открывать меню "
               "(«открой ClickGUI»), сохранять конфиг («сохрани конфиг PvP»), режимы Performance/Balanced/Ultra, пресеты, "
               "отвечать про КД трапки, ивенты HW Helper и просадки FPS, искать и ставить моды («установи Sodium»), искать конфликты и разбирать crash-лог. "
               "Команды влияют на игру только когда Minecraft запущен через INJECT.")

    def jarvis(self):
        from .jarvis.ai import AiClient
        from .jarvis.assistant import Assistant
        from .jarvis.ipc import JarvisClient
        if getattr(self, "_jarvis", None) and self._jarvis_pid == self.pid:
            return self._jarvis
        ai = None
        if self.settings["jarvis_ai"]:
            cli = AiClient(self.settings["jarvis_ai_url"], self.settings["jarvis_ai_model"], lambda: self.secrets.get("jarvis_ai_key"))
            ai = cli.ask if cli.configured else None
        local = {"help": lambda: self.HELP_RU,
                 "status": lambda: "; ".join(f"{'OK' if c.ok else '!!'} {c.title}: {c.detail}" for c in self.diagnose()),
                 "updates": lambda: str(self.check_updates())}
        self._jarvis = Assistant(JarvisClient(self.profiles.game_dir(self.pid)), local, ai, ModOps(self))
        self._jarvis_pid = self.pid
        return self._jarvis

    def report(self) -> Path:
        """Diagnostics text for bug reports — no tokens, no secrets, no account names."""
        lines = [f"RIMVISUALS launcher {APP_VERSION}", f"Python {sys.version.split()[0]} on {sys.platform}",
                 f"Target: Minecraft {TARGET_MC}, Fabric {TARGET_FABRIC}, Fabric API {TARGET_FABRIC_API}, Java {TARGET_JAVA}", ""]
        for c in self.diagnose():
            lines.append(f"[{'OK' if c.ok else '!!'}] {c.title}: {redact(c.detail)}")
        lines += ["", "--- last launcher log lines ---"] + [redact(m) for _, _, m in list(__import__('rimvisuals_launcher.core', fromlist=['RING']).RING.lines)[-80:]]
        lines += ["", "--- last game output ---"] + [redact(x) for x in self.run.tail[-60:]]
        out = self.paths.logs / f"diagnostics-{time.strftime('%Y%m%d-%H%M%S')}.txt"
        out.write_text("\n".join(lines), encoding="utf-8")
        return out
