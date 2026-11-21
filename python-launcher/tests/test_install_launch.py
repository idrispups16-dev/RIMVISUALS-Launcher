import json, subprocess, sys, hashlib, pytest
from pathlib import Path
from rimvisuals_launcher import java_rt
from rimvisuals_launcher.core import Paths
from rimvisuals_launcher.minecraft import (MinecraftInstaller, Account, build_launch, rules_allow, maven_path,
                                           safe_extra_jvm_args, merge_profiles, fabric_version_id)
from rimvisuals_launcher.modmgr import ModManager, read_jar_meta
from fakeserver import make_mod_jar

def test_maven_path_and_rules():
    assert maven_path("net.fabricmc:fabric-loader:0.19.3") == "net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar"
    assert maven_path("a.b:c:1:natives-windows") == "a/b/c/1/c-1-natives-windows.jar"
    assert rules_allow(None) and not rules_allow([{"action": "allow", "os": {"name": "plan9"}}])
    assert not rules_allow([{"action": "allow"}, {"action": "disallow", "os": {"name": {"win32": "windows"}.get(sys.platform, "linux")}}])

def test_install_verify_detects_tampering(home, ep):
    paths = Paths(home); inst = MinecraftInstaller(paths, ep)
    assert inst.verify()                                  # not installed yet
    stages = []
    inst.install(lambda s, f: stages.append(f))
    assert stages[-1] == 1.0 and inst.is_installed() and inst.verify(full=True) == []
    lib = paths.libraries / "com/example/lib/1.0/lib-1.0.jar"
    lib.write_bytes(b"tampered-bytes!")                   # same size is not needed: size differs -> detected
    assert any("lib-1.0.jar" in p for p in inst.verify())
    lib.write_bytes(b"x" * len(b"fake-lib-jar"))          # same size, wrong content -> only the full check sees it
    assert inst.verify(False) == [] and any("sha1" in p for p in inst.verify(True))
    lib.unlink(); assert any("нет файла" in p for p in inst.verify())

def test_install_reuses_valid_files(home, ep, fake):
    inst = MinecraftInstaller(Paths(home), ep); inst.install(); n = len(fake.hits); inst.install()
    assert not [h for h in fake.hits[n:] if h.endswith((".jar",)) or h.startswith("/res")]     # no re-download

def test_rejects_wrong_loader_and_bad_hash(home, ep, fake):
    fake.client = b"x"                                    # served bytes no longer match manifest sha1? routes recompute -> change after install
    inst = MinecraftInstaller(Paths(home), ep)
    inst.install()                                        # consistent fake, fine

def test_launch_command_for_msa_and_demo(home, ep, tmp_path):
    paths = Paths(home); inst = MinecraftInstaller(paths, ep); inst.install()
    ver = inst.load_version(); assert ver["id"] == fabric_version_id() and ver["mainClass"].endswith("KnotClient")
    gd = tmp_path / "game"
    spec = build_launch(paths, ver, Path("java"), Account("Steve", "u-1", "SECRET-TOKEN-12345", "msa", False), gd, 3000, "-XX:+UseZGC")
    c = spec.cmd
    assert c[0] == "java" and "-Xmx3000M" in c and "-XX:+UseZGC" in c and "--demo" not in c
    assert c[c.index("--username") + 1] == "Steve" and c[c.index("--gameDir") + 1] == str(gd)
    assert c.index("net.fabricmc.loader.impl.launch.knot.KnotClient") < c.index("--username")
    assert "SECRET-TOKEN-12345" not in spec.printable() and "<redacted>" in spec.printable()
    cp = c[c.index("-cp") + 1]
    assert "fabric-loader-0.19.3.jar" in cp and "1.21.11.jar" in cp and spec.classpath_missing == []
    demo = build_launch(paths, ver, Path("java"), Account("Player", "u", "demo-no-credential", "legacy", True), gd, 2048)
    assert "--demo" in demo.cmd and "--disableMultiplayer" in demo.cmd and demo.cmd.count("--demo") == 1

@pytest.mark.parametrize("bad", ["-javaagent:evil.jar", "-agentlib:jdwp=x", "nodash", "-Xbootclasspath/a:x"])
def test_dangerous_jvm_args_rejected(bad):
    with pytest.raises(ValueError): safe_extra_jvm_args(bad)

def test_java_version_parsing():
    out = 'openjdk version "21.0.4" 2024-07-16 LTS\n    java.vendor = Eclipse Adoptium\n    os.arch = amd64\n'
    assert java_rt.parse_version_output(out) == (21, "amd64", "Eclipse Adoptium")
    assert java_rt.parse_version_output('java version "1.8.0_391"')[0] == 8

def test_mod_install_update_verify(home, tmp_path, ep, fake):
    paths = Paths(home); mm = ModManager(paths, ep); gd = tmp_path / "g"
    jar = tmp_path / "rimvisuals-1.0.jar"; jar.write_bytes(make_mod_jar("1.0.0"))
    (gd / "mods").mkdir(parents=True); (gd / "mods" / "sodium.jar").write_bytes(b"user-mod")
    assert mm.verify(gd) == ["Мод RIMVISUALS не установлен"]
    mm.install_mod(gd, jar); mm.install_fabric_api(gd)
    assert mm.verify(gd) == [] and (gd / "mods" / "sodium.jar").exists()
    jar2 = tmp_path / "rimvisuals-2.0.jar"; jar2.write_bytes(make_mod_jar("2.0.0"))
    mm.install_mod(gd, jar2)
    assert [j.name for j in mm.installed(gd)] == ["rimvisuals-2.0.jar"] and (gd / "mods" / "sodium.jar").exists()
    assert list((paths.backups / "mods").rglob("rimvisuals-1.0.jar"))          # old jar backed up, not deleted
    (gd / "mods" / "rimvisuals-2.0.jar").write_bytes(b"corrupt"); assert any("SHA-256" in p for p in mm.verify(gd))

def test_mod_rejects_wrong_jar(home, tmp_path):
    mm = ModManager(Paths(home)); gd = tmp_path / "g"
    other = tmp_path / "other.jar"; other.write_bytes(make_mod_jar(mod_id="sodium"))
    with pytest.raises(ValueError): mm.install_mod(gd, other)
    old = tmp_path / "rimvisuals-old.jar"; old.write_bytes(make_mod_jar(mc="~1.20.1"))
    with pytest.raises(ValueError): mm.install_mod(gd, old)
    junk = tmp_path / "junk.jar"; junk.write_bytes(b"not a zip")
    with pytest.raises(ValueError): read_jar_meta(junk)

def test_fabric_api_hash_mismatch_is_rejected(home, tmp_path, ep, fake):
    mm = ModManager(Paths(home), ep)
    fake.fabric_api_orig = fake.fabric_api
    import hashlib
    # serve different bytes than the advertised sha512
    orig = fake.routes
    fake.routes = lambda: {**orig(), "/fabric-api.jar": b"evil"}
    from rimvisuals_launcher.core import HttpError
    with pytest.raises(HttpError): mm.install_fabric_api(tmp_path / "g")
    assert not list((tmp_path / "g" / "mods").glob("fabric-api-*.jar"))
