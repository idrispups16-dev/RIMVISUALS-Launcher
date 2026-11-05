package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.*;
import static org.junit.jupiter.api.Assertions.*;
class PlayerStatusTest {
 @Test void countsAndFuseAreBounded(){assertEquals(5,PlayerStatusHud.countTotems(4,1));assertEquals(0,PlayerStatusHud.countTotems(-1,-2));assertEquals(4,PlayerStatusHud.fuseSeconds(80));assertEquals(0,PlayerStatusHud.fuseSeconds(-1));}
 @Test void zoomIsVisualAndRestoresExactFov(){var z=new Zoom();z.smooth.value=false;assertEquals(90,z.apply(90,true));z.enabled=true;assertEquals(31.5,z.apply(90,true),1e-9);assertEquals(90,z.apply(90,false),1e-9);assertEquals(.1,Zoom.bounded(-1));assertEquals(1,Zoom.bounded(Double.NaN));}
}
