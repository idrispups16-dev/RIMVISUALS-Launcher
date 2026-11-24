"""Black main menu shown after a confirmed CONNECT (see ТЗ §2). Tabs: Главная / Моды / Помощник / Скин."""
from __future__ import annotations

import tkinter as tk

from .. import homemodel as HM
from ..jarvis.chats import DAILY, ChatStore
from ..skin import default_skin, fetch_skin, load_skin, scene
from ..winfocus import focus_pid
from .widgets import BAD, LINE, MUTED, OK, PANEL, PANEL2, TEXT, VIOLET, VIOLET_L, WARN, RButton, Toggle, entry, font, label

K = "#000000"
TABS = [("home", "Главная"), ("mods", "Моды"), ("bot", "Помощник"), ("skin", "Скин")]


class BlackHome(tk.Frame):
    def __init__(self, app):
        super().__init__(app, bg=K)
        self.app, self.link = app, HM.GameLink()
        self.tab = "home"
        self.sel = app.L.settings["home_version"]
        self.store = ChatStore(app.L.paths.root / "jarvis-chats.json")
        self.chat = self.store.chats()[0] if self.store.chats() else self.store.new_chat()
        self.skin = default_skin()
        self.yaw = -0.56
        self.rows: dict[str, Toggle] = {}
        bar = tk.Frame(self, bg=K, height=56)
        bar.pack(fill="x")
        bar.pack_propagate(False)
        lg = tk.Frame(bar, bg=K)
        lg.place(x=22, rely=0.5, anchor="w")
        tk.Label(lg, text="R", bg=TEXT, fg=K, font=font(10, True), width=2).pack(side="left")
        label(lg, "RIMVISUALS", fg=TEXT, size=10, bold=True, bg=K).pack(side="left", padx=10)
        mid = tk.Frame(bar, bg=K)
        mid.place(relx=0.5, rely=0.5, anchor="center")
        self.tabs: dict[str, tk.Label] = {}
        for key, ru in TABS:
            b = tk.Label(mid, text=ru, bg=K, fg=MUTED, font=font(10, True), cursor="hand2", padx=16, pady=7)
            b.pack(side="left", padx=2)
            b.bind("<Button-1>", lambda e, k=key: self.show(k))
            self.tabs[key] = b
        rt = tk.Frame(bar, bg=K)
        rt.place(relx=1.0, x=-22, rely=0.5, anchor="e")
        gear = tk.Label(rt, text="⚙", bg=K, fg=MUTED, font=font(14), cursor="hand2")
        gear.pack(side="right", padx=(14, 0))
        gear.bind("<Button-1>", lambda e: app.leave_black_home())
        self.conn = label(rt, "", fg=MUTED, size=9, bg=K)
        self.conn.pack(side="right", padx=(0, 14))
        self.nick = label(rt, "", fg=TEXT, size=10, bold=True, bg=K)
        self.nick.pack(side="right")
        self.head = tk.Canvas(rt, width=26, height=26, bg=K, highlightthickness=0)
        self.head.pack(side="right", padx=(0, 8))
        self.body = tk.Frame(self, bg=K)
        self.body.pack(fill="both", expand=True)
        self.frames = {k: tk.Frame(self.body, bg=K) for k, _ in TABS}
        self._build_home(self.frames["home"])
        self._build_mods(self.frames["mods"])
        self._build_bot(self.frames["bot"])
        self._build_skin(self.frames["skin"])
        self.render_home()
        self.show("home")

    # ------------------------------------------------------------- navigation
    def show(self, k: str) -> None:
        self.tab = k
        for key, b in self.tabs.items():
            b.configure(fg=TEXT if key == k else MUTED, bg="#1c1c1e" if key == k else K)
        for key, f in self.frames.items():
            f.place_forget()
        self.frames[k].place(x=0, y=0, relwidth=1, relheight=1)
        if k == "mods":
            self.load_mods()
        if k == "skin":
            self.load_skin_for(self.app.L.settings["player_name"] or self.link.player)
        if k == "bot":
            self.q_lbl.configure(text=self._quota_text())

    def set_link(self, link: HM.GameLink) -> None:
        self.link = link
        if link.connected and link.minecraft in HM.VERSIONS and self.sel not in HM.BUILT_FOR:
            self.sel = link.minecraft
        self.render_home()

    # ------------------------------------------------------------------ home
    def _build_home(self, f: tk.Frame) -> None:
        self.stage = tk.Canvas(f, bg=K, highlightthickness=0)
        self.stage.place(relx=0.46, y=0, relwidth=0.54, relheight=1)
        self.stage.bind("<Configure>", lambda e: self.paint(self.stage, True))
        self.stage.bind("<B1-Motion>", self._drag)
        self.stage.bind("<Button-1>", lambda e: setattr(self, "_x", e.x))
        col = tk.Frame(f, bg=K)
        col.place(x=53, rely=0.5, y=-20, anchor="w", width=320)
        self.big = label(col, "", fg="#FFFFFF", size=32, bold=True, bg=K)
        self.big.pack(anchor="w", pady=(0, 22))
        self.vbox = tk.Frame(col, bg="#111112")
        self.vbox.pack(fill="x", ipady=3)
        self.vrows: dict[str, tk.Label] = {}
        for v in HM.VERSIONS:
            r = tk.Label(self.vbox, text=v, bg="#111112", fg=MUTED, font=font(10), anchor="w", padx=16, pady=8, cursor="hand2")
            r.pack(fill="x", padx=4, pady=1)
            r.bind("<Button-1>", lambda e, v=v: self.pick(v))
            self.vrows[v] = r
        self.play = RButton(col, "▶   Играть", self.on_play, width=320, height=54, bg=K, light=True)
        self.play.pack(pady=(14, 8))
        self.msg = label(col, "", fg=MUTED, size=9, bg=K, wrap=320)
        self.msg.pack(anchor="w")
        row = tk.Frame(col, bg=K)
        row.pack(anchor="w", pady=(14, 0))
        self.btn_conn = RButton(row, "CONNECT", self.app.black_connect, width=120, height=30, bg=K, small=True)
        self.btn_conn.pack(side="left", padx=(0, 8))
        RButton(row, "Диагностика", self.app.black_diagnose, width=120, height=30, bg=K, small=True).pack(side="left")

    def pick(self, v: str) -> None:
        self.sel = v
        self.app.L.settings.set("home_version", v)
        self.render_home()

    def render_home(self) -> None:
        l = self.link
        name = l.player or self.app.L.settings["player_name"] or "Игрок"
        self.big.configure(text=name)
        self.nick.configure(text=name)
        for v, r in self.vrows.items():
            on = v == self.sel
            avail = v in HM.BUILT_FOR
            r.configure(text=v if avail else f"{v}   · сборки нет", bg="#262628" if on else "#111112",
                        fg=TEXT if on else MUTED, font=font(10, on))
        self.play.set_text("▶   Играть")
        self.msg.configure(text=HM.status_line(l, self.sel), fg=OK if l.connected and not l.lost else (BAD if l.lost or l.error else MUTED))
        self.conn.configure(text="● подключено" if l.connected else ("○ нет соединения" if l.lost else "○ не подключено"), fg=OK if l.connected else MUTED)
        self.btn_conn.set_text("ПЕРЕПОДКЛЮЧИТЬ" if l.connected or l.lost else "CONNECT")
        self.paint_head()
        if name != getattr(self, "_skin_for", None) and (l.connected or self.app.L.settings["player_name"]):
            self._skin_for = name
            self.load_skin_for(name if name != "Игрок" else "")

    def on_play(self) -> None:
        kind, val = HM.play_action(self.link, self.sel)
        if kind == "focus":
            ok, text = focus_pid(int(val))
            self.msg.configure(text=text, fg=OK if ok else WARN)
        else:
            self.msg.configure(text=val, fg=WARN)

    # ------------------------------------------------------------------ mods
    def _build_mods(self, f: tk.Frame) -> None:
        top = tk.Frame(f, bg=K)
        top.pack(fill="x", padx=28, pady=(14, 6))
        self.mod_count = label(top, "", fg=MUTED, size=10, bg=K)
        self.mod_count.pack(side="left")
        self.mq = tk.StringVar()
        e = entry(top, self.mq, 26)
        e.pack(side="right")
        self.mq.trace_add("write", lambda *a: self.draw_mods())
        wrap = tk.Frame(f, bg=K)
        wrap.pack(fill="both", expand=True, padx=28, pady=(0, 14))
        self.mc = tk.Canvas(wrap, bg=K, highlightthickness=0)
        sb = tk.Scrollbar(wrap, command=self.mc.yview)
        self.mc.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        self.mc.pack(side="left", fill="both", expand=True)
        self.mi = tk.Frame(self.mc, bg=K)
        self.mc.create_window(0, 0, window=self.mi, anchor="nw", tags="in")
        self.mi.bind("<Configure>", lambda e: self.mc.configure(scrollregion=self.mc.bbox("all")))
        self.mc.bind("<Configure>", lambda e: self.mc.itemconfigure("in", width=e.width))
        self.mc.bind("<MouseWheel>", lambda e: self.mc.yview_scroll(-1 * (e.delta // 120), "units"))
        self.mods: list[dict] = []
        self.mod_err = ""

    def load_mods(self) -> None:
        if not self.link.connected:
            self.mods, self.mod_err = [], "Нет подключения к игре — модули читаются у запущенного мода."
            self.draw_mods()
            return
        self.app.run_bg(self.app.L.loader.modules, self._mods_done)

    def _mods_done(self, r, err) -> None:
        self.mods, self.mod_err = (r or []), (str(err) if err else "")
        self.draw_mods()

    def draw_mods(self) -> None:
        for w in self.mi.winfo_children():
            w.destroy()
        self.rows.clear()
        q = self.mq.get().strip().lower()
        self.mod_count.configure(text=f"Модулей в запущенном моде: {len(self.mods)}" if self.mods else "")
        if self.mod_err or not self.mods:
            label(self.mi, self.mod_err or "Список пуст.", fg=MUTED, bg=K, wrap=600).pack(anchor="w", pady=20)
            return
        cat = None
        for m in sorted(self.mods, key=lambda m: (str(m.get("category", "")), str(m.get("name", m["id"])))):
            name = str(m.get("name", m["id"]))
            if q and q not in name.lower() and q not in str(m["id"]).lower():
                continue
            if m.get("category") != cat:
                cat = m.get("category")
                label(self.mi, str(cat or "Прочее").upper(), fg=VIOLET_L, size=9, bold=True, bg=K).pack(anchor="w", pady=(12, 2))
            r = tk.Frame(self.mi, bg=PANEL, highlightbackground=LINE, highlightthickness=1)
            r.pack(fill="x", pady=2)
            label(r, name, bg=PANEL, size=10, bold=True).pack(side="left", padx=12, pady=8)
            st = label(r, "", fg=MUTED, size=8, bg=PANEL)
            st.pack(side="right", padx=8)
            t = Toggle(r, bool(m.get("enabled")), lambda v, mid=m["id"], st=st: self.flip(mid, v, st), bg=PANEL)
            t.pack(side="right", padx=8)
            self.rows[m["id"]] = t

    def flip(self, mid: str, v: bool, st: tk.Label) -> None:
        st.configure(text="…", fg=WARN)
        def done(r, err):
            ok = bool(r) and not err
            st.configure(text="" if ok else "игра не подтвердила", fg=MUTED if ok else BAD)
            if not ok and mid in self.rows:
                self.rows[mid].set(not v, False)
        self.app.run_bg(lambda: self.app.L.loader.toggle(mid, v), done)

    # ------------------------------------------------------------- assistant
    def _quota_text(self) -> str:
        n = self.store.remaining()
        s = f"Осталось {n} из {DAILY} на сегодня · локальный счётчик"
        return s if n else f"Лимит исчерпан. Обновится {self.store.next_reset()}"

    def _build_bot(self, f: tk.Frame) -> None:
        side = tk.Frame(f, bg=PANEL, width=190)
        side.pack(side="left", fill="y", padx=(28, 10), pady=14)
        side.pack_propagate(False)
        RButton(side, "+ Новый чат", self.new_chat, primary=True, width=170, height=34, bg=PANEL, small=True).pack(pady=(10, 6))
        self.hist = tk.Frame(side, bg=PANEL)
        self.hist.pack(fill="both", expand=True)
        main = tk.Frame(f, bg=K)
        main.pack(side="left", fill="both", expand=True, padx=(0, 28), pady=14)
        self.log = tk.Text(main, bg=K, fg=TEXT, relief="flat", wrap="word", font=font(10), state="disabled", highlightthickness=0)
        self.log.pack(fill="both", expand=True)
        self.log.tag_configure("me", foreground=VIOLET_L)
        self.log.tag_configure("bot", foreground=TEXT)
        self.log.tag_configure("bad", foreground=BAD)
        self.log.tag_configure("ok", foreground=OK)
        self.card = tk.Frame(main, bg=PANEL, highlightbackground=VIOLET, highlightthickness=1)
        self.card_t = label(self.card, "", bg=PANEL, wrap=560)
        self.card_t.pack(anchor="w", padx=12, pady=(10, 6))
        cr = tk.Frame(self.card, bg=PANEL)
        cr.pack(anchor="w", padx=12, pady=(0, 10))
        RButton(cr, "Принять", lambda: self.ask("да"), primary=True, width=120, height=30, bg=PANEL, small=True).pack(side="left", padx=(0, 8))
        RButton(cr, "Отклонить", lambda: self.ask("нет"), width=120, height=30, bg=PANEL, small=True).pack(side="left")
        self.q_lbl = label(main, "", fg=MUTED, size=8, bg=K)
        self.q_lbl.pack(anchor="w", pady=(4, 0))
        row = tk.Frame(main, bg=K)
        row.pack(fill="x", pady=(4, 0))
        self.bv = tk.StringVar()
        e = entry(row, self.bv, 60)
        e.pack(side="left", fill="x", expand=True, ipady=5)
        e.bind("<Return>", lambda ev: self.send())
        RButton(row, "Отправить", self.send, primary=True, width=110, height=32, bg=K, small=True).pack(side="left", padx=(8, 0))
        self.draw_chat()

    def draw_chat(self) -> None:
        self.log.configure(state="normal")
        self.log.delete("1.0", "end")
        for m in self.chat["msgs"]:
            self.log.insert("end", ("› " if m["role"] == "me" else "") + m["text"] + "\n\n", m["role"])
        if not self.chat["msgs"]:
            self.log.insert("end", "Помощник RIMVISUALS. Скажите, что включить или настроить.\n", "bot")
        self.log.see("end")
        self.log.configure(state="disabled")
        for w in self.hist.winfo_children():
            w.destroy()
        for c in self.store.chats()[:12]:
            b = tk.Label(self.hist, text=c["title"], bg=PANEL2 if c is self.chat else PANEL, fg=TEXT if c is self.chat else MUTED,
                         font=font(9), anchor="w", padx=8, pady=5, cursor="hand2")
            b.pack(fill="x", pady=1)
            b.bind("<Button-1>", lambda e, c=c: self.open_chat(c))
        self.q_lbl.configure(text=self._quota_text())

    def open_chat(self, c) -> None:
        self.chat = c
        self.card.pack_forget()
        self.draw_chat()

    def new_chat(self) -> None:
        self.chat = self.store.new_chat()
        self.card.pack_forget()
        self.draw_chat()

    def send(self) -> None:
        t = self.bv.get().strip()
        if t:
            self.bv.set("")
            self.ask(t)

    def ask(self, text: str) -> None:
        import uuid
        rid = uuid.uuid4().hex
        is_answer = text.lower() in ("да", "нет")
        if not is_answer and not self.store.charge(rid):
            self.store.add(self.chat, "bad", f"Дневной лимит запросов исчерпан. Обновится {self.store.next_reset()}.")
            self.draw_chat()
            return
        self.store.add(self.chat, "me", text)
        self.draw_chat()
        def done(r, err):
            if err:
                self.store.add(self.chat, "bad", str(err))
                self.card.pack_forget()
            else:
                self.store.add(self.chat, "ok" if r.executed else ("bad" if not r.ok else "bot"), r.text)
                if r.pending:
                    self.card_t.configure(text=r.text)
                    self.card.pack(before=self.q_lbl, fill="x", pady=(6, 0))
                else:
                    self.card.pack_forget()
            self.draw_chat()
        self.app.run_bg(lambda: self.app.L.jarvis().ask(text), done)

    # ------------------------------------------------------------------ skin
    def _build_skin(self, f: tk.Frame) -> None:
        left = tk.Frame(f, bg=K)
        left.pack(side="left", fill="y", padx=(28, 10), pady=14)
        label(left, "Ник игрока", fg=MUTED, size=9, bg=K).pack(anchor="w", pady=(8, 2))
        self.nv = tk.StringVar(value=self.app.L.settings["player_name"])
        entry(left, self.nv, 22).pack(anchor="w", ipady=4)
        RButton(left, "Показать скин", lambda: self.load_skin_for(self.nv.get().strip(), save=True), primary=True, width=160, height=34, bg=K, small=True).pack(anchor="w", pady=10)
        self.skin_msg = label(left, "Скин берётся из публичного API Mojang по нику. Аккаунт и пароль не нужны.", fg=MUTED, size=9, bg=K, wrap=210)
        self.skin_msg.pack(anchor="w")
        self.sc = tk.Canvas(f, bg=K, highlightthickness=0, width=420, height=460)
        self.sc.pack(side="left", expand=True, fill="both", pady=14)
        self.sc.bind("<B1-Motion>", self._drag)
        self.sc.bind("<Button-1>", lambda e: setattr(self, "_x", e.x))
        self.draw_skin()

    def _drag(self, e) -> None:
        self.yaw += (e.x - getattr(self, "_x", e.x)) * 0.012
        self._x = e.x
        self.draw_skin()

    def draw_skin(self) -> None:
        if hasattr(self, "sc"):
            self.paint(self.sc)
        self.paint(self.stage, True)
        self.paint_head()

    def paint(self, c: tk.Canvas, stars: bool = False) -> None:
        c.delete("all")
        w, h = max(c.winfo_width(), 400), max(c.winfo_height(), 400)
        if stars:
            import random
            rnd = random.Random(7)
            for _ in range(26):
                x, y, g = rnd.randrange(w), rnd.randrange(h), rnd.choice(("#2a2a2e", "#3a3a40", "#555560"))
                c.create_oval(x, y, x + 2, y + 2, fill=g, outline="")
        k = min(w / 545, h / 600) * (7.1 if stars else 5.3)
        for pts, col in scene(self.skin, yaw=self.yaw, scale=k, cx=w * (0.54 if stars else 0.5), cy=h * (0.505 if stars else 0.5) - (0 if stars else 10.3 * k - 30)):
            c.create_polygon(*[v for p in pts for v in p], fill=col, outline=col)

    def paint_head(self) -> None:
        c = self.head
        c.delete("all")
        for y in range(8):
            for x in range(8):
                px = self.skin.at(8 + x, 8 + y)
                c.create_rectangle(1 + x * 3, 1 + y * 3, 4 + x * 3, 4 + y * 3, fill="#%02x%02x%02x" % tuple(px[:3]), outline="")

    def load_skin_for(self, name: str, save: bool = False) -> None:
        if not name:
            self.skin = default_skin()
            self.skin_msg.configure(text="Ник не указан — показан стандартный персонаж.", fg=MUTED)
            self.draw_skin()
            return
        if save:
            try:
                self.app.L.settings.set("player_name", name)
            except Exception:
                pass
        self.skin_msg.configure(text="Загрузка…", fg=WARN)
        def done(r, err):
            if err:
                self.skin = default_skin()
                self.skin_msg.configure(text=f"{err}. Показан стандартный персонаж.", fg=BAD)
            else:
                self.skin = r
                self.skin_msg.configure(text=f"Скин игрока {name}.", fg=OK)
            self.draw_skin()
        self.app.run_bg(lambda: load_skin(fetch_skin(name)), done)
