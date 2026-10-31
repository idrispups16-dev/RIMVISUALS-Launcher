package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;
public final class PerformanceOptions extends Module {
 public final Setting.Bool safe=add(new Setting.Bool("Performance Safe Mode",true));
 public final Setting.Bool reducedMotion=add(new Setting.Bool("Reduced motion",false));
 public final Setting.Bool adaptive=add(new Setting.Bool("Adaptive particles",true));
 public final Setting.Bool monitor=add(new Setting.Bool("Performance monitor",true));
 public final Setting.Num particleLimit=add(new Setting.Num("Particle limit",16,0,80,1));
 public final Setting.Num targetFps=add(new Setting.Num("Target FPS",60,30,240,1));
 public final Setting.Mode preset=add(new Setting.Mode("Preset",0,"Low-End","Balanced","Ultra","Custom"));
 private int applied=-1;
 public PerformanceOptions(){super("performance","Performance","CPU submission timings, bounded effects; no GPU/FPS claims",Category.OPTIMIZATION);toggleable=false;}
 public void seedPreset(){applied=preset.index;}
 public void applyPreset(){if(applied==preset.index)return;applied=preset.index;if(applied==3)return;safe.value=applied==0;particleLimit.set(applied==0?8:applied==1?24:64);}
 public int particles(int requested,int fps){if(safe.value)return 0;int n=Math.min(requested,(int)particleLimit.value);if(adaptive.value&&fps>0&&fps<targetFps.value)n=(int)(n*Math.max(.15,fps/targetFps.value));return Math.max(0,n);}
 /** Budget for optional combat/trail particles: limit, Safe Mode cap of 6 per event, adaptive scaling below target FPS. */
 public int effectParticles(int requested,int fps){return xyz.rimvisuals.core.ParticleMath.budget(requested,(int)particleLimit.value,safe.value,6,adaptive.value,fps,(int)targetFps.value);}
}
