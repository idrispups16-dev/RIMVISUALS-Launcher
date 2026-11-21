import json, threading, time, urllib.request, urllib.parse, pytest
from pathlib import Path
from rimvisuals_launcher.accounts import SecretStore, SiteAccount, MicrosoftAccount, MsEndpoints, pkce_pair
from rimvisuals_launcher.core import Paths, HttpError
from rimvisuals_launcher.service import Launcher
from fakeserver import make_mod_jar
import base64, hashlib

def test_pkce_pair_is_s256():
    v, c = pkce_pair()
    assert c == base64.urlsafe_b64encode(hashlib.sha256(v.encode()).digest()).rstrip(b"=").decode() and 43 <= len(v) <= 128

def test_site_device_flow_and_mod_sharing(home, fake, tmp_path):
    s = SecretStore(Paths(home)); site = SiteAccount(s, fake.base, fake.base)
    assert site.validate()["status"] == "signed_out"
    assert site.start() == "ABCD-1234" and "device=ABCD-1234" in site.browser_url()
    assert site.poll() == "pending"
    fake.device_state = "approved"; assert site.poll() == "approved"
    assert site.token and len(site.token) == 64
    assert site.validate() == {"status": "active", "lease_seconds": 300}
    gd = tmp_path / "g"; assert site.share_with_mod(gd)
    assert json.loads((gd / "config/rimvisuals/mod-session.json").read_text())["token"] == site.token
    site.sign_out(); assert site.token is None and not site.share_with_mod(gd)

def test_site_rejects_unexpected_auth_server(home, fake):
    site = SiteAccount(SecretStore(Paths(home)), fake.base, "https://evil.example")
    with pytest.raises(HttpError): site.start()

def test_microsoft_pkce_chain_and_state_check(home, fake):
    b = fake.base
    ep = MsEndpoints(authorize=b + "/authorize", token=b + "/ms/token", xbl=b + "/xbl", xsts=b + "/xsts",
                     mc_login=b + "/mc/login", entitlements=b + "/ent", profile=b + "/prof")
    ms = MicrosoftAccount(SecretStore(Paths(home)), "12345678-1234-1234-1234-123456789abc", ep)
    seen = {}
    def browser(url):
        q = urllib.parse.parse_qs(urllib.parse.urlparse(url).query); seen.update({k: v[0] for k, v in q.items()})
        def hit():
            time.sleep(.2)
            urllib.request.urlopen(f"{q['redirect_uri'][0]}/?code=abc&state={q['state'][0]}").read()
        threading.Thread(target=hit).start()
    ses = ms.sign_in(browser, timeout=10)
    assert seen["code_challenge_method"] == "S256" and seen["scope"].startswith("XboxLive.signin") and seen["redirect_uri"].startswith("http://localhost:")
    assert ses.name == "TestPlayer" and ses.uuid == "01234567-89ab-cdef-0123-456789abcdef" and ms.secrets.get("ms_refresh") == "ms-rt-1"
    ms.session = None; assert ms.refresh().name == "TestPlayer"

def test_microsoft_blocks_forged_state_and_non_owner(home, fake):
    b = fake.base
    ep = MsEndpoints(authorize=b + "/authorize", token=b + "/ms/token", xbl=b + "/xbl", xsts=b + "/xsts",
                     mc_login=b + "/mc/login", entitlements=b + "/ent", profile=b + "/prof")
    ms = MicrosoftAccount(SecretStore(Paths(home)), "12345678-1234-1234-1234-123456789abc", ep)
    def forged(url):
        q = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)
        threading.Thread(target=lambda: (time.sleep(.2), urllib.request.urlopen(f"{q['redirect_uri'][0]}/?code=abc&state=WRONG").read())).start()
    with pytest.raises(HttpError, match="state"): ms.sign_in(forged, timeout=10)
    fake.ms_entitled = False
    def ok(url):
        q = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)
        threading.Thread(target=lambda: (time.sleep(.2), urllib.request.urlopen(f"{q['redirect_uri'][0]}/?code=abc&state={q['state'][0]}").read())).start()
    with pytest.raises(HttpError, match="лицензии"): ms.sign_in(ok, timeout=10)
    assert MicrosoftAccount(SecretStore(Paths(home)), "").configured is False
    with pytest.raises(HttpError): MicrosoftAccount(SecretStore(Paths(home)), "").sign_in()

def test_launcher_prepare_end_to_end(home, ep, tmp_path, monkeypatch):
    jar = tmp_path / "rimvisuals-1.0.jar"; jar.write_bytes(make_mod_jar("1.0.0"))
    L = Launcher(home, ep); L.selected_jar = jar
    import rimvisuals_launcher.java_rt as jr
    from rimvisuals_launcher.java_rt import JavaInfo
    monkeypatch.setattr(jr, "find_java", lambda *a, **k: JavaInfo(Path("java"), 21, "amd64", "t", ""))
    d = {c.key: c for c in L.diagnose()}
    assert not d["minecraft"].ok and not d["mod"].ok and d["jar_src"].ok
    java = L.prepare(); assert java == Path("java")
    assert all(c.ok for c in L.diagnose()), [c for c in L.diagnose() if not c.ok]
    assert (L.profiles.game_dir(L.pid) / "config/rimvisuals/launcher.json").exists()
    assert L.mods.verify(L.profiles.game_dir(L.pid)) == []
    # the regular .minecraft is untouched and nothing is written outside RIMVISUALS' home
    assert not (tmp_path / ".minecraft").exists()

def test_play_starts_real_process_and_detects_crash(home, ep, tmp_path, monkeypatch):
    jar = tmp_path / "rimvisuals-1.0.jar"; jar.write_bytes(make_mod_jar("1.0.0"))
    L = Launcher(home, ep); L.selected_jar = jar
    import rimvisuals_launcher.java_rt as jr, rimvisuals_launcher.service as sv, sys
    from rimvisuals_launcher.java_rt import JavaInfo
    monkeypatch.setattr(jr, "find_java", lambda *a, **k: JavaInfo(Path(sys.executable), 21, "amd64", "t", ""))
    orig = sv.build_launch
    def fake_build(paths, version, java, acct, gd, ram, extra):
        spec = orig(paths, version, java, acct, gd, ram, extra)
        spec.cmd = [sys.executable, "-c", "print('Loading Minecraft 1.21.11 with Fabric Loader 0.19.3'); print('[rimvisuals] Initialized'); "
                    "print('java.lang.OutOfMemoryError: Java heap'); import sys; sys.exit(1)"]
        return spec
    monkeypatch.setattr(sv, "build_launch", fake_build)
    r = L.inject(); assert r["status"] == "started" and "Demo" in r["message"]
    L.run.proc.wait(timeout=20); time.sleep(.5)
    assert L.run.fabric_seen and L.run.mod_seen and L.run.exit_code == 1 and "памяти" in L.crash_hint()
    assert Path(r["log"]).exists()
    assert L.report().exists()
    txt = L.report().read_text(encoding="utf-8"); assert "demo-no-credential" not in txt
