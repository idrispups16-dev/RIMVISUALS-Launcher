package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.ScaleMath;
import xyz.rimvisuals.core.Setting;

/** Client-side transparency of End Crystals, including a fade-in on spawn. Visual only. */
public final class CrystalFade extends Module {
    public final Setting.Num opacity = add(new Setting.Num("Crystal opacity", 1, .3, 1, .05));
    public final Setting.Num fadeIn = add(new Setting.Num("Fade-in ticks", 0, 0, 20, 1));

    public CrystalFade() {
        super("crystal_fade", "Crystal Fade", "Makes End Crystals translucent and lets new crystals fade in. Visual only; minimum opacity is limited so crystals stay visible.", Category.VISUALS);
        enabled = false;
    }

    /** Alpha 0..1 for a crystal that has existed for {@code ageTicks}; 1 means draw exactly like vanilla. */
    public float alpha(float ageTicks) {
        if (!isActive()) return 1f;
        float ticks = xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value ? 0f : (float) fadeIn.value;
        return ScaleMath.fadeAlpha(ageTicks, ticks, (float) opacity.value);
    }
}
