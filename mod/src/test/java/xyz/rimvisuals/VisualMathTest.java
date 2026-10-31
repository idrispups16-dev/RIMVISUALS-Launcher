package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.VisualMath;
import static org.junit.jupiter.api.Assertions.*;
class VisualMathTest {
 @Test void attackBoundariesReturnToRest(){for(double speed:new double[]{.2,1,3})for(int easing=0;easing<3;easing++){assertEquals(0,VisualMath.envelope(0,speed,easing,1));assertEquals(0,VisualMath.envelope(1,speed,easing,1));}}
 @Test void invalidInputsDoNotReachMatrices(){assertEquals(0,VisualMath.envelope(Double.NaN,1,0,1));assertEquals(0,VisualMath.envelope(.5,Double.POSITIVE_INFINITY,0,1));}
 @Test void boundedTransforms(){for(int p=0;p<=100;p++)for(int e=0;e<3;e++){double v=VisualMath.envelope(p/100.,1,e,2);assertTrue(v>=0&&v<=2);}}
 @Test void smoothingIsFrameRateIndependent(){double a=VisualMath.smoothing(.05),b=VisualMath.smoothing(.025);assertEquals(a,1-(1-b)*(1-b),1e-12);}
}
