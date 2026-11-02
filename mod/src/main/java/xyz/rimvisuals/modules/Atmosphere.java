package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.*;
public final class Atmosphere extends xyz.rimvisuals.core.Module {
 public final Setting.Mode preset=add(new Setting.Mode("Palette",0,"Dusk","Cool Night","Warm Day","Custom"));
 public final Setting.Color color=add(new Setting.Color("Custom palette",0xb47adb));public final Setting.Num strength=add(new Setting.Num("Sky and fog blend",.6,0,1,.05));
 public Atmosphere(){super("atmosphere","Atmosphere","Shared sky/fog colour grading. Existing Custom Sky/Fog take priority. No time, weather or lighting simulation changes",Category.VISUALS);enabled=false;}
 public int rgb(){return switch(preset.index){case 0->0xb47adb;case 1->0x334b88;case 2->0xf4be81;default->color.rgb;};}
}
