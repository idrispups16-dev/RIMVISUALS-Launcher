package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;
import java.util.List;
/** Dedicated helper HUDs; existing HUD renderer/editor supplies layout, color and opacity. */
public final class HelperHud extends HudModule {
 public enum Kind{EVENTS,COOLDOWNS,REGIONS,NOTIFICATIONS}
 public final ServerHelper helper;public final Kind kind;
 public HelperHud(ServerHelper helper,Kind kind,int y){super(helper.id+"_"+kind.name().toLowerCase(java.util.Locale.ROOT),helper.name+" "+kind,"Gameplay settings: Utilities > HW/FT Settings",460,y);this.helper=helper;this.kind=kind;}
 @Override public boolean visible(){return enabled&&ServerHelpers.active()==helper&&(kind!=Kind.EVENTS||helper.events.value)&&(kind!=Kind.COOLDOWNS||helper.cooldowns.value)&&(kind!=Kind.NOTIFICATIONS||helper.notifications.value&&!HelperRuntime.notice().isEmpty());}
 private List<String> lines(){return switch(kind){case EVENTS->HelperRuntime.eventRows();case REGIONS->HelperRuntime.regionRows();case NOTIFICATIONS->List.of(HelperRuntime.notice());case COOLDOWNS->List.of();};}
 @Override public int width(MinecraftClient mc){return 260;}
 @Override public int height(MinecraftClient mc){return kind==Kind.COOLDOWNS?24+Math.max(1,HelperRuntime.cooldownRows().size())*24:24+Math.max(1,lines().size())*11;}
 @Override public void draw(DrawContext c,MinecraftClient mc,int x,int y){
  Gfx.round(c,x,y,width(mc),height(mc),4,0xc00c0914);Gfx.text(c,mc.textRenderer,name,x+6,y+5,accent());int yy=y+20;
  if(ServerHelpers.active()!=helper){Gfx.text(c,mc.textRenderer,"Inactive; enable helper on matching server",x+6,yy,accent());return;}
  if(kind==Kind.COOLDOWNS){var rows=HelperRuntime.cooldownRows();if(rows.isEmpty())Gfx.text(c,mc.textRenderer,"No observed cooldowns (~ = estimate)",x+6,yy,accent());
   for(var row:rows){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(row.text(),248),x+6,yy,0xff000000|row.rgb());Gfx.fill(c,x+6,yy+12,x+254,yy+16,0xff252030);Gfx.fill(c,x+6,yy+12,x+6+(int)(248*row.progress()),yy+16,0xff000000|row.rgb());yy+=24;}
  }else for(String line:lines()){Gfx.text(c,mc.textRenderer,mc.textRenderer.trimToWidth(line,248),x+6,yy,accent());yy+=11;}
 }
}
