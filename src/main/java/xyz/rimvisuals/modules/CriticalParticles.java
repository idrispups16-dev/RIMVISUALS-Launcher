package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Extra coloured particles when your attack meets vanilla's critical-hit conditions (client-side estimate). */
public final class CriticalParticles extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFFD34D));
    public final Setting.Num count = add(new Setting.Num("Particles per crit", 14, 1, 40, 1));
    public final Setting.Num size = add(new Setting.Num("Size", 1, .3, 2, .1));

    public final Setting.Num opacity = add(new Setting.Num("Opacity", .8, .1, 1, .05));

    public CriticalParticles() {
        super("crit_particles", "Critical Particles", "Adds your own coloured burst when an attack looks like a critical hit (falling, not sprinting, cooldown nearly full). The server decides the real crit; vanilla particles are unchanged.", Category.VISUALS);
        enabled = false;
    }
}
