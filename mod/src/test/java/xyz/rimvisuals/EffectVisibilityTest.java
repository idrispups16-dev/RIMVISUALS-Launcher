package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.HitColor;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class EffectVisibilityTest {
    @Test void hitColorStrengthUsesTheEntityShadersInverseAlpha() {
        var m = new HitColor();
        m.enabled = true;
        m.color.rgb = 0x2244FF;
        m.alpha.set(1);
        // entity.fsh mixes overlay.rgb into the model using (1 - overlay.a).
        assertEquals(0, m.argb() >>> 24, "maximum strength must not make the tint invisible");
        assertEquals(0x2244FF, m.argb() & 0xFFFFFF);
        m.alpha.set(.25);
        assertEquals(191, m.argb() >>> 24);
        m.enabled = false;
        assertEquals(HitColor.VANILLA_ARGB, m.argb());
    }

    @Test void modParticlesHaveTheirOwnVisibleSpriteInsteadOfPackControlledGenericDust() throws Exception {
        try (var definition = getClass().getResourceAsStream("/assets/rimvisuals/particles/spark.json");
             var pixels = getClass().getResourceAsStream("/assets/rimvisuals/textures/particle/spark.png")) {
            assertNotNull(definition, "custom effects must not use minecraft:generic_* from PvP packs");
            assertNotNull(pixels);
            var json = new String(definition.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(json.contains("rimvisuals:spark"));
            assertFalse(json.contains("minecraft:generic"));
            var image = ImageIO.read(pixels);
            int visible = 0;
            for (int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++)
                if ((image.getRGB(x,y) >>> 24) > 32) visible++;
            assertTrue(visible >= image.getWidth()*image.getHeight()/4, "not a one-pixel transparent replacement");
        }
    }
}
