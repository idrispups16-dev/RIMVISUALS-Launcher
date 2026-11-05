package xyz.rimvisuals.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.rimvisuals.core.HwIntegrations;
/** In this mod Mode.active()==true means suppress tooltip conversion. */
@Pseudo
@Mixin(targets="vupsenn.auctionprice.v12111fabric.Mode",remap=false)
public class AuctionPriceControlMixin {
 @Inject(method="active()Z",at=@At("HEAD"),cancellable=true,remap=false)
 private static void rimAuction(CallbackInfoReturnable<Boolean> ci){if(!HwIntegrations.auction())ci.setReturnValue(true);}
}
