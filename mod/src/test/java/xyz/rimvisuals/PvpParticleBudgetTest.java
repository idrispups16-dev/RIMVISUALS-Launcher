package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.ParticleMath;
import xyz.rimvisuals.modules.PerformanceOptions;
import static org.junit.jupiter.api.Assertions.*;
class PvpParticleBudgetTest {
 @Test void safeIsVisibleButBoundedAndExplicitlyDisableable(){var p=new PerformanceOptions();assertEquals(6,p.effectLimit());assertTrue(p.effectParticles(1,1)>=1);p.safeParticles.index=1;assertEquals(0,p.effectParticles(10,120));p.safeParticles.index=0;p.particleLimit.set(0);assertEquals(0,p.effectParticles(10,120));}
 @Test void oneAttackSharesBudgetAcrossEffects(){assertEquals(2,ParticleMath.fairShare(10,3,6));assertEquals(2,ParticleMath.fairShare(15,3,6));assertEquals(2,ParticleMath.fairShare(14,3,6));assertEquals(0,ParticleMath.fairShare(10,0,6));}
}
