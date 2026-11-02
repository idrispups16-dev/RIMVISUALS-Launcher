package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;
import java.util.*;
public final class UtilityHud extends HudModule {
 private final boolean keys;private final List<String> lines=new ArrayList<>();private long next;
 public UtilityHud(String id,String name,boolean keys,int y){super(id,name,keys?"Assigned RIM key bindings, up to eight rows; no input simulation":"Own heading and yaw",200,y);this.keys=keys;enabled=false;}
 private void refresh(MinecraftClient mc){long now=System.nanoTime();if(now<next)return;next=now+100_000_000;lines.clear();
  if(keys){for(var a:Keybinds.actions())if(!a.key().getBoundKeyTranslationKey().equals("key.keyboard.unknown")){lines.add(a.label()+": "+a.key().getBoundKeyLocalizedText().getString());if(lines.size()==8)break;}}
  else if(mc.player!=null)lines.add(mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT)+"  "+Math.round(net.minecraft.util.math.MathHelper.wrapDegrees(mc.player.getYaw()))+" deg");
  if(lines.isEmpty()&&mc.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen)lines.add(keys?"Open ClickGUI: Right Shift":"NORTH  180 deg");
 }
 @Override public int width(MinecraftClient mc){return 190;}
 @Override public int height(MinecraftClient mc){refresh(mc);return Math.max(1,lines.size())*12+8;}
 @Override public void draw(DrawContext c,MinecraftClient mc,int x,int y){refresh(mc);if(lines.isEmpty())return;Gfx.round(c,x,y,190,height(mc),4,0xb80c0914);for(String s:lines){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(s,178),x+6,y+4,accent());y+=12;}}
}
