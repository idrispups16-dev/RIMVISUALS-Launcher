package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pearl, projectile and sword trails drawn as vanilla dust particles inside one shared per-tick budget. */
public final class Trails {
    private static final Map<Integer, Float> swing = new HashMap<>();
    private static final Set<Integer> seen = new HashSet<>();
    private Trails() {}

    private static void dust(MinecraftClient mc, int rgb, float size, double x, double y, double z) {
        Fx.opacity(EffectParticles.spawn(mc,rgb,size,x,y,z,0,0,0),1);
    }

    public static void tick(MinecraftClient mc) {
        var pearl = Modules.PEARL_TRAILS; var proj = Modules.PROJECTILE_TRAILS; var sword = Modules.SWORD_TRAILS;
        if ((!pearl.isActive() && !proj.isActive() && !sword.isActive()) || mc.world == null || mc.player == null) { swing.clear(); return; }
        long t0 = PerformanceMonitor.begin();
        int budget = Fx.ambientBudget(24); // hard ceiling of particles for ALL trails this tick
        if (budget <= 0) { swing.clear();PerformanceMonitor.end("Trails", t0); return; }
        seen.clear();
        for (Entity e : mc.world.getOtherEntities(mc.player,mc.player.getBoundingBox().expand(48),e -> e instanceof ProjectileEntity || (sword.isActive() && e instanceof PlayerEntity))) {
            if (budget <= 0) break;
            double d2 = e.squaredDistanceTo(mc.player);
            if (e instanceof EnderPearlEntity) {
                if (pearl.isActive() && d2 < 48 * 48) budget -= projectile(mc, e, pearl.color.rgb, (float) pearl.size.value, (int) pearl.density.value, budget, pearl.ownOnly.value, mc.player);
            } else if (e instanceof ProjectileEntity) {
                if (proj.isActive() && d2 < 48 * 48) budget -= projectile(mc, e, proj.color.rgb, (float) proj.size.value, (int) proj.density.value, budget, proj.ownOnly.value, mc.player);
            } else if (sword.isActive() && e instanceof PlayerEntity p && d2 < 24 * 24 && (sword.who.index == 1 || p == mc.player)) {
                budget -= sword(mc, p, budget);
            }
        }
        if(sword.isActive() && budget>0)budget-=sword(mc,mc.player,budget);
        Fx.refund(budget);
        swing.keySet().retainAll(seen);
        PerformanceMonitor.end("Trails", t0);
    }

    private static int projectile(MinecraftClient mc, Entity e, int rgb, float size, int density, int budget, boolean ownOnly, PlayerEntity me) {
        if (ownOnly && !(e instanceof ProjectileEntity pe && pe.getOwner() == me)) return 0;
        if (e.getVelocity().lengthSquared() < .01 || e.isOnGround()) return 0; // stuck or at rest
        double px = e.lastRenderX, py = e.lastRenderY, pz = e.lastRenderZ;
        double len = Math.sqrt((e.getX() - px) * (e.getX() - px) + (e.getY() - py) * (e.getY() - py) + (e.getZ() - pz) * (e.getZ() - pz));
        int n = TrailMath.samples(len, density, Math.min(budget, 12));
        for (int i = 0; i < n; i++) {
            double k = n == 1 ? 1 : (i + 1) / (double) n;
            dust(mc, rgb, size, px + (e.getX() - px) * k, py + (e.getY() - py) * k + e.getHeight() * .5, pz + (e.getZ() - pz) * k);
        }
        return n;
    }

    private static int sword(MinecraftClient mc, PlayerEntity p, int budget) {
        int id = p.getId();
        seen.add(id);
        if (!p.handSwinging || p.preferredHand != Hand.MAIN_HAND || !p.getMainHandStack().isIn(ItemTags.SWORDS)) { swing.remove(id); return 0; }
        float now = p.getHandSwingProgress(1f);
        Float prev = swing.put(id, now);
        if (prev == null || now <= prev) return 0;
        var s = Modules.SWORD_TRAILS;
        int n = Math.min(budget, Math.max(1, (int) s.density.value));
        for (int i = 0; i < n; i++) {
            float pr = prev + (now - prev) * (i + 1) / n;
            double[] tip = TrailMath.swordTip(p.getX(), p.getEyeY(), p.getZ(), p.getYaw(), p.getPitch(), pr, s.reach.value);
            dust(mc, s.color.rgb, (float)s.size.value, tip[0], tip[1], tip[2]);
        }
        return n;
    }
}
