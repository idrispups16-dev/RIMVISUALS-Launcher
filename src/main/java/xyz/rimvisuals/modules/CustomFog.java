package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Overrides the world fog colour and scales how far fog starts and ends. Cosmetic: it does not change what the server sends. */
public final class CustomFog extends Module {
    public final Setting.Bool recolor = add(new Setting.Bool("Override color", true));
    public final Setting.Color color = add(new Setting.Color("Fog color", 0x8A5CFF));
    public final Setting.Num strength = add(new Setting.Num("Color strength", 1, 0, 1, .05));
    public final Setting.Num distance = add(new Setting.Num("Fog distance (%)", 100, 20, 200, 5));

    public final Setting.Mode mode=add(new Setting.Mode("Distance mode",0,"Visible range","Legacy percent"));
    public final Setting.Num near=add(new Setting.Num("Starts at blocks",12,0,128,1));
    public final Setting.Num far=add(new Setting.Num("Ends at blocks",48,4,192,1));
    public final Setting.Bool fluids=add(new Setting.Bool("Affect fluid fog",false));
    public void refresh(){distance.hidden=mode.index==0;near.hidden=far.hidden=mode.index!=0;}
    @Override public String statusNote(){return (mode.index==0?"Range "+(int)xyz.rimvisuals.core.FogMath.start(near.value,xyz.rimvisuals.core.FogMath.end(far.value))+"-"+(int)xyz.rimvisuals.core.FogMath.end(far.value)+" blocks. ":"Legacy percentage. ")+(xyz.rimvisuals.core.ClientCompatibility.iris?"Iris detected: compare with shader pack OFF; a shader can override fog.":xyz.rimvisuals.core.ClientCompatibility.sodium?"Sodium detected; world compatibility test required.":"Overworld air fog; fluid fog is opt-in.");}

    public CustomFog() {
        super("custom_fog", "Custom Fog", "Visible range changes render-distance fog in blocks; Legacy preserves percentage configs. Custom Fog takes priority over Atmosphere. Iris shader packs may override the result.", Category.VISUALS);
        enabled = false;
    }
}
