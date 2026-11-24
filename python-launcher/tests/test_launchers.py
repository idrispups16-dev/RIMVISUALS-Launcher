import json
from rimvisuals_launcher import launchers as LA


def fabric_version(root, loader="0.19.3", mc="1.21.11"):
    vid = f"fabric-loader-{loader}-{mc}"
    (root / "versions" / vid).mkdir(parents=True)
    (root / "versions" / vid / f"{vid}.json").write_text("{}")
    return vid


def test_official_folder_with_profile_and_game_dir(tmp_path):
    mc = tmp_path / ".minecraft"; vid = fabric_version(mc)
    (mc / "launcher_profiles.json").write_text(json.dumps({"profiles": {"a": {"name": "PvP", "lastVersionId": vid, "gameDir": str(tmp_path / "pvp")}}}))
    ps = LA.scan_official(mc)
    assert [(p.name, p.game_dir, p.mc_version, p.fabric_loader) for p in ps] == [("PvP", tmp_path / "pvp", "1.21.11", "0.19.3")]
    assert ps[0].compatible and not ps[0].verified


def test_official_version_without_profile_and_vanilla_ignored(tmp_path):
    mc = tmp_path / ".minecraft"; fabric_version(mc)
    (mc / "versions" / "1.21.11").mkdir(); (mc / "versions" / "1.21.11" / "1.21.11.json").write_text("{}")
    ps = LA.scan_official(mc)
    assert len(ps) == 1 and ps[0].game_dir == mc


def test_prism_style_instances(tmp_path):
    root = tmp_path / "PrismLauncher"; i = root / "instances" / "fv"; (i / ".minecraft" / "mods").mkdir(parents=True)
    (i / "instance.cfg").write_text("[General]\nname=My Fabric\n")
    (i / "mmc-pack.json").write_text(json.dumps({"components": [{"uid": "net.minecraft", "version": "1.21.11"}, {"uid": "net.fabricmc.fabric-loader", "version": "0.19.3"}]}))
    (i / ".minecraft" / "mods" / "rimvisuals-1.jar").write_bytes(b"x")
    old = root / "instances" / "old"; old.mkdir(); (old / "instance.cfg").write_text("name=Old\n")
    (old / "mmc-pack.json").write_text(json.dumps({"components": [{"uid": "net.minecraft", "version": "1.20.1"}]}))
    ps = {p.name: p for p in LA.scan_mmc_style(root, "PrismLauncher")}
    assert ps["My Fabric"].compatible and ps["My Fabric"].has_rimvisuals and "уже установлен" in ps["My Fabric"].note
    assert not ps["Old"].compatible and "1.20.1" in ps["Old"].note


def test_atlauncher_and_discover_with_custom_dirs_not_in_appdata(tmp_path):
    at = tmp_path / "Roaming" / "ATLauncher" / "instances" / "x"; at.mkdir(parents=True)
    (at / "instance.json").write_text(json.dumps({"id": "1.21.11", "launcher": {"name": "AT", "loaderVersion": {"type": "Fabric", "version": "0.19.3"}}}))
    custom = tmp_path / "D" / "games" / "mc"; fabric_version(custom)
    ps = LA.discover({"APPDATA": str(tmp_path / "Roaming")}, extra=[custom])
    assert {p.launcher for p in ps} == {"ATLauncher", "Папка mc"} and all(p.compatible for p in ps)


def test_discover_is_quiet_when_nothing_installed(tmp_path):
    assert LA.discover({"APPDATA": str(tmp_path / "none")}) == []
    assert LA.discover({}) == []
