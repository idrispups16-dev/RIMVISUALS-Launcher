package xyz.rimvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.modules.Fullbright;
import xyz.rimvisuals.modules.NightVisionFade;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    private static final String PUT = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;";

    /** putFloat calls in update(): 0 ambient, 1 skyFactor, 2 flicker, 3 night vision, 4 darkness, 5 sky darkness, 6 gamma. */
    @ModifyArg(method = "update(F)V", at = @At(value = "INVOKE", target = PUT, ordinal = 3), index = 0)
    private float rimNightVision(float vanilla) {
        if (!Modules.NIGHT_VISION_FADE.enabled) return vanilla;
        var p = MinecraftClient.getInstance().player;
        if (p == null || !p.hasStatusEffect(StatusEffects.NIGHT_VISION)) return vanilla;
        var e = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        return e == null ? vanilla : NightVisionFade.strength(e.getDuration());
    }

    @ModifyArg(method = "update(F)V", at = @At(value = "INVOKE", target = PUT, ordinal = 6), index = 0)
    private float rimGamma(float vanilla) {
        return Modules.FULLBRIGHT.enabled ? Fullbright.effective(vanilla, Modules.FULLBRIGHT.gamma.value) : vanilla;
    }
}
