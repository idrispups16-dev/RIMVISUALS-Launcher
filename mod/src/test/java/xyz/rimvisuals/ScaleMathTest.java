package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.ScaleMath;

import static org.junit.jupiter.api.Assertions.*;

class ScaleMathTest {
    @Test void growInStartsAtZeroEndsAtOneAndIsMonotonic() {
        assertEquals(0f, ScaleMath.growIn(0, 10), 1e-6);
        assertEquals(1f, ScaleMath.growIn(10, 10), 1e-6);
        assertEquals(1f, ScaleMath.growIn(500, 10), 1e-6);
        float prev = -1;
        for (int i = 0; i <= 20; i++) { float v = ScaleMath.growIn(i * .5f, 10); assertTrue(v >= prev); prev = v; }
    }
    @Test void growInDisabledWhenDurationIsZero() { assertEquals(1f, ScaleMath.growIn(0, 0), 0); }
    @Test void shrinkOutKeepsFullSizeUntilStartThenReachesEnd() {
        assertEquals(1f, ScaleMath.shrinkOut(.3f, .6f, 0f), 0);
        assertEquals(1f, ScaleMath.shrinkOut(.6f, .6f, 0f), 0);
        assertEquals(0f, ScaleMath.shrinkOut(1f, .6f, 0f), 1e-6);
        float mid = ScaleMath.shrinkOut(.8f, .6f, 0f);
        assertTrue(mid > 0f && mid < 1f);
    }
    @Test void shrinkOutHandlesOutOfRangeStart() {
        assertEquals(0f, ScaleMath.shrinkOut(1f, 5f, 0f), 1e-6);
        assertEquals(1f, ScaleMath.shrinkOut(0f, -3f, 0f), 1e-6);
    }
    @Test void fadeAlphaRampsToOpacityAndTintPacksAlpha() {
        assertEquals(0f, ScaleMath.fadeAlpha(0, 10, .8f), 1e-6);
        assertEquals(.8f, ScaleMath.fadeAlpha(10, 10, .8f), 1e-6);
        assertEquals(.5f, ScaleMath.fadeAlpha(0, 0, .5f), 1e-6);
        assertEquals(0xFFFFFFFF, ScaleMath.whiteTint(1f));
        assertEquals(0x00FFFFFF, ScaleMath.whiteTint(0f));
        assertEquals(0x80FFFFFF, ScaleMath.whiteTint(.5f));
    }
}
