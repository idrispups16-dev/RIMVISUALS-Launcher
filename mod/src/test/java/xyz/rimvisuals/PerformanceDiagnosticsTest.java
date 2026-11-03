package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.*;
import xyz.rimvisuals.modules.PerformanceOptions;
import static org.junit.jupiter.api.Assertions.*;
class PerformanceDiagnosticsTest {
 @Test void disabledDiagnosticsDoNotCollectOrReadTheClock(){
  PerformanceMonitor.setEnabled(false);PerformanceMonitor.reset();
  assertEquals(0,PerformanceMonitor.begin());PerformanceMonitor.end("disabled",1);
  assertTrue(PerformanceMonitor.labels().isEmpty());assertTrue(PerformanceMonitor.top(5).isEmpty());
 }
 @Test void enablingAndDisablingClearsCachedResults(){
  try {PerformanceMonitor.setEnabled(true);long t=PerformanceMonitor.begin();PerformanceMonitor.end("HUD",t);
   assertTrue(PerformanceMonitor.report().contains("HUD"));assertFalse(PerformanceMonitor.top(5).isEmpty());
   PerformanceMonitor.setEnabled(false);assertTrue(PerformanceMonitor.top(5).isEmpty());
  }finally{PerformanceMonitor.setEnabled(false);}
 }
 @Test void allEffectsShareOneTickCap(){
  var b=new EmissionBudget();assertEquals(10,b.take(10,24));assertEquals(14,b.take(20,24));assertEquals(0,b.take(20,24));
  b.refund(4);assertEquals(4,b.take(10,24));b.reset();assertEquals(24,b.take(40,24));
 }
 @Test void safeModeSuppressesExpensivePassesWithoutChangingModuleSwitches(){
  var p=new PerformanceOptions();assertEquals(0,p.glowLayers(4));assertFalse(p.outlineAllowed(0));assertEquals(6,p.effectParticles(100,200));p.safeParticles.index=1;assertEquals(0,p.effectParticles(100,200));
  p.preset.index=1;p.applyPreset();assertEquals(1,p.glowLayers(4));assertTrue(p.outlineAllowed(1024));assertFalse(p.outlineAllowed(1025));
  p.preset.index=2;p.applyPreset();assertEquals(4,p.glowLayers(4));assertTrue(p.outlineAllowed(4096));
 }
}
