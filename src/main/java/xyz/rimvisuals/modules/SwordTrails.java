package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Coloured particles along the sword tip while swinging a sword. Client-side cosmetic; uses the shared particle budget. */
public final class SwordTrails extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF4D6D));
    public final Setting.Num density = add(new Setting.Num("Density", 3, 1, 8, 1));
    public final Setting.Num size = add(new Setting.Num("Size", .8, .3, 2, .1));
    public final Setting.Mode who = add(new Setting.Mode("Whose swings", 0, "Self", "Everyone"));
    public final Setting.Num reach = add(new Setting.Num("Arc reach", 1.6, .8, 2.5, .1));

    public SwordTrails() {
        super("sword_trails", "Sword Trails", "Coloured particles along the sword tip while swinging a sword. Count per tick is capped by the performance budget (Safe Mode: 6).", Category.VISUALS);
        enabled = false;
    }
}
