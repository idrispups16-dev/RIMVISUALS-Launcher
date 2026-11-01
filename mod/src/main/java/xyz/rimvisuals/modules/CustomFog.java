package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Overrides the world fog colour and scales how far fog starts and ends. Cosmetic: it does not change what the server sends. */
public final class CustomFog extends Module {
    public final Setting.Bool recolor = add(new Setting.Bool("Override color", true));
    public final Setting.Color color = add(new Setting.Color("Fog color", 0x8A5CFF));
    public final Setting.Num strength = add(new Setting.Num("Color strength", 1, 0, 1, .05));
    public final Setting.Num distance = add(new Setting.Num("Fog distance (%)", 100, 20, 200, 5));

    public CustomFog() {
        super("custom_fog", "Custom Fog", "Changes fog colour (also the sky-edge/clear colour) and fog distance. Under 100% fog is thicker, over 100% thinner. Applies to every dimension and to water/lava fog distances.", Category.VISUALS);
        enabled = false;
    }
}
