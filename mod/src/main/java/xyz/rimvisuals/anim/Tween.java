package xyz.rimvisuals.anim;

/** Time-based interpolation from..to over a duration with optional delay. The value is a pure function of elapsed time. */
public final class Tween {
    private float from, to, duration, delay, elapsed;
    private Interpolator curve;

    public Tween(float from, float to, float durationSec, Interpolator curve) { start(from, to, durationSec, 0f, curve); }

    public Tween start(float from, float to, float durationSec, float delaySec, Interpolator curve) {
        this.from = from; this.to = to; this.duration = Math.max(0f, durationSec); this.delay = Math.max(0f, delaySec);
        this.curve = curve == null ? Curve.LINEAR : curve; this.elapsed = 0f;
        return this;
    }

    /** Advances by dt seconds (non-positive and NaN are ignored) and returns the current value. */
    public float advance(float dt) { if (dt > 0f) elapsed += dt; return value(); }

    public float progress() {
        if (elapsed < delay) return 0f;
        if (duration <= 0f) return 1f;
        return Math.min(1f, (elapsed - delay) / duration);
    }

    public float value() { float p = progress(); return p >= 1f ? to : from + (to - from) * curve.at(p); }
    public boolean finished() { return progress() >= 1f; }
    public float target() { return to; }

    /** Starts a new tween from the current value (no jump). */
    public Tween retarget(float newTo, float durationSec, Interpolator c) { return start(value(), newTo, durationSec, 0f, c); }
    public void snap(float v) { from = to = v; elapsed = delay + duration; }
}
