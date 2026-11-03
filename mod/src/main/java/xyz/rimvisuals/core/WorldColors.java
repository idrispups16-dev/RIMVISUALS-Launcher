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
        var w=Modules.WATER_COLOR; var l=Modules.LAVA_COLOR; var b=Modules.BIOME_COLORS;
        int h = w.isActive() ? 31*w.color.rgb + Double.hashCode(w.strength.value) : 0;
        h=31*h+(l.isActive() ? 31*l.color.rgb+Double.hashCode(l.strength.value) : 0);
        h=31*h+(b.isActive() ? 31*(b.grass.value ? b.grassColor.rgb : 0)
            +(b.foliage.value ? b.foliageColor.rgb : 0)+Double.hashCode(b.strength.value) : 0);
        return h;
    }

    private static int lastSig = Integer.MIN_VALUE, pending, cooldown;
    private static Object lastWorld;
    private static int appliedSig;

    /** Called every client tick; defers rebuilding until the menu closes and changes settle; at most once per 100 ticks. */
    public static void tick(net.minecraft.client.MinecraftClient mc) {
        if (mc.world != lastWorld) {lastWorld=mc.world;lastSig=signature();appliedSig=lastSig;pending=cooldown=0;return;}
        if (mc.world==null) return;
        if(cooldown>0)cooldown--;
        int sig = signature();
        if (lastSig == Integer.MIN_VALUE) { lastSig = sig; return; }
        if (sig != lastSig) { lastSig = sig; pending = 12; return; }
        if (pending>0) pending--;
        if(pending==0 && sig!=appliedSig && cooldown==0 && !(mc.currentScreen instanceof xyz.rimvisuals.gui.ClickGui)) {
            long t=PerformanceMonitor.begin();
            mc.worldRenderer.reload();
            PerformanceMonitor.end("World terrain reload",t);
            appliedSig=sig;cooldown=100;
        }
    }
}
