package xyz.rimvisuals.core;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
public final class ClientCommands {
 private ClientCommands(){}
 public static void init(){ClientSendMessageEvents.ALLOW_CHAT.register(message->{
  if(!message.matches("(?i)^\\.(way|friend)(\\s.*)?$"))return true;
  if(!xyz.rimvisuals.auth.AccessGate.allowed()){tell("Сначала войдите в RIMVISUALS");return false;}
  try{if(message.toLowerCase(java.util.Locale.ROOT).startsWith(".way")){
   var w=WayCommand.parse(message);var mc=MinecraftClient.getInstance();if(mc.world==null)throw new IllegalArgumentException("Зайдите в мир");
   LocalData.store().addMark(w.name(),LocalData.world(mc),LocalData.dimension(mc),w.x(),w.y(),w.z());LocalData.refresh();Modules.LOCAL_MARKS.enabled=true;Modules.save();tell("Метка «"+w.name()+"» сохранена");
  }else{
   var p=message.trim().split("\\s+");if(p.length!=3)throw new IllegalArgumentException(".friend add|accept|remove CODE");
   String action=switch(p[1].toLowerCase(java.util.Locale.ROOT)){case "add"->"request";case "accept"->"accept";case "remove"->"remove";default->throw new IllegalArgumentException(".friend add|accept|remove CODE");};SocialClient.action(action,p[2]);
  }}catch(Exception e){tell(e.getMessage()==null?"Не удалось выполнить команду":e.getMessage());}return false;
 });}
 public static void tell(String message){var mc=MinecraftClient.getInstance();if(mc.player!=null)mc.player.sendMessage(Text.literal("RIMVISUALS: "+message),false);}
}
