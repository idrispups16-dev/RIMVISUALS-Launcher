package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Particle burst when the entity you last hit dies within a few seconds. Client-side estimate. */
public final class KillEffects extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xA855F7));
    public final Setting.Num count = add(new Setting.Num("Particles", 24, 4, 60, 1));
    public final Setting.Num size = add(new Setting.Num("Size", 1.2, .3, 2.5, .1));
    public final Setting.Num spread = add(new Setting.Num("Spread", .25, .05, .6, .01));

    public KillEffects() {
        super("kill_effects", "Kill Effects", "Spawns a burst where your last target dies (health reaches 0 within 6 s of your hit). Obeys the performance budget.", Category.VISUALS);
        enabled = false;
    }
}
