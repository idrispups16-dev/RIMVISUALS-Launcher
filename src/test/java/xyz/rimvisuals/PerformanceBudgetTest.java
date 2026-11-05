package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.PerformanceOptions;
import static org.junit.jupiter.api.Assertions.*;
class PerformanceBudgetTest {
 @Test void safeModeSuppressesCustomParticles(){var p=new PerformanceOptions();assertEquals(0,p.particles(80,120));}
 @Test void budgetCapsAndAdapts(){var p=new PerformanceOptions();p.safe.value=false;p.particleLimit.set(24);assertEquals(24,p.particles(80,120));assertEquals(12,p.particles(80,30));assertEquals(0,p.particles(0,120));}
 @Test void missingFpsDoesNotExceedCap(){var p=new PerformanceOptions();p.safe.value=false;p.particleLimit.set(8);assertEquals(8,p.particles(80,0));}
 @Test void seededPresetPreservesManualSettings(){var p=new PerformanceOptions();p.particleLimit.set(11);p.seedPreset();p.applyPreset();assertEquals(11,p.particleLimit.value);p.preset.index=1;p.applyPreset();assertFalse(p.safe.value);assertEquals(24,p.particleLimit.value);}
}
