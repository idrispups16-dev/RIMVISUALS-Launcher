package xyz.rimvisuals.core;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import xyz.rimvisuals.modules.ServerHelper;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Game-thread coordinator. Network work and JSON parsing never run in rendering. */
public final class HelperRuntime {
 public record CdRow(String text,double progress,int rgb){}
 private record Attempt(String type,long at){}
 private static final Map<Identifier,Attempt> attempts=new HashMap<>();
 private static CompletableFuture<HolyEventsApi.Result> request;private static Set<String> apiSeen=Set.of();private static List<String> noticeLines=List.of();private static String notice="";private static long noticeUntil;private static long generation,nextPoll,lastRefresh;private static int ticks;
 private static String apiStatus="API not requested";private static List<String> eventRows=List.of("No received events"),regionRows=List.of("Region unknown");private static List<CdRow> cdRows=List.of();
 public static List<String> eventRows(){return eventRows;}public static List<CdRow> cooldownRows(){return cdRows;}public static List<String> regionRows(){return regionRows;}
 public static String status(){return apiStatus;}
 public static List<String> noticeLines(){return System.currentTimeMillis()<noticeUntil?noticeLines:List.of();}
 public static String notice(){return System.currentTimeMillis()<noticeUntil?notice:"";}
 public static void clear(){generation++;apiSeen=Set.of();noticeLines=List.of();notice="";noticeUntil=0;if(request!=null)request.cancel(false);request=null;nextPoll=lastRefresh=0;ticks=0;attempts.clear();eventRows=List.of("No received events");cdRows=List.of();regionRows=List.of("Region unknown");apiStatus="API not requested";Modules.HW_HELPER.tracker.clear();Modules.FT_HELPER.tracker.clear();}
 public static void init(){
  net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player,world,hand)->{if(world.isClient())attempt(player.getStackInHand(hand));return net.minecraft.util.ActionResult.PASS;});
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{if(world.isClient())attempt(player.getStackInHand(hand));return net.minecraft.util.ActionResult.PASS;});
 }
 public static void notify(ServerHelper h,String text){if(!h.notifications.value)return;notice=HelperData.clean(text);noticeLines=List.of(notice);noticeUntil=System.currentTimeMillis()+5000;Notifier.push(h.name+": "+HelperData.clean(text),0x8B8BFF);var mc=MinecraftClient.getInstance();if(h.sounds.value&&mc.player!=null)mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),(float)h.soundVolume.value,1.2f);}
 public static void received(String s){var h=ServerHelpers.active();if(h==null)return;long now=System.currentTimeMillis();
  if(h.events.value&&h.tracker.message(s,h.eventNames.value,HelperData.host(ServerHelpers.address()),now))notify(h,"Event update received");
  if(h.cooldowns.value){int trap=HelperData.seconds(s,h.trapPrefix.value),plast=HelperData.seconds(s,h.plastPrefix.value);if(trap>=0)h.tracker.cooldown("Trapka",trap,false,"server message",now);if(plast>=0)h.tracker.cooldown("Plast",plast,false,"server message",now);}
 }
 private static String type(ServerHelper h,ItemStack stack){String n=HelperData.clean(stack.getName().getString()).toLowerCase(Locale.ROOT);if(!h.trapName.value.isBlank()&&n.contains(h.trapName.value.toLowerCase(Locale.ROOT)))return "Trapka";if(!h.plastName.value.isBlank()&&n.contains(h.plastName.value.toLowerCase(Locale.ROOT)))return "Plast";return "";}
 public static void attempt(ItemStack stack){var h=ServerHelpers.active();var mc=MinecraftClient.getInstance();if(h==null||!h.cooldowns.value||stack.isEmpty()||mc.player==null)return;
  String type=type(h,stack);if(type.isEmpty())return;Identifier group=mc.player.getItemCooldownManager().getGroup(stack);long now=System.currentTimeMillis();var old=attempts.get(group);if(old!=null&&now-old.at<500)return;
  if(attempts.size()>=8)attempts.clear();attempts.put(group,new Attempt(type,now));
  // This is an observed use attempt, not a confirmation of server acceptance.
  double seconds=type.equals("Trapka")?h.trapSeconds.value:h.plastSeconds.value;
  var cd=h.tracker.cooldowns.get(type);if(h.estimates.value&&seconds>0&&(cd==null||cd.end()<=now))h.tracker.cooldown(type,seconds,true,"use attempt; manual duration",now);
 }
 public static void serverCooldown(Identifier group,int ticks){var h=ServerHelpers.active();var mc=MinecraftClient.getInstance();if(h==null||!h.cooldowns.value||mc.player==null)return;
  String type="";var a=attempts.get(group);long now=System.currentTimeMillis();if(a!=null&&now-a.at<10000)type=a.type;
  if(type.isEmpty())for(int i=0;i<mc.player.getInventory().size()&&i<64;i++){var stack=mc.player.getInventory().getStack(i);if(!stack.isEmpty()&&mc.player.getItemCooldownManager().getGroup(stack).equals(group)){type=type(h,stack);if(!type.isEmpty())break;}}
  if(!type.isEmpty()&&ticks>=0&&ticks<=1728000)h.tracker.cooldown(type,ticks/20.0,false,"server cooldown packet",now);
 }
 public static void refresh(){var h=ServerHelpers.active();if(h==null){apiStatus="Join matching server and enable helper";return;}long now=System.currentTimeMillis();if(now-lastRefresh<5000)return;lastRefresh=now;
  if(h.ft){var mc=MinecraftClient.getInstance();mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen("/event delay",false));apiStatus="Press Enter to request /event delay; no automatic command";}else if(!h.publicApi.value)apiStatus="Enable HolyWorld public API in Events settings";else poll(h,true);
 }
 private static void poll(ServerHelper h,boolean manual){if(h.ft||!h.events.value||!h.publicApi.value||request!=null)return;long now=System.currentTimeMillis();if(!manual&&now<nextPoll)return;
  nextPoll=now+(long)(h.interval.value*1000);long token=generation;apiStatus="API loading";request=HolyEventsApi.fetch(h.apiServer.value);
  request.thenAccept(result->MinecraftClient.getInstance().execute(()->{if(token!=generation||ServerHelpers.active()!=h)return;request=null;if(!h.events.value||!h.publicApi.value)return;apiStatus=result.status();if(!result.status().equals("API OK"))return;
   Set<String> previous=apiSeen;Set<String> incoming=new HashSet<>();for(var e:result.events())incoming.add(e.id());apiSeen=Set.copyOf(incoming);h.tracker.replaceApi(result.events().stream().filter(e->HelperTracker.interested(e.name(),h.interests.value)).limit(64).toList(),System.currentTimeMillis());int added=0;for(var e:result.events())if(!previous.contains(e.id())&&HelperTracker.interested(e.name(),h.interests.value))added++;
   if(added>0)notify(h,added+" new API event(s)");
  }));
 }
 public static void tick(){var h=ServerHelpers.active();if(h==null)return;if(++ticks%5!=0)return;long start=PerformanceMonitor.begin(),now=System.currentTimeMillis();
  poll(h,false);
  var mc=MinecraftClient.getInstance();
  if(h.events.value&&!h.eventMenu.value.isBlank()&&mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> menu&&HelperData.clean(menu.getTitle().getString()).toLowerCase(Locale.ROOT).contains(h.eventMenu.value.toLowerCase(Locale.ROOT))){
   int scanned=0;for(var slot:menu.getScreenHandler().slots){if(scanned++>=54)break;if(mc.player!=null&&slot.inventory==mc.player.getInventory())continue;var stack=slot.getStack();if(stack.isEmpty())continue;StringBuilder text=new StringBuilder(stack.getName().getString());var lore=stack.get(net.minecraft.component.DataComponentTypes.LORE);if(lore!=null)for(var line:lore.lines()){if(text.length()>192)break;text.append(' ').append(line.getString());}received(HelperData.clean(text.toString()));}
  }
  attempts.values().removeIf(a->now-a.at>10000);
  if(h.cooldowns.value&&h.readyNotify.value)for(String ready:h.tracker.ready(now))notify(h,ready);
  h.tracker.trim(now);List<String> events=new ArrayList<>();
  if(h.events.value){for(var e:h.tracker.events.values())if(HelperTracker.interested(e.name(),h.interests.value)&&(h.apiServer.value.isBlank()||h.ft||e.server().equals(h.apiServer.value))){
   events.add(e.name()+" | "+e.state());events.add(e.server()+" | "+e.countdown(now)+(now-e.seen()>60000?" [stale]":""));if(!e.coordinates().isEmpty())events.add("XYZ "+e.coordinates());if(events.size()>=18)break;
  }}
  if(events.isEmpty())events.add(h.ft?"No received events; request /event delay":apiStatus);eventRows=List.copyOf(events);
  List<CdRow> rows=new ArrayList<>();if(h.cooldowns.value)for(var c:h.tracker.cooldowns.values()){if(c.name().equals("Trapka")&&!h.trapVisible.value||c.name().equals("Plast")&&!h.plastVisible.value)continue;rows.add(new CdRow(c.name()+" "+(c.estimated()?"~ ":"")+(c.seconds(now)>0?c.seconds(now)+"s":"READY")+" ["+c.source()+"]",c.progress(now),c.name().equals("Trapka")?h.trapColor.rgb:h.plastColor.rgb));}
  cdRows=List.copyOf(rows);regionRows=List.of(h.session.region.isEmpty()?"Region unknown (no received data)":"Region: "+h.session.region);PerformanceMonitor.end(h.id+"_tracker",start);
 }
 private HelperRuntime(){}
}
