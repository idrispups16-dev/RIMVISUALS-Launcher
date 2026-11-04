package xyz.rimvisuals.core;
public final class VisualMath {
 private VisualMath(){}
 public static double phase(double progress,double speed,int easing){if(!Double.isFinite(progress)||!Double.isFinite(speed))return 0;double t=Math.max(0,Math.min(1,progress*speed));if(easing==1)t=t*t*(3-2*t);else if(easing==2)t=(1-Math.cos(t*Math.PI))/2;return t;}
 public static double envelope(double progress,double speed,int easing,double intensity){if(!Double.isFinite(progress)||!Double.isFinite(speed)||!Double.isFinite(intensity)||progress<=0||progress>=1)return 0;double t=Math.max(0,Math.min(1,progress*speed));if(easing==1)t=t*t*(3-2*t);else if(easing==2)t=(1-Math.cos(t*Math.PI))/2;return Math.sin(t*Math.PI)*Math.max(0,Math.min(2,intensity));}
 public static double smoothing(double seconds){return 1-Math.exp(-Math.max(0,Math.min(.1,seconds))*16);}
}
