package xyz.rimvisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.ColorMath;
import xyz.rimvisuals.core.Modules;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/state/SkyRenderState;)V", at = @At("RETURN"))
    private void rimSky(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        var sky = Modules.CUSTOM_SKY;
        if (!sky.enabled || sky.strength.value <= 0) return;
        String box = String.valueOf(state.skybox);
        if (box.equals("NONE") || box.equals("END")) return; // only the Overworld-style dome has a sky colour
        state.skyColor = ColorMath.mixArgb(state.skyColor | 0xFF000000, sky.color.rgb, (float) sky.strength.value);
    }
}
