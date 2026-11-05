package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.TrailMath;

import static org.junit.jupiter.api.Assertions.*;

class TrailMathTest {
    @Test void midSwingPointsStraightAheadAtReach() {
        double[] t = TrailMath.swordTip(0, 1.6, 0, 0, 0, .5f, 2);
        assertEquals(0, t[0], 1e-6); assertEquals(1.6, t[1], 1e-6); assertEquals(2, t[2], 1e-6);
    }
    @Test void swingSweepsFromRightToLeftOfView() {
        // yaw 0 faces +Z; the player's right is -X.
        double[] start = TrailMath.swordTip(0, 0, 0, 0, 0, 0f, 2), end = TrailMath.swordTip(0, 0, 0, 0, 0, 1f, 2);
        assertTrue(start[0] < 0, "starts on the right (-X)");
        assertTrue(end[0] > 0, "ends on the left (+X)");
    }
    @Test void tipStaysAtReachDistanceHorizontally() {
        double[] t = TrailMath.swordTip(10, 5, -3, 90, 0, .2f, 1.6);
        assertTrue(Math.hypot(t[0] - 10, t[2] + 3) <= 1.6 + 1e-6);
    }
    @Test void sampleCountIsBounded() {
        assertEquals(0, TrailMath.samples(0, 3, 12));
        assertEquals(1, TrailMath.samples(.1, 3, 12));
        assertEquals(6, TrailMath.samples(2, 3, 12));
        assertEquals(12, TrailMath.samples(100, 3, 12));
    }
}
