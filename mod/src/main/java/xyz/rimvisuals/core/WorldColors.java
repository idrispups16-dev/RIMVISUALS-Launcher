package xyz.rimvisuals.core;

import xyz.rimvisuals.modules.*;

/** Pure colour maths and change tracking for the world colour modules (water, lava, grass/foliage, weather). */
public final class WorldColors {
    private WorldColors() {}

    /** Mixes {@code rgb} into {@code base} by {@code strength} (0 = base, 1 = rgb). Keeps the base's alpha byte. */
    public static int tint(int base, int rgb, double strength) {
        float t = (float) Math.max(0, Math.min(1, strength));
        int out = base & 0xFF000000;
        for (int sh = 0; sh <= 16; sh += 8) {
            int a = (base >> sh) & 255, b = (rgb >> sh) & 255;
            out |= (Math.round(a + (b - a) * t) & 255) << sh;
        }
        return out;
    }

    /** Terrain-affecting settings in one number; when it changes, chunk meshes must be rebuilt. */
    public static int signature() {
        int h = 17;
        for (Module m : new Module[]{Modules.WATER_COLOR, Modules.LAVA_COLOR, Modules.BIOME_COLORS}) {
            h = h * 31 + (m.enabled ? 1 : 0);
            for (Setting s : m.settings) h = h * 31 + s.save().hashCode();
        }
        return h;
    }

    private static int lastSig = Integer.MIN_VALUE, pending;

    /** Called every client tick; rebuilds terrain once, 12 ticks after the colour settings stop changing. */
    public static void tick(net.minecraft.client.MinecraftClient mc) {
        int sig = signature();
        if (lastSig == Integer.MIN_VALUE) { lastSig = sig; return; }
        if (sig != lastSig) { lastSig = sig; pending = 12; return; }
        if (pending > 0 && --pending == 0 && mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
    }
}
