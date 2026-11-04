package xyz.rimvisuals.core;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/** Local effects use a namespaced sprite: PvP packs often hide vanilla generic dust. */
public final class EffectParticles {
    private static final SimpleParticleType SPARK = FabricParticleTypes.simple();
    private static SpriteProvider sprites;
    private EffectParticles() {}

    public static void init() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("rimvisuals", "spark"), SPARK);
        ParticleFactoryRegistry.getInstance().register(SPARK, Factory::new);
    }

    /** The caller owns the shared emission budget; this method never bypasses it. */
    public static Particle spawn(MinecraftClient mc, int rgb, float size,
                                 double x, double y, double z, double vx, double vy, double vz) {
        var p = mc.particleManager.addParticle(SPARK, x, y, z, vx, vy, vz);
        if (p instanceof Spark spark) {
            spark.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
            spark.scale(Math.max(.2f, Math.min(3f, size)));
        }
        return p;
    }

    public static void checkLoadedSprite() {
        if (sprites == null) throw new IllegalStateException("RIM effect sprite provider missing");
        var sprite = sprites.getSprite(0, 1);
        var id = sprite.getContents().getId();
        if (!id.getNamespace().equals("rimvisuals") || !id.getPath().endsWith("spark"))
            throw new IllegalStateException("RIM effect sprite missing or replaced: " + id);
        org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_EFFECT_SPRITE_OK: {}", id);
    }

    private static final class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider provider;
        private Factory(SpriteProvider provider) { this.provider = provider; sprites = provider; }
        @Override public Particle createParticle(SimpleParticleType type, ClientWorld world,
                double x, double y, double z, double vx, double vy, double vz, Random random) {
            return new Spark(world, x, y, z, vx, vy, vz, provider);
        }
    }

    private static final class Spark extends RedDustParticle {
        private Spark(ClientWorld world, double x, double y, double z,
                      double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z, vx, vy, vz, new DustParticleEffect(0xFFFFFF, 1), sprites);
        }
        @Override public BillboardParticle.RenderType getRenderType() {
            return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
        }
    }
}
