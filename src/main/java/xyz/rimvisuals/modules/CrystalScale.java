package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.ScaleMath;
import xyz.rimvisuals.core.Setting;

/** Client-side visual size of End Crystals. Hitboxes and gameplay are unchanged. */
public final class CrystalScale extends Module {
    public final Setting.Num scale = add(new Setting.Num("Crystal scale", 1, .4, 1.6, .05));
    public final Setting.Num growIn = add(new Setting.Num("Grow-in ticks", 0, 0, 20, 1));

    public CrystalScale() {
        super("crystal_scale", "Crystal Scale", "Changes how large End Crystals are drawn and lets new crystals grow in. Visual only; hitbox is unchanged.", Category.VISUALS);
        enabled = false;
    }

    /** Final model scale for a crystal that has existed for {@code ageTicks}. */
    public float factor(float ageTicks) {
        if (!isActive()) return 1f;
        boolean still = xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value;
        float grow = still ? 1f : ScaleMath.growIn(ageTicks, (float) growIn.value);
        return (float) scale.value * grow;
    }
}
