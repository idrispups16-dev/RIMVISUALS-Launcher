package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;

/** Spawns vanilla dust particles client-side, always through the shared performance budget. */
public final class Fx {
    private Fx() {}

    public static int budget(int requested) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return Modules.PERFORMANCE.effectParticles(requested, mc.getCurrentFps());
    }

    public static void burst(double x, double y, double z, int rgb, int requested, float size, double speed) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        int n = budget(requested);
        if (n <= 0) return;
        var pm = mc.particleManager;
        var rnd = mc.world.random;
        var effect = new DustParticleEffect(rgb & 0xFFFFFF, Math.max(.2f, Math.min(3f, size)));
        for (int i = 0; i < n; i++) {
            pm.addParticle(effect, x, y, z, (rnd.nextDouble() - .5) * speed * 2, (rnd.nextDouble() - .2) * speed * 2, (rnd.nextDouble() - .5) * speed * 2);
        }
    }
}
