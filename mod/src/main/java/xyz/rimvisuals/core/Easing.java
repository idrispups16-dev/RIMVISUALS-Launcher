package xyz.rimvisuals.core;

/** Easing curves for 0..1 animation progress. Indices match the "Easing" mode settings: Linear, Ease out, Ease in, Smooth. */
public final class Easing {
    /** Indices 0-3 are the original curves and must never change (saved configs store the index); new curves are appended. */
    public static final String[] NAMES = {"Linear", "Ease out", "Ease in", "Smooth", "Cubic out", "Quint out", "Expo out", "Cubic in-out"};
    private Easing() {}

    public static float apply(int mode, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return switch (mode) {
            case 1 -> 1f - (1f - t) * (1f - t);
            case 2 -> t * t;
            case 3 -> t * t * (3f - 2f * t);
            case 4 -> xyz.rimvisuals.anim.Curve.CUBIC_OUT.at(t);
            case 5 -> xyz.rimvisuals.anim.Curve.QUINT_OUT.at(t);
            case 6 -> xyz.rimvisuals.anim.Curve.EXPO_OUT.at(t);
            case 7 -> xyz.rimvisuals.anim.Curve.CUBIC_IN_OUT.at(t);
            default -> t;
        };
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
