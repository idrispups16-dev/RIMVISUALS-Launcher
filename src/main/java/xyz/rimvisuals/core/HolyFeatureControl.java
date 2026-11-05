package xyz.rimvisuals.core;

import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Official in-game API; sends feature identifiers only, never credentials or account data. */
public final class HolyFeatureControl {
 private static HolyFeaturePolicy policy;
 private static long joined;
 private static int attempts;
 public static final CustomPayload.Id<Payload> ID=new CustomPayload.Id<>(Identifier.of("liteapi","feature-control"));
 public record Payload(String json) implements CustomPayload {
  public static final PacketCodec<RegistryByteBuf,Payload> CODEC=new PacketCodec<>(){
   @Override public Payload decode(RegistryByteBuf buf){int n=buf.readableBytes();if(n>32768)throw new io.netty.handler.codec.DecoderException("LiteAPI payload exceeds limit");return new Payload(buf.readCharSequence(n,StandardCharsets.UTF_8).toString());}
   @Override public void encode(RegistryByteBuf buf,Payload p){byte[] bytes=p.json.getBytes(StandardCharsets.UTF_8);if(bytes.length>32768)throw new io.netty.handler.codec.EncoderException("LiteAPI payload exceeds limit");buf.writeBytes(bytes);}
  };
  @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
 }
 private HolyFeatureControl(){}
 public static Set<String> catalogue(){
  var ids=new TreeSet<String>();
  for(var m:Modules.ALL){ids.add(m.id);for(var setting:m.settings)if(setting instanceof Setting.Bool)ids.add(m.id+"."+setting.key().replaceAll("[^a-z0-9_]","_"));}
  ids.addAll(List.of("keybinds","profiles","hud_editor","client_settings"));
  return Collections.unmodifiableSet(ids);
 }
 public static void init(){
  policy=new HolyFeaturePolicy(catalogue());
  PayloadTypeRegistry.playC2S().register(ID,Payload.CODEC);PayloadTypeRegistry.playS2C().register(ID,Payload.CODEC);
  ClientPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
   var connection=context.client().getNetworkHandler();
   context.client().execute(()->{if(connection!=context.client().getNetworkHandler())return;
    boolean accepted=policy.receive(payload.json());
    LoggerFactory.getLogger("rimvisuals").info("HOLYWORLD_API response accepted={} state={} blockedCount={}",accepted,policy.state(),policy.blockedCount());
   });
  });
  ClientPlayConnectionEvents.JOIN.register((handler,sender,mc)->{
   var entry=mc.getCurrentServerEntry();policy.connect(entry==null?"":entry.address,entry!=null&&ClientPlayNetworking.canSend(ID));joined=now();attempts=0;tick(mc);
  });
  ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{policy.disconnect();attempts=0;});
 }
 private static long now(){return System.nanoTime()/1_000_000;}
 public static void tick(MinecraftClient mc){
  if(policy==null)return;
  if(policy.state()==HolyFeaturePolicy.State.OUTSIDE){var entry=mc.getCurrentServerEntry();if(entry==null||!ClientPlayNetworking.canSend(ID))return;policy.connect(entry.address,true);joined=now();attempts=0;}
  long time=now();policy.timeout(time);
  if(policy.state()==HolyFeaturePolicy.State.READY)return;
  if(attempts<2&&(attempts==0||time-joined>=10_000)&&ClientPlayNetworking.canSend(ID)){
   UUID id=policy.request(time);if(id!=null){attempts++;ClientPlayNetworking.send(new Payload(policy.requestJson(id)));LoggerFactory.getLogger("rimvisuals").info("HOLYWORLD_API checkFeatures sent client=rimvisuals features={} attempt={}",policy.catalogue().size(),attempts);}
  }else if(attempts==0&&time-joined>=5_000&&policy.state()!=HolyFeaturePolicy.State.FAILED){policy.fail();LoggerFactory.getLogger("rimvisuals").warn("HOLYWORLD_API channel unavailable; functions remain unavailable on this connection");}
 }
 public static boolean allowed(String id){return policy==null||policy.allowed(id);}
 public static boolean screenAllowed(net.minecraft.client.gui.screen.Screen screen){
  if(screen instanceof xyz.rimvisuals.gui.ClickGui)return allowed("interface");
  if(screen instanceof xyz.rimvisuals.gui.HudEditorScreen)return allowed("hud_editor");
  if(screen instanceof xyz.rimvisuals.gui.KeybindManagerScreen)return allowed("keybinds");
  if(screen instanceof xyz.rimvisuals.gui.FriendsScreen)return allowed("friends_options");
  if(screen instanceof xyz.rimvisuals.gui.LocalDataScreen local)return local.policyAvailable();
  if(screen instanceof xyz.rimvisuals.gui.HelperSettingsScreen helper)return helper.policyAvailable();
  return true;
 }
 public static boolean restricted(){return policy!=null&&policy.state()!=HolyFeaturePolicy.State.OUTSIDE;}
 public static long revision(){return policy==null?0:policy.revision();}
 public static String status(){return policy==null?"":switch(policy.state()){
  case OUTSIDE->"";
  case PENDING->"HolyWorld API: checking server policy";
  case READY->"HolyWorld API: response received; server policy applied";
  case FAILED->"HolyWorld API: check failed; reconnect to retry";
 };}
 /** Parent-level suppression deliberately hides the whole group if one child is restricted. */
 public static boolean actionAllowed(String id){
  if(Modules.ALL.stream().anyMatch(m->m.id.equals(id)))return allowed("keybinds")&&allowed(id);
  if(id.startsWith("helper_")&&(!allowed("hw_helper")||!allowed("ft_helper")))return false;
  String feature=switch(id){case "menu"->"interface";case "monitor"->"performance_hud";case "hud"->"hud_editor";case "friend_ping"->"friends_options";case "ft_offhand_swap"->"ft_helper";default->id.startsWith("helper_")?"hw_helper":"keybinds";};
  return allowed("keybinds")&&allowed(feature);
 }
}
