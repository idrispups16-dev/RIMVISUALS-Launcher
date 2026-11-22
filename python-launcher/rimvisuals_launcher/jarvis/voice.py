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
$picked = $false
foreach ($v in $s.GetInstalledVoices()) {
  $i = $v.VoiceInfo
  if ($i.Culture.Name -eq $culture -and $i.Gender -eq 'Male') { $s.SelectVoice($i.Name); $picked = $true; break }
}
if (-not $picked) { try { $s.SelectVoiceByHints('Male','Adult',0,[Globalization.CultureInfo]::GetCultureInfo($culture)) } catch {} }
$s.Volume = [int]$env:RV_VOLUME
$s.Rate = [int]$env:RV_RATE
$s.Speak($env:RV_TEXT)
"""

LISTEN_PS = r"""
Add-Type -AssemblyName System.Speech
try { $ci = [Globalization.CultureInfo]::GetCultureInfo($env:RV_CULTURE)
      $e = New-Object System.Speech.Recognition.SpeechRecognitionEngine($ci) } catch { Write-Output 'ERR:no-recognizer'; exit 3 }
$e.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
try { $e.SetInputToDefaultAudioDevice() } catch { Write-Output 'ERR:no-microphone'; exit 4 }
$r = $e.Recognize([TimeSpan]::FromSeconds([double]$env:RV_SECONDS))
if ($r -ne $null) { Write-Output ('TEXT:' + $r.Text) } else { Write-Output 'TEXT:' }
"""

CULTURE = {"ru": "ru-RU", "en": "en-GB"}
LISTEN_CULTURE = {"ru": "ru-RU", "en": "en-US"}


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


class Voice:
    def __init__(self, runner: Runner | None = None, platform: str | None = None):
        self.run = runner or subprocess.run
        self.platform = platform or sys.platform
        self._tones: dict[str, Path] = {}

    @property
    def available(self) -> bool:
        return self.platform == "win32"

    def _env(self, **kw: str) -> dict[str, str]:
        env = dict(os.environ)
        env.update({k: str(v) for k, v in kw.items()})
        return env

    def speak(self, text: str, lang: str = "ru", volume: int = 80, rate: int = 0) -> bool:
        if not self.available or not text.strip():
            return False
        try:
            r = self.run(PS + [SPEAK_PS], env=self._env(RV_TEXT=text[:600], RV_CULTURE=CULTURE.get(lang, "en-GB"), RV_VOLUME=max(0, min(100, volume)),
                                                         RV_RATE=max(-10, min(10, rate))),
                         capture_output=True, text=True, timeout=60, creationflags=CREATE_NO_WINDOW)
            return r.returncode == 0
        except (OSError, subprocess.SubprocessError) as e:
            log.warning("speech output failed: %s", e)
            return False

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

    def listen(self, lang: str = "ru", seconds: float = 6.0) -> str:
        """Record from the default microphone for up to `seconds` and return recognised text ('' if nothing)."""
        if not self.available:
            raise RuntimeError("Голосовой ввод доступен только на Windows")
        try:
            r = self.run(PS + [LISTEN_PS], env=self._env(RV_CULTURE=LISTEN_CULTURE.get(lang, "en-US"), RV_SECONDS=seconds),
                         capture_output=True, text=True, timeout=seconds + 25, creationflags=CREATE_NO_WINDOW)
        except (OSError, subprocess.SubprocessError) as e:
            raise RuntimeError(f"распознавание не запустилось: {e.__class__.__name__}") from None
        return parse_listen_output(r.stdout or "", r.returncode)


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
    """Polls a key (Windows virtual-key code) and calls on_text(recognised) after it is released. Default key: F8."""

    def __init__(self, voice: Voice, lang: Callable[[], str], on_text: Callable[[str], None], on_state: Callable[[str], None] | None = None, vk: int = 0x77):
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
                self.on_state("listening")
                self.voice.tone("wake")
                try:
                    text = self.voice.listen(self.lang(), 6)
                except RuntimeError as e:
                    self.on_state("error:" + str(e))
                    time.sleep(2)
                    continue
                self.on_state("idle")
                if text:
                    self.on_text(text)
                while self._down() and not self._halt.is_set():
                    time.sleep(0.05)
            time.sleep(0.04)


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
            except RuntimeError as e:
                self.on_state("error:" + str(e))
                return
            n = norm(text)
            if n and any(n.startswith(w) for w in WAKE):
                self.voice.tone("wake")
                self.on_text(text)
