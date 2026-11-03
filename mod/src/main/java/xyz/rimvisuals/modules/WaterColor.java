package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

public final class WaterColor extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0x2E7DFF));
    public final Setting.Num strength = add(new Setting.Num("Strength", .8, 0, 1, .05));

    public WaterColor() {
        super("water_color", "Water Color", "Recolours water in the world (blocks and cauldrons). Terrain is rebuilt shortly after you change a setting.", Category.VISUALS);
        enabled = false;
    }
}
