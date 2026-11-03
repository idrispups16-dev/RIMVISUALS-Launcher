package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.*;
public final class ItemSwap extends xyz.rimvisuals.core.Module {
 public final Setting.Mode item=add(new Setting.Mode("Move to offhand",0,"Totem","Shield","Pearl","Crystal"));
 public final Setting.Bool crouch=add(new Setting.Bool("Require crouch",true));
 private int lastTick=Integer.MIN_VALUE/2;
 public ItemSwap(){super("item_swap","Item Swap","Explicit hotkey: swap a selected inventory item into offhand via vanilla clickSlot. Assign Item Swap action in Keybinds",Category.UTILITIES);toggleable=false;}
 public void run(){var mc=MinecraftClient.getInstance();if(mc.player==null||mc.interactionManager==null)return;
  if(mc.currentScreen!=null||!mc.player.isAlive()||mc.player.isSpectator()||mc.player.currentScreenHandler!=mc.player.playerScreenHandler)return;
  if(crouch.value&&!mc.player.isSneaking()){mc.player.sendMessage(Text.literal("RIMVISUALS: hold crouch for Item Swap"),true);return;}
  if(mc.player.age-lastTick<10&&mc.player.age>=lastTick)return;lastTick=mc.player.age;
  var desired=switch(item.index){case 1->Items.SHIELD;case 2->Items.ENDER_PEARL;case 3->Items.END_CRYSTAL;default->Items.TOTEM_OF_UNDYING;};
  if(mc.player.getOffHandStack().isOf(desired))return;
  for(var slot:mc.player.playerScreenHandler.slots)if(slot.inventory==mc.player.getInventory()&&slot.getIndex()>=0&&slot.getIndex()<36&&slot.getStack().isOf(desired)){
   mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,slot.id,40,SlotActionType.SWAP,mc.player);return;
  }
  mc.player.sendMessage(Text.literal("RIMVISUALS: selected item is not in your inventory"),true);
 }
}
