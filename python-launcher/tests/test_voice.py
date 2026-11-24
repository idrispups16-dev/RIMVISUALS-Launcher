import subprocess, threading
import pytest
from rimvisuals_launcher.jarvis import audio, voice
from rimvisuals_launcher.jarvis.state import StateTracker, STATES
from rimvisuals_launcher.settings import Settings
from rimvisuals_launcher.core import Paths


def test_resolve_devices():
    names = ["Микрофон (Realtek High Definition", "Headset (USB)"]
    assert audio.resolve("", names) == audio.WAVE_MAPPER
    assert audio.resolve("Headset (USB)", names) == 1
    assert audio.resolve("Микрофон (Realtek High Definition Audio)", names) == 0     # winmm truncates names to 31 chars
    assert audio.resolve("gone", names) == audio.WAVE_MAPPER


def test_wav_and_peak(tmp_path):
    import struct, wave
    pcm = struct.pack("<4h", 0, -1200, 800, 30000)
    assert audio.peak(pcm) == 30000 and audio.peak(b"") == 0
    p = audio.write_wav(tmp_path / "a.wav", pcm)
    with wave.open(str(p)) as f:
        assert (f.getnchannels(), f.getsampwidth(), f.getframerate(), f.getnframes()) == (1, 2, 16000, 4)


def test_non_windows_is_graceful():
    assert audio.list_inputs() == [] and audio.list_outputs() == []
    v = voice.Voice(platform="linux")
    assert v.speak("привет") is False and "Windows" in v.diagnose()
    with pytest.raises(RuntimeError):
        v.listen_until(threading.Event())


def test_parse_diag_and_text():
    out = "RECOGNIZERS:en-US\nVOICES:Microsoft Irina Desktop/ru-RU/Female;Microsoft David Desktop/en-US/Male\n"
    d = voice.parse_diag(out)
    assert not d["ru_recognizer"] and d["ru_voice"] and not d["ru_male_voice"] and d["ru_voice_names"] == ["Microsoft Irina Desktop"]
    t = voice.diagnosis_text(d, [], ["Speakers"])
    assert "НЕ установлено" in t and "английский пакет не нужен" in t.lower() and "женский" in t and "Микрофоны: не найдены" in t
    d2 = voice.parse_diag("RECOGNIZERS:ru-RU,en-US\nVOICES:Microsoft Pavel/ru-RU/Male\n")
    assert d2["ru_recognizer"] and d2["ru_male_voice"]


def fake_run(stdout="", code=0, log=None):
    def run(cmd, **kw):
        if log is not None:
            log.append(kw.get("env", {}))
        return subprocess.CompletedProcess(cmd, code, stdout, "")
    return run


def test_speak_is_russian_only_and_reports_missing_voice():
    log = []
    v = voice.Voice(runner=fake_run("ERR:no-ru-voice\n", 5, log), platform="win32")
    assert v.speak("Готово") is False and "русский голос" in v.last_error.lower()
    assert log[0]["RV_CULTURE"] == "ru-RU" and log[0]["RV_TEXT"] == "Готово"
    ok = voice.Voice(runner=fake_run("VOICE:Microsoft Pavel|Male\n"), platform="win32")
    assert ok.speak("Привет") is True and ok.voice_used.startswith("Microsoft Pavel")


def test_speech_text_is_passed_via_env_not_script():
    log = []
    v = voice.Voice(runner=fake_run("VOICE:x|Male\n", 0, log), platform="win32")
    evil = '"; Remove-Item C:\\ -Recurse; "'
    v.speak(evil)
    assert log[0]["RV_TEXT"] == evil


def test_parse_listen_errors():
    with pytest.raises(RuntimeError, match="языковой пакет"):
        voice.parse_listen_output("ERR:no-recognizer\n", 3)
    assert voice.parse_listen_output("TEXT:включи частицы\n") == "включи частицы"
    assert voice.parse_listen_output("") == ""


def test_state_tracker_transitions_and_expiry():
    seen, timers = [], []
    tr = StateTracker(lambda s, d: seen.append(s), timer=lambda sec, fn: timers.append((sec, fn)))
    tr.set("listening"); tr.set("processing"); tr.set("success", "Частицы включены")
    assert tr.state == "success" and tr.text.startswith("Выполнено")
    sec, expire = timers[-1]; assert 1 <= sec <= 5
    expire(); assert tr.state == "idle" and seen[-1] == "idle"
    tr.set("error", "нет игры"); old = timers[-1][1]
    tr.set("listening"); old()                          # stale timer must not reset a newer state
    assert tr.state == "listening"
    with pytest.raises(ValueError):
        tr.set("dancing")
    assert set(STATES) == {"idle", "listening", "processing", "speaking", "success", "error"}


def test_settings_validation(home):
    s = Settings(Paths(home))
    s.set("jarvis_ptt_key", "F9")
    with pytest.raises(ValueError):
        s.set("jarvis_ptt_key", "Q")
    s.set("jarvis_mic", "Headset"); assert s["jarvis_mic"] == "Headset"
