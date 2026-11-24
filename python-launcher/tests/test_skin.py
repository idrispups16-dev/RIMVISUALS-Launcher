import base64, json, math
import pytest
from rimvisuals_launcher import skin as S


def test_png_roundtrip_and_default_skin():
    sk = S.default_skin()
    assert (sk.w, sk.h) == (64, 64)
    w, h, pix = S.decode_png(S.encode_png(sk.w, sk.h, sk.pix))
    assert (w, h) == (64, 64) and pix == sk.pix
    assert sk.at(8 + 4, 8 + 5)[3] == 255                                   # face drawn
    assert S.load_skin(S.encode_png(64, 64, sk.pix)).at(0, 0) == sk.at(0, 0)


def test_decode_rejects_garbage():
    for bad in (b"", b"nope", b"\x89PNG\r\n\x1a\n"):
        with pytest.raises(ValueError):
            S.decode_png(bad)


def test_legacy_64x32_skin_is_upgraded():
    pix = [(10, 20, 30, 255)] * (64 * 32)
    sk = S.load_skin(S.encode_png(64, 32, pix))
    assert (sk.w, sk.h) == (64, 64) and sk.at(17, 52) == (10, 20, 30, 255)


def test_scene_draws_character_on_pedestal_front_to_back():
    q = S.scene(None)
    assert 1200 < len(q) < 6000
    xs = [p[0] for pts, _ in q for p in pts]; ys = [p[1] for pts, _ in q for p in pts]
    assert min(xs) < 0 < max(xs) and min(ys) < 0 < max(ys)
    assert all(c.startswith("#") and len(c) == 7 for _, c in q)
    assert q != S.scene(None, yaw=math.radians(30))                        # rotation changes the picture


def test_fetch_skin_flow_with_fake_http_and_validation():
    sk = S.default_skin()
    png = S.encode_png(64, 64, sk.pix)
    tex = base64.b64encode(json.dumps({"textures": {"SKIN": {"url": "https://textures.minecraft.net/texture/abc"}}}).encode()).decode()
    def http(url, **kw):
        if "api.mojang.com" in url: return json.dumps({"id": "0" * 32, "name": "Steve"}).encode()
        if "sessionserver" in url: return json.dumps({"properties": [{"name": "textures", "value": tex}]}).encode()
        assert url.startswith("https://textures.minecraft.net/"); return png
    assert S.load_skin(S.fetch_skin("Rimzuk222", http)).at(0, 0) == sk.at(0, 0)
    for bad in ("a", "has space", "x" * 40, "", "ник"):
        with pytest.raises(ValueError):
            S.fetch_skin(bad, http)
    evil = base64.b64encode(json.dumps({"textures": {"SKIN": {"url": "https://evil.example/x"}}}).encode()).decode()
    with pytest.raises(RuntimeError):
        S.fetch_skin("Steve", lambda u, **k: json.dumps({"id": "1", "properties": [{"name": "textures", "value": evil}]}).encode())
