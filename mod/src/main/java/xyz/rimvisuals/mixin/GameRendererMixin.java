package xyz.rimvisuals.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.rimvisuals.core.Modules;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** The 14.0 is the vanilla roll in degrees at full damage tilt. */
    @ModifyConstant(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", constant = @Constant(doubleValue = 14.0))
    private double rimHurtTilt(double vanilla) { return vanilla * Modules.HURT_CAMERA.scale(); }
}
