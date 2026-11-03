package xyz.rimvisuals.mixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;
@Mixin(InGameHud.class)
public class InGameHudMixin {
 @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
 private void rimCrosshair(DrawContext c,RenderTickCounter tick,CallbackInfo ci){var mc=MinecraftClient.getInstance();if(!Modules.CROSSHAIR.isActive()||!mc.options.getPerspective().isFirstPerson()||mc.player==null||mc.player.isSpectator())return;Modules.CROSSHAIR.draw(c,c.getScaledWindowWidth(),c.getScaledWindowHeight());ci.cancel();}
}
