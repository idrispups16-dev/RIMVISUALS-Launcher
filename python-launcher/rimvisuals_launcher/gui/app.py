"""RIMVISUALS launcher window (tkinter). All real work lives in rimvisuals_launcher.service."""
from __future__ import annotations

import os
import queue
import subprocess
import sys
import threading
import time
import tkinter as tk
import webbrowser
from pathlib import Path
from tkinter import filedialog, messagebox

from ..core import APP_VERSION, RING, TARGET_FABRIC, TARGET_JAVA, TARGET_MC, CancelToken, log
from ..service import Launcher
from ..settings import DEFAULT_VISUALS
from . import widgets as W
from .widgets import (BAD, BG, VIOLET_D, LINE, MUTED, OK, PANEL, PANEL2, TEXT, VIOLET, VIOLET_L, WARN, Anim, Card, Logo,
                      PlayButton, ProgressBar, RButton, Slider, Toggle, entry, font, label, mix)

PAGES = [("home", "Главная", "Home"), ("jarvis", "RIM JARVIS", "RIM JARVIS"), ("visuals", "Визуалы", "Visuals"), ("configs", "Конфигурации", "Configs"),
         ("account", "Аккаунт", "Account"), ("settings", "Настройки", "Settings"),
         ("updates", "Обновления", "Updates"), ("logs", "Логи", "Logs")]


class App(tk.Tk):
    def __init__(self, home: str | None = None):
        super().__init__()
        self.L = Launcher(Path(home) if home else None)
        self.lang = self.L.settings["language"]
        self.title("RIMVISUALS")
        self.configure(bg=BG)
        self.geometry("980x640")
        self.minsize(940, 600)
        self.q: queue.Queue = queue.Queue()
        self.cancel: CancelToken | None = None
        self.busy = False
        self.page_name = "home"
        self.pages: dict[str, tk.Frame] = {}
        self.nav: dict[str, tuple[tk.Canvas, Anim]] = {}
        self._dpi()
        self.build()
        self.after(60, self._pump)
        self.after(400, self.refresh_status)
        self.protocol("WM_DELETE_WINDOW", self.on_close)

    # ---------------------------------------------------------------- infra
    def _dpi(self) -> None:
        if sys.platform == "win32":
            try:
                import ctypes
                ctypes.windll.shcore.SetProcessDpiAwareness(1)
            except Exception:
                pass

    def t(self, ru: str, en: str) -> str:
        return ru if self.lang == "ru" else en

    def run_bg(self, fn, done=None) -> None:
        def work():
            try:
                res, err = fn(), None
            except Exception as e:  # shown to the user, never swallowed silently
                res, err = None, e
                log.exception("task failed") if not isinstance(e, (RuntimeError, ValueError)) else log.error("%s", e)
            self.q.put((done, res, err))
        threading.Thread(target=work, daemon=True).start()

    def _pump(self) -> None:
        try:
            while True:
                cb, res, err = self.q.get_nowait()
                if cb:
                    cb(res, err)
        except queue.Empty:
            pass
        if self.L.run.exit_code is not None and getattr(self, "_seen_exit", None) != self.L.run.started:
            self._seen_exit = self.L.run.started
            hint = self.L.crash_hint()
            self.set_status(hint or self.t("Minecraft закрыт.", "Minecraft closed."), BAD if hint else MUTED)
            self.refresh_status()
        self.after(60, self._pump)

    def on_close(self) -> None:
        if self.L.run.running and not messagebox.askyesno("RIMVISUALS", self.t("Minecraft ещё запущен. Закрыть только лаунчер?", "Minecraft is still running. Close only the launcher?")):
            return
        self.destroy()

    # ---------------------------------------------------------------- layout
    def build(self) -> None:
        for w in self.winfo_children():
            w.destroy()
        self.pages.clear()
        self.nav.clear()
        side = tk.Frame(self, bg=PANEL, width=210)
        side.pack(side="left", fill="y")
        side.pack_propagate(False)
        Logo(side, bg=PANEL).pack(anchor="w", padx=18, pady=(22, 20))
        for key, ru, en in PAGES:
            c = tk.Canvas(side, width=210, height=44, bg=PANEL, highlightthickness=0, cursor="hand2")
            c.pack(fill="x")
            a = Anim(c, lambda v, c=c, k=key, ru=ru, en=en: self._draw_nav(c, k, ru, en, v), 0.18)
            self.nav[key] = (c, a)
            c.bind("<Button-1>", lambda e, k=key: self.show(k))
            c.bind("<Enter>", lambda e, k=key: self._nav_hover(k, True))
            c.bind("<Leave>", lambda e, k=key: self._nav_hover(k, False))
            self._draw_nav(c, key, ru, en, 0)
        tk.Label(side, text=f"v{APP_VERSION}\nMC {TARGET_MC} · Fabric {TARGET_FABRIC}", bg=PANEL, fg=MUTED,
                 font=font(8), justify="left", anchor="w").pack(side="bottom", fill="x", padx=18, pady=14)
        self.content = tk.Frame(self, bg=BG)
        self.content.pack(side="left", fill="both", expand=True)
        for key in ("home", "jarvis", "visuals", "configs", "account", "settings", "updates", "logs"):
            f = tk.Frame(self.content, bg=BG)
            self.pages[key] = f
            getattr(self, "page_" + key)(f)
        self.show(self.page_name, animate=False)

    def _draw_nav(self, c: tk.Canvas, key: str, ru: str, en: str, v: float) -> None:
        c.delete("all")
        active = key == self.page_name
        sel = 1.0 if active else 0.0
        if sel or v:
            W.round_rect(c, 10, 4, 200, 40, 10, fill=mix(PANEL, "#2A1B45", max(sel, v * 0.6)), outline="")
        if sel:
            c.create_rectangle(10, 12, 13, 32, fill=VIOLET, outline="")
        c.create_text(32, 22, text=self.t(ru, en), anchor="w", fill=TEXT if (active or v > .3) else MUTED, font=font(10, active))

    def _nav_hover(self, key: str, on: bool) -> None:
        self.nav[key][1].go(1.0 if on else 0.0)

    def show(self, name: str, animate: bool = True) -> None:
        old = self.pages.get(self.page_name)
        self.page_name = name
        for key, (c, a) in self.nav.items():
            self._draw_nav(c, key, *next((r, e) for k, r, e in PAGES if k == key), a.v)
        for k, f in self.pages.items():
            if k != name:
                f.place_forget()
        f = self.pages[name]
        f.place(x=0, y=0, relwidth=1, relheight=1)
        f.lift()
        if name == "logs":
            self.reload_logs()
        if name in ("visuals", "configs", "updates"):
            getattr(self, "refresh_" + name, lambda: None)()
        if animate:
            Anim(f, lambda v, f=f: f.place_configure(x=int(28 * (1 - v))), 0.22).go(1.0)

    def set_status(self, text: str, color: str = MUTED) -> None:
        if hasattr(self, "status_lbl"):
            self.status_lbl.configure(text=text, fg=color)

    def header(self, f: tk.Frame, title: str, sub: str = "") -> None:
        label(f, title, size=20, bold=True).pack(anchor="w", padx=32, pady=(26, 0))
        if sub:
            label(f, sub, fg=MUTED, size=10, wrap=720).pack(anchor="w", padx=32, pady=(2, 12))

    # ---------------------------------------------------------------- home
    def page_home(self, f: tk.Frame) -> None:
        self.header(f, self.t("Главная", "Home"),
                    self.t("INJECT устанавливает и проверяет Fabric и мод, затем запускает Minecraft с RIMVISUALS. "
                           "Подключить мод к уже запущенной игре нельзя: Fabric загружает моды только при старте.",
                           "INJECT installs and verifies Fabric and the mod, then starts Minecraft with RIMVISUALS. "
                           "A mod cannot be attached to an already running game: Fabric loads mods at startup only."))
        row = tk.Frame(f, bg=BG)
        row.pack(fill="x", padx=32)
        left = tk.Frame(row, bg=BG)
        left.pack(side="left", padx=(0, 28))
        self.play = PlayButton(left, self.on_inject)
        self.play.pack()
        self.prog = ProgressBar(left, width=230)
        self.prog.pack(pady=(10, 4))
        self.status_lbl = label(left, self.t("Готов к запуску", "Ready"), fg=MUTED, size=9, wrap=230)
        self.status_lbl.pack(anchor="w")
        right = tk.Frame(row, bg=BG)
        right.pack(side="left", fill="both", expand=True)
        pr = tk.Frame(right, bg=BG)
        pr.pack(fill="x")
        label(pr, self.t("Профиль", "Profile"), fg=MUTED, size=9).pack(side="left")
        self.profile_var = tk.StringVar()
        self.profile_menu = tk.OptionMenu(pr, self.profile_var, "")
        self.profile_menu.configure(bg=PANEL2, fg=TEXT, activebackground=PANEL2, activeforeground=TEXT, relief="flat",
                                    highlightthickness=1, highlightbackground=LINE, font=font(10))
        self.profile_menu["menu"].configure(bg=PANEL2, fg=TEXT, font=font(10))
        self.profile_menu.pack(side="left", padx=10)
        self.fill_profile_menu()
        card = Card(right, self.t("Состояние", "Status"))
        card.pack(fill="x", pady=(10, 0))
        self.checks_box = tk.Frame(card, bg=PANEL)
        self.checks_box.pack(fill="x", padx=16, pady=(0, 12))
        btns = tk.Frame(right, bg=BG)
        btns.pack(fill="x", pady=12)
        RButton(btns, self.t("Установить", "Install"), self.on_install, width=160).pack(side="left", padx=(0, 10))
        self.btn_stop = RButton(btns, self.t("Остановить игру", "Stop game"), self.on_stop, width=130)
        self.btn_stop.pack(side="left")
        RButton(btns, self.t("Проверка SHA", "SHA check"), self.on_full_verify, width=150).pack(side="left", padx=10)

    def fill_profile_menu(self) -> None:
        m = self.profile_menu["menu"]
        m.delete(0, "end")
        for pid, p in self.L.profiles.items.items():
            m.add_command(label=p.name, command=lambda pid=pid: self.pick_profile(pid))
        self.profile_var.set(self.L.profiles.items[self.L.pid].name)

    def pick_profile(self, pid: str) -> None:
        self.L.select_profile(pid)
        self.profile_var.set(self.L.profiles.items[pid].name)
        self.refresh_status()

    def refresh_status(self) -> None:
        def work():
            return self.L.diagnose()

        def done(res, err):
            for w in self.checks_box.winfo_children():
                w.destroy()
            if err:
                label(self.checks_box, str(err), fg=BAD, size=9, wrap=520).pack(anchor="w")
                return
            for c in res:
                r = tk.Frame(self.checks_box, bg=PANEL)
                r.pack(fill="x", pady=2)
                tk.Canvas(r, width=12, height=12, bg=PANEL, highlightthickness=0).pack(side="left")
                dot = r.winfo_children()[0]
                dot.create_oval(1, 1, 11, 11, fill=OK if c.ok else WARN, outline="")
                label(r, c.title, size=9, bold=True, bg=PANEL).pack(side="left", padx=(8, 8))
                label(r, c.detail, fg=MUTED, size=9, bg=PANEL, wrap=420).pack(side="left", fill="x")
        self.run_bg(work, done)

    # actions
    def _progress(self, s: str, f: float) -> None:
        self.q.put((lambda *_: (self.set_status(s, VIOLET_L), self.prog.set(f)), None, None))

    def _set_busy(self, on: bool) -> None:
        self.busy = on
        self.play.busy = on
        self.play.enabled = not on

    def on_inject(self) -> None:
        if self.busy:
            return
        self._set_busy(True)
        self.cancel = CancelToken()
        self.prog.set(0.02)
        self.set_status(self.t("Подготовка…", "Preparing…"), VIOLET_L)

        def done(res, err):
            self._set_busy(False)
            self.prog.set(0)
            if err:
                self.set_status(str(err), BAD)
                messagebox.showerror("RIMVISUALS", str(err))
            else:
                self.set_status(res["message"], OK if res["status"] in ("started", "already") else WARN)
                if res["status"] == "needs_restart":
                    messagebox.showinfo("RIMVISUALS", res["message"])
                elif res["status"] == "started" and self.L.settings["close_launcher_on_start"]:
                    self.after(2500, self.destroy)
            self.refresh_status()
        self.run_bg(lambda: self.L.inject(self._progress, self.cancel), done)

    def on_install(self) -> None:
        if self.busy:
            return
        self._set_busy(True)
        self.cancel = CancelToken()

        def done(res, err):
            self._set_busy(False)
            self.prog.set(0)
            self.set_status(str(err) if err else self.t("Всё установлено и проверено.", "Everything installed and verified."), BAD if err else OK)
            self.refresh_status()
        self.run_bg(lambda: self.L.prepare(self._progress, self.cancel), done)

    def on_full_verify(self) -> None:
        def work():
            return self.L.installer.verify(True) + self.L.mods.verify(self.L.profiles.game_dir(self.L.pid))

        def done(res, err):
            if err:
                self.set_status(str(err), BAD)
            elif res:
                self.set_status(self.t("Найдены проблемы: ", "Problems: ") + "; ".join(res[:3]), BAD)
            else:
                self.set_status(self.t("Все файлы совпадают с контрольными суммами.", "All files match their checksums."), OK)
        self.set_status(self.t("Проверка…", "Checking…"), VIOLET_L)
        self.run_bg(work, done)

    def on_stop(self) -> None:
        if self.cancel and self.busy:
            self.cancel.cancel()
        self.L.stop()

    # ---------------------------------------------------------------- visuals
    def page_visuals(self, f: tk.Frame) -> None:
        self.header(f, self.t("Менеджер визуалов", "Visuals manager"),
                    self.t("Эти настройки записываются в config/rimvisuals/launcher.json профиля, мод читает их при запуске и во время игры. "
                           "Остальные эффекты настраиваются в игре: Right Shift → ClickGUI.",
                           "These are written to the profile's config/rimvisuals/launcher.json; the mod reads them at start and in game. "
                           "All other effects are configured in game: Right Shift → ClickGUI."))
        card = Card(f, self.t("Быстрые настройки профиля", "Profile quick settings"))
        card.pack(fill="x", padx=32)
        self.vis_toggles: dict[str, Toggle] = {}
        names = {"pulseGlow": ("Pulse Glow", "Мягкое свечение по краям экрана"), "particles": ("Particles", "Частицы и оверлей"),
                 "watermark": ("Watermark", "Водяной знак RIMVISUALS"), "infoHud": ("Info HUD", "Панель FPS / ping / координаты")}
        for key, (title, ru) in names.items():
            r = tk.Frame(card, bg=PANEL)
            r.pack(fill="x", padx=16, pady=5)
            tg = Toggle(r, True, lambda v, k=key: self.set_visual(k, v))
            tg.pack(side="left")
            self.vis_toggles[key] = tg
            label(r, title, bold=True, bg=PANEL).pack(side="left", padx=(12, 8))
            label(r, ru if self.lang == "ru" else {"pulseGlow": "Soft screen-edge glow", "particles": "Particles and overlay",
                                                   "watermark": "RIMVISUALS watermark", "infoHud": "FPS / ping / coordinates panel"}[key],
                  fg=MUTED, size=9, bg=PANEL).pack(side="left")
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=(8, 4))
        label(r, self.t("Акцент", "Accent"), bg=PANEL, bold=True).pack(side="left")
        self.accent_var = tk.StringVar(value="#8B5CF6")
        entry(r, self.accent_var, 10).pack(side="left", padx=10)
        for hexc in ("#8B5CF6", "#EC4899", "#22D3EE", "#F59E0B", "#4ADE80"):
            sw = tk.Canvas(r, width=24, height=24, bg=PANEL, highlightthickness=0, cursor="hand2")
            sw.create_oval(2, 2, 22, 22, fill=hexc, outline=LINE)
            sw.bind("<Button-1>", lambda e, h=hexc: self.set_accent(h))
            sw.pack(side="left", padx=3)
        RButton(r, self.t("Применить", "Apply"), lambda: self.set_accent(self.accent_var.get()), width=100, height=30, bg=PANEL, small=True).pack(side="left", padx=12)
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=(6, 14))
        label(r, self.t("Масштаб HUD", "HUD scale"), bg=PANEL, bold=True).pack(side="left", padx=(0, 12))
        self.hud_slider = Slider(r, 60, 200, 100, lambda v: self.set_visual("hudScale", v), step=5, fmt="{}%")
        self.hud_slider.pack(side="left")
        mod = Card(f, self.t("Мод RIMVISUALS в профиле", "RIMVISUALS mod in the profile"))
        mod.pack(fill="x", padx=32, pady=14)
        self.mod_info = label(mod, "", fg=MUTED, size=9, bg=PANEL, wrap=700)
        self.mod_info.pack(anchor="w", padx=16)
        row = tk.Frame(mod, bg=PANEL)
        row.pack(fill="x", padx=16, pady=12)
        RButton(row, self.t("Выбрать JAR…", "Choose JAR…"), self.choose_jar, width=140, bg=PANEL).pack(side="left", padx=(0, 10))
        RButton(row, self.t("Установить / обновить", "Install / update"), self.install_mod, width=190, bg=PANEL, primary=True).pack(side="left")

    def refresh_visuals(self) -> None:
        pr = self.L.profiles.items[self.L.pid]
        for k, tg in self.vis_toggles.items():
            tg.set(pr.visuals.get(k, True))
        self.accent_var.set(pr.visuals.get("accent", "#8B5CF6"))
        self.hud_slider.set(pr.visuals.get("hudScale", 100))
        st = self.L.mods.status(self.L.profiles.game_dir(pr.id))
        try:
            src = self.L.mod_jar_source()
        except ValueError as e:
            src = None
            st["problems"].append(str(e))
        self.mod_info.configure(text=self.t("Установлен: ", "Installed: ") + (st["installed"] or "—") + "  ·  " +
                                self.t("Источник JAR: ", "JAR source: ") + (f"{src.path.name} v{src.version}" if src else "—") +
                                ("\n" + "; ".join(st["problems"]) if st["problems"] else "\n" + self.t("Целостность: OK", "Integrity: OK")),
                                fg=BAD if st["problems"] else MUTED)

    def set_visual(self, key: str, value) -> None:
        try:
            self.L.profiles.update(self.L.pid, visuals={key: value})
            self.L.profiles.write_bridge(self.L.pid)
        except ValueError as e:
            messagebox.showerror("RIMVISUALS", str(e))

    def set_accent(self, hexc: str) -> None:
        hexc = hexc.strip().upper()
        self.accent_var.set(hexc)
        self.set_visual("accent", hexc)

    def choose_jar(self) -> None:
        p = filedialog.askopenfilename(title="RIMVISUALS JAR", filetypes=[("Fabric mod", "*.jar")])
        if p:
            self.L.selected_jar = Path(p)
            self.refresh_visuals()

    def install_mod(self) -> None:
        def work():
            src = self.L.mod_jar_source()
            if not src:
                raise RuntimeError(self.t("Нет JAR: выберите файл.", "No JAR: choose a file."))
            gd = self.L.profiles.game_dir(self.L.pid)
            self.L.mods.install_mod(gd, src.path)
            self.L.profiles.write_bridge(self.L.pid)
        self.run_bg(work, lambda r, e: (messagebox.showerror("RIMVISUALS", str(e)) if e else None, self.refresh_visuals()))

    # ---------------------------------------------------------------- configs / profiles
    def page_configs(self, f: tk.Frame) -> None:
        self.header(f, self.t("Менеджер конфигураций", "Configuration manager"),
                    self.t("Профили игры лежат в отдельных папках внутри папки RIMVISUALS. Резервные копии CFG не содержат вход в аккаунт.",
                           "Game profiles live in separate folders inside RIMVISUALS' folder. CFG backups never include your login."))
        top = tk.Frame(f, bg=BG)
        top.pack(fill="x", padx=32)
        card = Card(top, self.t("Профили", "Profiles"))
        card.pack(side="left", fill="both", expand=True)
        self.prof_list = tk.Listbox(card, bg=PANEL2, fg=TEXT, selectbackground=VIOLET_D, selectforeground="#FFFFFF", relief="flat",
                                    highlightthickness=1, highlightbackground=LINE, font=font(10), height=7, exportselection=False)
        self.prof_list.pack(fill="both", expand=True, padx=16, pady=(0, 8))
        self.prof_list.bind("<<ListboxSelect>>", lambda e: self.on_prof_select())
        row = tk.Frame(card, bg=PANEL)
        row.pack(fill="x", padx=16, pady=(0, 12))
        for txt_ru, txt_en, cmd in (("Создать", "New", self.prof_new), ("Копия", "Copy", self.prof_copy), ("Переименовать", "Rename", self.prof_rename),
                                    ("Удалить", "Delete", self.prof_delete)):
            RButton(row, self.t(txt_ru, txt_en), cmd, width=96, height=30, bg=PANEL, small=True).pack(side="left", padx=(0, 6))
        row2 = tk.Frame(card, bg=PANEL)
        row2.pack(fill="x", padx=16, pady=(0, 12))
        RButton(row2, self.t("Экспорт JSON", "Export JSON"), self.prof_export, width=120, height=30, bg=PANEL, small=True).pack(side="left", padx=(0, 6))
        RButton(row2, self.t("Импорт JSON", "Import JSON"), self.prof_import, width=120, height=30, bg=PANEL, small=True).pack(side="left")
        right = Card(top, self.t("Профиль: память и Java", "Profile: RAM and Java"))
        right.pack(side="left", fill="y", padx=(14, 0))
        self.ram_slider = Slider(right, 1024, 16384, 4096, self.on_ram, step=512, width=200, fmt="{} MB")
        label(right, "RAM", fg=MUTED, size=9, bg=PANEL).pack(anchor="w", padx=16)
        self.ram_slider.pack(padx=16, pady=(0, 8))
        label(right, self.t("Путь к Java (пусто = авто)", "Java path (empty = auto)"), fg=MUTED, size=9, bg=PANEL).pack(anchor="w", padx=16)
        self.java_var = tk.StringVar()
        entry(right, self.java_var, 28).pack(padx=16, pady=(0, 6))
        RButton(right, self.t("Сохранить Java", "Save Java"), self.on_java_save, width=130, height=30, bg=PANEL, small=True).pack(anchor="w", padx=16, pady=(0, 12))
        cfg = Card(f, self.t("CFG мода (config/rimvisuals)", "Mod CFG (config/rimvisuals)"))
        cfg.pack(fill="x", padx=32, pady=14)
        self.cfg_info = label(cfg, "", fg=MUTED, size=9, bg=PANEL, wrap=760)
        self.cfg_info.pack(anchor="w", padx=16)
        row = tk.Frame(cfg, bg=PANEL)
        row.pack(fill="x", padx=16, pady=12)
        for ru, en, cmd in (("Резервная копия", "Backup now", self.cfg_backup), ("Экспорт ZIP", "Export ZIP", self.cfg_export),
                            ("Импорт ZIP", "Import ZIP", self.cfg_import), ("Восстановить копию", "Restore backup", self.cfg_restore)):
            RButton(row, self.t(ru, en), cmd, width=150, height=32, bg=PANEL, small=True).pack(side="left", padx=(0, 8))

    def refresh_configs(self) -> None:
        self.prof_list.delete(0, "end")
        ids = list(self.L.profiles.items)
        for pid in ids:
            p = self.L.profiles.items[pid]
            self.prof_list.insert("end", f"{p.name}   ·   {p.ram_mb} MB" + ("   ●" if pid == self.L.pid else ""))
        self.prof_list.selection_set(ids.index(self.L.pid))
        self.prof_ids = ids
        self.on_prof_select()
        n = len(self.L.cfg.files(self.L.pid))
        self.cfg_info.configure(text=self.t(f"Файлов CFG в профиле: {n}. Резервных копий: {len(self.L.cfg.list_backups(self.L.pid))}.",
                                            f"CFG files in profile: {n}. Backups: {len(self.L.cfg.list_backups(self.L.pid))}."))

    def sel_pid(self) -> str:
        s = self.prof_list.curselection()
        return self.prof_ids[s[0]] if s else self.L.pid

    def on_prof_select(self) -> None:
        pid = self.sel_pid()
        p = self.L.profiles.items[pid]
        self.ram_slider.set(p.ram_mb)
        self.java_var.set(p.java_path)

    def ask(self, title: str, prompt: str, initial: str = "") -> str | None:
        from tkinter import simpledialog
        return simpledialog.askstring(title, prompt, initialvalue=initial, parent=self)

    def prof_new(self) -> None:
        n = self.ask("RIMVISUALS", self.t("Название профиля", "Profile name"))
        if n:
            self.L.select_profile(self.L.profiles.create(n).id)
            self.after_profiles()

    def prof_copy(self) -> None:
        pid = self.sel_pid()
        self.L.select_profile(self.L.profiles.create(self.L.profiles.items[pid].name + " copy", copy_from=pid).id)
        self.after_profiles()

    def prof_rename(self) -> None:
        pid = self.sel_pid()
        n = self.ask("RIMVISUALS", self.t("Новое название", "New name"), self.L.profiles.items[pid].name)
        if n:
            self.L.profiles.rename(pid, n)
            self.after_profiles()

    def prof_delete(self) -> None:
        pid = self.sel_pid()
        name = self.L.profiles.items[pid].name
        if not messagebox.askyesno("RIMVISUALS", self.t(f"Удалить профиль «{name}» из списка?\nФайлы мира и настроек на диске останутся.",
                                                       f"Remove profile “{name}” from the list?\nWorld and settings files stay on disk.")):
            return
        try:
            self.L.profiles.delete(pid)
        except ValueError as e:
            messagebox.showerror("RIMVISUALS", str(e))
            return
        if self.L.pid == pid or self.L.settings["last_profile"] not in self.L.profiles.items:
            self.L.select_profile(next(iter(self.L.profiles.items)))
        self.after_profiles()

    def after_profiles(self) -> None:
        self.fill_profile_menu()
        self.refresh_configs()
        self.refresh_status()

    def prof_export(self) -> None:
        p = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON", "*.json")])
        if p:
            Path(p).write_text(self.L.profiles.export_json(self.sel_pid()), encoding="utf-8")

    def prof_import(self) -> None:
        p = filedialog.askopenfilename(filetypes=[("JSON", "*.json")])
        if p:
            try:
                self.L.profiles.import_json(Path(p).read_text(encoding="utf-8"))
            except (ValueError, OSError) as e:
                messagebox.showerror("RIMVISUALS", str(e))
            self.after_profiles()

    def on_ram(self, v: int) -> None:
        self.L.profiles.update(self.sel_pid(), ram_mb=v)
        self.refresh_configs()

    def on_java_save(self) -> None:
        self.L.profiles.update(self.sel_pid(), java_path=self.java_var.get().strip())
        self.refresh_status()

    def cfg_backup(self) -> None:
        p = self.L.cfg.backup(self.sel_pid())
        self.cfg_info.configure(text=self.t("Копия сохранена: ", "Backup saved: ") + p.name, fg=OK)

    def cfg_export(self) -> None:
        p = filedialog.asksaveasfilename(defaultextension=".zip", filetypes=[("ZIP", "*.zip")])
        if p:
            n = self.L.cfg.export_zip(self.sel_pid(), Path(p))
            self.cfg_info.configure(text=self.t(f"Экспортировано файлов: {n}", f"Exported files: {n}"), fg=OK)

    def cfg_import(self) -> None:
        p = filedialog.askopenfilename(filetypes=[("ZIP", "*.zip")])
        if p and messagebox.askyesno("RIMVISUALS", self.t("Перед импортом будет создана резервная копия. Продолжить?", "A backup is made before importing. Continue?")):
            try:
                n = self.L.cfg.import_zip(self.sel_pid(), Path(p))
                self.cfg_info.configure(text=self.t(f"Импортировано файлов: {n}", f"Imported files: {n}"), fg=OK)
            except (ValueError, OSError) as e:
                messagebox.showerror("RIMVISUALS", str(e))

    def cfg_restore(self) -> None:
        bs = self.L.cfg.list_backups(self.sel_pid())
        if not bs:
            messagebox.showinfo("RIMVISUALS", self.t("Резервных копий нет.", "No backups yet."))
            return
        if messagebox.askyesno("RIMVISUALS", self.t(f"Восстановить {bs[0].name}?", f"Restore {bs[0].name}?")):
            try:
                self.cfg_info.configure(text=self.t(f"Восстановлено файлов: {self.L.cfg.import_zip(self.sel_pid(), bs[0])}", "Restored."), fg=OK)
            except (ValueError, OSError) as e:
                messagebox.showerror("RIMVISUALS", str(e))

    # ---------------------------------------------------------------- account
    def page_account(self, f: tk.Frame) -> None:
        self.header(f, self.t("Аккаунт", "Account"),
                    self.t("Вход на сайте RIMVISUALS даёт доступ к моду (тот же вход, что и в игре). Для Minecraft используется только официальный вход Microsoft.",
                           "Signing in on the RIMVISUALS site unlocks the mod (same login as in game). Minecraft uses the official Microsoft sign-in only."))
        c1 = Card(f, self.t("Аккаунт RIMVISUALS (сайт)", "RIMVISUALS account (site)"))
        c1.pack(fill="x", padx=32)
        self.site_lbl = label(c1, self.t("Не выполнен вход", "Signed out"), bg=PANEL, bold=True)
        self.site_lbl.pack(anchor="w", padx=16)
        self.code_lbl = label(c1, "", fg=VIOLET_L, size=22, bold=True, bg=PANEL)
        self.code_lbl.pack(anchor="w", padx=16, pady=4)
        row = tk.Frame(c1, bg=PANEL)
        row.pack(fill="x", padx=16, pady=(4, 14))
        RButton(row, self.t("Войти через сайт", "Sign in via site"), self.site_login, width=170, bg=PANEL, primary=True).pack(side="left", padx=(0, 8))
        RButton(row, self.t("Проверить доступ", "Check access"), self.site_check, width=160, bg=PANEL).pack(side="left", padx=(0, 8))
        RButton(row, self.t("Выйти", "Sign out"), self.site_logout, width=100, bg=PANEL).pack(side="left")
        c2 = Card(f, self.t("Minecraft (Microsoft)", "Minecraft (Microsoft)"))
        c2.pack(fill="x", padx=32, pady=14)
        self.ms_lbl = label(c2, "", bg=PANEL, bold=True)
        self.ms_lbl.pack(anchor="w", padx=16)
        label(c2, self.t("Application (client) ID вашей регистрации Azure (публичный, не секрет):", "Application (client) ID of your Azure app registration (public, not a secret):"),
              fg=MUTED, size=9, bg=PANEL).pack(anchor="w", padx=16, pady=(6, 0))
        r = tk.Frame(c2, bg=PANEL)
        r.pack(fill="x", padx=16, pady=6)
        self.cid_var = tk.StringVar(value=self.L.settings["ms_client_id"])
        entry(r, self.cid_var, 44).pack(side="left")
        RButton(r, self.t("Сохранить", "Save"), self.save_cid, width=100, height=30, bg=PANEL, small=True).pack(side="left", padx=10)
        row = tk.Frame(c2, bg=PANEL)
        row.pack(fill="x", padx=16, pady=(4, 14))
        RButton(row, self.t("Войти через Microsoft", "Sign in with Microsoft"), self.ms_login, width=200, bg=PANEL, primary=True).pack(side="left", padx=(0, 8))
        RButton(row, self.t("Выйти", "Sign out"), self.ms_logout, width=100, bg=PANEL).pack(side="left")
        label(c2, self.t("Без входа Microsoft PLAY запускает только официальный Demo-режим (без мультиплеера). Токены хранятся зашифрованными через Windows DPAPI.",
                         "Without a Microsoft sign-in PLAY starts the official Demo mode only (no multiplayer). Tokens are stored encrypted with Windows DPAPI."),
              fg=MUTED, size=9, bg=PANEL, wrap=740).pack(anchor="w", padx=16, pady=(0, 12))
        self._site_poll = None
        self.update_account_labels()

    def update_account_labels(self) -> None:
        self.ms_lbl.configure(text=(self.t("Вошли как: ", "Signed in as: ") + self.L.ms.session.name) if self.L.ms.session else
                              (self.t("Client ID задан, вход не выполнен", "Client ID set, not signed in") if self.L.ms.configured else self.t("Client ID не задан", "Client ID not set")))

    def site_login(self) -> None:
        def work():
            code = self.L.site.start()
            return code
        def done(code, err):
            if err:
                self.site_lbl.configure(text=str(err), fg=BAD)
                return
            self.code_lbl.configure(text=code)
            self.site_lbl.configure(text=self.t("Подтвердите код на сайте…", "Confirm the code on the site…"), fg=VIOLET_L)
            webbrowser.open(self.L.site.browser_url())
            self._poll_site()
        self.run_bg(work, done)

    def _poll_site(self) -> None:
        def work():
            return self.L.site.poll()
        def done(st, err):
            if err or st in ("expired", "idle"):
                self.code_lbl.configure(text="")
                self.site_lbl.configure(text=str(err) if err else self.t("Код истёк. Начните заново.", "Code expired. Start again."), fg=BAD)
            elif st == "approved":
                self.code_lbl.configure(text="")
                self.site_check()
            else:
                self._site_poll = self.after(4000, self._poll_site)
        self.run_bg(work, done)

    def site_check(self) -> None:
        def done(r, err):
            if err:
                self.site_lbl.configure(text=str(err), fg=BAD)
                return
            st = r["status"]
            msg = {"active": self.t("Вход выполнен, доступ активен ✓", "Signed in, access active ✓"),
                   "signed_out": self.t("Не выполнен вход", "Signed out"),
                   "no_access": self.t("Вход есть, но доступ не выдан или отключён", "Signed in but access not granted or revoked"),
                   "offline": self.t("Нет связи с сервером", "Server unreachable")}[st]
            self.site_lbl.configure(text=msg, fg=OK if st == "active" else (WARN if st != "signed_out" else MUTED))
            if st == "active":
                self.L.site.share_with_mod(self.L.profiles.game_dir(self.L.pid))
        self.run_bg(self.L.site.validate, done)

    def site_logout(self) -> None:
        self.L.site.sign_out()
        self.code_lbl.configure(text="")
        self.site_lbl.configure(text=self.t("Не выполнен вход", "Signed out"), fg=MUTED)

    def save_cid(self) -> None:
        try:
            self.L.settings.set("ms_client_id", self.cid_var.get())
            self.L.reload_ms()
            self.update_account_labels()
        except ValueError as e:
            messagebox.showerror("RIMVISUALS", str(e))

    def ms_login(self) -> None:
        self.ms_lbl.configure(text=self.t("Завершите вход в браузере…", "Finish signing in in the browser…"), fg=VIOLET_L)
        def done(res, err):
            self.update_account_labels()
            if err:
                self.ms_lbl.configure(text=str(err), fg=BAD)
        self.run_bg(self.L.ms.sign_in, done)

    def ms_logout(self) -> None:
        self.L.ms.sign_out()
        self.update_account_labels()

    # ---------------------------------------------------------------- RIM JARVIS
    def page_jarvis(self, f: tk.Frame) -> None:
        self.header(f, "RIM JARVIS", self.t(
            "Пишите или говорите команды. Каждая команда проходит через локальный мост (127.0.0.1, токен сессии) и меняет настройки запущенного мода; "
            "если игра не запущена, я так и скажу.", "Type or speak commands. Each one goes through a local bridge (127.0.0.1, session token) and changes the running mod's "
            "settings; if the game is not running I will say so."))
        top = tk.Frame(f, bg=BG)
        top.pack(fill="x", padx=32)
        self.j_state = label(top, "", fg=MUTED, size=9)
        self.j_state.pack(side="left")
        for ru, en, key in (("Озвучка", "Voice replies", "jarvis_voice"),):
            Toggle(top, self.L.settings[key], lambda v, k=key: self.L.settings.set(k, v)).pack(side="right")
            label(top, self.t(ru, en), size=9).pack(side="right", padx=8)
        self.j_log = tk.Text(f, bg=PANEL, fg=TEXT, relief="flat", font=font(10), wrap="word", highlightthickness=1,
                             highlightbackground=LINE, state="disabled", height=14)
        self.j_log.pack(fill="both", expand=True, padx=32, pady=(8, 8))
        self.j_log.tag_configure("me", foreground=VIOLET_L)
        self.j_log.tag_configure("bot", foreground=TEXT)
        self.j_log.tag_configure("ok", foreground=OK)
        self.j_log.tag_configure("bad", foreground=BAD)
        self.j_confirm = tk.Frame(f, bg=BG)
        RButton(self.j_confirm, self.t("Подтвердить", "Confirm"), lambda: self.jarvis_say("да"), width=140, height=32, primary=True, small=True).pack(side="left", padx=(0, 8))
        RButton(self.j_confirm, self.t("Отмена", "Cancel"), lambda: self.jarvis_say("нет"), width=110, height=32, small=True).pack(side="left")
        row = tk.Frame(f, bg=BG)
        row.pack(fill="x", padx=32, pady=(0, 8))
        self.j_var = tk.StringVar()
        e = entry(row, self.j_var, 70)
        e.pack(side="left", fill="x", expand=True, ipady=5)
        e.bind("<Return>", lambda ev: self.jarvis_send())
        RButton(row, self.t("Отправить", "Send"), self.jarvis_send, width=110, height=32, primary=True, small=True).pack(side="left", padx=8)
        RButton(row, self.t("🎤 Говорить", "🎤 Speak"), self.jarvis_listen_once, width=120, height=32, small=True).pack(side="left")
        opts = tk.Frame(f, bg=BG)
        opts.pack(fill="x", padx=32, pady=(0, 6))
        label(opts, self.t("Режим микрофона:", "Microphone mode:"), fg=MUTED, size=9).pack(side="left")
        for code, ru, en in (("off", "выкл", "off"), ("ptt", "удерживать F8", "hold F8"), ("wake", "слово «Джарвис»", "wake word")):
            RButton(opts, self.t(ru, en), lambda c=code: self.set_listen(c), width=150, height=28, small=True,
                    primary=(self.L.settings["jarvis_listen"] == code)).pack(side="left", padx=4)
        label(f, self.t("Микрофон включается только в выбранном режиме; распознавание локальное (Windows), звук никуда не отправляется.",
                        "The mic is used only in the selected mode; recognition is local (Windows); audio is never sent anywhere."), fg=MUTED, size=8).pack(anchor="w", padx=32, pady=(0, 14))
        self.jarvis_add("bot", self.t("Готов. Скажите «помощь», чтобы увидеть команды.", "Ready. Say “help” to see commands."))
        self._ptt = None
        self.after(500, self.apply_listen_mode)
        self.after(1500, self.jarvis_poll_state)

    def jarvis_add(self, tag: str, text: str) -> None:
        self.j_log.configure(state="normal")
        self.j_log.insert("end", ("› " if tag == "me" else "") + text + "\n\n", tag)
        self.j_log.see("end")
        self.j_log.configure(state="disabled")

    def jarvis_poll_state(self) -> None:
        up = self.L.jarvis().client.ipc_path.exists() and self.L.run.running
        self.j_state.configure(text=self.t("● мост игры подключён", "● game bridge available") if up else self.t("○ игра не запущена", "○ game not running"), fg=OK if up else MUTED)
        self.after(2500, self.jarvis_poll_state)

    def jarvis_send(self) -> None:
        text = self.j_var.get().strip()
        if text:
            self.j_var.set("")
            self.jarvis_say(text)

    def jarvis_say(self, text: str) -> None:
        self.jarvis_add("me", text)

        def done(r, err):
            if err:
                self.jarvis_add("bad", str(err))
                return
            self.jarvis_add("ok" if r.executed else ("bad" if not r.ok else "bot"), r.text)
            if r.pending:
                self.j_confirm.pack(before=self.j_confirm.master.winfo_children()[-2], padx=32, pady=(0, 6), anchor="w")
            else:
                self.j_confirm.pack_forget()
            if self.L.settings["jarvis_voice"]:
                self.speak(r.text)
        self.run_bg(lambda: self.L.jarvis().ask(text), done)

    def speak(self, text: str) -> None:
        from ..jarvis.voice import Voice
        S = self.L.settings
        self.run_bg(lambda: Voice().speak(text, self.lang, S["jarvis_volume"], S["jarvis_rate"]))

    def jarvis_listen_once(self) -> None:
        from ..jarvis.voice import Voice
        self.jarvis_add("bot", self.t("Слушаю…", "Listening…"))

        def done(text, err):
            if err:
                self.jarvis_add("bad", str(err))
            elif text:
                self.jarvis_say(text)
            else:
                self.jarvis_add("bot", self.t("Ничего не расслышал.", "Did not catch that."))
        self.run_bg(lambda: Voice().listen(self.lang, 6), done)

    def set_listen(self, mode: str) -> None:
        self.L.settings.set("jarvis_listen", mode)
        self.apply_listen_mode()
        self.build()
        self.show("jarvis", animate=False)

    def apply_listen_mode(self) -> None:
        from ..jarvis.voice import PushToTalk, Voice, WakeWord
        if self._ptt:
            self._ptt.stop()
            self._ptt = None
        mode = self.L.settings["jarvis_listen"]
        if mode == "off" or sys.platform != "win32":
            return
        on_text = lambda t: self.q.put((lambda *_: self.jarvis_say(t), None, None))  # noqa: E731
        state = lambda s: self.q.put((lambda *_: self.jarvis_add("bad", s[6:]) if s.startswith("error:") else None, None, None))  # noqa: E731
        cls = PushToTalk if mode == "ptt" else WakeWord
        self._ptt = cls(Voice(), lambda: self.lang, on_text, state)
        self._ptt.start()

    # ---------------------------------------------------------------- settings
    def page_settings(self, f: tk.Frame) -> None:
        self.header(f, self.t("Настройки", "Settings"))
        card = Card(f)
        card.pack(fill="x", padx=32)
        S = self.L.settings
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=(14, 6))
        label(r, self.t("Язык", "Language"), bg=PANEL, bold=True).pack(side="left", padx=(0, 14))
        for code, name in (("ru", "Русский"), ("en", "English")):
            RButton(r, name, lambda c=code: self.set_lang(c), width=110, height=30, bg=PANEL, small=True, primary=(self.lang == code)).pack(side="left", padx=4)
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=8)
        label(r, self.t("RAM по умолчанию для новых профилей", "Default RAM for new profiles"), bg=PANEL, bold=True).pack(side="left", padx=(0, 14))
        Slider(r, 1024, 16384, S["ram_mb"], lambda v: (S.set("ram_mb", v), setattr(self.L.profiles, "default_ram", v)), step=512, width=220, fmt="{} MB").pack(side="left")
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=8)
        label(r, self.t("Java 21 (пусто = автопоиск / загрузка)", "Java 21 (empty = auto-find / download)"), bg=PANEL, bold=True).pack(anchor="w")
        self.set_java = tk.StringVar(value=S["java_path"])
        entry(r, self.set_java, 56).pack(side="left", pady=4)
        RButton(r, self.t("Обзор…", "Browse…"), self.browse_java, width=90, height=30, bg=PANEL, small=True).pack(side="left", padx=8)
        RButton(r, self.t("Сохранить", "Save"), lambda: S.set("java_path", self.set_java.get().strip()), width=100, height=30, bg=PANEL, small=True).pack(side="left")
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=8)
        label(r, self.t("Доп. JVM-аргументы (без -javaagent)", "Extra JVM args (no -javaagent)"), bg=PANEL, bold=True).pack(anchor="w")
        self.jvm_var = tk.StringVar(value=S["jvm_args"])
        entry(r, self.jvm_var, 56).pack(side="left", pady=4)
        RButton(r, self.t("Сохранить", "Save"), self.save_jvm, width=100, height=30, bg=PANEL, small=True).pack(side="left", padx=8)
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=8)
        label(r, "RIM JARVIS", bg=PANEL, bold=True).pack(anchor="w")
        rr = tk.Frame(r, bg=PANEL)
        rr.pack(fill="x", pady=3)
        Toggle(rr, S["jarvis_bridge"], lambda v: (S.set("jarvis_bridge", v), setattr(self.L.profiles, "jarvis_enabled", v),
                                                  self.L.profiles.write_bridge(self.L.pid))).pack(side="left")
        label(rr, self.t("Мост JARVIS в моде (только 127.0.0.1, вступает в силу при следующем запуске игры)", "JARVIS bridge in the mod (127.0.0.1 only; applies at next game start)"), bg=PANEL).pack(side="left", padx=10)
        rr = tk.Frame(r, bg=PANEL)
        rr.pack(fill="x", pady=3)
        Toggle(rr, S["jarvis_ai"], lambda v: S.set("jarvis_ai", v)).pack(side="left")
        label(rr, self.t("AI Mode (внешний сервис, по желанию; отвечает текстом, команды не исполняет)", "AI Mode (optional external service; text answers only, never runs commands)"), bg=PANEL).pack(side="left", padx=10)
        rr = tk.Frame(r, bg=PANEL)
        rr.pack(fill="x", pady=3)
        self.ai_url, self.ai_model, self.ai_key = tk.StringVar(value=S["jarvis_ai_url"]), tk.StringVar(value=S["jarvis_ai_model"]), tk.StringVar()
        entry(rr, self.ai_url, 34).pack(side="left", padx=(0, 6))
        entry(rr, self.ai_model, 14).pack(side="left", padx=(0, 6))
        entry(rr, self.ai_key, 22, show="•").pack(side="left", padx=(0, 6))
        RButton(rr, self.t("Сохранить AI", "Save AI"), self.save_ai, width=120, height=30, bg=PANEL, small=True).pack(side="left")
        label(r, self.t("Адрес https, модель и ключ API. Ключ хранится зашифрованным (DPAPI). Платных сервисов по умолчанию нет.",
                        "https endpoint, model and API key. The key is stored encrypted (DPAPI). No paid service is preconfigured."), fg=MUTED, size=8, bg=PANEL).pack(anchor="w")
        r = tk.Frame(card, bg=PANEL)
        r.pack(fill="x", padx=16, pady=(8, 16))
        for key, ru, en in (("close_launcher_on_start", "Закрывать лаунчер после запуска игры", "Close launcher after the game starts"),
                            ("check_updates", "Проверять обновления при открытии", "Check for updates on open")):
            rr = tk.Frame(r, bg=PANEL)
            rr.pack(fill="x", pady=3)
            Toggle(rr, S[key], lambda v, k=key: S.set(k, v)).pack(side="left")
            label(rr, self.t(ru, en), bg=PANEL).pack(side="left", padx=10)

    def set_lang(self, code: str) -> None:
        self.L.settings.set("language", code)
        self.lang = code
        self.build()

    def save_ai(self) -> None:
        try:
            self.L.settings.set("jarvis_ai_url", self.ai_url.get().strip())
            self.L.settings.set("jarvis_ai_model", self.ai_model.get().strip())
            if self.ai_key.get().strip():
                self.L.secrets.set("jarvis_ai_key", self.ai_key.get().strip())
                self.ai_key.set("")
            self.L._jarvis = None
        except ValueError as e:
            messagebox.showerror("RIMVISUALS", str(e))

    def browse_java(self) -> None:
        p = filedialog.askopenfilename(title="java.exe") if sys.platform == "win32" else filedialog.askopenfilename(title="java")
        if p:
            self.set_java.set(p)

    def save_jvm(self) -> None:
        from ..minecraft import safe_extra_jvm_args
        try:
            safe_extra_jvm_args(self.jvm_var.get())
            self.L.settings.set("jvm_args", self.jvm_var.get())
        except ValueError as e:
            messagebox.showerror("RIMVISUALS", str(e))

    # ---------------------------------------------------------------- updates
    def page_updates(self, f: tk.Frame) -> None:
        self.header(f, self.t("Проверка обновлений", "Updates"))
        card = Card(f)
        card.pack(fill="x", padx=32)
        self.upd_lbl = label(card, "", fg=MUTED, size=10, bg=PANEL, wrap=760)
        self.upd_lbl.pack(anchor="w", padx=16, pady=16)
        row = tk.Frame(card, bg=PANEL)
        row.pack(fill="x", padx=16, pady=(0, 16))
        RButton(row, self.t("Проверить", "Check"), self.refresh_updates, width=130, bg=PANEL, primary=True).pack(side="left", padx=(0, 8))
        RButton(row, self.t("Обновить мод из встроенного JAR", "Update mod from bundled JAR"), self.install_mod, width=260, bg=PANEL).pack(side="left")

    def refresh_updates(self) -> None:
        self.upd_lbl.configure(text=self.t("Проверка…", "Checking…"))
        def done(r, err):
            if err:
                self.upd_lbl.configure(text=str(err), fg=BAD)
                return
            m = r["mod"]
            lines = [f"{self.t('Лаунчер', 'Launcher')}: {r['launcher']}" + (f"  ·  GitHub: {r['latest']}" if r.get("latest") else ""),
                     f"{self.t('Мод в профиле', 'Mod in profile')}: {m['installed'] or '—'}  ·  {self.t('во встроенном JAR', 'bundled')}: {m['bundled'] or '—'}",
                     (self.t("Доступно обновление мода.", "Mod update available.") if m["update_available"] else self.t("Мод актуален.", "Mod is up to date.")),
                     r.get("error", "")]
            self.upd_lbl.configure(text="\n".join(x for x in lines if x), fg=WARN if m["update_available"] else MUTED)
        self.run_bg(self.L.check_updates, done)

    # ---------------------------------------------------------------- logs
    def page_logs(self, f: tk.Frame) -> None:
        self.header(f, self.t("Логи и диагностика", "Logs and diagnostics"))
        row = tk.Frame(f, bg=BG)
        row.pack(fill="x", padx=32, pady=(0, 8))
        RButton(row, self.t("Сохранить диагностику", "Save diagnostics"), self.save_report, width=190, primary=True).pack(side="left", padx=(0, 8))
        RButton(row, self.t("Открыть папку логов", "Open logs folder"), self.open_logs, width=170).pack(side="left")
        self.log_text = tk.Text(f, bg=PANEL, fg=TEXT, relief="flat", font=("Consolas", 9), wrap="word", highlightthickness=1,
                                highlightbackground=LINE, insertbackground=VIOLET_L, state="disabled")
        self.log_text.pack(fill="both", expand=True, padx=32, pady=(0, 24))
        for lvl, col in (("INFO", TEXT), ("DEBUG", MUTED), ("WARNING", WARN), ("ERROR", BAD), ("CRITICAL", BAD)):
            self.log_text.tag_configure(lvl, foreground=col)
        RING.listeners.append(self._on_log)

    def _on_log(self, ts: float, lvl: str, msg: str) -> None:
        self.q.put((lambda *_: self._append_log(ts, lvl, msg), None, None))

    def _append_log(self, ts: float, lvl: str, msg: str) -> None:
        try:
            self.log_text.configure(state="normal")
            self.log_text.insert("end", f"{time.strftime('%H:%M:%S', time.localtime(ts))}  {msg}\n", lvl)
            self.log_text.see("end")
            self.log_text.configure(state="disabled")
        except tk.TclError:
            pass

    def reload_logs(self) -> None:
        self.log_text.configure(state="normal")
        self.log_text.delete("1.0", "end")
        self.log_text.configure(state="disabled")
        for ts, lvl, msg in list(RING.lines)[-400:]:
            self._append_log(ts, lvl, msg)

    def save_report(self) -> None:
        p = self.L.report()
        messagebox.showinfo("RIMVISUALS", self.t("Диагностика сохранена (без токенов):\n", "Diagnostics saved (no tokens):\n") + str(p))

    def open_logs(self) -> None:
        d = self.L.paths.logs
        if sys.platform == "win32":
            os.startfile(d)  # type: ignore[attr-defined]
        else:
            subprocess.Popen(["xdg-open", str(d)])


# ------------------------------------------------------------------ entry points
def run(home: str | None = None) -> int:
    app = App(home)
    if app.L.settings["check_updates"]:
        app.after(1500, app.refresh_updates)
    app.mainloop()
    return 0


def _grab(path: Path) -> bool:
    try:
        if sys.platform == "win32":
            from PIL import ImageGrab  # CI only
            ImageGrab.grab().save(path)
        else:
            subprocess.run(["import", "-window", "root", str(path)], check=True, timeout=20)
        return True
    except Exception as e:
        print("screenshot failed:", e)
        return False


def selftest(home: str | None, shots: str | None) -> int:
    """Build every page, exercise widgets and navigation; optionally save a PNG per page."""
    import tempfile
    home = home or tempfile.mkdtemp(prefix="rv-selftest-")
    app = App(home)
    app.geometry("980x640+0+0")
    problems: list[str] = []
    out = Path(shots) if shots else None
    if out:
        out.mkdir(parents=True, exist_ok=True)

    def pump(sec: float) -> None:
        end = time.monotonic() + sec
        while time.monotonic() < end:
            app.update()
            time.sleep(0.01)

    pump(2.0)
    for key, _, _ in PAGES:
        app.show(key)
        pump(0.6)
        if app.page_name != key or not app.pages[key].winfo_ismapped():
            problems.append(f"page {key} not visible")
        if out and not _grab(out / f"{key}.png"):
            problems.append(f"no screenshot for {key}")
    app.set_lang("en")
    pump(0.4)
    app.show("settings")
    pump(0.4)
    if out:
        _grab(out / "settings-en.png")
    app.set_lang("ru")
    app.show("visuals")
    pump(0.3)
    app.vis_toggles["pulseGlow"].set(False, True)
    app.hud_slider.value = 130
    app.set_visual("hudScale", 130)
    import json
    bridge = json.loads((app.L.profiles.game_dir(app.L.pid) / "config/rimvisuals/launcher.json").read_text())
    if bridge["visuals"]["pulseGlow"] is not False or bridge["visuals"]["hudScale"] != 130:
        problems.append("visual toggles did not reach launcher.json")
    app.destroy()
    print("GUI selftest:", "OK" if not problems else "; ".join(problems))
    return 1 if problems else 0
