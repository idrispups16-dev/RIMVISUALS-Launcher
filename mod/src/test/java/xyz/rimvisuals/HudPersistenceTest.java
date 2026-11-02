package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Modules;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class HudPersistenceTest {
    @Test void positionScaleOpacityAndAccentSurviveSaveAndLoad() throws Exception {
        assertFalse(Modules.HUD.isEmpty());
        HudModule m = Modules.HUD.get(0);
        m.enabled = true;
        m.posX.set(123); m.posY.set(77); m.scale.set(1.35); m.elementOpacity.set(.4);
        m.customAccent.value = true; m.accentColor.rgb = 0x12AB34;
        StringWriter w = new StringWriter();
        Modules.snapshot().store(w, "t");               // what config.properties contains
        Properties disk = new Properties();
        disk.load(new StringReader(w.toString()));
        m.posX.set(0); m.posY.set(0); m.scale.set(1); m.elementOpacity.set(1); m.customAccent.value = false; m.accentColor.rgb = 0;
        Modules.apply(disk);                              // what happens on the next launch
        assertEquals(123, m.posX.value, 1e-9);
        assertEquals(77, m.posY.value, 1e-9);
        assertEquals(1.35, m.scale.value, 1e-9);
        assertEquals(.4, m.elementOpacity.value, 1e-9);
        assertTrue(m.customAccent.value);
        assertEquals(0x12AB34, m.accentColor.rgb);
    }

    @Test void everyHudElementCanBeMovedScaledFadedAndPersisted() throws Exception {
        assertTrue(Modules.HUD.size() >= 14);
        int i = 0;
        for (HudModule m : Modules.HUD) {
            m.posX.set(10 + i); m.posY.set(20 + i); m.scale.set(.5 + (i % 7) * .2); m.elementOpacity.set(.2 + (i % 5) * .15);
            i++;
        }
        StringWriter w = new StringWriter();
        Modules.snapshot().store(w, "t");
        Properties disk = new Properties();
        disk.load(new StringReader(w.toString()));
        for (HudModule m : Modules.HUD) { m.posX.set(0); m.posY.set(0); m.scale.set(1); m.elementOpacity.set(1); }
        Modules.apply(disk);
        i = 0;
        for (HudModule m : Modules.HUD) {
            assertEquals(10 + i, m.posX.value, 1e-9, m.id);
            assertEquals(20 + i, m.posY.value, 1e-9, m.id);
            assertEquals(Math.round((.5 + (i % 7) * .2) * 20) / 20.0, m.scale.value, 1e-9, m.id);
            assertEquals(Math.round((.2 + (i % 5) * .15) * 20) / 20.0, m.elementOpacity.value, 1e-9, m.id);
            i++;
        }
    }

    @Test void opacityMultipliesAlphaAndNeverTouchesOpaqueDefault() {
        int c = 0xFF336699;
        assertEquals(c, Gfx.ha(c));
        Gfx.hudAlpha = .5f;
        try {
            assertEquals(0x80336699, Gfx.ha(c));
        } finally { Gfx.hudAlpha = 1f; }
    }
}
