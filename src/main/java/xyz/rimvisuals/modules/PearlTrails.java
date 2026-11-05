package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Coloured particle trail behind ender pearls in flight. Client-side cosmetic; uses the shared particle budget. */
public final class PearlTrails extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0x8A5CFF));
    public final Setting.Num density = add(new Setting.Num("Density", 3, 1, 8, 1));
    public final Setting.Num size = add(new Setting.Num("Size", .8, .3, 2, .1));
    public final Setting.Bool ownOnly = add(new Setting.Bool("Own projectiles only", false));

    public PearlTrails() {
        super("pearl_trails", "Pearl Trails", "Coloured particle trail behind ender pearls in flight. Count per tick is capped by the performance budget (Safe Mode: 6).", Category.VISUALS);
        enabled = false;
    }
}
