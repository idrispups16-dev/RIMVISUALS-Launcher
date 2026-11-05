package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Blends the Overworld-style sky colour towards a chosen colour. Sun, moon, stars and the End sky are unchanged. */
public final class CustomSky extends Module {
    public final Setting.Color color = add(new Setting.Color("Sky color", 0x6D28D9));
    public final Setting.Num strength = add(new Setting.Num("Sky fade (blend)", 1, 0, 1, .05));

    public CustomSky() {
        super("custom_sky", "Custom Sky", "Blends the daytime sky dome colour with your colour; strength 0 is vanilla. Does not change the horizon fog (use Custom Fog) or the End/Nether sky.", Category.VISUALS);
        enabled = false;
    }
}
