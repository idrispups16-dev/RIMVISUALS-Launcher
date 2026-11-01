package xyz.rimvisuals.core;

import java.util.ArrayDeque;
import java.util.Locale;

/** Pure helpers for HUD modules (no Minecraft types, unit tested). */
public final class HudMath {
    private HudMath() {}

    /** Counts events inside a sliding window; used for clicks per second. */
    public static final class RateCounter {
        private final ArrayDeque<Long> stamps = new ArrayDeque<>();
        private final long windowNanos;
        public RateCounter(double windowSeconds) { windowNanos = (long) (windowSeconds * 1e9); }
        public void hit(long nowNanos) { stamps.addLast(nowNanos); trim(nowNanos); }
        public int count(long nowNanos) { trim(nowNanos); return stamps.size(); }
        private void trim(long now) { while (!stamps.isEmpty() && now - stamps.peekFirst() > windowNanos) stamps.pollFirst(); }
    }

    /** Linear fade: 1 at age 0, 0 at {@code duration}; duration <= 0 means "already gone". */
    public static float fadeOut(double age, double duration) {
        if (duration <= 0 || age < 0) return age < 0 ? 1f : 0f;
        return (float) Math.max(0, Math.min(1, 1 - age / duration));
    }

    /** Snaps a value to a grid; grid <= 0 disables snapping. */
    public static double snap(double v, int grid) { return grid <= 0 ? v : Math.round(v / grid) * (double) grid; }

    /** Size of an element after scaling, never below one unit. */
    public static int scaled(int size, double scale) { return Math.max(1, (int) Math.round(size * scale)); }

    /** Horizontal speed in blocks per second from a position delta. */
    public static double bps(double dx, double dz, double seconds) {
        if (seconds <= 0) return 0;
        return Math.sqrt(dx * dx + dz * dz) / seconds;
    }

    /** "m:ss" under an hour, "h:mm:ss" otherwise. */
    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600, m = (s / 60) % 60, r = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, r) : String.format(Locale.ROOT, "%d:%02d", m, r);
    }

    /** Effect time in ticks as "m:ss"; infinite effects show "**". */
    public static String effectTime(int ticks, boolean infinite) {
        return infinite ? "**" : clock(ticks / 20);
    }
}
