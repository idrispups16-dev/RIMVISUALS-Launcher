package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.*;
public final class AspectRatio extends xyz.rimvisuals.core.Module {
 public final Setting.Num ratio=add(new Setting.Num("Width divided by height",1.78,.5,4,.01));
 public AspectRatio(){super("aspect_ratio","Aspect Ratio","Changes horizontal camera projection; viewport, GUI and mouse targeting stay vanilla",Category.VISUALS);enabled=false;}
 public static float xScale(double actual,double desired){return (float)(actual/Math.max(.5,Math.min(4,desired)));}
}
