package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import xyz.rimvisuals.core.CombatEvents;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Outlines the entity you are aiming at and/or the one you hit last, using the game's glow-outline pass. */
public final class TargetHighlight extends Module {
    public final Setting.Bool crosshair = add(new Setting.Bool("Crosshair target", true));
    public final Setting.Bool lastHit = add(new Setting.Bool("Last hit target", true));
    public final Setting.Num hold = add(new Setting.Num("Last hit hold (s)", 3, .5, 10, .5));
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF3B6B));
    public final Setting.Color hitColor = add(new Setting.Color("Last hit color", 0xFFC93C));

    public TargetHighlight() {
        super("target_highlight", "Target Highlight", "Outlines the entity under your crosshair and/or the entity you hit last, through walls.", Category.VISUALS);
        enabled = false;
    }

    /** Outline colour (RGB) for this entity, or -1 when it is not highlighted. Last hit wins over crosshair. */
    public int colorFor(Entity e, MinecraftClient mc) {
        if (!isActive() || e == null || mc.player == null || e == mc.player || !xyz.rimvisuals.core.Modules.PERFORMANCE.outlineAllowed(e.squaredDistanceTo(mc.player))) return -1;
        if (lastHit.value) {
            LivingEntity t = CombatEvents.recentTarget(hold.value);
            if (t == e) return hitColor.rgb;
        }
        if (crosshair.value && mc.targetedEntity == e && e instanceof LivingEntity) return color.rgb;
        return -1;
    }
}
