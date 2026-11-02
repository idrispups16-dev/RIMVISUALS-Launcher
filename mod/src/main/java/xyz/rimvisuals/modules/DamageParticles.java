package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Particles when a nearby living entity loses health (observed from synced health). */
public final class DamageParticles extends Module {
    public final Setting.Mode source=add(new Setting.Mode("Trigger",0,"Server hurt packet","Observed health loss"));
    public final Setting.Num hurtCount=add(new Setting.Num("Particles per hurt",4,1,16,1));
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF2222));
    public final Setting.Mode targets = add(new Setting.Mode("Targets", 0, "Others", "Self", "Everyone"));
    public final Setting.Num perHeart = add(new Setting.Num("Particles per heart", 3, 1, 10, 1));
    public final Setting.Num cap = add(new Setting.Num("Max per event", 20, 1, 40, 1));
    public final Setting.Num range = add(new Setting.Num("Range", 24, 4, 48, 1));
    public final Setting.Num size = add(new Setting.Num("Size", 1, .3, 2, .1));

    public final Setting.Num opacity = add(new Setting.Num("Opacity", .8, .1, 1, .05));

    public DamageParticles() {
        super("damage_particles", "Damage Particles", "Spawns particles on entities that lose health, scaled by damage taken. Server hurt packets by default; optional synced-health delta mode. Hurt packets do not reveal damage amount.", Category.VISUALS);
        enabled = false;
    }
}
