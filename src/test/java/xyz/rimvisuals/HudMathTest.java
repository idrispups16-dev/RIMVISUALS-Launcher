package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.modules.DamageTint;

import static org.junit.jupiter.api.Assertions.*;

class HudMathTest {
    @Test void rateCounterCountsOnlyInsideTheWindow() {
        var c = new HudMath.RateCounter(1);
        long s = 1_000_000_000L;
        c.hit(0); c.hit(s / 2); c.hit(s - 1);
        assertEquals(3, c.count(s - 1));
        assertEquals(2, c.count(s + 1), "the click at t=0 has left the 1 s window");
        assertEquals(0, c.count(3 * s));
    }
    @Test void fadeOutIsLinearAndBounded() {
        assertEquals(1f, HudMath.fadeOut(0, 300), 1e-6);
        assertEquals(.5f, HudMath.fadeOut(150, 300), 1e-6);
        assertEquals(0f, HudMath.fadeOut(300, 300), 1e-6);
        assertEquals(0f, HudMath.fadeOut(900, 300), 1e-6);
        assertEquals(0f, HudMath.fadeOut(10, 0), 1e-6);
    }
    @Test void bpsMatchesDistanceOverTime() {
        assertEquals(5.0, HudMath.bps(3, 4, 1), 1e-9);
        assertEquals(10.0, HudMath.bps(3, 4, .5), 1e-9);
        assertEquals(0.0, HudMath.bps(1, 1, 0), 0);
    }
    @Test void clockFormatsMinutesAndHours() {
        assertEquals("0:05", HudMath.clock(5));
        assertEquals("1:05", HudMath.clock(65));
        assertEquals("1:01:01", HudMath.clock(3661));
        assertEquals("0:00", HudMath.clock(-4));
    }
    @Test void effectTimeShowsInfiniteAndTicks() {
        assertEquals("**", HudMath.effectTime(0, true));
        assertEquals("1:30", HudMath.effectTime(1800, false));
    }
    @Test void damageTintIntensityFollowsHurtTimer() {
        assertEquals(0f, DamageTint.intensity(0, 10), 0);
        assertEquals(1f, DamageTint.intensity(10, 10), 1e-6);
        assertEquals(.5f, DamageTint.intensity(5, 10), 1e-6);
        assertEquals(0f, DamageTint.intensity(5, 0), 0);
        assertEquals(1f, DamageTint.intensity(20, 10), 0);
    }
    @Test void snapRoundsToGridAndZeroDisables() {
        assertEquals(16.0, HudMath.snap(13, 8), 0);
        assertEquals(8.0, HudMath.snap(11, 8) - 0, 0);
        assertEquals(13.4, HudMath.snap(13.4, 0), 0);
        assertEquals(0.0, HudMath.snap(3, 8), 0);
    }
    @Test void scaledSizeNeverCollapses() {
        assertEquals(40, HudMath.scaled(20, 2));
        assertEquals(10, HudMath.scaled(20, .5));
        assertEquals(1, HudMath.scaled(0, 1));
    }

    @org.junit.jupiter.api.Test void guideSnapAndMatch() {
        int[] lines = {0, 100, 200};
        assertEquals(100, xyz.rimvisuals.core.HudMath.guideSnap(97, 40, lines, 4), 1e-9);      // left edge to 100
        assertEquals(160, xyz.rimvisuals.core.HudMath.guideSnap(158, 40, lines, 4), 1e-9);     // right edge (160+40) to 200
        assertEquals(80, xyz.rimvisuals.core.HudMath.guideSnap(82, 40, lines, 4), 1e-9);       // centre (80+20) to 100
        assertEquals(50, xyz.rimvisuals.core.HudMath.guideSnap(50, 40, lines, 4), 1e-9);       // nothing close
        assertEquals(100, xyz.rimvisuals.core.HudMath.matchedGuide(100, 40, lines));
        assertEquals(200, xyz.rimvisuals.core.HudMath.matchedGuide(160, 40, lines));
        assertEquals(-1, xyz.rimvisuals.core.HudMath.matchedGuide(50, 40, lines));
    }
}
