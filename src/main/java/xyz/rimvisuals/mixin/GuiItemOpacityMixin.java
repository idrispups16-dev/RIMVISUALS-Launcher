package xyz.rimvisuals.mixin;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.ItemOpacity;
@Mixin(GuiRenderer.class)
public class GuiItemOpacityMixin {
 @Unique private float rimAlpha=1;
 @Inject(method="prepareItem",at=@At("HEAD"))
 private void rimItem(ItemGuiElementRenderState state,float u,float v,int pixels,int side,CallbackInfo ci){rimAlpha=((ItemOpacity)(Object)state).rimOpacity();}
 @ModifyArg(method="prepareItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/ScreenRect;Lnet/minecraft/client/gui/ScreenRect;)V"),index=11)
 private int rimTint(int color){return xyz.rimvisuals.core.Gfx.multiplyAlpha(color,rimAlpha);}
}
