package xyz.rimvisuals.anim;

import java.util.ArrayList;
import java.util.List;

/** Ordered keyframes; the curve stored on a key shapes the segment from that key to the next. Clamped outside the range. */
public final class Keyframes {
    private final float[] times, values;
    private final Interpolator[] curves;

    private Keyframes(float[] t, float[] v, Interpolator[] c) { times = t; values = v; curves = c; }

    public float duration() { return times[times.length - 1]; }

    public float sample(float t) {
        if (!(t > times[0])) return values[0];
        int n = times.length;
        if (t >= times[n - 1]) return values[n - 1];
        int lo = 0, hi = n - 1;                             // binary search: last key with time <= t
        while (hi - lo > 1) { int mid = (lo + hi) >>> 1; if (times[mid] <= t) lo = mid; else hi = mid; }
        float span = times[lo + 1] - times[lo];
        float p = (t - times[lo]) / span;
        return values[lo] + (values[lo + 1] - values[lo]) * curves[lo].at(p);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<float[]> keys = new ArrayList<>();
        private final List<Interpolator> curves = new ArrayList<>();

        public Builder at(float time, float value) { return at(time, value, Curve.LINEAR); }

        public Builder at(float time, float value, Interpolator toNext) {
            if (!Float.isFinite(time) || !Float.isFinite(value)) throw new IllegalArgumentException("keyframe must be finite");
            if (!keys.isEmpty() && time <= keys.get(keys.size() - 1)[0]) throw new IllegalArgumentException("keyframe times must strictly increase");
            keys.add(new float[]{time, value}); curves.add(toNext == null ? Curve.LINEAR : toNext);
            return this;
        }

        public Keyframes build() {
            if (keys.size() < 2) throw new IllegalStateException("need at least two keyframes");
            int n = keys.size(); float[] t = new float[n], v = new float[n];
            for (int i = 0; i < n; i++) { t[i] = keys.get(i)[0]; v[i] = keys.get(i)[1]; }
            return new Keyframes(t, v, curves.toArray(new Interpolator[0]));
        }
    }
}
