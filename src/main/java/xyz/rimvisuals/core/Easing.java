package xyz.rimvisuals.core;

/** Easing curves for 0..1 animation progress. Indices match the "Easing" mode settings: Linear, Ease out, Ease in, Smooth. */
public final class Easing {
    public static final String[] NAMES = {"Linear", "Ease out", "Ease in", "Smooth"};
    private Easing() {}

    public static float apply(int mode, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return switch (mode) {
            case 1 -> 1f - (1f - t) * (1f - t);
            case 2 -> t * t;
            case 3 -> t * t * (3f - 2f * t);
            default -> t;
        };
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
