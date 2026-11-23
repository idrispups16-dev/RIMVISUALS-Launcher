"""Command line for CI and support: everything the GUI does, without a window."""
from __future__ import annotations

import argparse
import sys

from .core import CancelToken
from .service import Launcher


def main(argv: list[str]) -> int:
    for stream in (sys.stdout, sys.stderr):      # Windows consoles default to cp1252; our messages are Russian
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, OSError, ValueError):
            pass
    ap = argparse.ArgumentParser(prog="RIMVISUALS", description="RIMVISUALS launcher")
    ap.add_argument("--home", help="data folder (default: %%APPDATA%%\\RIMVISUALS)")
    sub = ap.add_subparsers(dest="cmd")
    sub.add_parser("diagnose")
    sub.add_parser("install", help="install Java/Minecraft/Fabric/mod, do not start the game")
    pl = sub.add_parser("play", help="install if needed and start Minecraft with RIMVISUALS")
    pl.add_argument("--probe", type=int, metavar="SECONDS", help="CI: start the game, wait, report whether Fabric and the mod loaded, then stop it")
    ja = sub.add_parser("jarvis", help="run one RIM JARVIS command against the running game")
    ja.add_argument("text")
    sub.add_parser("loader-scan", help="find running Minecraft clients and print their Loader state as JSON")
    lc = sub.add_parser("loader-connect", help="CONNECT to the first running Minecraft over the IPC; with --install also install the JAR first")
    lc.add_argument("--install", action="store_true")
    ld = sub.add_parser("loader-diagnose", help="step-by-step IPC diagnosis of the first running Minecraft (or --game-dir)")
    ld.add_argument("--game-dir")
    ji = sub.add_parser("jar-info", help="which RIMVISUALS JAR this build uses and whether it is compatible (no manual search needed)")
    ji.add_argument("--report", help="write the JSON here (the windowed EXE has no console)")
    sub.add_parser("verify", help="full SHA re-check of game files and the mod")
    sub.add_parser("report")
    gs = sub.add_parser("gui-selftest", help="build every GUI page, optionally save screenshots")
    gs.add_argument("--shots", help="folder for PNG screenshots (needs Pillow)")
    gs.add_argument("--report", help="write the result to this file (the windowed EXE has no console)")
    a = ap.parse_args(argv)
    if a.cmd == "gui-selftest":
        from .gui.app import selftest
        import io, contextlib
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = selftest(a.home, a.shots)
        if a.report:
            open(a.report, "w", encoding="utf-8").write(buf.getvalue())
        else:
            print(buf.getvalue(), end="")
        return code
    if a.cmd == "jar-info":
        import json
        from .modmgr import find_bundled_jar
        try:
            j = find_bundled_jar()
            res = {"source": "bundled", "file": j.path.name, "version": j.version, "sha256": j.sha256, "minecraft": j.minecraft,
                   "mod_id": j.mod_id, "compatible": j.targets_current, "path": str(j.path)} if j else {"source": None, "compatible": False}
        except ValueError as e:
            res = {"source": "bundled", "compatible": False, "error": str(e)}
        text = json.dumps(res, ensure_ascii=False, indent=2)
        open(a.report, "w", encoding="utf-8").write(text) if a.report else print(text)
        return 0 if res.get("compatible") else 4
    L = Launcher(a.home and __import__("pathlib").Path(a.home))

    def prog(s: str, f: float) -> None:
        print(f"[{int(f * 100):3d}%] {s}", flush=True)

    if a.cmd == "diagnose":
        for c in L.diagnose():
            print(("OK " if c.ok else "!! ") + c.title + " — " + c.detail)
        return 0
    if a.cmd == "loader-diagnose":
        from .ipcdiag import diagnose, format_steps
        gd = __import__("pathlib").Path(a.game_dir) if a.game_dir else next((sc.instance.game_dir for sc in L.loader.scan() if sc.instance), None)
        steps = diagnose(gd)
        print(format_steps(steps))
        return 0 if steps and all(x.ok for x in steps) else 3
    if a.cmd in ("loader-scan", "loader-connect"):
        import json
        scans = L.loader.scan()
        if a.cmd == "loader-scan":
            print(json.dumps([{"state": sc.state.value, "text": sc.text, "restart": sc.restart, "jar_in_mods": sc.jar_in_mods, "mod_loaded": sc.mod_loaded,
                               **({"pid": sc.instance.pid, "game_dir": str(sc.instance.game_dir), "mc_version": sc.instance.mc_version,
                                   "fabric": sc.instance.fabric, "fabric_loader": sc.instance.fabric_loader} if sc.instance else {})} for sc in scans],
                             ensure_ascii=False, indent=2))
            return 0
        inst = next((sc.instance for sc in scans if sc.instance), None)
        if not inst:
            print("Minecraft не найден")
            return 2
        if a.install:
            print(json.dumps(L.loader.install(inst, CancelToken()), ensure_ascii=False))
        r = L.loader.connect(inst)
        print(json.dumps(r, ensure_ascii=False))
        return 0 if r["ok"] else 3
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
        if r["status"] == "started" and a.probe:
            import time
            end = time.time() + a.probe
            while time.time() < end and L.run.exit_code is None and not (L.run.fabric_seen and L.run.mod_seen):
                time.sleep(0.5)
            time.sleep(1.0)
            print(f"fabric_loaded={L.run.fabric_seen} rimvisuals_listed={L.run.mod_seen} exit_code={L.run.exit_code}")
            print("--- game output tail ---\n" + "\n".join(L.run.tail[-40:]))
            L.stop()
            return 0 if (L.run.fabric_seen and L.run.mod_seen) else 3
        if r["status"] == "started":
            L.run.proc.wait()
            print(L.crash_hint() or "exited")
        return 0 if r["status"] in ("started", "already") else 2
    if a.cmd == "jarvis":
        rep = L.jarvis().ask(a.text)
        print(rep.text)
        return 0 if rep.ok else 1
    if a.cmd == "report":
        print(L.report())
        return 0
    from .gui.app import run
    return run(a.home)
