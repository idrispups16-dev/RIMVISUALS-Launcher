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
}
