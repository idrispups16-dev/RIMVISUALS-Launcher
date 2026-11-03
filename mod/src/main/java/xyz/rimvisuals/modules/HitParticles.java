package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Coloured particles at the point where YOU hit a living entity. Client-side cosmetic only. */
public final class HitParticles extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF3355));
    public final Setting.Num count = add(new Setting.Num("Particles per hit", 10, 1, 40, 1));
    public final Setting.Num size = add(new Setting.Num("Size", 1, .3, 2, .1));
    public final Setting.Num speed = add(new Setting.Num("Spread", .15, .03, .4, .01));

    public final Setting.Num opacity = add(new Setting.Num("Opacity", .8, .1, 1, .05));

    public HitParticles() {
        super("hit_particles", "Hit Particles", "Spawns coloured particles when you attack a living entity. Count obeys the performance budget and Safe Mode cap.", Category.VISUALS);
        enabled = false;
    }
}
