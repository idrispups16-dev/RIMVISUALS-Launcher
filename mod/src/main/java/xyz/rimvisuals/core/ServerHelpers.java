package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import xyz.rimvisuals.modules.ServerHelper;
import java.util.*;

/** Runs on the client thread. No network requests, outgoing chat, file IO or render hooks. */
public final class ServerHelpers {
 private static String address="";private static ServerHelper current;
 private static int tick;private static boolean low;private static final boolean[] warned=new boolean[4];
 private static List<String> display=List.of();private static long actionUntil;private static Set<String> sidebar=Set.of();private static long budgetAt;private static int budget;private static int auctionMatches;
 private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
 private ServerHelpers(){}
 public static String address(){return address;}
 public static ServerHelper active(){return current!=null&&current.enabled?current:null;}
 public static void clear(){Notifier.clearPrefix("HW Helper:");Notifier.clearPrefix("FT Helper:");Modules.HW_HELPER.session.clear();Modules.FT_HELPER.session.clear();address="";current=null;display=List.of();low=false;Arrays.fill(warned,false);tick=0;actionUntil=0;sidebar=Set.of();budgetAt=0;budget=0;auctionMatches=0;}
 public static void init(){
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,mc)->clear());
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->clear());
  net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message,overlay)->receive(message.getString(),overlay?1:0));
 }
 public static void receive(String message,int source){
  ServerHelper h=active();if(h==null)return;
  if(source==0&&!h.chat.value||source==1&&!h.action.value||source==2&&!h.scoreboard.value)return;
  long now=System.nanoTime();if(now-budgetAt>=1_000_000_000L){budgetAt=now;budget=0;}if(++budget>64)return;String s=HelperData.clean(message);
  if(source==1){boolean same=s.equals(h.session.action);h.session.action=s;actionUntil=now+5_000_000_000L;if(same)return;}
  if(h.session.accept(s,h.eventPrefix.value,h.timerPrefix.value,h.ft?h.regionPrefix.value:"",now)&&h.notifications.value)Notifier.push(h.name+": "+s,0x8B8BFF);
 }
 public static List<String> lines(ServerHelper h){return active()==h?display:List.of(h.name+" — inactive");}
 public static void tick(MinecraftClient mc){
  if(mc.player==null||mc.world==null){if(current!=null||!address.isEmpty())clear();return;}
  var entry=mc.getCurrentServerEntry();String connected=entry==null?"":entry.address;
  ServerHelper next=Modules.HW_HELPER.enabled&&HelperData.matches(connected,false,Modules.HW_HELPER.aliases.value)?Modules.HW_HELPER:Modules.FT_HELPER.enabled&&HelperData.matches(connected,true,Modules.FT_HELPER.aliases.value)?Modules.FT_HELPER:null;
  if(!connected.equals(address)||next!=current){clear();address=connected;current=next;}
  if(current==null||++tick%10!=0)return;
  ServerHelper h=current;long started=PerformanceMonitor.begin();long now=System.nanoTime();Set<String> fresh=new HashSet<>();List<String> lines=new ArrayList<>(12);lines.add(h.name+" • "+HelperData.host(address));
  if(h.scoreboard.value){var board=mc.world.getScoreboard();var objective=board.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
   if(objective!=null){String title=HelperData.clean(objective.getDisplayName().getString());fresh.add(title);if(!sidebar.contains(title))receive(title,2);lines.add(title);
    int count=0,scanned=0;for(var row:board.getScoreboardEntries(objective)){if(scanned++>=15)break;if(row.hidden())continue;String text=HelperData.clean(net.minecraft.scoreboard.Team.decorateName(board.getScoreHolderTeam(row.owner()),row.name()).getString());fresh.add(text);if(!sidebar.contains(text))receive(text,2);if(count++<4)lines.add(text);}
   }
  }
  if(h.action.value&&now<actionUntil&&!h.session.action.isEmpty())lines.add(h.session.action);
  if(!h.session.event.isEmpty())lines.add("Event: "+h.session.event);
  if(h.ft&&!h.session.region.isEmpty())lines.add("Region (received): "+h.session.region);
  h.session.timers.values().removeIf(t->t.remaining(now)==0);for(var timer:h.session.timers.values())lines.add(timer.label()+": "+timer.remaining(now)+"s");
  if(h.health.value){float hp=mc.player.getHealth();if(hp<=h.hp.value&&hp>0){lines.add("LOW HP: "+Math.round(hp));if(!low&&h.notifications.value)Notifier.push(h.name+": low HP",0xff5555);low=true;}else if(hp>h.hp.value+2)low=false;}
  else low=false;
  for(int i=0;i<4;i++){var stack=mc.player.getEquippedStack(SLOTS[i]);boolean danger=h.armor.value&&!stack.isEmpty()&&Notifier.lowDurability(stack.getDamage(),stack.getMaxDamage(),h.durability.value);
   if(danger){lines.add("Armor: "+SLOTS[i].getName()+" low");if(!warned[i]&&h.notifications.value)Notifier.push(h.name+": "+SLOTS[i].getName()+" durability low",0xffbb55);warned[i]=true;}
   else if(stack.isEmpty()||!h.armor.value||Notifier.rearm(stack.getDamage(),stack.getMaxDamage(),h.durability.value))warned[i]=false;
  }
  int matches=0;
  if(!h.auctionTitle.value.isBlank()&&!h.auctionFilter.value.isBlank()&&mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen&&HelperData.clean(screen.getTitle().getString()).startsWith(h.auctionTitle.value)){
   String filter=h.auctionFilter.value.toLowerCase(java.util.Locale.ROOT);int scanned=0;
   for(var slot:screen.getScreenHandler().slots){if(scanned++>=90)break;if(slot.inventory==mc.player.getInventory())continue;var stack=slot.getStack();if(!stack.isEmpty()&&HelperData.clean(stack.getName().getString()).toLowerCase(java.util.Locale.ROOT).contains(filter))matches++;}
   lines.add("Auction visible matches: "+matches);
   if(matches>0&&auctionMatches==0&&h.auctionNotify.value&&h.notifications.value)Notifier.push(h.name+": matching auction items visible",0x8B8BFF);
  }
  auctionMatches=matches;
  sidebar=Set.copyOf(fresh);display=List.copyOf(lines);PerformanceMonitor.end(h.id+"_tick",started);
 }
}
