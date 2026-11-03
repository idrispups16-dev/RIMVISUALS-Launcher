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
    private Properties before;
    @org.junit.jupiter.api.BeforeEach void capture() { before = Modules.snapshot(); }
    @org.junit.jupiter.api.AfterEach void restore() { Modules.apply(before); }

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
        assertEquals(Modules.ALL.stream().filter(HudModule.class::isInstance).count(), Modules.HUD.size());
        assertTrue(Modules.HUD.containsAll(java.util.List.of(Modules.INFO, Modules.WATERMARK,
            Modules.CPS_HUD, Modules.KEYSTROKES_HUD, Modules.ARMOR_HUD, Modules.INVENTORY_HUD,
            Modules.POTION_HUD, Modules.TARGET_HUD, Modules.COOLDOWNS_HUD, Modules.NOTIFICATIONS,
            Modules.SPEED_HUD, Modules.SESSION_HUD, Modules.PERFORMANCE_HUD)));
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

    @Test void allModuleSettingsAndHudSurviveDiskJsonAndBackup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var store=new xyz.rimvisuals.core.ConfigStore(dir);
        int i=0;
        for(var m:Modules.HUD){m.posX.set(31+i++);m.posY.set(53+i);m.scale.set(1.25);m.elementOpacity.set(.35);m.customAccent.value=true;m.accentColor.rgb=0x1234ab;}
        Modules.VIEWMODEL.main.x.set(.2);Modules.SWING.intensity.set(.7);
        Modules.PERFORMANCE.preset.index=2;
        Properties expected=Modules.snapshot();
        assertTrue(store.create("PvP",expected));store.backup(expected);
        var json=store.exportJson("PvP");String imported=store.importJson(json).orElseThrow();
        var disk=store.load(imported).orElseThrow();
        assertEquals(expected,disk);assertEquals(expected,store.latestBackup().orElseThrow());
        Modules.apply(before);Modules.apply(disk);
        Properties actual=Modules.snapshot();
        for(String key:expected.stringPropertyNames()) {
            String a=expected.getProperty(key),b=actual.getProperty(key);
            if(!a.equals(b)) assertEquals(Double.parseDouble(a),Double.parseDouble(b),1e-8,key);
        }
    }

    @Test void alignmentKeepsOversizedAndNormalElementsInBounds() {
        assertEquals(0, xyz.rimvisuals.core.HudMath.align(100, 120, 2));
        assertEquals(40, xyz.rimvisuals.core.HudMath.align(100, 20, 1));
        assertEquals(80, xyz.rimvisuals.core.HudMath.align(100, 20, 2));
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
