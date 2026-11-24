from pathlib import Path

from rimvisuals_launcher import loader as L

FABRIC = ('"C:\\Program Files\\Java\\bin\\javaw.exe" -Xmx2G -DFabricMcEmu= net.minecraft.client.main.Main '
          '-Dfabric.gameJarPath=x net.fabricmc.loader.impl.launch.knot.KnotClient --username Steve '
          '--version fabric-loader-0.19.3-1.21.11 --gameDir "C:\\Users\\Иван Иванов\\AppData\\Roaming\\.minecraft"')


def test_splitter_quotes_and_backslashes():
    assert L.split_windows_cmdline('a "b c" d\\ e') == ["a", "b c", "d\\", "e"]
    assert L.split_windows_cmdline('x "C:\\dir with space\\\\" y') == ["x", "C:\\dir with space\\", "y"]
    assert L.split_windows_cmdline("") == []


def test_parse_fabric_instance():
    i = L.parse_instance(42, FABRIC)
    assert i and i.fabric and i.mc_version == "1.21.11" and i.fabric_loader == "0.19.3"
    assert i.game_dir == Path("C:\\Users\\Иван Иванов\\AppData\\Roaming\\.minecraft")


def test_parse_vanilla_and_foreign():
    v = L.parse_instance(1, 'javaw.exe net.minecraft.client.main.Main --version 1.21.11 --gameDir D:\\mc')
    assert v and not v.fabric and v.mc_version == "1.21.11"
    assert L.parse_instance(2, 'java.exe -jar server.jar nogui') is None


def test_states():
    i = L.parse_instance(1, FABRIC)
    E = L.evaluate
    assert E(None, jar_in_mods=False, mod_loaded=False) is L.State.NOT_FOUND
    assert E(i, jar_in_mods=False, mod_loaded=False) is L.State.RIMVISUALS_MISSING
    assert E(i, jar_in_mods=True, mod_loaded=False) is L.State.INSTALL_REQUIRED
    assert E(i, jar_in_mods=True, mod_loaded=True) is L.State.READY
    assert E(i, jar_in_mods=True, mod_loaded=True, connected=True) is L.State.CONNECTED
    assert E(i, jar_in_mods=True, mod_loaded=True, failed=True) is L.State.FAILED
    old = L.parse_instance(1, FABRIC.replace("1.21.11", "1.20.4"))
    assert E(old, jar_in_mods=True, mod_loaded=True, connected=True) is L.State.WRONG_VERSION
    van = L.parse_instance(1, 'javaw.exe net.minecraft.client.main.Main --version 1.21.11')
    assert E(van, jar_in_mods=False, mod_loaded=False) is L.State.FABRIC_MISSING
    assert L.needs_restart(L.State.INSTALL_REQUIRED) and not L.needs_restart(L.State.READY)


def test_connected_is_never_reported_without_flag():
    i = L.parse_instance(1, FABRIC)
    for jar in (True, False):
        for loaded in (True, False):
            assert E_ok(i, jar, loaded) is not L.State.CONNECTED


def E_ok(i, jar, loaded):
    return L.evaluate(i, jar_in_mods=jar, mod_loaded=loaded)


def test_mod_jars(tmp_path):
    (tmp_path / "RIMVISUALS-1.0.jar").write_bytes(b"x"); (tmp_path / "sodium.jar").write_bytes(b"x")
    assert [p.name for p in L.mod_jars(tmp_path)] == ["RIMVISUALS-1.0.jar"]
    assert L.mod_jars(tmp_path / "none") == []
