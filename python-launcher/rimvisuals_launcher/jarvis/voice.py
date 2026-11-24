"""Local voice for RIM JARVIS on Windows: Windows' own offline speech engines via PowerShell (System.Speech).

* Speech output: a synthesised male voice (en-GB / ru-RU when installed), volume and rate configurable. Original synth voice,
  no recordings of anyone. Short original UI tones are generated here (sine sweeps).
* Speech input: System.Speech.Recognition with the default microphone. It runs on the PC; this code never sends audio anywhere.
  The mic is opened only while listening (push-to-talk key held, or wake-word mode which the user must switch on).
Text goes to PowerShell through an environment variable, never spliced into the script, so spoken text cannot inject code.
"""
from __future__ import annotations

import math
import os
import struct
import subprocess
import sys
import tempfile
import threading
import time
import wave
from pathlib import Path
from typing import Callable

from ..core import log

CREATE_NO_WINDOW = 0x08000000 if sys.platform == "win32" else 0
PS = ["powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command"]

SPEAK_PS = r"""
Add-Type -AssemblyName System.Speech
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$culture = $env:RV_CULTURE
$picked = ''
foreach ($pass in 'Male','Any') {
  foreach ($v in $s.GetInstalledVoices()) {
    $i = $v.VoiceInfo
    if ($v.Enabled -and $i.Culture.Name -eq $culture -and ($pass -eq 'Any' -or $i.Gender -eq 'Male')) { $s.SelectVoice($i.Name); $picked = $i.Name + '|' + $i.Gender; break }
  }
  if ($picked) { break }
}
if (-not $picked) { Write-Output 'ERR:no-ru-voice'; exit 5 }
$s.Volume = [int]$env:RV_VOLUME
$s.Rate = [int]$env:RV_RATE
if ($env:RV_OUT) { $s.SetOutputToWaveFile($env:RV_OUT) }
$s.Speak($env:RV_TEXT)
$s.Dispose()
Write-Output ('VOICE:' + $picked)
"""

# recognise a recorded WAV (16 kHz mono 16-bit) with the offline ru-RU engine
RECOG_PS = r"""
Add-Type -AssemblyName System.Speech
try { $ci = [Globalization.CultureInfo]::GetCultureInfo($env:RV_CULTURE)
      $e = New-Object System.Speech.Recognition.SpeechRecognitionEngine($ci) } catch { Write-Output 'ERR:no-recognizer'; exit 3 }
$e.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
$e.SetInputToWaveFile($env:RV_WAV)
$t = @()
while ($true) { $r = $e.Recognize([TimeSpan]::FromSeconds(8)); if ($r -eq $null) { break }; $t += $r.Text }
Write-Output ('TEXT:' + ($t -join ' '))
"""

# what the system can actually do for Russian
DIAG_PS = r"""
Add-Type -AssemblyName System.Speech
$rec = @(); try { foreach ($r in [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()) { $rec += $r.Culture.Name } } catch {}
Write-Output ('RECOGNIZERS:' + ($rec -join ','))
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$v = @(); foreach ($x in $s.GetInstalledVoices()) { $i = $x.VoiceInfo; $v += ($i.Name + '/' + $i.Culture.Name + '/' + $i.Gender) }
Write-Output ('VOICES:' + ($v -join ';'))
"""

CULTURE = {"ru": "ru-RU"}
LISTEN_CULTURE = {"ru": "ru-RU"}


def beep_wav(path: Path, kind: str = "ack") -> Path:
    """Original UI tones: sine sweeps with a soft envelope. kind: wake | ack | error."""
    spec = {"wake": [(520, 880, .14), (880, 1320, .12)], "ack": [(900, 1250, .09)], "error": [(420, 260, .22)]}[kind]
    rate, frames = 22050, bytearray()
    for f0, f1, dur in spec:
        n = int(rate * dur)
        for i in range(n):
            x = i / n
            f = f0 + (f1 - f0) * x
            env = math.sin(math.pi * x) ** 0.6
            frames += struct.pack("<h", int(9000 * env * math.sin(2 * math.pi * f * i / rate)))
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(bytes(frames))
    return path


Runner = Callable[..., subprocess.CompletedProcess]

VK = {**{f"F{i}": 0x6F + i for i in range(1, 13)}, "Insert": 0x2D, "Home": 0x24, "End": 0x23, "PageUp": 0x21, "PageDown": 0x22, "Pause": 0x13}
class SilentMic(RuntimeError):
    """The microphone returned no signal (muted / wrong device)."""


SILENCE_PEAK = 250        # below this the microphone delivered (almost) nothing: muted or wrong device


def parse_diag(out: str) -> dict:
    rec, voices = [], []
    for ln in out.splitlines():
        if ln.startswith("RECOGNIZERS:"):
            rec = [x for x in ln[12:].strip().split(",") if x]
        elif ln.startswith("VOICES:"):
            for item in ln[7:].strip().split(";"):
                p = item.split("/")
                if len(p) == 3:
                    voices.append({"name": p[0], "culture": p[1], "gender": p[2]})
    ru_voices = [v for v in voices if v["culture"] == "ru-RU"]
    return {"recognizers": rec, "ru_recognizer": "ru-RU" in rec, "voices": voices,
            "ru_voice": bool(ru_voices), "ru_male_voice": any(v["gender"] == "Male" for v in ru_voices),
            "ru_voice_names": [v["name"] for v in ru_voices]}


def diagnosis_text(d: dict, mics: list[str], speakers: list[str]) -> str:
    """Plain-Russian summary with the exact fix for each gap. Text mode always keeps working."""
    L = []
    L.append(("OK  " if d["ru_recognizer"] else "!!  ") + "Распознавание русской речи: " +
             ("установлено" if d["ru_recognizer"] else "НЕ установлено. Параметры → Время и язык → Язык и регион → Русский → Языковые параметры → Речь "
                                                       "(скачать). Английский пакет не нужен и не используется. Пока работает текстовый режим."))
    L.append(("OK  " if d["ru_voice"] else "!!  ") + "Русский голос: " +
             (", ".join(d["ru_voice_names"]) + ("" if d["ru_male_voice"] else " (мужского русского голоса нет — будет использован женский)") if d["ru_voice"]
              else "НЕ найден. Установите русский языковой пакет с функцией «Речь»; английский голос для русского текста не используется, ответы будут только текстом."))
    L.append(("OK  " if mics else "!!  ") + "Микрофоны: " + ("; ".join(mics) if mics else "не найдены (подключите микрофон и разрешите доступ в «Конфиденциальность → Микрофон»)"))
    L.append(("OK  " if speakers else "!!  ") + "Динамики: " + ("; ".join(speakers) if speakers else "не найдены"))
    return "\n".join(L)


class Voice:
    def __init__(self, runner: Runner | None = None, platform: str | None = None, mic: str = "", speaker: str = ""):
        self.run = runner or subprocess.run
        self.platform = platform or sys.platform
        self._tones: dict[str, Path] = {}
        self.mic, self.speaker = mic, speaker
        self.last_error = ""
        self.voice_used = ""

    @property
    def available(self) -> bool:
        return self.platform == "win32"

    def _env(self, **kw: str) -> dict[str, str]:
        env = dict(os.environ)
        env.update({k: str(v) for k, v in kw.items()})
        return env

    def _ps(self, script: str, timeout: float, **env) -> subprocess.CompletedProcess:
        return self.run(PS + [script], env=self._env(**env), capture_output=True, text=True, timeout=timeout,
                        creationflags=CREATE_NO_WINDOW, encoding="utf-8", errors="replace")

    # ---- devices / diagnosis
    def mics(self) -> list[str]:
        from . import audio
        return audio.list_inputs()

    def speakers(self) -> list[str]:
        from . import audio
        return audio.list_outputs()

    def diagnose(self) -> str:
        if not self.available:
            return "Голос доступен только на Windows. Текстовый режим работает."
        try:
            d = parse_diag(self._ps(DIAG_PS, 40).stdout or "")
        except (OSError, subprocess.SubprocessError) as e:
            return f"Не удалось опросить системные речевые движки: {e.__class__.__name__}. Текстовый режим работает."
        return diagnosis_text(d, self.mics(), self.speakers())

    # ---- speech out
    def speak(self, text: str, lang: str = "ru", volume: int = 80, rate: int = 0) -> bool:
        """Russian only. Returns True only if audio was really produced and played; the reason is in last_error otherwise."""
        self.last_error = ""
        if not self.available or not text.strip():
            return False
        from . import audio
        dev = audio.resolve(self.speaker, self.speakers()) if self.speaker else audio.WAVE_MAPPER
        wav = None
        try:
            if self.speaker and dev != audio.WAVE_MAPPER:
                wav = Path(tempfile.gettempdir()) / f"rimvisuals-tts-{os.getpid()}.wav"
            r = self._ps(SPEAK_PS, 60, RV_TEXT=text[:600], RV_CULTURE=CULTURE["ru"], RV_VOLUME=max(0, min(100, volume)),
                         RV_RATE=max(-10, min(10, rate)), RV_OUT=str(wav) if wav else "")
            out = r.stdout or ""
            if "ERR:no-ru-voice" in out:
                self.last_error = "Не найден русский голос Windows: ответы только текстом (установите русский языковой пакет с «Речь»)."
                return False
            if r.returncode != 0:
                self.last_error = "Синтез речи завершился с ошибкой"
                return False
            for ln in out.splitlines():
                if ln.startswith("VOICE:"):
                    self.voice_used = ln[6:].strip()
            if wav:
                audio.play(dev, wav)
            return True
        except (OSError, subprocess.SubprocessError, RuntimeError) as e:
            self.last_error = str(e) if isinstance(e, RuntimeError) else f"синтез речи не запустился: {e.__class__.__name__}"
            log.warning("speech output failed: %s", self.last_error)
            return False
        finally:
            if wav:
                wav.unlink(missing_ok=True)

    def tone(self, kind: str = "ack") -> None:
        if self.platform != "win32":
            return
        try:
            import winsound
            p = self._tones.get(kind) or beep_wav(Path(tempfile.gettempdir()) / f"rimvisuals-{kind}.wav", kind)
            self._tones[kind] = p
            winsound.PlaySound(str(p), winsound.SND_FILENAME | winsound.SND_ASYNC)
        except Exception as e:  # sound is decoration only
            log.debug("tone failed: %s", e)

    # ---- speech in
    def listen_until(self, stop: threading.Event, max_seconds: float = 12.0) -> str:
        """Records from the selected microphone until `stop` is set (push-to-talk release) and recognises it locally. '' if nothing."""
        if not self.available:
            raise RuntimeError("Голосовой ввод доступен только на Windows")
        from . import audio
        dev = audio.resolve(self.mic, self.mics()) if self.mic else audio.WAVE_MAPPER
        pcm = audio.record(dev, stop, max_seconds)
        if audio.peak(pcm) < SILENCE_PEAK:
            if len(pcm) < audio.RATE:          # <0.5 s: a key tap, not a command
                return ""
            raise SilentMic("Микрофон не передал звук: проверьте, что он не выключен, выбрано верное устройство и разрешён доступ в «Конфиденциальность → Микрофон»")
        wav = Path(tempfile.gettempdir()) / f"rimvisuals-mic-{os.getpid()}.wav"
        try:
            audio.write_wav(wav, pcm)
            try:
                r = self._ps(RECOG_PS, 60, RV_CULTURE=LISTEN_CULTURE["ru"], RV_WAV=str(wav))
            except (OSError, subprocess.SubprocessError) as e:
                raise RuntimeError(f"распознавание не запустилось: {e.__class__.__name__}") from None
            return parse_listen_output(r.stdout or "", r.returncode)
        finally:
            wav.unlink(missing_ok=True)

    def listen(self, lang: str = "ru", seconds: float = 6.0) -> str:
        """Fixed-length capture (button / wake word)."""
        stop = threading.Event()
        threading.Timer(seconds, stop.set).start()
        return self.listen_until(stop, seconds + 1)


def parse_listen_output(out: str, code: int = 0) -> str:
    for line in out.splitlines():
        if line.startswith("ERR:no-recognizer"):
            raise RuntimeError("Не установлен языковой пакет распознавания речи Windows (Параметры → Время и язык → Речь)")
        if line.startswith("ERR:no-microphone"):
            raise RuntimeError("Микрофон по умолчанию не найден")
        if line.startswith("TEXT:"):
            return line[5:].strip()
    if code:
        raise RuntimeError("распознавание завершилось с ошибкой")
    return ""


class PushToTalk(threading.Thread):
    """While the key is held the selected microphone records; on release the audio is recognised locally. Default key F8."""

    def __init__(self, voice: Voice, lang: Callable[[], str], on_text: Callable[[str], None], on_state: Callable[[str], None] | None = None,
                 vk: int = 0x77):
        super().__init__(daemon=True, name="jarvis-ptt")
        self.voice, self.lang, self.on_text, self.on_state, self.vk = voice, lang, on_text, on_state or (lambda s: None), vk
        self._halt = threading.Event()

    def stop(self) -> None:
        self._halt.set()

    def _down(self) -> bool:
        import ctypes
        return bool(ctypes.windll.user32.GetAsyncKeyState(self.vk) & 0x8000)

    def run(self) -> None:
        if not self.voice.available:
            return
        while not self._halt.is_set():
            if self._down():
                rel = threading.Event()
                box: dict = {}

                def work():
                    try:
                        box["text"] = self.voice.listen_until(rel, 15)
                    except RuntimeError as e:
                        box["err"] = str(e)
                t = threading.Thread(target=work, daemon=True)
                self.on_state("listening")
                self.voice.tone("wake")
                t.start()
                while self._down() and not self._halt.is_set() and t.is_alive():
                    time.sleep(0.03)
                rel.set()
                self.on_state("processing")
                t.join(70)
                if "err" in box:
                    self.on_state("error:" + box["err"])
                    time.sleep(1.5)
                else:
                    self.on_state("idle")
                    if box.get("text"):
                        self.on_text(box["text"])
                while self._down() and not self._halt.is_set():
                    time.sleep(0.05)
            time.sleep(0.03)


class WakeWord(threading.Thread):
    """Optional wake word: loops short local recognitions; acts only on phrases that start with the wake word."""

    def __init__(self, voice: Voice, lang: Callable[[], str], on_text: Callable[[str], None], on_state: Callable[[str], None] | None = None):
        super().__init__(daemon=True, name="jarvis-wake")
        self.voice, self.lang, self.on_text, self.on_state = voice, lang, on_text, on_state or (lambda s: None)
        self._halt = threading.Event()

    def stop(self) -> None:
        self._halt.set()

    def run(self) -> None:
        from .intents import WAKE, norm
        while not self._halt.is_set():
            try:
                text = self.voice.listen(self.lang(), 4)
            except SilentMic:
                time.sleep(0.3)                  # quiet room: normal in wake-word mode
                continue
            except RuntimeError as e:
                self.on_state("error:" + str(e))
                return
            n = norm(text)
            if n and any(n.startswith(w) for w in WAKE):
                self.voice.tone("wake")
                self.on_text(text)
