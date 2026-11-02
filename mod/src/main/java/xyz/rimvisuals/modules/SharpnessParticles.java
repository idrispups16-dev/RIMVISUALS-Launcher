package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Extra particles when you hit with a weapon that has Sharpness. More levels, more particles. */
public final class SharpnessParticles extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0x7DD3FC));
    public final Setting.Num perLevel = add(new Setting.Num("Particles per level", 3, 1, 10, 1));
    public final Setting.Num size = add(new Setting.Num("Size", .9, .3, 2, .1));

    public SharpnessParticles() {
        super("sharpness_particles", "Sharpness Particles", "Adds a coloured burst on hits made with a Sharpness-enchanted held item (level read from the item you hold). Obeys the performance budget.", Category.VISUALS);
        enabled = false;
    }
}
