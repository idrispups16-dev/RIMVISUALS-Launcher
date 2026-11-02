package xyz.rimvisuals;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.Easing;
import xyz.rimvisuals.core.Notifier;
import xyz.rimvisuals.modules.AnchorFade;
import xyz.rimvisuals.modules.ExplosionEffects;

import static org.junit.jupiter.api.Assertions.*;

class EventEffectsTest {
    @Test void easingEndpointsAndMonotonic() {
        for (int m = 0; m < Easing.NAMES.length; m++) {
            assertEquals(0f, Easing.apply(m, 0f), 1e-6);
            assertEquals(1f, Easing.apply(m, 1f), 1e-6);
            float prev = -1;
            for (int i = 0; i <= 20; i++) { float v = Easing.apply(m, i / 20f); assertTrue(v >= prev); prev = v; }
        }
        assertEquals(0f, Easing.apply(0, -3f));
        assertEquals(1f, Easing.apply(3, 9f));
    }

    @Test void anchorExplosionIsRecognisedOnlyAtExactBlockCentreWithRadiusFive() {
        assertNotNull(AnchorFade.anchorCandidate(new Vec3d(10.5, 64.5, -3.5), 5.0f));
        assertEquals(10, AnchorFade.anchorCandidate(new Vec3d(10.5, 64.5, -3.5), 5.0f).getX());
        assertNull(AnchorFade.anchorCandidate(new Vec3d(10.5, 64.5, -3.5), 4.0f), "crystal radius");
        assertNull(AnchorFade.anchorCandidate(new Vec3d(10.5, 64.5, -3.5), 6.0f), "bed radius");
        assertNull(AnchorFade.anchorCandidate(new Vec3d(10.0, 64.5, -3.5), 5.0f), "off centre (TNT etc.)");
        assertNull(AnchorFade.anchorCandidate(new Vec3d(10.5, 64.0, -3.5), 5.0f), "off centre in Y");
    }

    @Test void anchorGhostCurvesRespectSettings() {
        AnchorFade a = new AnchorFade();
        a.easing.index = 0;
        a.startOpacity.set(.8); a.endScale.set(.2);
        assertEquals(.8f, a.alphaAt(0f), 1e-6);
        assertEquals(0f, a.alphaAt(1f), 1e-6);
        assertEquals(1f, a.scaleAt(0f), 1e-6);
        assertEquals(.2f, a.scaleAt(1f), 1e-6);
        a.shrink.value = false;
        assertEquals(1f, a.scaleAt(1f), 1e-6);
        a.fade.value = false;
        assertEquals(.8f, a.alphaAt(.5f), 1e-6, "no fade keeps opacity until the end");
        assertTrue(a.duration.min >= 200 && a.duration.max <= 500);
    }

    @Test void explosionParticleCountScalesAndIsCapped() {
        assertEquals(24, ExplosionEffects.count(24, 4f));
        assertEquals(48, ExplosionEffects.count(24, 8f));
        assertEquals(80, ExplosionEffects.count(60, 20f));
        assertEquals(4, ExplosionEffects.count(4, .5f));
    }

    @Test void durabilityWarningHasHysteresis() {
        assertTrue(Notifier.lowDurability(900, 1000, 10));
        assertFalse(Notifier.lowDurability(800, 1000, 10));
        assertFalse(Notifier.lowDurability(0, 0, 10), "undamageable");
        assertFalse(Notifier.rearm(900, 1000, 10));
        assertTrue(Notifier.rearm(800, 1000, 10));
    }
}
