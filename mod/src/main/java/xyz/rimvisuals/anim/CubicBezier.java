package xyz.rimvisuals.anim;

/** CSS-style cubic-bezier(x1, y1, x2, y2) easing. x1/x2 are clamped to 0..1 so x(t) stays monotonic and solvable. */
public final class CubicBezier implements Interpolator {
    private final float x1, y1, x2, y2;

    public CubicBezier(float x1, float y1, float x2, float y2) {
        this.x1 = clamp01(x1); this.x2 = clamp01(x2); this.y1 = y1; this.y2 = y2;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
    private static double bez(double a, double b, double t) { double u = 1 - t; return 3 * u * u * t * a + 3 * u * t * t * b + t * t * t; }
    private static double dbez(double a, double b, double t) { double u = 1 - t; return 3 * u * u * a + 6 * u * t * (b - a) + 3 * t * t * (1 - b); }

    @Override public float at(float x) {
        if (!(x > 0f)) return 0f;
        if (x >= 1f) return 1f;
        double t = x;                                           // Newton–Raphson, then bisection fallback
        for (int i = 0; i < 8; i++) {
            double err = bez(x1, x2, t) - x, d = dbez(x1, x2, t);
            if (Math.abs(err) < 1e-6) return (float) bez(y1, y2, t);
            if (Math.abs(d) < 1e-6) break;
            t -= err / d;
        }
        double lo = 0, hi = 1; t = x;
        for (int i = 0; i < 24; i++) {
            double v = bez(x1, x2, t);
            if (Math.abs(v - x) < 1e-6) break;
            if (v < x) lo = t; else hi = t;
            t = (lo + hi) / 2;
        }
        return (float) bez(y1, y2, t);
    }
}
