"""Minecraft skin handling without third-party libraries: PNG decode, an original default skin, Mojang skin lookup, and a small
software renderer that turns a skin into coloured quads (3/4 view on a dark cube) for a tkinter Canvas."""
from __future__ import annotations

import base64
import json
import math
import random
import re
import struct
import zlib
from pathlib import Path

RGBA = tuple[int, int, int, int]


# ----------------------------------------------------------------------------------------------- PNG
def decode_png(data: bytes) -> tuple[int, int, list[RGBA]]:
    """8-bit, non-interlaced PNG (colour types 0, 2, 3, 4, 6). Raises ValueError for anything else (skins are always 8-bit)."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, plte, trns, hdr = 8, b"", None, None, None
    while pos + 8 <= len(data):
        n, typ = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if typ == b"IHDR":
            hdr = struct.unpack(">IIBBBBB", body)
        elif typ == b"PLTE":
            plte = body
        elif typ == b"tRNS":
            trns = body
        elif typ == b"IDAT":
            idat += body
        elif typ == b"IEND":
            break
    if not hdr:
        raise ValueError("no IHDR")
    w, h, depth, ct, _, _, inter = hdr
    if depth != 8 or inter != 0 or ct not in (0, 2, 3, 4, 6) or not (0 < w <= 1024 and 0 < h <= 1024):
        raise ValueError("unsupported PNG")
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * bpp
    if len(raw) < (stride + 1) * h:
        raise ValueError("truncated")
    prev = bytearray(stride)
    pix: list[RGBA] = []
    for y in range(h):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + b) & 255
            elif f == 3:
                line[i] = (line[i] + ((a + b) >> 1)) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
            elif f != 0:
                raise ValueError("bad filter")
        prev = line
        for x in range(w):
            o = x * bpp
            if ct == 6:
                pix.append((line[o], line[o + 1], line[o + 2], line[o + 3]))
            elif ct == 2:
                pix.append((line[o], line[o + 1], line[o + 2], 255))
            elif ct == 0:
                pix.append((line[o], line[o], line[o], 255))
            elif ct == 4:
                pix.append((line[o], line[o], line[o], line[o + 1]))
            else:
                i = line[o]
                if not plte or i * 3 + 2 >= len(plte):
                    raise ValueError("bad palette")
                pix.append((plte[i * 3], plte[i * 3 + 1], plte[i * 3 + 2], trns[i] if trns and i < len(trns) else 255))
    return w, h, pix


def encode_png(w: int, h: int, pix: list[RGBA]) -> bytes:
    raw = b"".join(b"\x00" + bytes(v for p in pix[y * w:(y + 1) * w] for v in p) for y in range(h))

    def chunk(t: bytes, b: bytes) -> bytes:
        return struct.pack(">I", len(b)) + t + b + struct.pack(">I", zlib.crc32(t + b) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


# ------------------------------------------------------------------------------------- default skin (original art, not a Mojang asset)
class Skin:
    def __init__(self, w: int, h: int, pix: list[RGBA]):
        if w != 64 or h not in (32, 64):
            raise ValueError("skin must be 64x64 or 64x32")
        self.w, self.h, self.pix = w, h, pix

    def at(self, x: int, y: int) -> RGBA:
        return self.pix[y * self.w + x] if 0 <= x < self.w and 0 <= y < self.h else (0, 0, 0, 0)


def _fill(px: list[list[RGBA]], x0: int, y0: int, w: int, h: int, c: RGBA) -> None:
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[y][x] = c


def default_skin() -> Skin:
    """Neutral original character: graphite hair, skin tone, white shirt with an accent line, dark trousers."""
    px = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
    skin, hair, shirt, trs, shoe, accent = (222, 184, 150, 255), (52, 44, 40, 255), (232, 232, 236, 255), (48, 52, 70, 255), (30, 30, 34, 255), (139, 92, 246, 255)
    def box(u, v, w, h, d, cols):          # cols: top, bottom, right, front, left, back
        _fill(px, u + d, v, w, d, cols[0]); _fill(px, u + d + w, v, w, d, cols[1])
        _fill(px, u, v + d, d, h, cols[2]); _fill(px, u + d, v + d, w, h, cols[3])
        _fill(px, u + d + w, v + d, d, h, cols[4]); _fill(px, u + 2 * d + w, v + d, w, h, cols[5])
    box(0, 0, 8, 8, 8, [hair, skin, hair, skin, hair, hair])
    _fill(px, 8 + 1, 8 + 3, 2, 1, (40, 40, 48, 255)); _fill(px, 8 + 5, 8 + 3, 2, 1, (40, 40, 48, 255))        # eyes
    _fill(px, 8, 8, 8, 2, hair)                                                                                  # fringe
    box(16, 16, 8, 12, 4, [shirt, shirt, shirt, shirt, shirt, shirt])
    _fill(px, 20, 16 + 4 + 9, 8, 1, accent)
    for (u, v) in ((40, 16), (32, 48)):
        box(u, v, 4, 12, 4, [shirt, skin, shirt, shirt, shirt, shirt]); _fill(px, u + 4, v + 4 + 9, 4, 3, skin)
    for (u, v) in ((0, 16), (16, 48)):
        box(u, v, 4, 12, 4, [trs, trs, trs, trs, trs, trs]); _fill(px, u + 4, v + 4 + 10, 4, 2, shoe)
    return Skin(64, 64, [p for row in px for p in row])


def load_skin(data: bytes) -> Skin:
    w, h, pix = decode_png(data)
    if (w, h) == (64, 32):                    # legacy skin: mirror the right limbs onto the left
        pix = pix + [(0, 0, 0, 0)] * (64 * 32)
        def blit(sx, sy, dx, dy, bw, bh):
            for yy in range(bh):
                for xx in range(bw):
                    pix[(dy + yy) * 64 + dx + xx] = pix[(sy + yy) * 64 + sx + (bw - 1 - xx)]
        blit(0, 20, 16, 52, 4, 12)
        blit(40, 20, 32, 52, 4, 12)
        h = 64
    return Skin(w, h, pix)


# ------------------------------------------------------------------------------------------------- Mojang lookup
NAME_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")


def fetch_skin(name: str, http=None) -> bytes:
    """Public Mojang endpoints, no account needed. Raises ValueError/RuntimeError with a readable reason."""
    if not NAME_RE.match(name or ""):
        raise ValueError("Ник должен быть 3–16 символов: латиница, цифры, _")
    if http is None:
        from .core import http_request as http
    try:
        prof = json.loads(http(f"https://api.mojang.com/users/profiles/minecraft/{name}", timeout=15, max_bytes=1 << 14))
        sess = json.loads(http(f"https://sessionserver.mojang.com/session/minecraft/profile/{prof['id']}", timeout=15, max_bytes=1 << 16))
        tex = next(p for p in sess["properties"] if p["name"] == "textures")
        url = json.loads(base64.b64decode(tex["value"]))["textures"]["SKIN"]["url"]
        if url.startswith("http://textures.minecraft.net/"):      # Mojang still returns http:// here; we only ever fetch over TLS
            url = "https://" + url[len("http://"):]
        if not url.startswith("https://textures.minecraft.net/"):
            raise RuntimeError("неожиданный адрес скина")
        return http(url, timeout=15, max_bytes=1 << 17)
    except (KeyError, StopIteration, ValueError) as e:
        raise RuntimeError(f"Профиль «{name}» не найден или без скина ({e.__class__.__name__})") from None


# ----------------------------------------------------------------------------------------------------- renderer
BOXES = {   # name: (origin xyz of min corner in model px, size w,h,d, uv u,v)   y up, x = character's left (+) / right (-)
    "head": ((-4, 24, -4), (8, 8, 8), (0, 0)), "body": ((-4, 12, -2), (8, 12, 4), (16, 16)),
    "rarm": ((-8, 12, -2), (4, 12, 4), (40, 16)), "larm": ((4, 12, -2), (4, 12, 4), (32, 48)),
    "rleg": ((-4, 0, -2), (4, 12, 4), (0, 16)), "lleg": ((0, 0, -2), (4, 12, 4), (16, 48)),
}
FACES = ["top", "bottom", "right", "front", "left", "back"]


def _rot(p, yaw, pitch):
    x, y, z = p
    c, s = math.cos(yaw), math.sin(yaw)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(pitch), math.sin(pitch)
    y, z = y * c - z * s, y * s + z * c
    return x, y, z


def _hex(c, shade=1.0):
    return "#%02x%02x%02x" % (min(255, int(c[0] * shade)), min(255, int(c[1] * shade)), min(255, int(c[2] * shade)))


def box_quads(origin, size, uv, tex_at, yaw, pitch, scale, cx, cy, grow=0.0):
    """Visible faces of one skin box, one quad per skin pixel, with simple directional shading. Returns [(depth, points, colour)]."""
    (ox, oy, oz), (w, h, d), (u, v) = origin, size, uv
    ox, oy, oz, w, h, d = ox - grow, oy - grow, oz - grow, w + 2 * grow, h + 2 * grow, d + 2 * grow
    iw, ih, idp = size
    # face: (normal, texture rect (x, y, w, h), function (i, j)->3D corner offsets)
    out = []
    faces = {
        "front": ((0, 0, -1), (u + idp, v + idp, iw, ih)), "back": ((0, 0, 1), (u + 2 * idp + iw, v + idp, iw, ih)),
        "right": ((-1, 0, 0), (u, v + idp, idp, ih)), "left": ((1, 0, 0), (u + idp + iw, v + idp, idp, ih)),
        "top": ((0, 1, 0), (u + idp, v, iw, idp)), "bottom": ((0, -1, 0), (u + idp + iw, v, iw, idp)),
    }
    for name, (n, (tx, ty, tw, th)) in faces.items():
        rn = _rot(n, yaw, pitch)
        if rn[2] >= 0:                                           # camera looks along +z after rotation: keep faces turned toward it
            continue
        shade = 0.55 + 0.45 * max(0.0, -rn[2]) + 0.12 * rn[1]
        for j in range(th):
            for i in range(tw):
                col = tex_at(tx + i, ty + j)
                if col[3] < 128:
                    continue
                if name == "front":   a = (ox + w * (1 - i / tw), oy + h * (1 - j / th), oz); b = (-w / tw, 0, 0); c = (0, -h / th, 0)
                elif name == "back":  a = (ox + w * (i / tw), oy + h * (1 - j / th), oz + d); b = (w / tw, 0, 0); c = (0, -h / th, 0)
                elif name == "right": a = (ox, oy + h * (1 - j / th), oz + d * (1 - i / tw)); b = (0, 0, -d / tw); c = (0, -h / th, 0)
                elif name == "left":  a = (ox + w, oy + h * (1 - j / th), oz + d * (i / tw)); b = (0, 0, d / tw); c = (0, -h / th, 0)
                elif name == "top":   a = (ox + w * (1 - i / tw), oy + h, oz + d * (j / th)); b = (-w / tw, 0, 0); c = (0, 0, d / th)
                else:                 a = (ox + w * (1 - i / tw), oy, oz + d * (1 - j / th)); b = (-w / tw, 0, 0); c = (0, 0, -d / th)
                pts3 = [a, (a[0] + b[0], a[1] + b[1], a[2] + b[2]), (a[0] + b[0] + c[0], a[1] + b[1] + c[1], a[2] + b[2] + c[2]), (a[0] + c[0], a[1] + c[1], a[2] + c[2])]
                r = [_rot(p, yaw, pitch) for p in pts3]
                out.append((sum(p[2] for p in r) / 4, [(cx + p[0] * scale, cy - p[1] * scale) for p in r], _hex(col, shade)))
    return out


def pedestal_quads(yaw, pitch, scale, cx, cy, seed=7):
    """Dark ore-like cube under the character (original procedural texture)."""
    rnd = random.Random(seed)
    tex = {}
    def at(x, y):
        k = (x, y)
        if k not in tex:
            g = 22 + rnd.randint(0, 10)
            if rnd.random() < 0.08:
                g = 70 + rnd.randint(0, 60)
            tex[k] = (g, g, g + 2, 255)
        return tex[k]
    # a 16x16x16 block whose top is at y=0; texture atlas 16 wide so every face reads the same noise
    q = []
    q += box_quads((-16, -16, -16), (32, 16, 32), (0, 0), lambda x, y: at(x % 16, y % 16), yaw, pitch, scale, cx, cy)
    return q


def scene(skin: Skin | None, yaw=math.radians(-32), pitch=math.radians(-18), scale=5.2, cx=0.0, cy=0.0):
    """Everything to draw, back to front: [(points, colour)]."""
    sk = skin or default_skin()
    quads = pedestal_quads(yaw, pitch, scale, cx, cy)
    parts = []
    for name, (o, s, uv) in BOXES.items():
        qs = box_quads(o, s, uv, sk.at, yaw, pitch, scale, cx, cy, 0.0)
        if name == "head":                                   # hat layer
            qs += box_quads(o, s, (32, 0), sk.at, yaw, pitch, scale, cx, cy, 0.5)
        parts.append((sum(q[0] for q in qs) / max(1, len(qs)), qs))
    for _, qs in sorted(parts, key=lambda x: -x[0]):
        quads += sorted(qs, key=lambda q: -q[0])
    return [(pts, col) for _, pts, col in quads]
