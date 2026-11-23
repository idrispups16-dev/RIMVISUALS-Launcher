package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.anim.*;

import static org.junit.jupiter.api.Assertions.*;

class AnimationEngineTest {
    @Test void everyCurveHasExactEndpointsAndIsFiniteInside() {
        for (Curve c : Curve.values()) {
            assertEquals(0f, c.at(0f), 0f, c.name());
            assertEquals(1f, c.at(1f), 0f, c.name());
            assertEquals(0f, c.at(-5f), 0f);
            assertEquals(1f, c.at(9f), 0f);
            assertEquals(0f, c.at(Float.NaN), 0f);
            for (int i = 1; i < 100; i++) assertTrue(Float.isFinite(c.at(i / 100f)), c.name());
        }
    }

    @Test void monotonicCurvesNeverGoBackwards() {
        for (Curve c : Curve.values()) {
            if (c == Curve.BACK_OUT) continue;                 // overshoots by design
            float prev = 0f;
            for (int i = 0; i <= 200; i++) { float v = c.at(i / 200f); assertTrue(v >= prev - 1e-6f, c.name() + " at " + i); prev = v; }
        }
    }

    @Test void curveShapes() {
        assertEquals(.25f, Curve.QUAD_IN.at(.5f), 1e-6);
        assertEquals(.75f, Curve.QUAD_OUT.at(.5f), 1e-6);
        assertEquals(.5f, Curve.CUBIC_IN_OUT.at(.5f), 1e-6);
        assertTrue(Curve.EXPO_OUT.at(.1f) > Curve.CUBIC_OUT.at(.1f) - .2f);
        assertTrue(Curve.BACK_OUT.at(.7f) > 1f);               // overshoot exists
        assertTrue(Curve.QUINT_OUT.at(.2f) > Curve.QUAD_OUT.at(.2f));
    }

    @Test void cubicBezierMatchesKnownCssEases() {
        CubicBezier linear = new CubicBezier(0f, 0f, 1f, 1f);
        for (int i = 0; i <= 10; i++) assertEquals(i / 10f, linear.at(i / 10f), 1e-4);
        CubicBezier ease = new CubicBezier(.25f, .1f, .25f, 1f);   // CSS "ease"
        assertEquals(0.8024f, ease.at(.5f), 2e-3);
        assertEquals(0f, ease.at(0f), 0f); assertEquals(1f, ease.at(1f), 0f);
        float prev = 0f; for (int i = 0; i <= 100; i++) { float v = ease.at(i / 100f); assertTrue(v >= prev - 1e-5f); prev = v; }
    }

    @Test void tweenIsAFunctionOfTimeNotOfFrameCount() {
        float[] results = new float[4]; int[] fps = {20, 30, 60, 240};
        for (int k = 0; k < fps.length; k++) {
            Tween t = new Tween(0f, 10f, 1f, Curve.CUBIC_OUT);
            for (int f = 0; f < fps[k] / 2; f++) t.advance(1f / fps[k]);   // 0.5 s
            results[k] = t.value();
        }
        float expected = 10f * Curve.CUBIC_OUT.at(.5f);
        for (float r : results) assertEquals(expected, r, 0.02);
    }

    @Test void tweenDelayFinishAndRetarget() {
        Tween t = new Tween(0f, 1f, 1f, Curve.LINEAR).start(0f, 1f, 1f, .5f, Curve.LINEAR);
        t.advance(.4f); assertEquals(0f, t.value(), 0f); assertFalse(t.finished());
        t.advance(.6f); assertEquals(.5f, t.value(), 1e-5);
        t.advance(5f); assertTrue(t.finished()); assertEquals(1f, t.value(), 0f);
        Tween r = new Tween(0f, 1f, 1f, Curve.LINEAR); r.advance(.5f);
        float mid = r.value(); r.retarget(0f, 1f, Curve.LINEAR);
        assertEquals(mid, r.value(), 1e-6);                     // no jump on retarget
        t.advance(-3f); t.advance(Float.NaN); assertEquals(1f, t.value(), 0f);
    }

    @Test void springIsFrameRateIndependentAndSettles() {
        for (float zeta : new float[]{.4f, 1f, 2.5f}) {
            Spring a = new Spring(0f, 2f, zeta), b = new Spring(0f, 2f, zeta);
            a.target = b.target = 1f;
            a.advance(.5f);
            for (int i = 0; i < 30; i++) b.advance(.5f / 30f);
            assertEquals(a.value, b.value, 1e-3, "zeta " + zeta);
            assertEquals(a.velocity, b.velocity, 5e-3, "velocity zeta " + zeta);
            for (int i = 0; i < 600; i++) a.advance(1 / 60f);
            assertTrue(a.settled()); assertEquals(1f, a.value, 0f);
        }
    }

    @Test void springOvershootsOnlyWhenUnderdamped() {
        Spring under = new Spring(0f, 2f, .3f), crit = new Spring(0f, 2f, 1f);
        under.target = crit.target = 1f;
        float maxU = 0, maxC = 0;
        for (int i = 0; i < 300; i++) { maxU = Math.max(maxU, under.advance(1 / 60f)); maxC = Math.max(maxC, crit.advance(1 / 60f)); }
        assertTrue(maxU > 1.05f); assertTrue(maxC <= 1.0001f);
    }

    @Test void springSurvivesHugeAndTinyFrames() {
        Spring s = new Spring(0f, 3f, .5f); s.target = 1f;
        s.advance(10f); assertTrue(Float.isFinite(s.value) && Float.isFinite(s.velocity));
        s.advance(0f); s.advance(-1f); s.advance(Float.NaN); assertTrue(Float.isFinite(s.value));
    }

    @Test void keyframesInterpolateClampAndValidate() {
        Keyframes k = Keyframes.builder().at(0f, 0f, Curve.CUBIC_OUT).at(.3f, 1f).at(1f, .5f).build();
        assertEquals(0f, k.sample(-1f), 0f); assertEquals(.5f, k.sample(2f), 0f);
        assertEquals(1f, k.sample(.3f), 1e-6);
        assertEquals(Curve.CUBIC_OUT.at(.5f), k.sample(.15f), 1e-5);
        assertEquals(.75f, k.sample(.65f), 1e-5);              // linear second segment halfway
        assertEquals(1f, k.duration(), 0f);
        assertThrows(IllegalArgumentException.class, () -> Keyframes.builder().at(0f, 0f).at(0f, 1f));
        assertThrows(IllegalStateException.class, () -> Keyframes.builder().at(0f, 0f).build());
        assertThrows(IllegalArgumentException.class, () -> Keyframes.builder().at(Float.NaN, 0f));
    }

    @Test void transitionOpacityScalePositionAndSmoothReverse() {
        Transition t = new Transition(.2f, .1f).curve(Curve.LINEAR).hiddenState(0f, .9f, 0f, 12f);
        assertEquals(0f, t.opacity(), 0f); assertEquals(.9f, t.scale(), 1e-6); assertEquals(12f, t.offsetY(), 1e-6);
        t.show(); t.advance(.1f);
        assertEquals(.5f, t.opacity(), 1e-5); assertEquals(.95f, t.scale(), 1e-5); assertEquals(6f, t.offsetY(), 1e-4);
        t.hide(); t.advance(.025f);                             // reverses from 0.5 progress, no jump
        assertEquals(.25f, t.raw(), 1e-5);
        t.advance(1f); assertTrue(t.settled()); assertFalse(t.visible());
        t.snap(true); assertEquals(1f, t.opacity(), 0f); assertEquals(1f, t.scale(), 0f);
    }

    @Test void transitionDelayForStaggeredCategories() {
        Transition t = new Transition(.2f, .2f).delay(.3f).curve(Curve.LINEAR);
        t.show(); t.advance(.2f); assertEquals(0f, t.raw(), 0f);
        t.advance(.2f); assertTrue(t.raw() > 0f);
    }

    @Test void staggerIsCappedAndOrdered() {
        assertEquals(0f, Stagger.delay(0, 10, .05f, .3f), 0f);
        assertEquals(.3f, Stagger.delay(9, 10, .05f, .3f), 1e-6);
        assertTrue(Stagger.delay(4, 10, .05f, .3f) < Stagger.delay(5, 10, .05f, .3f));
        assertEquals(.1f, Stagger.delay(2, 3, .05f, 5f), 1e-6);
        assertEquals(0f, Stagger.delay(3, 1, .05f, .3f), 0f);
    }

    @Test void frameClockClampsStallsAndBackwardsTime() {
        FrameClock c = new FrameClock();
        assertEquals(0f, c.tick(1_000_000_000L), 0f);
        assertEquals(.016f, c.tick(1_016_000_000L), 1e-6);
        assertEquals(FrameClock.MAX_DT, c.tick(9_000_000_000L), 0f);   // 8 s stall
        assertEquals(0f, c.tick(1L), 0f);                               // clock went backwards
        assertEquals(.016f + FrameClock.MAX_DT, c.time(), 1e-6);
    }

    @Test void legacyEasingIndicesAreUnchangedAndNewOnesAreAppended() {
        float t = .37f;
        assertEquals(t, xyz.rimvisuals.core.Easing.apply(0, t), 1e-7);
        assertEquals(1f - (1f - t) * (1f - t), xyz.rimvisuals.core.Easing.apply(1, t), 1e-7);
        assertEquals(t * t, xyz.rimvisuals.core.Easing.apply(2, t), 1e-7);
        assertEquals(t * t * (3f - 2f * t), xyz.rimvisuals.core.Easing.apply(3, t), 1e-7);
        assertEquals("Smooth", xyz.rimvisuals.core.Easing.NAMES[3]);
        for (int i = 0; i < xyz.rimvisuals.core.Easing.NAMES.length; i++) {
            assertEquals(0f, xyz.rimvisuals.core.Easing.apply(i, 0f), 0f); assertEquals(1f, xyz.rimvisuals.core.Easing.apply(i, 1f), 0f);
        }
    }

    @Test void uiMotionAdvancesTransitionsAndHonoursReducedMotion() {
        Transition tr = new Transition(.2f, .2f).curve(Curve.LINEAR);
        tr.show();
        xyz.rimvisuals.core.UiMotion.frame();
        xyz.rimvisuals.core.UiMotion.advance(tr);
        assertTrue(tr.raw() >= 0f && tr.raw() <= 1f);
        boolean before = xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value;
        try {
            xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value = true;
            xyz.rimvisuals.core.UiMotion.advance(tr);
            assertEquals(1f, tr.opacity(), 0f);
        } finally { xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value = before; }
    }
}
