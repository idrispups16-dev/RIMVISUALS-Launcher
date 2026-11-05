package xyz.rimvisuals.core;
/** Keeps the render-distance fog interval finite and ordered. */
public final class FogMath {
 public static float start(double start,double end){return (float)Math.max(0,Math.min(start,Math.max(1,end)-1));}
 public static float end(double end){return (float)Math.max(1,Math.min(512,end));}
}
