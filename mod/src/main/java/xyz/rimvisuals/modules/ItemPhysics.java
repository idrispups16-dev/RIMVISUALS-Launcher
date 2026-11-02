package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.*;
public final class ItemPhysics extends xyz.rimvisuals.core.Module {
 public final Setting.Num groundTilt=add(new Setting.Num("Ground tilt",90,0,180,1)),spin=add(new Setting.Num("Extra spin degrees per tick",2,0,10,.1)),scale=add(new Setting.Num("Scale",1,.5,2,.05));
 public ItemPhysics(){super("item_physics","Item Physics (visual)","Cosmetic dropped-item tilt/spin/scale. Vanilla bob remains; item position, pickup and physics are unchanged",Category.VISUALS);enabled=false;}
}
