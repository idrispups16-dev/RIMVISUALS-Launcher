package xyz.rimvisuals.anim;

/** Standard easing curves (Penner-style closed forms). Input is clamped to 0..1; endpoints are exact (0 -> 0, 1 -> 1). */
public enum Curve implements Interpolator {
    LINEAR("Linear"), QUAD_IN("Quad in"), QUAD_OUT("Quad out"), QUAD_IN_OUT("Quad in-out"),
    CUBIC_IN("Cubic in"), CUBIC_OUT("Cubic out"), CUBIC_IN_OUT("Cubic in-out"),
    QUINT_IN("Quint in"), QUINT_OUT("Quint out"), QUINT_IN_OUT("Quint in-out"),
    EXPO_IN("Expo in"), EXPO_OUT("Expo out"), EXPO_IN_OUT("Expo in-out"),
    SINE_IN_OUT("Sine in-out"), SMOOTHSTEP("Smoothstep"), SMOOTHERSTEP("Smootherstep"), BACK_OUT("Back out");

    public final String label;
    Curve(String label) { this.label = label; }

    @Override public float at(float t) {
        if (!(t > 0f)) return 0f;          // also catches NaN
        if (t >= 1f) return 1f;
        switch (this) {
            case QUAD_IN: return t * t;
            case QUAD_OUT: return 1f - (1f - t) * (1f - t);
            case QUAD_IN_OUT: return t < .5f ? 2f * t * t : 1f - sq(-2f * t + 2f) / 2f;
            case CUBIC_IN: return t * t * t;
            case CUBIC_OUT: return 1f - cube(1f - t);
            case CUBIC_IN_OUT: return t < .5f ? 4f * t * t * t : 1f - cube(-2f * t + 2f) / 2f;
            case QUINT_IN: return pow5(t);
            case QUINT_OUT: return 1f - pow5(1f - t);
            case QUINT_IN_OUT: return t < .5f ? 16f * pow5(t) : 1f - pow5(-2f * t + 2f) / 2f;
            case EXPO_IN: return (float) Math.pow(2, 10 * t - 10);
            case EXPO_OUT: return 1f - (float) Math.pow(2, -10 * t);
            case EXPO_IN_OUT: return t < .5f ? (float) Math.pow(2, 20 * t - 10) / 2f : (2f - (float) Math.pow(2, -20 * t + 10)) / 2f;
            case SINE_IN_OUT: return -(float) (Math.cos(Math.PI * t) - 1) / 2f;
            case SMOOTHSTEP: return t * t * (3f - 2f * t);
            case SMOOTHERSTEP: return t * t * t * (t * (t * 6f - 15f) + 10f);
            case BACK_OUT: { float c1 = 1.70158f, c3 = c1 + 1f; return 1f + c3 * cube(t - 1f) + c1 * sq(t - 1f); }
            default: return t;
        }
    }

    private static float sq(float v) { return v * v; }
    private static float cube(float v) { return v * v * v; }
    private static float pow5(float v) { float s = v * v; return s * s * v; }
}
