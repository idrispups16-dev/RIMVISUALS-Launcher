package xyz.rimvisuals.anim;

/** Converts monotonic timestamps into clamped frame deltas. A stall (alt-tab, loading) never turns into one giant animation jump. */
public final class FrameClock {
    public static final float MAX_DT = 0.1f;
    private long last = Long.MIN_VALUE;
    private float dt;
    private double time;

    public float tick(long nowNanos) {
        if (last == Long.MIN_VALUE) { last = nowNanos; dt = 0f; return 0f; }
        float raw = (nowNanos - last) / 1e9f;
        last = nowNanos;
        dt = raw < 0f || Float.isNaN(raw) ? 0f : Math.min(MAX_DT, raw);
        time += dt;
        return dt;
    }

    public float dt() { return dt; }
    /** Accumulated animation seconds (clamped deltas only), for looping effects such as rainbow phase. */
    public double time() { return time; }
}
