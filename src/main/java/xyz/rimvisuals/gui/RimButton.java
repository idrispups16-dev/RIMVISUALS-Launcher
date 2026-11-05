package xyz.rimvisuals.gui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.Gfx;
/** Lightweight original button style; uses Minecraft's focus, narration and click handling. */
public final class RimButton extends ButtonWidget {
 public RimButton(int x,int y,int w,int h,net.minecraft.text.Text text,PressAction action){super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);}
 @Override public void drawIcon(DrawContext c,int mx,int my,float delta){
  c.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xff111111);
  boolean hover=isHovered()||isFocused();
  Gfx.roundBorder(c,getX(),getY(),getWidth(),getHeight(),5,hover?0xff353535:0xff202020,hover?0xff999999:0xff484848);
  c.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-8)/2,active?0xffeeeeee:0xff777777);
 }
}
