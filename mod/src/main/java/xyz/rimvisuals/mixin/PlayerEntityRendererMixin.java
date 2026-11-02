package xyz.rimvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.PerformanceMonitor;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Unique private boolean rimRight;
    @Unique private Identifier rimSkin;
    @Unique private boolean rimGlowDone;

    @Inject(method = "renderRightArm", at = @At("HEAD"))
    private void rimRightSide(MatrixStack m, OrderedRenderCommandQueue q, int light, Identifier skin, boolean sleeve, CallbackInfo ci) { rimRight = true; rimSkin = skin; rimGlowDone = false; }

    @Inject(method = "renderLeftArm", at = @At("HEAD"))
    private void rimLeftSide(MatrixStack m, OrderedRenderCommandQueue q, int light, Identifier skin, boolean sleeve, CallbackInfo ci) { rimRight = false; rimSkin = skin; rimGlowDone = false; }

    @Redirect(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Lnet/minecraft/client/model/ModelPart;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"))
    private void rimArm(OrderedRenderCommandQueue queue, ModelPart part, MatrixStack matrices, RenderLayer layer, int light, int overlay, Sprite sprite) {
        long t0 = PerformanceMonitor.begin();
        int tint = -1;
        if (Modules.HAND_COLOR.enabled) {
            boolean mainIsRight = MinecraftClient.getInstance().options.getMainArm().getValue() == Arm.RIGHT;
            tint = Modules.HAND_COLOR.tint(rimRight == mainIsRight, System.nanoTime());
        }
        PerformanceMonitor.end("Hand Color", t0);
        if (Modules.HAND_GLOW.enabled && !rimGlowDone && rimSkin != null) {
            rimGlowDone = true; // renderArm submits the sleeve too; glow is drawn once per arm
            long g0 = PerformanceMonitor.begin();
            boolean mainIsRight = MinecraftClient.getInstance().options.getMainArm().getValue() == Arm.RIGHT;
            var glow = Modules.HAND_GLOW;
            if (rimRight == mainIsRight ? glow.main.value : glow.off.value) {
                int n = glow.layerCount();
                int rgb = glow.rgb(System.nanoTime());
                RenderLayer shell = net.minecraft.client.render.RenderLayers.entityTranslucent(rimSkin);
                for (int i = n - 1; i >= 0; i--) { // outermost first so nearer shells blend over it
                    float s = xyz.rimvisuals.modules.HandGlow.shellScale(i, n, glow.size.value);
                    matrices.push();
                    matrices.scale(s, s, s);
                    queue.submitModelPart(part, matrices, shell, 15728880, overlay, sprite,
                            xyz.rimvisuals.core.ColorMath.argb(rgb, xyz.rimvisuals.modules.HandGlow.shellAlpha(i, n, glow.strength.value)), null);
                    matrices.pop();
                }
            }
            PerformanceMonitor.end("Hand Glow", g0);
        }
        if (tint == -1) queue.submitModelPart(part, matrices, layer, light, overlay, sprite);
        else queue.submitModelPart(part, matrices, layer, light, overlay, sprite, tint, null);
    }
}
