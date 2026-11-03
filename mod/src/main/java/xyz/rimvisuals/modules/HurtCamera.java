package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Scales the camera roll when you take damage. The death tilt is untouched. */
public final class HurtCamera extends Module {
    public final Setting.Num strength = add(new Setting.Num("Tilt strength (%)", 0, 0, 150, 5));

    public HurtCamera() {
        super("hurt_camera", "Hurt Camera", "Scales the camera shake/roll when damaged on top of the vanilla Damage Tilt option (0% removes it). Death animation is unchanged.", Category.VISUALS);
        enabled = false;
    }

    public double scale() { return isActive() ? strength.value / 100.0 : 1.0; }
}
