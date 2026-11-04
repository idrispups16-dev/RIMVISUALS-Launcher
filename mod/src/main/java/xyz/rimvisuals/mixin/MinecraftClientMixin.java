package xyz.rimvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.rimvisuals.core.Modules;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method="doAttack()Z",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
    private void rimAttackSwing(CallbackInfoReturnable<Boolean> cir){
        var mc=(MinecraftClient)(Object)this;
        if(mc.player!=null&&!mc.player.isSpectator()&&mc.crosshairTarget!=null&&mc.crosshairTarget.getType()!=net.minecraft.util.hit.HitResult.Type.BLOCK)
            Modules.SWING.attack(true,System.nanoTime());
    }
    @Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    private void rimOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        MinecraftClient mc = (MinecraftClient) (Object) this;
        long t=xyz.rimvisuals.core.PerformanceMonitor.begin();
        if ((Modules.ENTITY_OUTLINE.isActive() && Modules.ENTITY_OUTLINE.selects(entity, mc.player)) || Modules.TARGET_HIGHLIGHT.colorFor(entity, mc) != -1)
            cir.setReturnValue(true);
        xyz.rimvisuals.core.PerformanceMonitor.end("Entity outlines",t);
    }
}
