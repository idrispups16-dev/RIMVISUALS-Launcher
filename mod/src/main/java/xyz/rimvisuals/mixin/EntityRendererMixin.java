package xyz.rimvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.rimvisuals.core.Modules;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /** Keeps vanilla/team colours for really glowing entities; recolours only outlines this mod added. */
    @Redirect(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int rimOutlineColor(Entity entity) {
        var o = Modules.ENTITY_OUTLINE;
        if (o.enabled && !entity.isGlowing() && o.selects(entity, MinecraftClient.getInstance().player)) return o.color.rgb;
        return entity.getTeamColorValue();
    }
}
