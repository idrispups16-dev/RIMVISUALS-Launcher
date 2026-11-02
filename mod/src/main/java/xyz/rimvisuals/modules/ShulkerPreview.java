package xyz.rimvisuals.modules;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.tooltip.TooltipData;
import xyz.rimvisuals.core.*;
import java.util.*;
public final class ShulkerPreview extends xyz.rimvisuals.core.Module {
 public record Data(List<ItemStack> stacks) implements TooltipData{}
 private ContainerComponent last;
 private Data cached;
 public ShulkerPreview(){super("shulker_preview","Shulker Preview","27-slot tooltip from container data already supplied by Minecraft. No server inventory queries",Category.UTILITIES);enabled=false;}
 public Optional<TooltipData> data(ItemStack stack){
  if(!enabled||!(stack.getItem() instanceof BlockItem b)||!(b.getBlock() instanceof ShulkerBoxBlock))return Optional.empty();
  var contents=stack.get(DataComponentTypes.CONTAINER);if(contents==null)return Optional.empty();
  if(contents!=last){last=contents;cached=new Data(contents.stream().limit(27).map(ItemStack::copy).toList());}
  return Optional.ofNullable(cached);
 }
}
