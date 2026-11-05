package xyz.rimvisuals.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.RimOverlayRecolor;
import xyz.rimvisuals.core.RimOverlays;

@Mixin(OverlayTexture.class)
public class OverlayTextureMixin implements RimOverlayRecolor {
    @Shadow @Final private NativeImageBackedTexture texture;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rimInit(CallbackInfo ci) { RimOverlays.textureRecreated(this); }

    /** Rows 0-7 of the 16x16 overlay are the "hurt" rows (V=3 selects them). */
    @Override public void rimRecolor(int argb) {
        NativeImage image = texture.getImage();
        if (image == null) return;
        for (int row = 0; row < 8; row++) for (int col = 0; col < 16; col++) image.setColorArgb(col, row, argb);
        texture.upload();
    }
}
