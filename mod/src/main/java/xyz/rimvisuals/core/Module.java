package xyz.rimvisuals.core;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum Category { HUD, VISUALS, INTERFACE, OPTIMIZATION, VIEWMODEL, SWING, SHADER_HANDS, UTILITIES }

    public final String id, name, description;
    public final Category category;
    public boolean enabled = true;
    public boolean toggleable = true;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String id, String name, String description, Category category) {
        this.id = id; this.name = name; this.description = description; this.category = category;
    }

    private static final java.util.Set<String> SAFE_SUPPRESSED=java.util.Set.of("china_hat","block_overlay","anchor_fade","hand_glow","entity_outline","target_highlight","damage_particles","hit_particles","critical_particles","crit_particles","sharpness_particles","kill_effects","pearl_trails","projectile_trails","sword_trails","explosion_effects","particles","pulse_glow");
    public String suppressionReason(){return enabled&&Modules.PERFORMANCE.safe.value&&SAFE_SUPPRESSED.contains(id)?"SAFE MODE":"";}
    public <T extends Setting> T add(T setting) { settings.add(setting); return setting; }
}
