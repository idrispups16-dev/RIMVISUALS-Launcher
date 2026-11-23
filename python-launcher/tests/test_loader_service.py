import hashlib, json, socket, threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import pytest
from fakeserver import make_mod_jar

from rimvisuals_launcher import loader as L
from rimvisuals_launcher.core import HttpError
from rimvisuals_launcher.jarfetch import JarUnavailable, fetch_release_jar
from rimvisuals_launcher.minecraft import Endpoints
from rimvisuals_launcher.service import Launcher

JAR = make_mod_jar("2.0.0")
SHA = hashlib.sha256(JAR).hexdigest()


class Rel:
    def __init__(self, manifest=True, bad_hash=False):
        files = {"/rel/rimvisuals-2.0.0.jar": JAR}
        if manifest:
            files["/rel/rimvisuals-mod.json"] = json.dumps({"file": "rimvisuals-2.0.0.jar", "version": "2.0.0",
                                                            "sha256": ("0" * 64) if bad_hash else SHA}).encode()
        class H(BaseHTTPRequestHandler):
            def do_GET(s):
                b = files.get(s.path)
                s.send_response(200 if b else 404); s.send_header("Content-Length", str(len(b or b""))); s.end_headers(); s.wfile.write(b or b"")
            def log_message(s, *a): pass
        self.srv = ThreadingHTTPServer(("127.0.0.1", 0), H)
        self.url = f"http://127.0.0.1:{self.srv.server_address[1]}/rel"
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()

    def close(self): self.srv.shutdown(); self.srv.server_close()


@pytest.fixture()
def rel():
    r = Rel(); yield r; r.close()


def test_fetch_release_jar_verifies_and_caches(home, rel):
    from rimvisuals_launcher.core import Paths
    paths = Paths(home); paths.ensure()
    info = fetch_release_jar(paths, Endpoints(release=rel.url))
    assert info.version == "2.0.0" and info.sha256 == SHA and info.path.parent == paths.cache / "jar"
    rel.close()                                       # network gone: the verified cache keeps the install possible
    again = fetch_release_jar(paths, Endpoints(release=rel.url))
    assert again.sha256 == SHA


def test_fetch_rejects_wrong_hash_and_missing_release(home):
    from rimvisuals_launcher.core import Paths
    paths = Paths(home); paths.ensure()
    bad = Rel(bad_hash=True)
    try:
        with pytest.raises(JarUnavailable) as e:
            fetch_release_jar(paths, Endpoints(release=bad.url))
        assert not list((paths.cache / "jar").glob("*.jar"))
    finally:
        bad.close()
    none = Rel(manifest=False)
    try:
        with pytest.raises(JarUnavailable) as e:
            fetch_release_jar(paths, Endpoints(release=none.url))
        assert "404" in str(e.value) and "релиз" in str(e.value).lower()
    finally:
        none.close()


def test_dns_failure_is_explained(home):
    from rimvisuals_launcher.core import Paths
    paths = Paths(home); paths.ensure()
    with pytest.raises(JarUnavailable) as e:
        fetch_release_jar(paths, Endpoints(release="https://nonexistent-host.invalid/rel"))
    assert "DNS" in str(e.value) or "имя узла" in str(e.value) or "network" in str(e.value).lower()


def _instance(tmp_path, fabric=True, ver="1.21.11"):
    gd = tmp_path / "game"; (gd / "mods").mkdir(parents=True)
    return L.GameInstance(1, gd, ver, fabric, "0.19.3")


def test_install_into_found_profile_needs_restart(home, tmp_path, rel):
    la = Launcher(home); la.installer.ep = Endpoints(release=rel.url)
    la.mods.fabric_api_present = lambda gd: True      # Modrinth is covered by other tests
    la.mod_jar_source = lambda: None                  # CI bundles the real JAR; here we exercise the release download path
    inst = _instance(tmp_path)
    r = la.loader.install(inst)
    assert r["installed"] == "2.0.0" and r["needs_restart"] is True
    assert (inst.game_dir / "mods" / "rimvisuals-2.0.0.jar").is_file()
    assert json.loads((inst.game_dir / "config/rimvisuals/launcher.json").read_text())["jarvis"]["enabled"] is True
    s = la.loader.evaluate(inst)
    assert s.state is L.State.INSTALL_REQUIRED and s.restart


def test_install_refuses_vanilla_and_wrong_version(home, tmp_path):
    la = Launcher(home)
    with pytest.raises(RuntimeError, match="Fabric"):
        la.loader.install(_instance(tmp_path, fabric=False))
    with pytest.raises(RuntimeError, match="1.20"):
        la.loader.install(_instance(tmp_path / "x", ver="1.20.4"))


def test_connect_refuses_without_mod_and_never_fakes_connected(home, tmp_path):
    la = Launcher(home)
    inst = _instance(tmp_path)
    r = la.loader.connect(inst)
    assert r["ok"] is False and r["state"] == L.State.RIMVISUALS_MISSING.value
    (inst.game_dir / "mods" / "rimvisuals-1.jar").write_bytes(b"x")
    r = la.loader.connect(inst)
    assert r["ok"] is False and r["needs_restart"] is True


def test_connect_real_handshake_and_catalog(home, tmp_path):
    from test_jarvis import FakeGame
    la = Launcher(home)
    inst = _instance(tmp_path)
    (inst.game_dir / "mods" / "rimvisuals-1.jar").write_bytes(b"x")
    g = FakeGame(inst.game_dir); 
    try:
        assert la.loader.evaluate(inst).state is L.State.READY
        r = la.loader.connect(inst)
        assert r["ok"] and r["state"] == "connected"
        assert la.loader.evaluate(inst).state is L.State.CONNECTED
        mods = la.loader.modules()
        assert len(mods) >= 2 and {"id", "name", "category", "enabled"} <= set(mods[0])
        assert la.loader.toggle(mods[0]["id"], not mods[0]["enabled"]) is True
        assert la.jarvis() is not None
    finally:
        la.loader.disconnect(); g.close()


def test_wrong_token_is_a_failure(home, tmp_path):
    from test_jarvis import FakeGame
    la = Launcher(home)
    inst = _instance(tmp_path)
    (inst.game_dir / "mods" / "rimvisuals-1.jar").write_bytes(b"x")
    g = FakeGame(inst.game_dir, token="a" * 64, token_in_file="f" * 64)
    try:
        r = la.loader.connect(inst)
        assert r["ok"] is False and r["state"] == "connection_failed"
    finally:
        g.close()
