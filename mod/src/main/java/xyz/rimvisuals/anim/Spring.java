package xyz.rimvisuals.anim;

/**
 * Damped spring with the exact closed-form solution per step, so stepping 1/30 s twice equals stepping 1/15 s once:
 * the motion is the same at any frame rate and cannot blow up at low FPS.
 */
public final class Spring {
    public float value, velocity, target;
    private final double omega, zeta;

    /** @param frequencyHz natural frequency (about 1/period); @param dampingRatio 1 = critical, below 1 overshoots. */
    public Spring(float start, float frequencyHz, float dampingRatio) {
        this.value = start; this.target = start;
        this.omega = 2 * Math.PI * Math.max(.05f, frequencyHz);
        this.zeta = Math.max(.05f, dampingRatio);
    }

    public float advance(float dt) {
        if (!(dt > 0f)) return value;
        double x0 = value - target, v0 = velocity, t = dt, x, v;
        if (zeta < 1 - 1e-4) {
            double wd = omega * Math.sqrt(1 - zeta * zeta), e = Math.exp(-zeta * omega * t), c = Math.cos(wd * t), s = Math.sin(wd * t);
            double a = x0, b = (v0 + zeta * omega * x0) / wd;
            x = e * (a * c + b * s);
            v = e * ((-zeta * omega * a + wd * b) * c + (-zeta * omega * b - wd * a) * s);
        } else if (zeta <= 1 + 1e-4) {
            double e = Math.exp(-omega * t), k = v0 + omega * x0;
            x = e * (x0 + k * t);
            v = e * (v0 - omega * k * t);
        } else {
            double q = Math.sqrt(zeta * zeta - 1), r1 = -omega * (zeta - q), r2 = -omega * (zeta + q);
            double c2 = (v0 - r1 * x0) / (r2 - r1), c1 = x0 - c2;
            double e1 = Math.exp(r1 * t), e2 = Math.exp(r2 * t);
            x = c1 * e1 + c2 * e2;
            v = c1 * r1 * e1 + c2 * r2 * e2;
        }
        value = (float) (target + x);
        velocity = (float) v;
        if (Math.abs(value - target) < 1e-4f && Math.abs(velocity) < 1e-3f) { value = target; velocity = 0f; }
        return value;
    }

    public boolean settled() { return value == target && velocity == 0f; }
    public void snap(float v) { value = target = v; velocity = 0f; }
}
