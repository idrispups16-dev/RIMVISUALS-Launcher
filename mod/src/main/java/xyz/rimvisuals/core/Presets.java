package xyz.rimvisuals.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Built-in starting points. A preset only switches the cosmetic HUD/visual modules it manages on or off; your Viewmodel,
 * Swing, theme and every module's own settings are left alone, so it never wipes a personal setup.
 */
public final class Presets {
    public static final Map<String, Set<String>> ENABLED = new LinkedHashMap<>();
    static {
        ENABLED.put("PvP_Clean", Set.of("info_hud","armor_hud","target_hud","potion_hud","cps_hud","hit_marker","hit_color","hit_particles","crit_particles","sharpness_particles","crosshair","visual_hitbox"));
        ENABLED.put("Minimal", Set.of("info_hud"));
        ENABLED.put("Crystal_PvP", Set.of("info_hud", "armor_hud", "target_hud", "potion_hud", "cps_hud", "hit_marker", "damage_tint", "kill_effects"));
        ENABLED.put("Sword_PvP", Set.of("info_hud", "armor_hud", "target_hud", "cps_hud", "keystrokes_hud", "hit_marker", "hit_particles", "sharpness_particles", "sword_trails", "kill_effects"));
    }
    private Presets() {}

    /** Modules a preset may switch: toggleable HUD and VISUALS modules except the decorative Pulse Glow and Particle Effects. */
    public static boolean managed(Module m) {
        return m.toggleable && (m.category == Module.Category.HUD || m.category == Module.Category.VISUALS)
                && !m.id.equals("pulse_glow") && !m.id.equals("particles");
    }

    public static boolean apply(String name) {
        Set<String> on = ENABLED.get(name);
        if (on == null) return false;
        for (Module m : Modules.ALL) if (managed(m)) m.enabled = on.contains(m.id);
        if(name.equals("PvP_Clean"))clean();
        return true;
    }
    private static void clean(){
        Modules.PERFORMANCE.preset.index=0;Modules.PERFORMANCE.applyPreset();Modules.PERFORMANCE.safe.value=true;Modules.PERFORMANCE.safeParticles.index=0;
        Modules.HIT_PARTICLES.count.set(6);Modules.HIT_PARTICLES.size.set(.6);Modules.HIT_PARTICLES.opacity.set(.7);Modules.HIT_PARTICLES.speed.set(.08);Modules.HIT_PARTICLES.color.rgb=0x7dd3fc;
        Modules.CRIT_PARTICLES.count.set(6);Modules.CRIT_PARTICLES.size.set(.65);Modules.CRIT_PARTICLES.opacity.set(.75);Modules.CRIT_PARTICLES.color.rgb=0xfde68a;
        Modules.SHARPNESS_PARTICLES.perLevel.set(2);Modules.SHARPNESS_PARTICLES.size.set(.55);Modules.SHARPNESS_PARTICLES.opacity.set(.65);Modules.SHARPNESS_PARTICLES.color.rgb=0xc4b5fd;
        Modules.DAMAGE_PARTICLES.opacity.set(.5);Modules.DAMAGE_PARTICLES.cap.set(6);Modules.EXPLOSION_EFFECTS.amount.set(12);Modules.EXPLOSION_EFFECTS.opacity.set(.55);
        Modules.HIT_COLOR.color.rgb=0xfda4af;Modules.HIT_COLOR.alpha.set(.35);Modules.DAMAGE_TINT.strength.set(.18);Modules.DAMAGE_TINT.edge.set(9);
        Modules.HIT_MARKER.color.rgb=0xe0f2fe;Modules.HIT_MARKER.duration.set(200);Modules.HIT_MARKER.size.set(3);Modules.HIT_MARKER.gap.set(5);
        Modules.CROSSHAIR.color.rgb=0x7dd3fc;Modules.CROSSHAIR.size.set(4);Modules.CROSSHAIR.gap.set(2);Modules.CROSSHAIR.thickness.set(1);
        Modules.VISUAL_HITBOX.targetOnly.value=true;Modules.VISUAL_HITBOX.distance.set(24);Modules.VISUAL_HITBOX.thickness.set(.01);Modules.VISUAL_HITBOX.opacity.set(.5);
        Modules.reset(Modules.SWING);Modules.SWING.enabled=true;Modules.SWING.style.index=1;Modules.SWING.angle.set(25);Modules.SWING.offset.set(.08);Modules.SWING.intensity.set(.45);Modules.SWING.easing.index=1;
        Modules.reset(Modules.VIEWMODEL);Modules.VIEWMODEL.enabled=true;Modules.VIEWMODEL.main.y.set(-.08);Modules.VIEWMODEL.main.z.set(-.1);Modules.VIEWMODEL.main.scale.set(.95);Modules.VIEWMODEL.off.y.set(-.08);Modules.VIEWMODEL.off.scale.set(.95);Modules.VIEWMODEL.preset.index=4;Modules.VIEWMODEL.seedPreset();
        Modules.CRYSTAL_SCALE.scale.set(1);Modules.CRYSTAL_FADE.opacity.set(.9);Modules.CRYSTAL_FADE.fadeIn.set(4);
    }
}
