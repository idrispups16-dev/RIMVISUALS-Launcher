package xyz.rimvisuals.gui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.HolyFeatureControl;
/** Generic policy status: never enumerates or names forbidden functionality. */
public final class HolyApiScreen extends Screen {
 public HolyApiScreen(){super(Text.literal("RIMVISUALS / HolyWorld API"));}
 @Override protected void init(){addDrawableChild(new RimButton(width/2-75,height/2+30,150,20,Text.literal("Back to game"),b->close()));}
 @Override public void render(DrawContext c,int x,int y,float delta){c.fill(0,0,width,height,0xf0101010);super.render(c,x,y,delta);c.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-35,0xffeeeeee);c.drawCenteredTextWithShadow(textRenderer,Text.literal(HolyFeatureControl.status()),width/2,height/2-10,0xffcccccc);}
 @Override public boolean shouldPause(){return false;}
 @Override public void close(){client.setScreen(null);}
}
