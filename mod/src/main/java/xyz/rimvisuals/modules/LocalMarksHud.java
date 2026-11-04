package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;
import java.util.*;
public final class LocalMarksHud extends HudModule {
 private List<String> lines=List.of();private long next;
 public LocalMarksHud(){super("local_marks","Local Markers","Local .way markers and temporary friend pings for this server and dimension",200,90);enabled=false;}
 private void refresh(MinecraftClient mc){long now=System.nanoTime();if(now<next)return;next=now+100_000_000L;var out=new ArrayList<String>();
  if(mc.player!=null){String w=LocalData.world(mc),d=LocalData.dimension(mc);for(var m:LocalData.marks())if(m.world().equals(w)&&m.dimension().equals(d)){
   double dx=m.x()-mc.player.getX(),dy=m.y()-mc.player.getY(),dz=m.z()-mc.player.getZ();out.add(m.name()+"  "+Math.round(Math.sqrt(dx*dx+dy*dy+dz*dz))+" m");if(out.size()==12)break;
  }}if(out.isEmpty()&&mc.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen)out.add("Local marker  24 m");lines=List.copyOf(out);
 }
 @Override public int width(MinecraftClient mc){return 180;}
 @Override public int height(MinecraftClient mc){refresh(mc);return Math.max(1,lines.size())*12+8;}
 @Override public void draw(DrawContext c,MinecraftClient mc,int x,int y){refresh(mc);if(lines.isEmpty())return;Gfx.round(c,x,y,180,height(mc),4,0xb80d0a16);for(String line:lines){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(line,168),x+6,y+4,accent());y+=12;}}
}
