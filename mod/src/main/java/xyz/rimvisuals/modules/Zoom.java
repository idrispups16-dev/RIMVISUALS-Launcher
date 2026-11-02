package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;
/** Camera-only FOV multiplier; bind Toggle Zoom in Keybinds. No options/sensitivity mutation. */
public final class Zoom extends Module {
 public final Setting.Num factor=add(new Setting.Num("FOV multiplier",.35,.1,1,.05));
 public final Setting.Bool smooth=add(new Setting.Bool("Smooth transition",true));
 private double current=1;private long last;
 public Zoom(){super("zoom","Zoom","Camera FOV only. Assign Toggle Zoom in Keybinds; no attack, movement or sensitivity changes",Category.UTILITIES);enabled=false;}
 public static double bounded(double value){return Double.isFinite(value)?Math.max(.1,Math.min(1,value)):1;}
 public double apply(double fov,boolean inWorld){
  double target=enabled&&inWorld?bounded(factor.value):1;
  if(target==1&&Math.abs(current-1)<.001){current=1;last=0;return fov;}
  long now=System.nanoTime();double dt=last==0?.05:Math.min(.05,Math.max(0,(now-last)/1e9));last=now;
  if(!smooth.value||Modules.PERFORMANCE.reducedMotion.value)current=target;
  else current+=(target-current)*(1-Math.exp(-14*dt));
  if(Math.abs(current-target)<.001)current=target;
  return fov*current;
 }
}
