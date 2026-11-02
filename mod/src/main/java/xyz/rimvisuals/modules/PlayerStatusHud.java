package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import xyz.rimvisuals.core.*;
import java.util.*;
/** Small real HUDs reading only locally available game state, refreshed at 10 Hz. */
public final class PlayerStatusHud extends HudModule {
 public enum Kind {TOTEMS,SATURATION,LOW_HP,TNT}
 public final Kind kind;
 public final Setting.Num threshold=add(new Setting.Num("Health threshold",6,1,20,1));
 private final List<String> lines=new ArrayList<>();private long next;private Object lastWorld;
 public PlayerStatusHud(String id,String name,Kind kind,int y){super(id,name,"Own-player / loaded-world information; no server or inventory actions",200,y);this.kind=kind;threshold.hidden=kind!=Kind.LOW_HP;enabled=false;}
 public static int countTotems(int main,int off){return Math.max(0,main)+Math.max(0,off);}
 public static double fuseSeconds(int ticks){return Math.max(0,ticks)/20.0;}
 private void refresh(MinecraftClient mc){long now=System.nanoTime();if(now<next&&lastWorld==mc.world)return;lastWorld=mc.world;next=now+100_000_000L;lines.clear();
  if(mc.player!=null&&mc.world!=null)switch(kind){
   case TOTEMS->{int count=0;var inventory=mc.player.getInventory();for(int i=0;i<Math.min(36,inventory.size());i++){var st=inventory.getStack(i);if(st.isOf(Items.TOTEM_OF_UNDYING))count+=st.getCount();}var off=mc.player.getOffHandStack();count=countTotems(count,off.isOf(Items.TOTEM_OF_UNDYING)?off.getCount():0);lines.add("Totems: "+count);}
   case SATURATION->{var h=mc.player.getHungerManager();lines.add("Food: "+h.getFoodLevel()+" / 20");lines.add(String.format(Locale.ROOT,"Saturation: %.1f",h.getSaturationLevel()));}
   case LOW_HP->{float hp=mc.player.getHealth();if(hp>0&&hp<=threshold.value)lines.add(String.format(Locale.ROOT,"LOW HP: %.1f",hp));}
   case TNT->{for(var tnt:mc.world.getEntitiesByClass(TntEntity.class,mc.player.getBoundingBox().expand(48),e->true)){lines.add(String.format(Locale.ROOT,"TNT %.1fs | %dm",fuseSeconds(tnt.getFuse()),Math.round(mc.player.distanceTo(tnt))));if(lines.size()==8)break;}}
  }
  if(lines.isEmpty()&&mc.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen)lines.add(switch(kind){case TOTEMS->"Totems: 3";case SATURATION->"Saturation: 5.0";case LOW_HP->"LOW HP: 4.0";case TNT->"TNT 2.5s | 12m";});
 }
 @Override public int width(MinecraftClient mc){return 150;}
 @Override public int height(MinecraftClient mc){refresh(mc);return Math.max(1,lines.size())*12+8;}
 @Override public void draw(DrawContext c,MinecraftClient mc,int x,int y){refresh(mc);if(lines.isEmpty())return;Gfx.round(c,x,y,150,height(mc),4,0xb80b0914);Gfx.fill(c,x,y+3,x+2,y+height(mc)-3,accent());for(String line:lines){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(line,136),x+8,y+4,accent());y+=12;}}
}
