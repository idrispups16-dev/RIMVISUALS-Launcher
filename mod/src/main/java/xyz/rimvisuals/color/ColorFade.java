package xyz.rimvisuals.color;

import xyz.rimvisuals.anim.Curve;
import xyz.rimvisuals.anim.Interpolator;

/** Cross-fades from the previous colour spec to a new one over a duration, so a colour change in a menu never pops. */
public final class ColorFade {
    private ColorSpec from, to;
    private float elapsed, duration;
    private Interpolator curve = Curve.CUBIC_OUT;

    public ColorFade(ColorSpec initial) { from = to = initial; }

    public void set(ColorSpec next, float durationSec) {
        if (next == null || next.equals(to)) return;
        from = to; to = next; duration = Math.max(0f, durationSec); elapsed = 0f;
    }

    public void advance(float dt) { if (dt > 0f) elapsed += dt; }
    public boolean settled() { return duration <= 0f || elapsed >= duration; }
    public ColorSpec target() { return to; }

    public int resolve(double timeSec, float pos) {
        int b = to.resolve(timeSec, pos);
        if (settled()) return b;
        return Rgb.lerpArgb(from.resolve(timeSec, pos), b, curve.at(elapsed / duration));
    }
}
