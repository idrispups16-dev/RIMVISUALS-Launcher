package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;


/** Spawns namespaced cosmetic particles, always through the shared performance budget. */
public final class Fx {
    private Fx() {}
    private static final EmissionBudget shared=new EmissionBudget();
    private static final EmissionBudget ambient=new EmissionBudget(),damage=new EmissionBudget();
    private static long requested,admitted,created;
    public static void beginTick(){shared.reset();ambient.reset();damage.reset();}
    public static int remaining(){return shared.remaining(Modules.PERFORMANCE.effectLimit());}
    public static String report(){return "particles requested="+requested+" admitted="+admitted+" created="+created+" tick-cap="+Modules.PERFORMANCE.effectLimit();}
    public static int ambientBudget(int n){int amount=ambient.take(n,Modules.PERFORMANCE.effectLimit()/6);return budget(amount);}
    public static void burstAmbient(double x,double y,double z,int rgb,int count,float size,double speed,double alpha){int n=damage.take(count,Modules.PERFORMANCE.effectLimit()/6);burst(x,y,z,rgb,n,size,speed,alpha);}
    public static void refund(int count){shared.refund(count);ambient.refund(count);admitted-=Math.max(0,count);}
    private static boolean near(MinecraftClient mc,double x,double y,double z){return mc.player!=null&&mc.player.squaredDistanceTo(x,y,z)<=48*48;}

    public static int budget(int requested) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Fx.requested+=Math.max(0,requested);int n=shared.take(Modules.PERFORMANCE.effectParticles(requested, mc.getCurrentFps()), Modules.PERFORMANCE.effectLimit());admitted+=n;return n;
    }

    public static void burst(double x,double y,double z,int rgb,int requested,float size,double speed){burst(x,y,z,rgb,requested,size,speed,1);}
    public static void opacity(net.minecraft.client.particle.Particle particle,float alpha){if(particle!=null)created++;if(particle instanceof ManagedParticleAlpha p)p.rimOpacity(alpha);}
    public static int burst(double x, double y, double z, int rgb, int requested, float size, double speed,double alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !near(mc,x,y,z)) return 0;
        int n = budget(requested);
        if (n <= 0) return 0;
        long before=created;
        var rnd = mc.world.random;
        for (int i = 0; i < n; i++) {
            opacity(EffectParticles.spawn(mc,rgb,size,x,y,z,(rnd.nextDouble()-.5)*speed*2,(rnd.nextDouble()-.2)*speed*2,(rnd.nextDouble()-.5)*speed*2),(float)alpha);
        }
        return (int)(created-before);
    }

    /** Flat expanding ring of dust particles around (x,y,z), through the shared budget. */
    public static void ring(double x,double y,double z,int rgb,int requested,float size,double speed){ring(x,y,z,rgb,requested,size,speed,1);}
    public static void ring(double x, double y, double z, int rgb, int requested, float size, double speed,double alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !near(mc,x,y,z)) return;
        int n = budget(requested);
        if (n <= 0) return;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            opacity(EffectParticles.spawn(mc,rgb,size,x,y,z,Math.cos(a)*speed,.02,Math.sin(a)*speed),(float)alpha);
        }
    }
}
