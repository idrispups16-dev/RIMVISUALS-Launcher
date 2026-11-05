package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.*;
import xyz.rimvisuals.core.Modules;
import static org.junit.jupiter.api.Assertions.*;
class ExpansionVisualTest {
 @Test void aspectPreservesMatchingRatioAndBounds(){assertEquals(1,AspectRatio.xScale(16d/9,16d/9),1e-6);assertEquals(4,AspectRatio.xScale(2,.1),1e-6);assertEquals(.5,AspectRatio.xScale(2,9),1e-6);}
 @Test void geometryBudgetAndAllNewSettingsRoundTrip(){assertEquals(16,WorldVisual.segments(false));assertEquals(32,WorldVisual.segments(true));var saved=Modules.snapshot();try{Modules.CHINA_HAT.enabled=true;Modules.CROSSHAIR.gap.set(7);Modules.ATMOSPHERE.preset.index=1;var expected=Modules.snapshot();Modules.apply(saved);Modules.apply(expected);assertTrue(Modules.CHINA_HAT.enabled);assertEquals(7,Modules.CROSSHAIR.gap.value);assertEquals(0x334b88,Modules.ATMOSPHERE.rgb());}finally{Modules.apply(saved);}}
}
