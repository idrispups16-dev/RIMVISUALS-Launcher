package xyz.rimvisuals.mixin;

import org.joml.Vector4f;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.rimvisuals.core.ColorMath;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.PerformanceMonitor;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    /** Arguments: 0 buffer, 1 pos, 2 colour, 3 envStart, 4 envEnd, 5 renderStart, 6 renderEnd, 7 skyEnd, 8 cloudEnd. */
    @ModifyArgs(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void rimFog(Args args,net.minecraft.client.render.Camera camera,int viewDistance,net.minecraft.client.render.RenderTickCounter counter,float partial,net.minecraft.client.world.ClientWorld world) {
        var fog = Modules.CUSTOM_FOG;
        if(!fog.enabled){if(Modules.ATMOSPHERE.enabled){Vector4f c=args.get(2);int rgb=Modules.ATMOSPHERE.rgb();float t=(float)Modules.ATMOSPHERE.strength.value;c.set(c.x+(((rgb>>16)&255)/255f-c.x)*t,c.y+(((rgb>>8)&255)/255f-c.y)*t,c.z+((rgb&255)/255f-c.z)*t,c.w);}return;}
        if(!fog.fluids.value&&camera.getSubmersionType()!=net.minecraft.block.enums.CameraSubmersionType.NONE)return;
        long t0 = PerformanceMonitor.begin();
        if (fog.recolor.value && fog.strength.value > 0) {
            // Mutate the same Vector4f the game returns as the clear colour so sky edge and fog agree.
            Vector4f c = args.get(2);
            float t = (float) fog.strength.value;
            int rgb = fog.color.rgb;
            c.set(c.x + (((rgb >> 16) & 255) / 255f - c.x) * t, c.y + (((rgb >> 8) & 255) / 255f - c.y) * t, c.z + ((rgb & 255) / 255f - c.z) * t, 1f);
        }
        if(fog.mode.index==0){float end=xyz.rimvisuals.core.FogMath.end(fog.far.value),start=xyz.rimvisuals.core.FogMath.start(fog.near.value,end);args.set(5,start);args.set(6,end);args.set(7,end);args.set(8,end);}
        else{float scale=(float)(fog.distance.value/100.0);if(scale!=1f)for(int i=3;i<=8;i++)args.set(i,ColorMath.scaleDistance(args.<Float>get(i),scale));}
        PerformanceMonitor.end("Custom Fog", t0);
    }
}
