package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;

public final class ServerHelperHud extends HudModule {
 private final ServerHelper helper;private final java.util.List<String> inactive;
 public ServerHelperHud(ServerHelper h,int y){super(h.id+"_hud",h.name+" HUD","Received server information only; edit layout here, parsing rules in Utilities",230,y);helper=h;inactive=java.util.List.of(h.name+" — inactive");}
 private java.util.List<String> lines(){return ServerHelpers.active()==helper?ServerHelpers.lines(helper):inactive;}
 @Override public boolean visible(){return enabled&&(ServerHelpers.active()==helper||MinecraftClient.getInstance().currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen);}
 @Override public int width(MinecraftClient mc){return 222;}
 @Override public int height(MinecraftClient mc){return 8+12*Math.max(1,lines().size());}
 @Override public void draw(DrawContext c,MinecraftClient mc,int x,int y){
  Gfx.round(c,x,y,222,height(mc),4,0xb80c0914);int yy=y+4;
  for(String line:lines()){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(line,210),x+6,yy,accent());yy+=12;}
 }
}
