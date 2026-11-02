package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.*;

import static org.junit.jupiter.api.Assertions.*;

class WorldTweaksTest {
    @Test void nightVisionFadesLinearlyInsteadOfFlickering() {
        assertEquals(1f, NightVisionFade.strength(5000));
        assertEquals(1f, NightVisionFade.strength(200));
        assertEquals(.5f, NightVisionFade.strength(100), 1e-6);
        assertEquals(0f, NightVisionFade.strength(0));
        float prev = 1f;
        for (int d = 199; d >= 0; d--) { float s = NightVisionFade.strength(d); assertTrue(s <= prev); prev = s; }
    }

    @Test void fullbrightNeverDarkensVanillaGamma() {
        assertEquals(12f, Fullbright.effective(1f, 12));
        assertEquals(15f, Fullbright.effective(15f, 12));
    }

    @Test void goldenAppleGlowScalesWithAbsorption() {
        assertEquals(0f, GoldenAppleGlow.intensity(0f));
        assertTrue(GoldenAppleGlow.intensity(4f) > .45f);
        assertEquals(1f, GoldenAppleGlow.intensity(16f), 1e-6);
        assertEquals(1f, GoldenAppleGlow.intensity(100f), 1e-6);
    }

    @Test void tintMixesAndKeepsAlpha() {
        assertEquals(0xFF336699, xyz.rimvisuals.core.WorldColors.tint(0xFF336699, 0xFF0000, 0));
        assertEquals(0xFFFF0000, xyz.rimvisuals.core.WorldColors.tint(0xFF336699, 0xFF0000, 1));
        assertEquals(0x80808080, xyz.rimvisuals.core.WorldColors.tint(0x80FFFFFF, 0x000000, .5) | 0x00010101 & 0, "half way, alpha kept");
        assertEquals(0x00FFFFFF, xyz.rimvisuals.core.WorldColors.tint(0x00FFFFFF, 0x123456, 0));
    }

    @Test void signatureChangesWithTerrainSettingsOnly() {
        int a = xyz.rimvisuals.core.WorldColors.signature();
        xyz.rimvisuals.core.Modules.WATER_COLOR.enabled = true;
        int b = xyz.rimvisuals.core.WorldColors.signature();
        assertNotEquals(a, b);
        xyz.rimvisuals.core.Modules.WEATHER_COLOR.enabled = true;
        assertEquals(b, xyz.rimvisuals.core.WorldColors.signature(), "weather needs no rebuild");
        xyz.rimvisuals.core.Modules.WATER_COLOR.enabled = false;
        xyz.rimvisuals.core.Modules.WEATHER_COLOR.enabled = false;
    }

    @Test void handGlowShellsGrowOutwardAndFade() {
        int n = 3;
        float prevScale = 1f, prevAlpha = 2f;
        float sum = 0;
        for (int i = 0; i < n; i++) {
            float sc = HandGlow.shellScale(i, n, .09), a = HandGlow.shellAlpha(i, n, .36);
            assertTrue(sc > prevScale); assertTrue(a < prevAlpha);
            prevScale = sc; prevAlpha = a; sum += a;
        }
        assertEquals(1.09f, prevScale, 1e-6);
        assertEquals(.36f, sum, 1e-5);
    }
}
