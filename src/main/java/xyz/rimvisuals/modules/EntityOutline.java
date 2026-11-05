package xyz.rimvisuals.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Uses the game's own glow-outline pass for chosen entities. No extra rendering pass is added. */
public final class EntityOutline extends Module {
    public final Setting.Bool players = add(new Setting.Bool("Players", true));
    public final Setting.Bool hostile = add(new Setting.Bool("Hostile mobs", false));
    public final Setting.Bool others = add(new Setting.Bool("Other living entities", false));
    public final Setting.Color color = add(new Setting.Color("Color", 0xA855F7));

    public EntityOutline() {
        super("entity_outline", "Entity Outline", "Outlines selected entities through walls using the game's glow outline. Your own body is not outlined.", Category.VISUALS);
        enabled = false;
    }

    public boolean selects(Entity e, Entity self) {
        if (e == self || e == null || self == null || !xyz.rimvisuals.core.Modules.PERFORMANCE.outlineAllowed(e.squaredDistanceTo(self))) return false;
        if (e instanceof PlayerEntity) return players.value;
        if (e instanceof Monster) return hostile.value;
        return others.value && e instanceof net.minecraft.entity.LivingEntity;
    }
}
