package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
public final class SwingAnimations extends Module {
 private final xyz.rimvisuals.core.AttackSwingClock mainAttack=new xyz.rimvisuals.core.AttackSwingClock(),offAttack=new xyz.rimvisuals.core.AttackSwingClock();
 private Object world;
 public void attack(boolean main,long now){if(!isActive()||style.index==0||xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value)return;(main?mainAttack:offAttack).start(now,(long)(300_000_000d*duration.value/(main?mainSpeed.value:offSpeed.value)));}
 public void clearAttacks(){mainAttack.clear();offAttack.clear();}
 public void worldChanged(Object current){if(world!=current){world=current;clearAttacks();}}
 public final Setting.Mode style=add(new Setting.Mode("Style",1,"Default","Smooth","Classic","Slide","Spin","Swipe","Push","Reverse","Minimal","Custom"));
 public final Setting.Num mainSpeed=add(new Setting.Num("Main visual speed",1,.2,3,.05)),offSpeed=add(new Setting.Num("Off visual speed",1,.2,3,.05));
 public final Setting.Num angle=add(new Setting.Num("Angle",60,0,180,1)),offset=add(new Setting.Num("Offset",.3,0,1,.01)),intensity=add(new Setting.Num("Intensity",1,0,2,.05));
 public final Setting.Num duration=add(new Setting.Num("Visual duration multiplier",1,.25,3,.05));
 public final Setting.Mode easing=add(new Setting.Mode("Easing",1,"Linear","Smoothstep","Sine"));
 public SwingAnimations(){super("swing","Swing Animations","Adds submission-time swing transforms; attack timing unchanged. Reduced Motion disables this.",Category.SWING);enabled=false;}
 public boolean apply(MatrixStack m,int sign,float progress,boolean mainHand){return apply(m,sign,progress,mainHand,System.nanoTime());}
 public boolean apply(MatrixStack m,int sign,float progress,boolean mainHand,long now){if(!isActive()||style.index==0||xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value){clearAttacks();return false;}
  long start=xyz.rimvisuals.core.PerformanceMonitor.begin();try{
  float attack=(mainHand?mainAttack:offAttack).progress(now);double source=attack>=0?attack:progress,speed=attack>=0?1:(mainHand?mainSpeed.value:offSpeed.value)/duration.value;
  float phase=(float)xyz.rimvisuals.core.VisualMath.phase(source,speed,easing.index);
  float wave=(float)xyz.rimvisuals.core.VisualMath.envelope(source,speed,easing.index,intensity.value),a=(float)angle.value*wave*sign,o=(float)offset.value*wave;
  switch(style.index){case 1->m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-a*.5f));case 2->{m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(a));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-a));}case 3->m.translate(-sign*o,0,0);case 4->m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(360*phase*(float)intensity.value*sign));case 5->{m.translate(sign*o,0,0);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-a));}case 6->m.translate(0,0,-o);case 7->m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(a));case 8->m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-a*.1f));case 9->{m.translate(sign*o,-o*.5f,-o);m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(a));}}
  return true;
 }finally{xyz.rimvisuals.core.PerformanceMonitor.end("Swing",start);}}
}
