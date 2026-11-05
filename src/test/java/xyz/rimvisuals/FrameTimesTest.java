package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.FrameTimes;
import static org.junit.jupiter.api.Assertions.*;
class FrameTimesTest {
 @Test void recordsBoundedIntervalsAndResets(){try{FrameTimes.reset();FrameTimes.record(1);FrameTimes.record(2_000_001);FrameTimes.record(4_000_001);assertEquals(3,FrameTimes.percentile(.95));assertTrue(FrameTimes.report().contains("n=2"));FrameTimes.record(2_000_000_001L);assertTrue(FrameTimes.report().contains("n=2"));}finally{FrameTimes.reset();}assertEquals(0,FrameTimes.percentile(.95));}
 @Test void slowFramesUseAnHonestOverflowBound(){try{FrameTimes.reset();FrameTimes.record(1);FrameTimes.record(500_000_001);assertEquals(1000,FrameTimes.percentile(.95));assertTrue(FrameTimes.report().contains("500.00ms"));}finally{FrameTimes.reset();}}
}
