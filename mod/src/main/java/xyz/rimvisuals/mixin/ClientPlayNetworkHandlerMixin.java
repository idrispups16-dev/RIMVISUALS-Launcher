package xyz.rimvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method="onEntityAnimation(Lnet/minecraft/network/packet/s2c/play/EntityAnimationS2CPacket;)V",at=@At("HEAD"))
    private void rimAnimation(net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket packet,CallbackInfo ci){var mc=MinecraftClient.getInstance();if(mc.isOnThread()&&mc.world!=null)xyz.rimvisuals.core.CombatEvents.onAnimation(packet.getEntityId(),packet.getAnimationId());}
    @Inject(method="onEntityDamage(Lnet/minecraft/network/packet/s2c/play/EntityDamageS2CPacket;)V",at=@At("HEAD"))
    private void rimDamage(net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket packet,CallbackInfo ci){var mc=MinecraftClient.getInstance();if(mc.isOnThread()&&mc.world!=null)xyz.rimvisuals.core.CombatEvents.onDamage(packet.entityId());}
    /** Runs twice per packet (network thread, then game thread); only the game-thread pass acts. */
    @Inject(method = "onExplosion(Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;)V", at = @At("HEAD"))
    private void rimExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread() || mc.world == null) return;
        xyz.rimvisuals.core.CombatEvents.explosionObserved();
        Modules.ANCHOR_FADE.onExplosion(mc.world, packet.center(), packet.radius());
        Modules.EXPLOSION_EFFECTS.onExplosion(packet.center(), packet.radius());
    }
}
