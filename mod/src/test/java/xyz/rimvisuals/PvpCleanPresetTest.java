package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.*;
import static org.junit.jupiter.api.Assertions.*;
class PvpCleanPresetTest {
 @Test void cleanPresetShowsBoundedCombatAndAvoidsHeavyPasses(){var old=Modules.snapshot();try{Modules.HAND_GLOW.enabled=true;Modules.PERFORMANCE.particleLimit.set(0);Modules.VISUAL_HITBOX.players.value=false;Modules.VISUAL_HITBOX.mobs.value=false;assertTrue(Presets.apply("PvP_Clean"));assertTrue(Modules.HIT_PARTICLES.enabled);assertTrue(Modules.CRIT_PARTICLES.enabled);assertTrue(Modules.VISUAL_HITBOX.enabled);assertTrue(Modules.VISUAL_HITBOX.players.value);assertTrue(Modules.VISUAL_HITBOX.mobs.value);assertTrue(Modules.PERFORMANCE.safe.value);assertEquals(6,Modules.PERFORMANCE.effectLimit());assertFalse(Modules.HAND_GLOW.enabled);assertFalse(Modules.DAMAGE_TINT.enabled);assertTrue(Modules.SWING.enabled);assertTrue(Modules.VIEWMODEL.enabled);assertEquals(.45,Modules.SWING.intensity.value,1e-9);}finally{Modules.apply(old);}}
}
