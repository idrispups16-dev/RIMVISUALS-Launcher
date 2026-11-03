package xyz.rimvisuals.core;

/** Pure helpers for combat particle counts (unit tested). */
public final class ParticleMath {
    private ParticleMath() {}

    /** Particle count for {@code damage} health points: {@code perHeart} per 2 HP, at least 1 for any damage, capped. */
    public static int damageCount(float damage, int perHeart, int cap) {
        if (damage <= 0f || perHeart <= 0 || cap <= 0) return 0;
        int n = Math.round(damage / 2f * perHeart);
        return Math.max(1, Math.min(cap, n));
    }

    public static int fairShare(int requested,int active,int cap){return requested<=0||active<=0||cap<=0?0:Math.min(requested,Math.max(1,cap/active));}

    /** Applies the shared effect budget: limit, optional safe-mode cap, adaptive scaling when FPS is below target. */
    public static int budget(int requested, int limit, boolean safe, int safeCap, boolean adaptive, int fps, int targetFps) {
        int n = Math.min(requested, limit);
        if (safe) n = Math.min(n, safeCap);
        if (adaptive && fps > 0 && fps < targetFps) n = (int) (n * Math.max(.15, fps / (double) targetFps));
        return Math.max(0, n);
    }
}
