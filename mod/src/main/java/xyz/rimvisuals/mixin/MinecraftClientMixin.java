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
