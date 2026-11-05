package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import xyz.rimvisuals.core.Fx;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.PerformanceMonitor;
import xyz.rimvisuals.core.Setting;

/** Adds a coloured shockwave ring and spark burst on top of every explosion the server tells your client about. */
public final class ExplosionEffects extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF8A3D));
    public final Setting.Bool ring = add(new Setting.Bool("Shockwave ring", true));
    public final Setting.Bool sparks = add(new Setting.Bool("Sparks", true));
    public final Setting.Num amount = add(new Setting.Num("Amount", 24, 4, 60, 2));
    public final Setting.Num minRadius = add(new Setting.Num("Minimum radius", 2, 0, 10, .5));
    public final Setting.Num maxDistance = add(new Setting.Num("Max distance (blocks)", 48, 8, 128, 4));

    public final Setting.Num opacity = add(new Setting.Num("Opacity", .8, .1, 1, .05));

    public ExplosionEffects() {
        super("explosion_effects", "Explosion Effects", "Extra shockwave ring and sparks for explosions near you (crystals, TNT, anchors, beds). Uses the shared particle budget; Safe Mode suppresses it.", Category.VISUALS);
        enabled = false;
    }

    /** Number of particles to request: scales with explosion radius (radius 4 = the configured amount). */
    public static int count(double amount, float radius) { return (int) Math.max(4, Math.min(80, Math.round(amount * radius / 4.0))); }

    public void onExplosion(Vec3d c, float radius) {
        if (!isActive() || radius < minRadius.value) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.squaredDistanceTo(c) > maxDistance.value * maxDistance.value) return;
        long t0 = PerformanceMonitor.begin();
        int n = count(amount.value, radius);
        float size = Math.min(2.5f, .8f + radius / 6f);
        int parts=(ring.value?1:0)+(sparks.value?1:0),cap=Fx.remaining();
        if (ring.value) Fx.ring(c.x, c.y + .1, c.z, color.rgb, xyz.rimvisuals.core.ParticleMath.fairShare(n,parts,cap), size, .18 + radius * .05,opacity.value);
        if (sparks.value) Fx.burst(c.x, c.y, c.z, color.rgb, xyz.rimvisuals.core.ParticleMath.fairShare(n/2,parts,cap), Math.max(.6f, size * .6f), .25,opacity.value);
        PerformanceMonitor.end("Explosion Effects", t0);
    }
}
