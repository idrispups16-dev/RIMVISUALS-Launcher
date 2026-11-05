package xyz.rimvisuals.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;
/** Observe local visual swings, including swords; never alter combat or packets. */
@Mixin(LivingEntity.class)
public class LivingEntitySwingMixin {
 @Inject(method="swingHand(Lnet/minecraft/util/Hand;Z)V",at=@At("HEAD"))
 private void rimSwing(Hand hand,boolean fromServer,CallbackInfo ci){
  var mc=MinecraftClient.getInstance();
  if((Object)this!=mc.player||mc.player==null||mc.player.isSpectator())return;
  if(mc.crosshairTarget!=null&&mc.crosshairTarget.getType()==HitResult.Type.BLOCK){Modules.SWING.clearAttacks();return;}
  Modules.SWING.attack(hand==Hand.MAIN_HAND,System.nanoTime());
 }
}
