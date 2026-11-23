import hashlib, io, json, socket, threading, zipfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import pytest

from rimvisuals_launcher.jarvis import modtools, voice
from rimvisuals_launcher.jarvis.ai import AiClient
from rimvisuals_launcher.jarvis.assistant import Assistant
from rimvisuals_launcher.jarvis.intents import parse
from rimvisuals_launcher.jarvis.ipc import BridgeUnavailable, JarvisClient
from rimvisuals_launcher.minecraft import Endpoints
from rimvisuals_launcher.core import Paths


# --------------------------------------------------------------------------- fake game (same wire protocol as JarvisServer.java)
class FakeGame:
    """Newline-JSON loopback server with the same allowlist/confirmation rules as the mod; keeps real state we can assert on."""
    def __init__(self, game_dir: Path, token="t" * 64):
        self.token = token
        self.mods = {"crystal_fade": {"enabled": False, "name": "Crystal Fade", "settings": [{"key": "crystal_opacity", "type": "number", "value": 1.0}]},
                     "hit_particles": {"enabled": True, "name": "Hit Particles", "settings": [{"key": "color", "type": "color", "value": "#FF3355"}]},
                     "crit_particles": {"enabled": True, "name": "Critical Particles", "settings": [{"key": "color", "type": "color", "value": "#FDE68A"}]},
                     "damage_particles": {"enabled": True, "name": "Damage Particles", "settings": [{"key": "color", "type": "color", "value": "#FFFFFF"}]},
                     "sharpness_particles": {"enabled": True, "name": "Sharpness Particles", "settings": [{"key": "color", "type": "color", "value": "#C4B5FD"}]}}
        self.preset, self.saved, self.log, self.fps = "Balanced", [], [], 38
        self.srv = socket.socket(); self.srv.bind(("127.0.0.1", 0)); self.srv.listen(2)
        d = game_dir / "config" / "rimvisuals"; d.mkdir(parents=True)
        (d / "jarvis-ipc.json").write_text(json.dumps({"schemaVersion": 1, "port": self.srv.getsockname()[1], "token": token, "pid": 1}))
        threading.Thread(target=self._accept, daemon=True).start()

    def close(self): self.srv.close()

    def _accept(self):
        while True:
            try: c, _ = self.srv.accept()
            except OSError: return
            threading.Thread(target=self._serve, args=(c,), daemon=True).start()

    def _serve(self, c):
        f = c.makefile("rw", encoding="utf-8", newline="\n")
        if json.loads(f.readline()).get("auth") != self.token:
            f.write('{"ok":false,"error":"auth"}\n'); f.flush(); c.close(); return
        f.write('{"ok":true,"hello":"rimvisuals-jarvis","protocol":1}\n'); f.flush()
        for line in f:
            f.write(json.dumps(self.handle(json.loads(line))) + "\n"); f.flush()

    def handle(self, r):
        a = r.get("action"); self.log.append(a)
        if a in ("apply_preset", "load_config", "reset_module") and not r.get("confirmed"):
            return {"ok": False, "error": "confirmation_required"}
        if a == "list_modules": return {"ok": True, "modules": [{"id": k, "name": v["name"]} for k, v in self.mods.items()]}
        if a == "get_module": m = self.mods.get(r["id"]); return {"ok": True, "settings": m["settings"]} if m else {"ok": False, "error": "no_module"}
        if a == "set_module":
            if r["module"] not in self.mods: return {"ok": False, "error": "no_module"}
            self.mods[r["module"]]["enabled"] = r["enabled"]; return {"ok": True}
        if a == "set_setting":
            for s in self.mods[r["module"]]["settings"]:
                if s["key"] == r["setting"]: s["value"] = r["value"]; return {"ok": True}
            return {"ok": False, "error": "no_setting"}
        if a == "performance_mode": self.preset = r["mode"]; return {"ok": True, "mode": r["mode"], "particleLimit": 8}
        if a == "save_config": self.saved.append(r["name"]); return {"ok": True, "name": r["name"]}
        if a == "apply_preset": self.preset = "preset:" + r["name"]; return {"ok": True, "name": r["name"]}
        if a == "open_gui": return {"ok": True}
        if a == "query" and r["what"] == "fps": return {"ok": True, "fps": self.fps, "targetFps": 60, "preset": self.preset, "heavyEnabled": ["hit_particles"], "topCpu": ["hit_particles 0.8 ms"], "monitorOn": True}
        if a == "query" and r["what"] == "cooldowns": return {"ok": True, "cooldowns": [{"text": "Trapka 12s", "progress": .4}]}
        if a == "query" and r["what"] == "events": return {"ok": True, "events": ["No received events"], "status": "API not requested"}
        return {"ok": False, "error": "unknown_action"}


@pytest.fixture()
def game(tmp_path):
    g = FakeGame(tmp_path); g.dir = tmp_path
    yield g
    g.close()


def jarvis(game, **kw): return Assistant(JarvisClient(game.dir), **kw)


# --------------------------------------------------------------------------- the user's own examples
def test_enable_crystal_fade_really_changes_state(game):
    r = jarvis(game).ask("Джарвис, включи Crystal Fade")
    assert r.executed and game.mods["crystal_fade"]["enabled"] is True and "Crystal Fade" in r.text

def test_purple_particles_sets_every_particle_colour(game):
    r = jarvis(game).ask("Джарвис, поставь фиолетовые частицы")
    assert r.executed
    for m in ("hit_particles", "crit_particles", "damage_particles", "sharpness_particles"):
        assert game.mods[m]["settings"][0]["value"] == "#8B5CF6"

def test_open_save_and_performance(game):
    a = jarvis(game)
    assert a.ask("Джарвис, открой ClickGUI").executed
    assert a.ask("Джарвис, сохрани конфиг PvP").executed and game.saved == ["PvP"]
    assert a.ask("Джарвис, включи Performance Mode").executed and game.preset == "Performance"

def test_preset_needs_confirmation_then_applies(game):
    a = jarvis(game)
    r = a.ask("примени пресет PvP Clean")
    assert r.pending and not r.executed and game.preset == "Balanced"
    assert a.ask("да").executed and game.preset == "preset:PvP_Clean"

def test_cancel_pending(game):
    a = jarvis(game); a.ask("примени пресет PvP Clean"); a.ask("нет")
    assert game.preset == "Balanced" and a.pending is None

def test_cooldown_events_and_fps_answers_use_game_data(game):
    a = jarvis(game)
    assert "12s" in a.ask("Джарвис, сколько осталось КД трапки?").text
    assert "нет полученных" in a.ask("Джарвис, какие сейчас ивенты на HolyWorld?").text.lower()
    r = a.ask("Джарвис, почему у меня упал FPS?")
    assert "38" in r.text and "hit_particles" in r.text and "Performance" in r.text

def test_game_closed_is_reported_not_faked(tmp_path):
    r = Assistant(JarvisClient(tmp_path)).ask("включи Crystal Fade")
    assert not r.ok and not r.executed and ("не запущен" in r.text)

def test_wrong_token_rejected(game, tmp_path):
    p = game.dir / "config/rimvisuals/jarvis-ipc.json"; d = json.loads(p.read_text()); d["token"] = "bad"; p.write_text(json.dumps(d))
    with pytest.raises(Exception): JarvisClient(game.dir).connect()

def test_unknown_game_refusal_is_explained(game):
    r = jarvis(game).ask("включи фулбрайт")
    assert not r.executed        # fake game has no such module: refusal is surfaced, never reported as success

def test_unknown_phrase_without_ai(game):
    r = jarvis(game).ask("расскажи анекдот"); assert not r.ok and "помощь" in r.text

def test_english_commands(game):
    a = jarvis(game); r = a.ask("jarvis, enable crystal fade")      # English input is understood, the answer is always Russian
    assert r.executed and "включён" in r.text


# --------------------------------------------------------------------------- parser
@pytest.mark.parametrize("text,kind,name", [
    ("Джарвис, включи Crystal Fade", "game", "enable"), ("выключи туман", "game", "disable"), ("Джарвис, открой ClickGUI", "game", "open_gui"),
    ("открой редактор HUD", "game", "open_gui"), ("сохрани конфиг PvP", "game", "save_config"), ("включи Performance Mode", "game", "performance"),
    ("режим баланс", "game", "performance"), ("почему упал fps", "game", "fps"), ("что умеешь", "local", "help"),
    ("установи Sodium", "mods", "install_mod"), ("найди конфликт модов", "mods", "conflicts"), ("объясни этот crash log", "mods", "crash"),
    ("создай PvP-сборку", "mods", "pvp_pack"), ("проверь обновления модов", "mods", "mod_updates"), ("проверь обновления", "local", "updates")])
def test_parser(text, kind, name):
    i = parse(text); assert (i.kind, i.name) == (kind, name)

def test_config_name_keeps_case():
    assert parse("Джарвис, сохрани конфиг PvP").params["config"] == "PvP"
    assert parse("save config Legit Rage").params["config"] == "Legit_Rage"


# --------------------------------------------------------------------------- mod manager (fake Modrinth)
def _jar(mod_id, version="1.0", depends=None, breaks=None):
    b = io.BytesIO()
    with zipfile.ZipFile(b, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": mod_id, "version": version, "depends": depends or {"minecraft": "~1.21.11"}, "breaks": breaks or {}}))
    return b.getvalue()

class Modrinth:
    def __init__(self):
        self.files = {"sodium.jar": _jar("sodium", "0.6"), "dep.jar": _jar("fabric-api-lib")}
        outer = self
        class H(BaseHTTPRequestHandler):
            def log_message(self, *a): pass
            def do_GET(self):
                b = f"http://127.0.0.1:{outer.srv.server_address[1]}"
                def ver(fn, vn, deps=()):
                    data = outer.files[fn]
                    return [{"name": vn, "version_number": vn, "version_type": "release", "dependencies": list(deps),
                             "files": [{"primary": True, "filename": fn, "url": f"{b}/f/{fn}", "size": len(data), "hashes": {"sha512": hashlib.sha512(data).hexdigest()}}]}]
                if self.path.startswith("/v2/search"): body = {"hits": [{"slug": "sodium", "title": "Sodium", "description": "x", "downloads": 9}]}
                elif "/project/sodium/version" in self.path: body = ver("sodium.jar", "0.6", [{"dependency_type": "required", "project_id": "depproj"}])
                elif "/project/depproj/version" in self.path: body = ver("dep.jar", "1.2")
                elif self.path.startswith("/f/"):
                    data = outer.files[self.path[3:]]; self.send_response(200); self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data); return
                else: self.send_response(404); self.end_headers(); return
                raw = json.dumps(body).encode(); self.send_response(200); self.send_header("Content-Length", str(len(raw))); self.end_headers(); self.wfile.write(raw)
        self.srv = ThreadingHTTPServer(("127.0.0.1", 0), H); threading.Thread(target=self.srv.serve_forever, daemon=True).start()
        self.ep = Endpoints(modrinth=f"http://127.0.0.1:{self.srv.server_address[1]}")

def test_plan_install_resolves_dependency_and_verifies_hash(tmp_path):
    m = Modrinth(); mods = tmp_path / "mods"
    plan = modtools.plan_install("sodium", mods, m.ep)
    assert [i.filename for i in plan.items] == ["sodium.jar", "dep.jar"] and plan.items[1].role == "dependency"
    paths = Paths(tmp_path / "RV"); paths.ensure()
    assert modtools.apply_plan(plan, mods, paths) == ["sodium.jar", "dep.jar"]
    assert (mods / "sodium.jar").read_bytes() == m.files["sodium.jar"]
    assert all(i.action == "keep" for i in modtools.plan_install("sodium", mods, m.ep).items)
    m.srv.shutdown()

def test_tampered_download_is_rejected(tmp_path):
    m = Modrinth(); mods = tmp_path / "mods"; plan = modtools.plan_install("sodium", mods, m.ep)
    m.files["sodium.jar"] = _jar("evil", "6.6")          # CDN serves different bytes than the hash promised
    paths = Paths(tmp_path / "RV"); paths.ensure()
    with pytest.raises(Exception): modtools.apply_plan(plan, mods, paths)
    assert not (mods / "sodium.jar").exists()
    m.srv.shutdown()

def test_assistant_install_flow_needs_yes(tmp_path, game):
    m = Modrinth()
    class Ops:
        PVP_PACK = ["sodium"]
        mods_dir = tmp_path / "mods"
        def plan(self, slugs): return modtools.plan_install(slugs[0], self.mods_dir, m.ep)
        def apply(self, plan):
            p = Paths(tmp_path / "RV"); p.ensure(); return modtools.apply_plan(plan, self.mods_dir, p)
    a = jarvis(game, mods=Ops())
    r = a.ask("Джарвис, установи Sodium")
    assert r.pending and "sodium.jar" in r.text and not (Ops.mods_dir / "sodium.jar").exists()      # shown first, nothing installed yet
    assert a.ask("да").executed and (Ops.mods_dir / "sodium.jar").exists()
    m.srv.shutdown()

def test_conflict_detection_and_remove_goes_to_backup(tmp_path):
    mods = tmp_path / "mods"; mods.mkdir()
    (mods / "a.jar").write_bytes(_jar("a", depends={"minecraft": "~1.21.11", "needed": "*"}))
    (mods / "b1.jar").write_bytes(_jar("b")); (mods / "b2.jar").write_bytes(_jar("b"))
    (mods / "c.jar").write_bytes(_jar("c", breaks={"a": "*"})); (mods / "junk.jar").write_bytes(b"not a zip")
    out = " | ".join(modtools.find_conflicts(mods))
    assert "needed" in out and "дубликат" in out and "несовместим с «a»" in out and "junk.jar" in out
    paths = Paths(tmp_path / "RV"); paths.ensure()
    dest = modtools.remove_mod("b2.jar", mods, paths)
    assert dest.exists() and not (mods / "b2.jar").exists()
    with pytest.raises(ValueError): modtools.remove_mod("../x.jar", mods, paths)

def test_crash_analysis():
    assert any("Java 21" in x for x in modtools.analyze_crash("java.lang.UnsupportedClassVersionError: class file version 65.0"))
    assert any("памяти" in x for x in modtools.analyze_crash("java.lang.OutOfMemoryError: Java heap space"))
    assert any("Mixin" in x for x in modtools.analyze_crash("Mixin apply failed foo.json:bar"))


# --------------------------------------------------------------------------- voice / AI
def test_voice_text_never_enters_script_and_non_windows_is_inert():
    calls = []
    v = voice.Voice(runner=lambda *a, **k: calls.append((a, k)) or type("R", (), {"returncode": 0, "stdout": "", "stderr": ""})(), platform="win32")
    evil = "\"; Remove-Item C:\\ -Recurse #"
    assert v.speak(evil, "ru", 70, 1)
    args, kw = calls[0]
    assert evil not in " ".join(args[0]) and kw["env"]["RV_TEXT"] == evil and kw["env"]["RV_VOLUME"] == "70"
    assert voice.Voice(platform="linux").speak("hi") is False

def test_listen_output_parsing():
    assert voice.parse_listen_output("TEXT:джарвис включи фейд\n") == "джарвис включи фейд"
    with pytest.raises(RuntimeError): voice.parse_listen_output("ERR:no-microphone\n", 4)
    with pytest.raises(RuntimeError): voice.parse_listen_output("ERR:no-recognizer\n", 3)

def test_tones_are_generated(tmp_path):
    for k in ("wake", "ack", "error"):
        p = voice.beep_wav(tmp_path / f"{k}.wav", k); assert p.stat().st_size > 1000

def test_ai_mode_sends_only_text_and_redacts_logs(fake):
    seen = {}
    key = "sk-test"
    import rimvisuals_launcher.jarvis.ai as ai
    def fake_http(url, payload=None, headers=None, timeout=0):
        seen.update(url=url, payload=payload, headers=headers); return {"choices": [{"message": {"content": "ответ"}}]}
    ai.http_json = fake_http
    c = AiClient("https://example.test/v1/chat", "m", lambda: key)
    assert c.ask("объясни", context="accessToken=eyJabcdefghijklmnop.abc.def") == "ответ"
    assert "eyJabcdefghijklmnop" not in json.dumps(seen["payload"]) and seen["headers"]["Authorization"] == "Bearer sk-test"
    assert AiClient("", "", lambda: None).configured is False
