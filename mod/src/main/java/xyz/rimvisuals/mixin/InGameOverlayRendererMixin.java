package xyz.rimvisuals.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.rimvisuals.core.Modules;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Shadow private int floatingItemTimer;
    @Unique private float rimTick;

    @Inject(method = "renderFloatingItem(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V", at = @At("HEAD"))
    private void rimTickProgress(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        rimTick = tickProgress;
    }

    @ModifyArgs(method = "renderFloatingItem(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void rimTotemScale(Args args) {
        float progress = (40 - floatingItemTimer + rimTick) / 40.0F;
        float f = Modules.TOTEM_SCALE.factor(progress);
        args.set(0, (float) args.get(0) * f);
        args.set(1, (float) args.get(1) * f);
        args.set(2, (float) args.get(2) * f);
    }
}
