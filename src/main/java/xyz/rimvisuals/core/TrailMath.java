package xyz.rimvisuals.core;

/** Pure geometry for trails (unit tested, no Minecraft types). */
public final class TrailMath {
    private TrailMath() {}

    /**
     * Approximate sword-tip position during a swing: an arc from the right of the view to the left, reach blocks
     * from the eye. progress 0..1. Minecraft yaw 0 faces +Z, pitch positive looks down.
     */
    public static double[] swordTip(double ex, double ey, double ez, float yawDeg, float pitchDeg, float progress, double reach) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double len = Math.hypot(fx, fz);
        double rx = len < 1e-6 ? 1 : -fz / len, rz = len < 1e-6 ? 0 : fx / len;
        float p = ScaleMath.clamp01(progress);
        double a = Math.toRadians(60 - 120 * p);
        double dx = fx * Math.cos(a) + rx * Math.sin(a);
        double dy = fy * Math.cos(a) + .35 * (.5 - p);
        double dz = fz * Math.cos(a) + rz * Math.sin(a);
        return new double[]{ex + dx * reach, ey + dy * reach, ez + dz * reach};
    }

    /** Number of evenly spaced samples to draw for a path segment of {@code length} blocks at {@code perBlock} density. */
    public static int samples(double length, double perBlock, int max) {
        if (length <= 0 || perBlock <= 0 || max <= 0) return 0;
        return Math.max(1, Math.min(max, (int) Math.ceil(length * perBlock)));
    }
}
