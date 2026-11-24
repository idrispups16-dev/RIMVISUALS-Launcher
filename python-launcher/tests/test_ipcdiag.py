import json
from test_jarvis import FakeGame

from rimvisuals_launcher.ipcdiag import diagnose, format_steps


def mk(tmp_path, jar=True, bridge=True):
    gd = tmp_path / "g"; (gd / "mods").mkdir(parents=True); (gd / "config/rimvisuals").mkdir(parents=True)
    if jar: (gd / "mods/rimvisuals-1.jar").write_bytes(b"x")
    (gd / "config/rimvisuals/launcher.json").write_text(json.dumps({"jarvis": {"enabled": bridge}}))
    return gd


def first_fail(steps): return next((s for s in steps if not s.ok), None)


def test_no_game_dir():
    assert first_fail(diagnose(None)).name == "Папка игры"


def test_missing_jar_then_bridge_then_ipc_file(tmp_path):
    assert first_fail(diagnose(mk(tmp_path, jar=False))).name == "JAR мода в mods"
    t2 = tmp_path / "b"; t2.mkdir()
    assert "Мост" in first_fail(diagnose(mk(t2, bridge=False))).name
    t3 = tmp_path / "c"; t3.mkdir()
    f = first_fail(diagnose(mk(t3)))
    assert f.name == "Файл jarvis-ipc.json" and f.fix


def test_stale_port_is_reported(tmp_path):
    gd = mk(tmp_path)
    (gd / "config/rimvisuals/jarvis-ipc.json").write_text(json.dumps({"port": 1, "token": "a" * 64}))
    f = first_fail(diagnose(gd))
    assert f.name.startswith("Порт") and "прошлого запуска" in f.fix


def test_bad_token_and_full_success(tmp_path):
    gd = mk(tmp_path)
    g = FakeGame(gd, token="a" * 64, token_in_file="f" * 64)
    try:
        assert first_fail(diagnose(gd)).name == "Авторизация токеном"
    finally:
        g.close()
    t2 = tmp_path / "ok"; t2.mkdir()
    gd2 = mk(t2); g2 = FakeGame(gd2)
    try:
        st = diagnose(gd2)
        assert first_fail(st) is None and st[-1].name == "Статус мода"
        assert "[OK]" in format_steps(st)
    finally:
        g2.close()
