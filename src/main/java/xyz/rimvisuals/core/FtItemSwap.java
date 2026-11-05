package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import xyz.rimvisuals.modules.ServerHelper;
import java.util.ArrayList;

/** One explicit key press, one vanilla SWAP click. No queued retries or health trigger. */
public final class FtItemSwap {
 private static final FtSwapPolicy.Gate gate=new FtSwapPolicy.Gate();
 private FtItemSwap() {}
 public static boolean available(MinecraftClient mc){
  return mc.player!=null&&mc.world!=null&&mc.interactionManager!=null&&mc.currentScreen==null
   &&mc.player.isAlive()&&!mc.player.isSpectator()&&Modules.FT_HELPER.isActive()
   &&Modules.FT_HELPER.itemSwap.value&&(mc.isInSingleplayer()||ServerHelpers.active()==Modules.FT_HELPER)
   &&mc.player.currentScreenHandler==mc.player.playerScreenHandler&&mc.player.playerScreenHandler.getCursorStack().isEmpty();
 }
 public static boolean matches(ItemStack stack,int kind,String filter){
  return !stack.isEmpty()&&FtSwapPolicy.matches(kind,stack.isOf(Items.PLAYER_HEAD),stack.isOf(Items.TOTEM_OF_UNDYING),stack.getName().getString(),filter);
 }
 public static void press(){
  var mc=MinecraftClient.getInstance();if(!available(mc))return;
  ServerHelper h=Modules.FT_HELPER;var player=mc.player;long now=System.nanoTime();
  if(!gate.ready(mc.world,player.age,now))return;
  var off=player.getOffHandStack();boolean first=matches(off,h.swapFrom.index,h.swapFromName.value),second=matches(off,h.swapTo.index,h.swapToName.value);
  if(first==second){message("Вторая рука: нужен один из двух выбранных предметов");return;}
  int target=first?h.swapTo.index:h.swapFrom.index;String filter=first?h.swapToName.value:h.swapFromName.value;
  var eligible=new ArrayList<Boolean>(36);for(int i=0;i<36;i++)eligible.add(matches(player.getInventory().getStack(i),target,filter));
  int inventorySlot=FtSwapPolicy.choose(first,second,eligible);if(inventorySlot<0){message("Нужный предмет не найден");return;}
  int slot=FtSwapPolicy.screenSlot(inventorySlot);var handler=player.playerScreenHandler;
  // Validate the real player handler mapping before sending the only action.
  if(slot<0||slot>=handler.slots.size()||handler.getSlot(slot).inventory!=player.getInventory()
    ||handler.getSlot(slot).getIndex()!=inventorySlot||!matches(handler.getSlot(slot).getStack(),target,filter))return;
  gate.record(mc.world,player.age,now);
  mc.interactionManager.clickSlot(handler.syncId,slot,40,SlotActionType.SWAP,player);
 }
 private static void message(String text){MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("FT Item Swap: "+text),true);}
}
