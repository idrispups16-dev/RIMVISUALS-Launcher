"""Command line for CI and support: everything the GUI does, without a window."""
from __future__ import annotations

import argparse
import sys

from .core import CancelToken
from .service import Launcher


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(prog="RIMVISUALS", description="RIMVISUALS launcher")
    ap.add_argument("--home", help="data folder (default: %%APPDATA%%\\RIMVISUALS)")
    sub = ap.add_subparsers(dest="cmd")
    sub.add_parser("diagnose")
    sub.add_parser("install", help="install Java/Minecraft/Fabric/mod, do not start the game")
    sub.add_parser("play", help="install if needed and start Minecraft with RIMVISUALS")
    sub.add_parser("verify", help="full SHA re-check of game files and the mod")
    sub.add_parser("report")
    gs = sub.add_parser("gui-selftest", help="build every GUI page, optionally save screenshots")
    gs.add_argument("--shots", help="folder for PNG screenshots (needs Pillow)")
    a = ap.parse_args(argv)
    if a.cmd == "gui-selftest":
        from .gui.app import selftest
        return selftest(a.home, a.shots)
    L = Launcher(a.home and __import__("pathlib").Path(a.home))

    def prog(s: str, f: float) -> None:
        print(f"[{int(f * 100):3d}%] {s}", flush=True)

    if a.cmd == "diagnose":
        for c in L.diagnose():
            print(("OK " if c.ok else "!! ") + c.title + " — " + c.detail)
        return 0
    if a.cmd == "install":
        L.prepare(prog, CancelToken())
        return 0
    if a.cmd == "verify":
        probs = L.installer.verify(True) + L.mods.verify(L.profiles.game_dir(L.pid))
        print("\n".join(probs) or "all files match")
        return 1 if probs else 0
    if a.cmd == "play":
        r = L.inject(prog, CancelToken())
        print(r["message"])
        if r["status"] == "started":
            L.run.proc.wait()
            print(L.crash_hint() or "exited")
        return 0 if r["status"] in ("started", "already") else 2
    if a.cmd == "report":
        print(L.report())
        return 0
    from .gui.app import run
    return run(a.home)
