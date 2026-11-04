package xyz.rimvisuals.core;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import xyz.rimvisuals.modules.ServerHelper;
/** One explicit hotbar item use per key press, scoped to the enabled server helper. */
public final class HelperItemBindings {
 private static long next;
 private HelperItemBindings(){}
 private static String clean(String s){return s.replaceAll("§.","").toLowerCase(Locale.ROOT).trim();}
 public static int findSlot(List<String> names,String query){String q=clean(query);if(q.isEmpty())return -1;for(int i=0;i<names.size();i++)if(clean(names.get(i)).contains(q))return i;return -1;}
 public static void use(ServerHelper h,int action){
  var mc=MinecraftClient.getInstance();
  if(!h.isActive()||ServerHelpers.active()!=h){ClientCommands.tell("Включите нужный хелпер и зайдите на его сервер");return;}
  if(mc.player==null||mc.interactionManager==null||mc.currentScreen!=null||mc.player.isSpectator()||!mc.player.isAlive())return;
  long now=System.nanoTime();if(now<next)return;next=now+250_000_000L;
  String name=action==0?h.trapName.value:action==1?h.plastName.value:h.bindItem.value;
  var inventory=mc.player.getInventory();var names=new ArrayList<String>();
  for(int i=0;i<9;i++)names.add(inventory.getStack(i).isEmpty()?"":inventory.getStack(i).getName().getString());
  int slot=findSlot(names,name);if(slot<0){ClientCommands.tell("Предмет «"+name+"» не найден на панели быстрого доступа");return;}
  int old=inventory.getSelectedSlot();
  try {inventory.setSelectedSlot(slot);mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);}
  finally{inventory.setSelectedSlot(old);mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(old));}
 }
}
