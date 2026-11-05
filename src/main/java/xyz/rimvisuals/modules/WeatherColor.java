package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

public final class WeatherColor extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0x9FD8FF));
    public final Setting.Num strength = add(new Setting.Num("Strength", .8, 0, 1, .05));
    public final Setting.Num opacity = add(new Setting.Num("Precipitation opacity", 1, .1, 1, .05));

    public WeatherColor() {
        super("weather_color", "Weather Color", "Tints falling rain and snow. Applies live, no rebuild needed.", Category.VISUALS);
        enabled = false;
    }
}
