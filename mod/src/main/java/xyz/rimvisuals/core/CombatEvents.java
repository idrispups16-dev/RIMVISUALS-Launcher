package xyz.rimvisuals.core;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client-side combat hooks: your attacks and observed health loss. Nothing here is sent to the server. */
public final class CombatEvents {
    private static LivingEntity lastTarget;
    private static long lastHitNanos;
    private static boolean killHandled = true;
    private static final Map<Integer, Float> health = new HashMap<>();
    private static final Set<Integer> seen = new HashSet<>();
    private CombatEvents() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (world.isClient() && player == mc.player && entity instanceof LivingEntity living) onAttack(mc, player, living);
            return ActionResult.PASS;
        });
    }

    /** Milliseconds since your last attack on a living entity, or -1 if none yet. */
    public static double msSinceLastHit() { return lastHitNanos == 0 ? -1 : (System.nanoTime() - lastHitNanos) / 1e6; }

    /** Entity you hit most recently, if still valid and within {@code holdSeconds}. */
    public static LivingEntity recentTarget(double holdSeconds) {
        LivingEntity t = lastTarget;
        if (t == null || t.isRemoved() || !t.isAlive()) return null;
        return (System.nanoTime() - lastHitNanos) / 1e9 <= holdSeconds ? t : null;
    }

    static boolean looksCritical(PlayerEntity p) {
        return p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle() && !p.isSprinting()
                && p.getAttackCooldownProgress(.5f) > .9f;
    }

    static int sharpnessLevel(MinecraftClient mc, PlayerEntity player) {
        try {
            var entry = mc.world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT).getOrThrow(net.minecraft.enchantment.Enchantments.SHARPNESS);
            return net.minecraft.enchantment.EnchantmentHelper.getLevel(entry, player.getMainHandStack());
        } catch (RuntimeException e) {
            return 0; // registry not ready; skip the effect instead of risking a crash in an event handler
        }
    }

    private static void onAttack(MinecraftClient mc, PlayerEntity player, LivingEntity target) {
        lastTarget = target;
        lastHitNanos = System.nanoTime();
        killHandled = false;
        double x = target.getX(), y = target.getY() + target.getHeight() * .6, z = target.getZ();
        var hp = Modules.HIT_PARTICLES;
        if (hp.enabled) {
            long t = PerformanceMonitor.begin();
            Fx.burst(x, y, z, hp.color.rgb, (int) hp.count.value, (float) hp.size.value, hp.speed.value);
            PerformanceMonitor.end("Hit Particles", t);
        }
        var sp = Modules.SHARPNESS_PARTICLES;
        if (sp.enabled) {
            long t = PerformanceMonitor.begin();
            int level = sharpnessLevel(mc, player);
            if (level > 0) Fx.burst(x, y, z, sp.color.rgb, level * (int) sp.perLevel.value, (float) sp.size.value, .2);
            PerformanceMonitor.end("Sharpness Particles", t);
        }
        var cp = Modules.CRIT_PARTICLES;
        if (cp.enabled && looksCritical(player)) {
            long t = PerformanceMonitor.begin();
            Fx.burst(x, y, z, cp.color.rgb, (int) cp.count.value, (float) cp.size.value, .25);
            PerformanceMonitor.end("Critical Particles", t);
        }
    }

    private static void killCheck() {
        LivingEntity t = lastTarget;
        if (killHandled || t == null || !Modules.KILL_EFFECTS.enabled) return;
        if ((System.nanoTime() - lastHitNanos) / 1e9 > 6) { killHandled = true; return; }
        if (!t.isDead()) return;
        killHandled = true;
        long t0 = PerformanceMonitor.begin();
        var k = Modules.KILL_EFFECTS;
        Fx.burst(t.getX(), t.getY() + t.getHeight() * .5, t.getZ(), k.color.rgb, (int) k.count.value, (float) k.size.value, k.spread.value);
        PerformanceMonitor.end("Kill Effects", t0);
    }

    public static void tick(MinecraftClient mc) {
        if(Modules.PERFORMANCE.safe.value){health.clear();return;}
        killCheck();
        var dp = Modules.DAMAGE_PARTICLES;
        if (!dp.enabled || mc.world == null || mc.player == null) { health.clear(); return; }
        long t = PerformanceMonitor.begin();
        double r2 = dp.range.value * dp.range.value;
        seen.clear();
        for (Entity e : mc.world.getEntitiesByClass(LivingEntity.class,mc.player.getBoundingBox().expand(dp.range.value),e -> true)) {
            if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
            boolean self = le == mc.player;
            int mode = dp.targets.index;
            if ((mode == 0 && self) || (mode == 1 && !self)) continue;
            if (!self && le.squaredDistanceTo(mc.player) > r2) continue;
            int id = le.getId();
            seen.add(id);
            float now = le.getHealth() + le.getAbsorptionAmount();
            Float prev = health.put(id, now);
            if (prev != null && now < prev - .01f) {
                int n = ParticleMath.damageCount(prev - now, (int) dp.perHeart.value, (int) dp.cap.value);
                Fx.burst(le.getX(), le.getY() + le.getHeight() * .6, le.getZ(), dp.color.rgb, n, (float) dp.size.value, .12);
            }
        }
        health.keySet().retainAll(seen);
        PerformanceMonitor.end("Damage Particles", t);
    }
}
