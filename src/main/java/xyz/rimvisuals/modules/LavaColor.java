package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

public final class LavaColor extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF3B1F));
    public final Setting.Num strength = add(new Setting.Num("Strength", .7, 0, 1, .05));

    public LavaColor() {
        super("lava_color", "Lava Color", "Recolours lava surfaces. Terrain is rebuilt shortly after you change a setting.", Category.VISUALS);
        enabled = false;
    }
}
