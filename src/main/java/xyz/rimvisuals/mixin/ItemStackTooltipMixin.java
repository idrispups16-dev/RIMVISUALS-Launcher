package xyz.rimvisuals.mixin;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
import xyz.rimvisuals.core.Modules;
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {
 @Inject(method="getTooltipData",at=@At("RETURN"),cancellable=true)
 private void rimShulker(CallbackInfoReturnable<Optional<TooltipData>> cir){var preview=Modules.SHULKER_PREVIEW.data((ItemStack)(Object)this);if(preview.isPresent())cir.setReturnValue(preview);}
}
