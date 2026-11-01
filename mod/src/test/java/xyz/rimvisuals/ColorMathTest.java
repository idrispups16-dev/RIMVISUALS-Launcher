package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.ColorMath;

import static org.junit.jupiter.api.Assertions.*;

class ColorMathTest {
    @Test void gradientStartsAtFirstColourPeaksAtSecondAndWraps() {
        assertEquals(0xFF0000, ColorMath.gradient(0xFF0000, 0x0000FF, 0));
        assertEquals(0x0000FF, ColorMath.gradient(0xFF0000, 0x0000FF, .5));
        assertEquals(0xFF0000, ColorMath.gradient(0xFF0000, 0x0000FF, 1));
        int mid = ColorMath.gradient(0xFF0000, 0x0000FF, .25);
        assertTrue(((mid >> 16) & 255) > 0 && (mid & 255) > 0);
    }
    @Test void rainbowIsPeriodicAndStaysInRgbRange() {
        assertEquals(ColorMath.rainbow(.3), ColorMath.rainbow(1.3));
        for (double p = 0; p < 2; p += .13) assertEquals(0, ColorMath.rainbow(p) & 0xFF000000);
        assertNotEquals(ColorMath.rainbow(0), ColorMath.rainbow(.5));
    }
    @Test void lerpClampsAndInterpolates() {
        assertEquals(0x000000, ColorMath.lerp(0, 0xFFFFFF, -1));
        assertEquals(0xFFFFFF, ColorMath.lerp(0, 0xFFFFFF, 2));
        assertEquals(0x808080, ColorMath.lerp(0, 0xFFFFFF, .5f));
    }
    @Test void argbPacksAlphaAndClamps() {
        assertEquals(0xFFFFFFFF, ColorMath.argb(0xFFFFFF, 1f));
        assertEquals(0x80102030, ColorMath.argb(0x102030, .5f));
        assertEquals(0x00102030, ColorMath.argb(0x102030, -3f));
    }
    @Test void mixArgbKeepsAlphaAndStrengthZeroIsVanilla() {
        assertEquals(0xFF336699, ColorMath.mixArgb(0xFF336699, 0xFFFFFF, 0));
        assertEquals(0xFFFFFFFF, ColorMath.mixArgb(0xFF336699, 0xFFFFFF, 1));
    }
    @Test void fogDistanceScalingLeavesSentinelsAlone() {
        assertEquals(200f, ColorMath.scaleDistance(100f, 2f), 1e-4);
        assertEquals(-8f, ColorMath.scaleDistance(-8f, 2f));
        assertEquals(0f, ColorMath.scaleDistance(0f, 2f));
        assertTrue(Float.isInfinite(ColorMath.scaleDistance(Float.POSITIVE_INFINITY, 2f)));
        assertTrue(Float.isNaN(ColorMath.scaleDistance(Float.NaN, 2f)));
        assertEquals(5f, ColorMath.scaleDistance(100f, 0f), 1e-4, "factor is floored at 5% so fog never collapses to zero");
    }
}
