package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;

/** Spawns vanilla dust particles client-side, always through the shared performance budget. */
public final class Fx {
    private Fx() {}
    private static final EmissionBudget shared=new EmissionBudget();
    public static void beginTick(){shared.reset();}
    public static void refund(int count){shared.refund(count);}
    private static boolean near(MinecraftClient mc,double x,double y,double z){return mc.player!=null&&mc.player.squaredDistanceTo(x,y,z)<=48*48;}

    public static int budget(int requested) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return shared.take(Modules.PERFORMANCE.effectParticles(requested, mc.getCurrentFps()), Modules.PERFORMANCE.effectLimit());
    }

    public static void burst(double x,double y,double z,int rgb,int requested,float size,double speed){burst(x,y,z,rgb,requested,size,speed,1);}
    public static void opacity(net.minecraft.client.particle.Particle particle,float alpha){if(particle instanceof net.minecraft.client.particle.BillboardParticle)((xyz.rimvisuals.mixin.ParticleOpacityAccessor)(Object)particle).rimAlpha(Math.max(0,Math.min(1,alpha)));}
    public static void burst(double x, double y, double z, int rgb, int requested, float size, double speed,double alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !near(mc,x,y,z)) return;
        int n = budget(requested);
        if (n <= 0) return;
        var pm = mc.particleManager;
        var rnd = mc.world.random;
        var effect = new DustParticleEffect(rgb & 0xFFFFFF, Math.max(.2f, Math.min(3f, size)));
        for (int i = 0; i < n; i++) {
            opacity(pm.addParticle(effect,x,y,z,(rnd.nextDouble()-.5)*speed*2,(rnd.nextDouble()-.2)*speed*2,(rnd.nextDouble()-.5)*speed*2),(float)alpha);
        }
    }

    /** Flat expanding ring of dust particles around (x,y,z), through the shared budget. */
    public static void ring(double x,double y,double z,int rgb,int requested,float size,double speed){ring(x,y,z,rgb,requested,size,speed,1);}
    public static void ring(double x, double y, double z, int rgb, int requested, float size, double speed,double alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !near(mc,x,y,z)) return;
        int n = budget(requested);
        if (n <= 0) return;
        var effect = new DustParticleEffect(rgb & 0xFFFFFF, Math.max(.2f, Math.min(3f, size)));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            opacity(mc.particleManager.addParticle(effect,x,y,z,Math.cos(a)*speed,.02,Math.sin(a)*speed),(float)alpha);
        }
    }
}
