package xyz.rimvisuals.core;

/** Pure curve helpers for Crystal/Totem scale effects (unit tested, no Minecraft types). */
public final class ScaleMath {
    private ScaleMath() {}

    public static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    public static float smoothstep(float t) { t = clamp01(t); return t * t * (3 - 2 * t); }

    /** 0 at spawn, 1 once {@code duration} ticks have passed. A non-positive duration disables the animation. */
    public static float growIn(float ageTicks, float duration) {
        if (duration <= 0) return 1f;
        return smoothstep(ageTicks / duration);
    }

    /** 1 until {@code start} (0..1) of the animation, then smoothly down to {@code end} at progress 1. */
    public static float shrinkOut(float progress, float start, float end) {
        float s = Math.max(0f, Math.min(.99f, start));
        if (progress <= s) return 1f;
        float t = smoothstep((progress - s) / (1f - s));
        return 1f + (end - 1f) * t;
    }
}
