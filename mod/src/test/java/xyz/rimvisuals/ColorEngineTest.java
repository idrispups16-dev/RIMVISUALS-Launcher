package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.color.*;
import xyz.rimvisuals.core.Setting;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ColorEngineTest {
    private static int ch(int argb, int shift) { return (argb >>> shift) & 255; }

    @Test void hsvRoundTripsAcrossTheCube() {
        for (int r = 0; r < 256; r += 17) for (int g = 0; g < 256; g += 17) for (int b = 0; b < 256; b += 17) {
            int rgb = (r << 16) | (g << 8) | b;
            float[] hsv = Hsv.fromRgb(rgb);
            int back = Hsv.toRgb(hsv[0], hsv[1], hsv[2]);
            assertEquals(r, ch(back, 16), 1); assertEquals(g, ch(back, 8), 1); assertEquals(b, ch(back, 0), 1);
        }
    }

    @Test void hsvPrimaries() {
        assertEquals(0xFF0000, Hsv.toRgb(0f, 1f, 1f) & 0xFFFFFF);
        assertEquals(0x00FF00, Hsv.toRgb(1f / 3f, 1f, 1f) & 0xFFFFFF);
        assertEquals(0x0000FF, Hsv.toRgb(2f / 3f, 1f, 1f) & 0xFFFFFF);
        assertEquals(0x000000, Hsv.toRgb(.5f, 1f, 0f) & 0xFFFFFF);
        assertEquals(0xFFFFFF, Hsv.toRgb(.5f, 0f, 1f) & 0xFFFFFF);
    }

    @Test void hexParsing() {
        assertEquals(0xFF8B5CF6L, Rgb.parse("8B5CF6"));
        assertEquals(0xFF8B5CF6L, Rgb.parse("#8b5cf6"));
        assertEquals(0xFFFF8800L, Rgb.parse("#f80"));
        assertEquals(0x808B5CF6L, Rgb.parse("#8B5CF680"));
        for (String bad : new String[]{"", "#", "12345", "GGGGGG", "#1234567", null, "8B5CF6 7"}) assertEquals(Rgb.INVALID, Rgb.parse(bad), String.valueOf(bad));
        assertEquals("0000FF", Rgb.hex(0x0000FF));
    }

    @Test void rgbLerpAndAlpha() {
        int mid = Rgb.lerp(0x000000, 0xFFFFFF, .5f);
        for (int sh = 0; sh <= 16; sh += 8) assertEquals(127, ch(mid, sh), 1);
        assertEquals(0x000000, Rgb.lerp(0x000000, 0xFFFFFF, 0f) & 0xFFFFFF);
        assertEquals(0xFFFFFF, Rgb.lerp(0x000000, 0xFFFFFF, 1f) & 0xFFFFFF);
        assertEquals(128, ch(Rgb.withAlpha(0xFF0000, .5f), 24), 1);
        assertEquals(0, ch(Rgb.withAlpha(0xFF0000, -3f), 24));
        assertEquals(255, ch(Rgb.withAlpha(0xFF0000, 9f), 24));
    }

    @Test void solidIsConstantAndKeepsAlpha() {
        ColorSpec s = ColorSpec.solid(0x123456, .5f);
        int a = s.resolve(0, 0), b = s.resolve(99.7, .9f);
        assertEquals(a, b);
        assertEquals(0x123456, a & 0xFFFFFF);
        assertEquals(128, ch(a, 24), 1);
    }

    @Test void rainbowMovesWithTimeAndPosition() {
        ColorSpec r = ColorSpec.rainbow(.5f, 1f, 1f, 1f);
        assertNotEquals(r.resolve(0, 0), r.resolve(.5, 0));
        assertNotEquals(r.resolve(0, 0), r.resolve(0, .5f));
        assertEquals(r.resolve(0, 0) & 0xFFFFFF, r.resolve(2, 0) & 0xFFFFFF);   // full loop at 0.5 cycles/s
        assertEquals(255, ch(r.resolve(1.234, .3f), 24));
    }

    @Test void gradientEndpointsAndLoop() {
        ColorSpec g = ColorSpec.gradient(0f, 1f, false, 0xFF0000, 0x0000FF);
        assertEquals(0xFF0000, g.resolve(0, 0f) & 0xFFFFFF);
        assertEquals(0x0000FF, g.resolve(0, 1f) & 0xFFFFFF);
        int mid = g.resolve(0, .5f);
        assertEquals(128, ch(mid, 16), 2); assertEquals(128, ch(mid, 0), 2);
        assertEquals(0x0000FF, g.resolve(0, 5f) & 0xFFFFFF);                    // clamped
    }

    @Test void pulseStaysWithinDepth() {
        ColorSpec p = ColorSpec.pulse(0xFFFFFF, 1f, .5f);
        int lo = 255, hi = 0;
        for (int i = 0; i < 200; i++) { int c = ch(p.resolve(i / 100.0, 0), 0); lo = Math.min(lo, c); hi = Math.max(hi, c); }
        assertTrue(hi > lo, "pulse must vary");
        assertTrue(lo >= 100, "depth .5 must not go fully dark: " + lo);
    }

    @Test void stopsAreLimitedAndNormalised() {
        int[] many = new int[20];
        ColorSpec g = ColorSpec.gradient(0f, 0f, false, many);
        assertTrue(g.stopCount() <= ColorSpec.MAX_STOPS);
        assertEquals(1, ColorSpec.gradient(0f, 0f, false).stopCount());          // empty list falls back to a colour
    }

    @Test void serializeParseRoundTripsEveryMode() {
        ColorSpec[] all = {
            ColorSpec.solid(0x8B5CF6), ColorSpec.solid(0x8B5CF6, .4f), ColorSpec.rainbow(.3f, .7f, .8f, .9f),
            ColorSpec.gradient(.2f, .5f, true, 0xFF0000, 0x00FF00, 0x0000FF), ColorSpec.pulse(0x00FFFF, 1.5f, .6f),
            ColorSpec.fade(0xFFAA00, .8f, .4f), ColorSpec.cycle(.25f, 0xFF0000, 0x00FF00).withAlpha(.7f)
        };
        for (ColorSpec s : all) {
            ColorSpec p = ColorSpec.parse(s.serialize());
            assertNotNull(p, s.serialize());
            assertEquals(s.serialize(), p.serialize());
            for (double t : new double[]{0, .37, 5.5}) assertEquals(s.resolve(t, .3f), p.resolve(t, .3f), s.serialize());
        }
    }

    @Test void plainSolidSavesAsLegacyHex() {
        assertEquals("8B5CF6", ColorSpec.solid(0x8B5CF6).serialize());
        assertTrue(ColorSpec.solid(0x8B5CF6, .5f).serialize().startsWith(ColorSpec.PREFIX));
    }

    @Test void parseRejectsGarbage() {
        for (String bad : new String[]{null, "", "zzz", "c2|", "c2|nope|FF0000", "c2|solid|XYZ", "c2|solid|FF0000|a=abc", "c2|gradient|" + "FF0000,".repeat(12) + "00FF00"})
            assertNull(ColorSpec.parse(bad), String.valueOf(bad));
        assertNull(ColorSpec.parse("c2|solid|FF0000|" + "x".repeat(500)));
    }

    @Test void parseToleratesOutOfRangeNumbers() {
        ColorSpec s = ColorSpec.parse("c2|rainbow|FF0000|a=9|s=-5|sat=7|br=-1");
        assertNotNull(s);
        int c = s.resolve(1, 0);
        assertEquals(255, ch(c, 24));
    }

    @Test void settingColorLoadsLegacyAndNewFormats() {
        Setting.Color c = new Setting.Color("c", 0x8B5CF6);
        c.load("#112233"); assertEquals(0x112233, c.rgb); assertEquals("112233", c.save());
        c.load("c2|gradient|FF0000,0000FF|a=1|s=.5|p=1|d=0|sat=1|br=1|l=1");
        assertEquals(0xFF0000, c.rgb); assertTrue(c.save().startsWith("c2|gradient"));
        String keep = c.save(); c.load("not a colour"); assertEquals(keep, c.save());   // malformed keeps the old value
    }

    @Test void directRgbAssignmentStillWinsOverStaleSpec() {
        Setting.Color c = new Setting.Color("c", 0x8B5CF6);
        c.setSpec(ColorSpec.rainbow(1f, 1f, 1f, 1f));
        c.rgb = 0x00FF00;                                        // legacy writer (old ClickGUI / presets)
        assertEquals("00FF00", c.save());
        assertEquals(0x00FF00, c.argb(3.0, .2f) & 0xFFFFFF);
    }

    @Test void builtinPresetsAreValid() {
        assertTrue(ColorPresets.BUILTIN.size() >= 10);
        assertTrue(ColorPresets.BUILTIN.containsKey("Violet") || ColorPresets.BUILTIN.keySet().stream().anyMatch(k -> k.toLowerCase().contains("viol")));
        for (var e : ColorPresets.BUILTIN.entrySet()) {
            assertNotNull(ColorSpec.parse(e.getValue().serialize()), e.getKey());
            assertEquals(255, ch(e.getValue().resolve(0, 0), 24) , e.getKey());
        }
    }

    @Test void userPresetsPersistAtomicallyAndAreBounded() throws Exception {
        Path dir = Files.createTempDirectory("rvcol");
        Path f = dir.resolve("presets.txt");
        ColorPresets.User u = new ColorPresets.User(f);
        assertTrue(u.put("Мой цвет", ColorSpec.gradient(.1f, .5f, true, 0xFF0000, 0x0000FF)));
        assertTrue(u.put("My red", ColorSpec.solid(0xFF0000)));
        assertFalse(u.put("   ", ColorSpec.solid(1)));
        assertFalse(u.put("x", null));
        assertFalse(u.put(ColorPresets.BUILTIN.keySet().iterator().next(), ColorSpec.solid(1)), "builtin names are reserved");
        ColorPresets.User again = new ColorPresets.User(f);
        assertEquals(2, again.all().size());
        assertEquals(ColorSpec.solid(0xFF0000), again.all().get("My red"));
        assertEquals("Мой цвет", ColorPresets.User.cleanName("Мой цвет<>=\n"));
        assertEquals(32, ColorPresets.User.cleanName("a".repeat(100)).length());
        assertTrue(again.remove("My red")); assertFalse(again.remove("My red"));
        for (int i = 0; i < 80; i++) again.put("p" + i, ColorSpec.solid(i));
        assertTrue(again.all().size() <= 64);
        try (var s = Files.list(dir)) { assertEquals(1, s.count(), "no temp files left behind"); }
    }

    @Test void userPresetsSurviveCorruptFile() throws Exception {
        Path f = Files.createTempFile("rvcol", ".txt");
        Files.writeString(f, "garbage line\n=novalue\nGood=FF0000\nBad=zzz\n");
        ColorPresets.User u = new ColorPresets.User(f);
        assertEquals(1, u.all().size());
        assertTrue(u.all().containsKey("Good"));
    }

    @Test void colorFadeBlendsAndSettles() {
        ColorFade f = new ColorFade(ColorSpec.solid(0xFF0000));
        f.set(ColorSpec.solid(0x0000FF), .5f);
        assertFalse(f.settled());
        assertEquals(0xFF0000, f.resolve(0, 0) & 0xFFFFFF);
        f.advance(.25f);
        int m = f.resolve(0, 0);
        assertTrue(ch(m, 16) > 0 && ch(m, 16) < 255 && ch(m, 0) > 0, "mid-fade is a blend");
        f.advance(1f);
        assertTrue(f.settled());
        assertEquals(0x0000FF, f.resolve(0, 0) & 0xFFFFFF);
        f.set(ColorSpec.solid(0x0000FF), 1f);                    // same target: no restart
        assertTrue(f.settled());
    }

    @Test void pickerModelHexSquareAndHue() {
        ColorPickerModel m = new ColorPickerModel(0x8B5CF6, 1f);
        assertEquals("8B5CF6", m.hex());
        assertTrue(m.setHex("#00ff00")); assertEquals(0x00FF00, m.rgb() & 0xFFFFFF);
        assertFalse(m.setHex("nope")); assertEquals(0x00FF00, m.rgb() & 0xFFFFFF);
        m.dragSquare(0f, 0f); assertEquals(0xFFFFFF, m.rgb() & 0xFFFFFF);
        m.dragSquare(1f, 1f); assertEquals(0x000000, m.rgb() & 0xFFFFFF);
        m.dragSquare(1f, 0f); m.dragHue(0f); assertEquals(0xFF0000, m.rgb() & 0xFFFFFF);
        m.dragAlpha(.5f); assertEquals(128, ch(m.argb(), 24), 1);
        m.dragSquare(-4f, 9f); assertTrue(m.s >= 0 && m.v >= 0);
        m.setRgbChannels(0, 0, 255); assertEquals(0x0000FF, m.rgb() & 0xFFFFFF);
    }

    @Test void pickerKeepsHueWhenColourGoesGrey() {
        ColorPickerModel m = new ColorPickerModel(0x00FF00, 1f);
        float h = m.h;
        m.setRgb(0x000000);
        assertEquals(h, m.h, 1e-6f);        // dragging to black must not snap the hue slider to red
    }
}
