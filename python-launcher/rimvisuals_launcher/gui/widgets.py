"""Canvas-drawn black/violet widgets with small animations (standard-library tkinter only)."""
from __future__ import annotations

import math
import time
import tkinter as tk
from typing import Callable

BG = "#0A0710"
PANEL = "#120C1D"
PANEL2 = "#1A1129"
LINE = "#2A1D40"
TEXT = "#F3EEFF"
MUTED = "#9A8DB5"
VIOLET = "#8B5CF6"
VIOLET_D = "#6D3FE0"
VIOLET_L = "#B79CFF"
OK = "#4ADE80"
WARN = "#FBBF24"
BAD = "#F87171"
FONT = "Segoe UI"


def font(size: int = 10, bold: bool = False):
    return (FONT, size, "bold" if bold else "normal")


def _rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def mix(a: str, b: str, t: float) -> str:
    t = max(0.0, min(1.0, t))
    ra, rb = _rgb(a), _rgb(b)
    return "#%02x%02x%02x" % tuple(round(x + (y - x) * t) for x, y in zip(ra, rb))


def ease(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def round_rect(c: tk.Canvas, x1, y1, x2, y2, r, **kw):
    pts = [x1 + r, y1, x2 - r, y1, x2, y1, x2, y1 + r, x2, y2 - r, x2, y2, x2 - r, y2, x1 + r, y2, x1, y2, x1, y2 - r, x1, y1 + r, x1, y1]
    return c.create_polygon(pts, smooth=True, **kw)


class Anim:
    """Drive a 0..1 value toward a target with easing; calls `apply(v)` each frame."""

    def __init__(self, widget: tk.Misc, apply: Callable[[float], None], dur: float = 0.18):
        self.w, self.apply, self.dur = widget, apply, dur
        self.v = 0.0
        self._from = self._to = 0.0
        self._t0 = 0.0
        self._job = None

    def go(self, target: float) -> None:
        self._from, self._to, self._t0 = self.v, target, time.monotonic()
        if self._job is None:
            self._tick()

    def _tick(self) -> None:
        try:
            if not self.w.winfo_exists():
                return
        except tk.TclError:
            return
        k = (time.monotonic() - self._t0) / self.dur
        self.v = self._from + (self._to - self._from) * ease(k)
        self.apply(self.v)
        self._job = self.w.after(16, self._tick) if k < 1 else None
        if k >= 1:
            self.v = self._to
            self.apply(self.v)


class RButton(tk.Canvas):
    def __init__(self, master, text: str, command: Callable[[], None], *, primary=False, width=150, height=38,
                 bg=BG, small=False):
        super().__init__(master, width=width, height=height, bg=bg, highlightthickness=0, cursor="hand2")
        self.text, self.command, self.primary, self.w_, self.h_ = text, command, primary, width, height
        self.enabled = True
        self.small = small
        self.hover = Anim(self, lambda v: self._draw(), 0.14)
        self._draw()
        self.bind("<Enter>", lambda e: self.enabled and self.hover.go(1))
        self.bind("<Leave>", lambda e: self.hover.go(0))
        self.bind("<Button-1>", lambda e: self.enabled and self.command())

    def set_text(self, t: str) -> None:
        self.text = t
        self._draw()

    def set_enabled(self, on: bool) -> None:
        self.enabled = on
        self.configure(cursor="hand2" if on else "arrow")
        self._draw()

    def _draw(self) -> None:
        self.delete("all")
        v = self.hover.v
        if not self.enabled:
            fill, fg, out = PANEL2, MUTED, LINE
        elif self.primary:
            fill, fg, out = mix(VIOLET_D, VIOLET, 0.35 + 0.65 * v), "#FFFFFF", mix(VIOLET, VIOLET_L, v)
        else:
            fill, fg, out = mix(PANEL2, "#2A1B45", v), TEXT, mix(LINE, VIOLET, v)
        round_rect(self, 1, 1, self.w_ - 1, self.h_ - 1, 12, fill=fill, outline=out, width=1)
        self.create_text(self.w_ / 2, self.h_ / 2, text=self.text, fill=fg, font=font(9 if self.small else 10, True))


class Toggle(tk.Canvas):
    def __init__(self, master, value: bool, command: Callable[[bool], None], bg=PANEL):
        super().__init__(master, width=46, height=24, bg=bg, highlightthickness=0, cursor="hand2")
        self.value, self.command = bool(value), command
        self.anim = Anim(self, lambda v: self._draw(), 0.16)
        self.anim.v = 1.0 if self.value else 0.0
        self._draw()
        self.bind("<Button-1>", lambda e: self.set(not self.value, True))

    def set(self, v: bool, fire=False) -> None:
        self.value = bool(v)
        self.anim.go(1.0 if v else 0.0)
        if fire:
            self.command(self.value)

    def _draw(self) -> None:
        self.delete("all")
        v = self.anim.v
        round_rect(self, 1, 1, 45, 23, 11, fill=mix(LINE, VIOLET, v), outline="")
        x = 4 + 22 * v
        self.create_oval(x, 4, x + 16, 20, fill=mix(MUTED, "#FFFFFF", v), outline="")


class Slider(tk.Canvas):
    def __init__(self, master, lo, hi, value, command: Callable[[int], None], *, step=1, width=260, bg=PANEL, fmt="{}"):
        super().__init__(master, width=width + 70, height=26, bg=bg, highlightthickness=0, cursor="hand2")
        self.lo, self.hi, self.step, self.w_, self.command, self.fmt = lo, hi, step, width, command, fmt
        self.value = value
        self._draw()
        self.bind("<Button-1>", self._drag)
        self.bind("<B1-Motion>", self._drag)
        self.bind("<ButtonRelease-1>", lambda e: self.command(self.value))

    def set(self, v) -> None:
        self.value = v
        self._draw()

    def _drag(self, e) -> None:
        f = max(0.0, min(1.0, (e.x - 8) / (self.w_ - 16)))
        v = self.lo + f * (self.hi - self.lo)
        self.value = int(round(v / self.step) * self.step)
        self._draw()

    def _draw(self) -> None:
        self.delete("all")
        f = (self.value - self.lo) / max(1, self.hi - self.lo)
        x = 8 + f * (self.w_ - 16)
        self.create_line(8, 13, self.w_ - 8, 13, fill=LINE, width=4, capstyle="round")
        self.create_line(8, 13, x, 13, fill=VIOLET, width=4, capstyle="round")
        self.create_oval(x - 8, 5, x + 8, 21, fill="#FFFFFF", outline=VIOLET, width=2)
        self.create_text(self.w_ + 8, 13, text=self.fmt.format(self.value), fill=TEXT, font=font(9, True), anchor="w")


class ProgressBar(tk.Canvas):
    def __init__(self, master, width=420, height=10, bg=BG):
        super().__init__(master, width=width, height=height, bg=bg, highlightthickness=0)
        self.w_, self.h_ = width, height
        self.target = 0.0
        self.shown = 0.0
        self.busy = False
        self.phase = 0.0
        self._loop()

    def set(self, f: float) -> None:
        self.target = max(0.0, min(1.0, f))

    def _loop(self) -> None:
        try:
            if not self.winfo_exists():
                return
        except tk.TclError:
            return
        self.shown += (self.target - self.shown) * 0.2
        self.phase = (self.phase + 0.03) % 1.0
        self.delete("all")
        round_rect(self, 0, 0, self.w_, self.h_, self.h_ // 2, fill=PANEL2, outline="")
        if self.shown > 0.005:
            wv = max(self.h_, self.shown * self.w_)
            round_rect(self, 0, 0, wv, self.h_, self.h_ // 2, fill=VIOLET, outline="")
            sx = (self.phase * 1.4 - 0.2) * wv
            self.create_rectangle(max(0, sx), 1, min(wv - 3, sx + 40), self.h_ - 1, fill=VIOLET_L, outline="", stipple="gray50")
        self.after(33, self._loop)


class Logo(tk.Canvas):
    """RIMVISUALS mark: a violet faceted R-shard with a soft glow ring, drawn with canvas primitives."""

    def __init__(self, master, size=56, bg=BG, text=True):
        w = size + (170 if text else 0)
        super().__init__(master, width=w, height=size, bg=bg, highlightthickness=0)
        s = size / 56
        self.create_oval(2 * s, 2 * s, 54 * s, 54 * s, outline=LINE, width=2)
        pts = [(18, 12), (36, 12), (44, 20), (44, 28), (38, 33), (46, 44), (36, 44), (29, 35), (25, 35), (25, 44), (18, 44)]
        self.create_polygon([c * s for p in pts for c in p], fill=VIOLET, outline=VIOLET_L, width=1)
        self.create_polygon([c * s for c in (25, 19, 34, 19, 37, 22, 37, 25, 34, 28, 25, 28)], fill=BG, outline="")
        if text:
            self.create_text(size + 12, size * 0.38, text="RIMVISUALS", anchor="w", fill=TEXT, font=font(13, True))
            self.create_text(size + 12, size * 0.72, text="visual client", anchor="w", fill=MUTED, font=font(8))


class PlayButton(tk.Canvas):
    """Big INJECT / PLAY button with a breathing glow ring."""

    def __init__(self, master, command: Callable[[], None], bg=BG, size=190):
        super().__init__(master, width=size, height=size, bg=bg, highlightthickness=0, cursor="hand2")
        self.size, self.command, self.text, self.sub = size, command, "INJECT", "PLAY"
        self.busy = False
        self.enabled = True
        self.hover = Anim(self, lambda v: None, 0.2)
        self.t0 = time.monotonic()
        self.bind("<Button-1>", lambda e: self.enabled and not self.busy and self.command())
        self.bind("<Enter>", lambda e: self.hover.go(1))
        self.bind("<Leave>", lambda e: self.hover.go(0))
        self._loop()

    def _loop(self) -> None:
        try:
            if not self.winfo_exists():
                return
        except tk.TclError:
            return
        t = time.monotonic() - self.t0
        c, s = self.size / 2, self.size
        self.delete("all")
        h = self.hover.v
        pulse = 0.5 + 0.5 * math.sin(t * (4.0 if self.busy else 1.6))
        for i in range(5, 0, -1):
            r = s * 0.36 + i * (5 + 4 * pulse)
            self.create_oval(c - r, c - r, c + r, c + r, outline=mix(BG, VIOLET, (0.5 - i * 0.08) * (0.5 + 0.5 * pulse) + 0.1 * h), width=2)
        r = s * 0.36
        fill = mix(VIOLET_D, VIOLET, 0.5 + 0.5 * h) if self.enabled else PANEL2
        self.create_oval(c - r, c - r, c + r, c + r, fill=fill, outline=mix(VIOLET, VIOLET_L, h), width=3)
        if self.busy:
            a = (t * 260) % 360
            self.create_arc(c - r + 8, c - r + 8, c + r - 8, c + r - 8, start=a, extent=110, style="arc", outline="#FFFFFF", width=4)
        self.create_text(c, c - 9, text=self.text, fill="#FFFFFF", font=font(19, True))
        self.create_text(c, c + 17, text=self.sub, fill=VIOLET_L if self.enabled else MUTED, font=font(9, True))
        self.after(33, self._loop)


class Card(tk.Frame):
    def __init__(self, master, title: str = "", **kw):
        super().__init__(master, bg=PANEL, highlightbackground=LINE, highlightthickness=1, **kw)
        if title:
            tk.Label(self, text=title, bg=PANEL, fg=VIOLET_L, font=font(10, True), anchor="w").pack(fill="x", padx=16, pady=(12, 4))


def label(master, text, *, fg=TEXT, size=10, bold=False, bg=None, anchor="w", wrap=0, **kw):
    return tk.Label(master, text=text, fg=fg, bg=bg or master.cget("bg"), font=font(size, bold), anchor=anchor,
                    justify="left", wraplength=wrap, **kw)


def entry(master, var: tk.StringVar, width=34, show=""):
    e = tk.Entry(master, textvariable=var, width=width, bg=PANEL2, fg=TEXT, insertbackground=VIOLET_L, relief="flat",
                 highlightthickness=1, highlightbackground=LINE, highlightcolor=VIOLET, font=font(10), show=show)
    return e
