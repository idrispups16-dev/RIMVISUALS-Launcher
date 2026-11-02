package xyz.rimvisuals.modules;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;
public final class Crosshair extends xyz.rimvisuals.core.Module {
 public final Setting.Num size=add(new Setting.Num("Arm length",5,1,20,1)),gap=add(new Setting.Num("Gap",3,0,12,1)),thickness=add(new Setting.Num("Thickness",1,1,5,1)),opacity=add(new Setting.Num("Opacity",1,.1,1,.05));
 public final Setting.Color color=add(new Setting.Color("Color",0x60a5fa));public final Setting.Bool dot=add(new Setting.Bool("Center dot",true));
 public Crosshair(){super("crosshair","Crosshair","Own first-person crosshair; does not change aim, attacks or targeting",Category.VISUALS);enabled=false;}
 public void draw(DrawContext c,int w,int h){int x=w/2,y=h/2,n=(int)size.value,g=(int)gap.value,t=(int)thickness.value,col=Gfx.withAlpha(color.rgb,(float)opacity.value);c.fill(x-g-n,y-t/2,x-g,y-t/2+t,col);c.fill(x+g+1,y-t/2,x+g+n+1,y-t/2+t,col);c.fill(x-t/2,y-g-n,x-t/2+t,y-g,col);c.fill(x-t/2,y+g+1,x-t/2+t,y+g+n+1,col);if(dot.value)c.fill(x,y,x+t,y+t,col);}
}
