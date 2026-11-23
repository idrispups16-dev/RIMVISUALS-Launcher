package xyz.rimvisuals.core;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
public final class SocialClient {
 private static JsonObject state=new JsonObject();private static long next,lastPing,lastState;private static boolean busy;
 private static String context="",status="Войдите в аккаунт RIMVISUALS";
 private static final Gson JSON=new Gson();
 private SocialClient(){}
 public static String status(){return status;}
 public static String code(){return text(state,"code");}
 public static List<JsonObject> rows(String key){if(System.nanoTime()-lastState>35_000_000_000L)return List.of();var out=new ArrayList<JsonObject>();if(state.has(key))for(var e:state.getAsJsonArray(key))out.add(e.getAsJsonObject());return List.copyOf(out);}
 public static String text(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
 public static String server(MinecraftClient mc){
  String raw=LocalData.world(mc);if(raw.isEmpty()||mc.getCurrentServerEntry()==null)return "";
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((raw+"|"+Modules.FRIENDS.room.value.trim().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}
 }
 private static JsonObject payload(MinecraftClient mc){var p=new JsonObject();p.addProperty("server",server(mc));p.addProperty("dimension",LocalData.dimension(mc));
  p.addProperty("nickname",mc.player==null?mc.getSession().getUsername():mc.player.getName().getString());p.addProperty("player_uuid",mc.player==null?"":mc.player.getUuidAsString());p.addProperty("online",Modules.FRIENDS.presence.value&&mc.player!=null&&!server(mc).isEmpty());return p;}
 public static void tick(MinecraftClient mc){
  String c=server(mc)+"/"+LocalData.dimension(mc);
  if(!c.equals(context)){context=c;state=new JsonObject();next=0;}
  if(!xyz.rimvisuals.auth.AccessGate.allowed()){state=new JsonObject();return;}
  if(busy||System.nanoTime()<next)return;next=System.nanoTime()+4_000_000_000L;send("poll",payload(mc),false);
 }
 private static void send(String action,JsonObject p,boolean announce){
  if(busy){if(announce)ClientCommands.tell("Дождитесь ответа службы друзей");return;}
  var mc=MinecraftClient.getInstance();String captured=context;long revision=xyz.rimvisuals.auth.DeviceAuth.INSTANCE.socialRevision();busy=true;
  xyz.rimvisuals.auth.DeviceAuth.INSTANCE.social(action,p,result->mc.execute(()->{
   busy=false;if(!captured.equals(context)||revision!=xyz.rimvisuals.auth.DeviceAuth.INSTANCE.socialRevision()||!xyz.rimvisuals.auth.AccessGate.allowed())return;
   if(text(result,"status").equals("ok")){state=result;lastState=System.nanoTime();if(!rows("markers").isEmpty()&&Modules.FRIENDS.pings.value)Modules.LOCAL_MARKS.enabled=true;status="Ваш код: "+code()+" | "+text(result,"note");}
   else {state=new JsonObject();status=switch(text(result,"status")){case "unauthorized"->"Сначала войдите в аккаунт RIMVISUALS";case "invalid_friend_code"->"Код друга не найден";case "rate_limited"->"Слишком часто. Повторите позже";case "no_incoming_request"->"Нет входящего запроса от этого аккаунта";case "friend_limit"->"Достигнут лимит друзей и запросов";default->"Служба друзей временно недоступна";};}
   if(announce)ClientCommands.tell(status);
  }));
 }
 public static void action(String action,String code){var p=payload(MinecraftClient.getInstance());p.addProperty("code",code.trim().toUpperCase(Locale.ROOT));send(action,p,true);}
 public static void ping(){var mc=MinecraftClient.getInstance();if(mc.player==null||mc.world==null)return;
  if(mc.getCurrentServerEntry()==null){ClientCommands.tell("Общие метки доступны на сервере");return;}
  if(System.nanoTime()-lastPing<1_000_000_000L)return;lastPing=System.nanoTime();
  var hit=mc.player.raycast(128,1,false);if(hit.getType()==HitResult.Type.MISS){ClientCommands.tell("Наведитесь на блок в пределах 128 блоков");return;}
  var p=payload(mc);var pos=hit.getPos();p.addProperty("x",pos.x);p.addProperty("y",pos.y);p.addProperty("z",pos.z);p.addProperty("name","Метка");
  Modules.LOCAL_MARKS.enabled=true;Modules.save();send("ping",p,true);
 }
 public static List<LocalDataStore.Mark> markers(){
  var mc=MinecraftClient.getInstance();var result=new ArrayList<LocalDataStore.Mark>();if(!Modules.FRIENDS.pings.value)return result;
  for(var p:rows("markers"))try{if(java.time.Instant.parse(text(p,"expires_at")).isBefore(java.time.Instant.now()))continue;result.add(new LocalDataStore.Mark("shared:"+text(p,"id"),text(p,"nickname")+": "+text(p,"name"),LocalData.world(mc),LocalData.dimension(mc),p.get("x").getAsDouble(),p.get("y").getAsDouble(),p.get("z").getAsDouble(),0xD1D1D1));}catch(RuntimeException ignored){}
  return result;
 }
 public static boolean rimFriend(Entity e){if(!Modules.FRIENDS.badges.value)return false;for(var f:rows("friends"))if(f.has("here")&&f.get("here").getAsBoolean()&&text(f,"player_uuid").equals(e.getUuidAsString()))return true;return false;}
}
