package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Overrides the biome tint of grass and foliage. Terrain is rebuilt shortly after a setting changes. */
public final class BiomeColors extends Module {
    public final Setting.Bool grass = add(new Setting.Bool("Tint grass", true));
    public final Setting.Color grassColor = add(new Setting.Color("Grass color", 0x5CFF8A));
    public final Setting.Bool foliage = add(new Setting.Bool("Tint leaves", true));
    public final Setting.Color foliageColor = add(new Setting.Color("Leaves color", 0x3FD07F));
    public final Setting.Num strength = add(new Setting.Num("Strength", .8, 0, 1, .05));

    public BiomeColors() {
        super("biome_colors", "Biome Colors", "Overrides the biome tint on grass, ferns and leaves. Terrain is rebuilt shortly after you change a setting.", Category.VISUALS);
        enabled = false;
    }
}
