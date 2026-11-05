package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.ParticleMath;

import static org.junit.jupiter.api.Assertions.*;

class ParticleMathTest {
    @Test void damageCountScalesWithDamageAndIsCapped() {
        assertEquals(0, ParticleMath.damageCount(0, 3, 20));
        assertEquals(1, ParticleMath.damageCount(.1f, 3, 20));
        assertEquals(3, ParticleMath.damageCount(2, 3, 20));
        assertEquals(9, ParticleMath.damageCount(6, 3, 20));
        assertEquals(20, ParticleMath.damageCount(100, 3, 20));
    }
    @Test void budgetRespectsLimitSafeCapAndAdaptiveScaling() {
        assertEquals(10, ParticleMath.budget(10, 16, false, 6, false, 144, 60));
        assertEquals(16, ParticleMath.budget(40, 16, false, 6, false, 144, 60));
        assertEquals(6, ParticleMath.budget(40, 16, true, 6, false, 144, 60));
        assertEquals(8, ParticleMath.budget(16, 16, false, 6, true, 30, 60));
        assertEquals(0, ParticleMath.budget(40, 0, false, 6, true, 30, 60));
        assertTrue(ParticleMath.budget(16, 16, false, 6, true, 1, 60) <= 16);
    }
}
