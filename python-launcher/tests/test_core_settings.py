import json, zipfile, pytest
from rimvisuals_launcher.core import (Paths, atomic_write, read_json, safe_join, redact, clean_name, download, HttpError, http_request)
from rimvisuals_launcher.settings import Settings, Profiles, CfgManager, clamp_ram

def test_atomic_write_keeps_backup_and_recovers(tmp_path):
    f = tmp_path / "a.json"
    atomic_write(f, '{"v":1}'); atomic_write(f, '{"v":2}')
    assert json.loads(f.read_text())["v"] == 2
    f.write_text("{broken")
    assert read_json(f, {})["v"] == 1            # falls back to .bak

def test_safe_join_blocks_traversal(tmp_path):
    with pytest.raises(ValueError): safe_join(tmp_path, "..", "x")
    assert safe_join(tmp_path, "a", "b").parent.name == "a"

def test_redact_hides_tokens():
    assert "abcdef1234567890" not in redact("--accessToken abcdef1234567890 and token=abcdef1234567890")

def test_non_https_refused():
    with pytest.raises(HttpError): http_request("http://example.com/x")

def test_download_verifies_hash(fake, tmp_path):
    import hashlib
    ok = hashlib.sha1(fake.lib).hexdigest()
    p = download(fake.base + "/lib.jar", tmp_path / "x.jar", sha1=ok)
    assert p.read_bytes() == fake.lib
    with pytest.raises(HttpError): download(fake.base + "/lib.jar", tmp_path / "y.jar", sha1="0" * 40, retries=1)
    assert not (tmp_path / "y.jar").exists()

def test_settings_validation(home):
    s = Settings(Paths(home))
    s.set("ram_mb", 99999); assert s["ram_mb"] == 32768
    s.set("ram_mb", 10); assert s["ram_mb"] == 1024
    with pytest.raises(ValueError): s.set("ms_client_id", "not-a-guid")
    s.set("ms_client_id", "12345678-1234-1234-1234-123456789abc")
    assert Settings(Paths(home))["ms_client_id"].startswith("1234")      # persisted

def test_profiles_isolated_and_bridge(home):
    paths = Paths(home); pr = Profiles(paths)
    a = pr.create("PvP"); b = pr.create("PvP")
    assert a.id != b.id and pr.game_dir(a.id) != pr.game_dir(b.id)
    pr.update(a.id, ram_mb=6144, visuals={"accent": "#112233", "hudScale": 500})
    assert pr.items[a.id].visuals["hudScale"] == 200
    with pytest.raises(ValueError): pr.update(a.id, visuals={"accent": "red"})
    with pytest.raises(ValueError): pr.update(a.id, visuals={"nope": 1})
    bridge = json.loads(pr.write_bridge(a.id).read_text())
    assert bridge["schemaVersion"] == 1 and bridge["visuals"]["accent"] == "#112233"
    assert Profiles(paths).items[a.id].ram_mb == 6144
    pr.delete(b.id)
    assert b.id not in pr.items and pr.game_dir(b.id).exists()          # files never auto-deleted

def test_profile_export_import_has_no_paths_or_secrets(home):
    pr = Profiles(Paths(home)); text = pr.export_json("default")
    assert "java" not in text.lower() and "token" not in text.lower()
    assert pr.import_json(text).id != "default"
    with pytest.raises(ValueError): pr.import_json('{"x":1}')

def test_cfg_backup_export_import_skip_session(home):
    paths = Paths(home); pr = Profiles(paths); cm = CfgManager(paths, pr)
    d = cm.cfg_dir("default"); d.mkdir(parents=True)
    (d / "config.properties").write_text("a=1"); (d / "mod-session.json").write_text('{"token":"secret"}')
    z = home / "out.zip"; assert cm.export_zip("default", z) == 1
    assert "mod-session.json" not in zipfile.ZipFile(z).namelist()
    (d / "config.properties").write_text("a=2")
    assert cm.import_zip("default", z) == 1
    assert (d / "config.properties").read_text() == "a=1" and cm.list_backups("default")      # auto backup made
    evil = home / "evil.zip"
    with zipfile.ZipFile(evil, "w") as zz: zz.writestr("../../escape.txt", "x")
    with pytest.raises(ValueError): cm.import_zip("default", evil)
    assert (d / "mod-session.json").read_text() == '{"token":"secret"}'                       # never overwritten
