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
        return true;
    }
}
