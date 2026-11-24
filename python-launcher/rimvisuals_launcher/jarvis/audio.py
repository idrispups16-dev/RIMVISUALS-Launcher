"""Audio devices through winmm (ships with Windows): list microphones/speakers, record from a chosen microphone, play to a chosen speaker.

Windows speech APIs used by voice.py can only read the *default* microphone and write to the default speaker, so we do the
device I/O ourselves: record a WAV from the selected input, hand the file to the offline recogniser; synthesise to a WAV and play it
on the selected output. Nothing leaves the PC. On other platforms every function reports "no devices".
"""
from __future__ import annotations

import sys
import threading
import time
import wave
from pathlib import Path

WAVE_MAPPER = 0xFFFFFFFF
RATE, CHANNELS, BITS = 16000, 1, 16


def available() -> bool:
    return sys.platform == "win32"


def _winmm():
    import ctypes
    return ctypes, ctypes.windll.winmm


def _caps(kind: str) -> list[str]:
    if not available():
        return []
    ctypes, mm = _winmm()
    from ctypes import wintypes as w

    class Caps(ctypes.Structure):
        _fields_ = [("mid", w.WORD), ("pid", w.WORD), ("ver", w.UINT), ("name", ctypes.c_wchar * 32), ("formats", w.DWORD),
                    ("channels", w.WORD), ("res", w.WORD)] + ([("support", w.DWORD)] if kind == "out" else [])
    num = mm.waveOutGetNumDevs() if kind == "out" else mm.waveInGetNumDevs()
    names = []
    for i in range(num):
        c = Caps()
        fn = mm.waveOutGetDevCapsW if kind == "out" else mm.waveInGetDevCapsW
        if fn(ctypes.c_size_t(i), ctypes.byref(c), ctypes.sizeof(c)) == 0:
            names.append(c.name)
    return names


def list_inputs() -> list[str]:
    try:
        return _caps("in")
    except Exception:
        return []


def list_outputs() -> list[str]:
    try:
        return _caps("out")
    except Exception:
        return []


def resolve(name: str, names: list[str]) -> int:
    """Device index for a saved name (the 31-char winmm name may be a prefix match); WAVE_MAPPER when unset/unknown."""
    if not name:
        return WAVE_MAPPER
    for i, n in enumerate(names):
        if n == name:
            return i
    for i, n in enumerate(names):
        if n.startswith(name) or name.startswith(n):
            return i
    return WAVE_MAPPER


def write_wav(path: Path, pcm: bytes, rate: int = RATE) -> Path:
    with wave.open(str(path), "wb") as f:
        f.setnchannels(CHANNELS)
        f.setsampwidth(BITS // 8)
        f.setframerate(rate)
        f.writeframes(pcm)
    return path


def peak(pcm: bytes) -> int:
    """Loudest 16-bit sample in a buffer (0..32767); used to tell 'microphone muted' from speech."""
    import array
    a = array.array("h")
    a.frombytes(pcm[: len(pcm) // 2 * 2])
    return max((abs(x) for x in a), default=0)


def _structs():
    import ctypes
    from ctypes import wintypes as w

    class WAVEFORMATEX(ctypes.Structure):
        _fields_ = [("tag", w.WORD), ("ch", w.WORD), ("rate", w.DWORD), ("avg", w.DWORD), ("align", w.WORD), ("bits", w.WORD), ("cb", w.WORD)]

    class WAVEHDR(ctypes.Structure):
        pass
    WAVEHDR._fields_ = [("data", ctypes.c_void_p), ("len", w.DWORD), ("rec", w.DWORD), ("user", ctypes.c_size_t), ("flags", w.DWORD),
                        ("loops", w.DWORD), ("next", ctypes.c_void_p), ("res", ctypes.c_size_t)]
    return WAVEFORMATEX, WAVEHDR


def _fmt(WAVEFORMATEX, ch: int, rate: int, bits: int):
    f = WAVEFORMATEX(1, ch, rate, rate * ch * bits // 8, ch * bits // 8, bits, 0)
    return f


def record(device: int, stop: threading.Event, max_seconds: float = 12.0) -> bytes:
    """Records 16 kHz mono PCM from `device` until `stop` is set or `max_seconds` pass. Raises RuntimeError with a readable reason."""
    if not available():
        raise RuntimeError("Запись звука доступна только на Windows")
    ctypes, mm = _winmm()
    from ctypes import wintypes as w
    WAVEFORMATEX, WAVEHDR = _structs()
    fmt = _fmt(WAVEFORMATEX, CHANNELS, RATE, BITS)
    h = ctypes.c_void_p()
    mm.waveInOpen.argtypes = [ctypes.POINTER(ctypes.c_void_p), ctypes.c_size_t, ctypes.POINTER(WAVEFORMATEX), ctypes.c_size_t, ctypes.c_size_t, w.DWORD]
    r = mm.waveInOpen(ctypes.byref(h), ctypes.c_size_t(device), ctypes.byref(fmt), 0, 0, 0)
    if r != 0:
        raise RuntimeError(f"Не удалось открыть микрофон (код {r}): устройство занято, отключено или запрещено в «Параметры → Конфиденциальность → Микрофон»")
    chunk = RATE * 2 // 4                       # 250 ms
    bufs = []
    for _ in range(int(max_seconds * 4) + 2):
        b = ctypes.create_string_buffer(chunk)
        hd = WAVEHDR()
        hd.data, hd.len = ctypes.cast(b, ctypes.c_void_p), chunk
        bufs.append((b, hd))
    out = bytearray()
    try:
        idx, queued = 0, 0
        for b, hd in bufs[:4]:
            mm.waveInPrepareHeader(h, ctypes.byref(hd), ctypes.sizeof(hd)); mm.waveInAddBuffer(h, ctypes.byref(hd), ctypes.sizeof(hd)); queued += 1
        mm.waveInStart(h)
        t0 = time.monotonic()
        while idx < queued:
            b, hd = bufs[idx]
            if hd.flags & 1:                    # WHDR_DONE
                out += b.raw[: hd.rec]
                mm.waveInUnprepareHeader(h, ctypes.byref(hd), ctypes.sizeof(hd))
                idx += 1
                if not stop.is_set() and time.monotonic() - t0 < max_seconds and queued < len(bufs):
                    nb, nh = bufs[queued]
                    mm.waveInPrepareHeader(h, ctypes.byref(nh), ctypes.sizeof(nh)); mm.waveInAddBuffer(h, ctypes.byref(nh), ctypes.sizeof(nh)); queued += 1
                continue
            if (stop.is_set() or time.monotonic() - t0 >= max_seconds) and not (hd.flags & 1):
                mm.waveInReset(h)               # returns every pending buffer as DONE with what was captured
                stop.set()
                max_seconds = 0
            time.sleep(0.01)
    finally:
        mm.waveInReset(h)
        for b, hd in bufs:
            if hd.flags & 2:                    # still prepared
                mm.waveInUnprepareHeader(h, ctypes.byref(hd), ctypes.sizeof(hd))
        mm.waveInClose(h)
    return bytes(out)


def play(device: int, wav_path: Path, timeout: float = 60.0) -> None:
    """Plays a PCM WAV on `device` (WAVE_MAPPER = default). Blocks until finished."""
    if not available():
        raise RuntimeError("Воспроизведение доступно только на Windows")
    ctypes, mm = _winmm()
    from ctypes import wintypes as w
    WAVEFORMATEX, WAVEHDR = _structs()
    with wave.open(str(wav_path), "rb") as f:
        ch, rate, bits, data = f.getnchannels(), f.getframerate(), f.getsampwidth() * 8, f.readframes(f.getnframes())
    fmt = _fmt(WAVEFORMATEX, ch, rate, bits)
    h = ctypes.c_void_p()
    mm.waveOutOpen.argtypes = [ctypes.POINTER(ctypes.c_void_p), ctypes.c_size_t, ctypes.POINTER(WAVEFORMATEX), ctypes.c_size_t, ctypes.c_size_t, w.DWORD]
    r = mm.waveOutOpen(ctypes.byref(h), ctypes.c_size_t(device), ctypes.byref(fmt), 0, 0, 0)
    if r != 0:
        raise RuntimeError(f"Не удалось открыть динамики (код {r})")
    buf = ctypes.create_string_buffer(data, len(data))
    hd = WAVEHDR()
    hd.data, hd.len = ctypes.cast(buf, ctypes.c_void_p), len(data)
    try:
        mm.waveOutPrepareHeader(h, ctypes.byref(hd), ctypes.sizeof(hd))
        mm.waveOutWrite(h, ctypes.byref(hd), ctypes.sizeof(hd))
        t0 = time.monotonic()
        while not (hd.flags & 1) and time.monotonic() - t0 < timeout:
            time.sleep(0.02)
    finally:
        mm.waveOutReset(h)
        mm.waveOutUnprepareHeader(h, ctypes.byref(hd), ctypes.sizeof(hd))
        mm.waveOutClose(h)
