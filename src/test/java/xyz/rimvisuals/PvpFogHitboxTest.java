package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.*;
import xyz.rimvisuals.modules.VisualHitbox;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class PvpFogHitboxTest {
 @Test void fogIntervalsStayOrderedAndLegacyLoads(){assertEquals(39,FogMath.start(80,40));assertEquals(0,FogMath.start(-3,40));assertEquals(512,FogMath.end(900));var saved=Modules.snapshot();try{var p=new Properties();p.setProperty("custom_fog.fog_distance_(%)","50");Modules.apply(p);assertEquals(1,Modules.CUSTOM_FOG.mode.index);assertEquals(50,Modules.CUSTOM_FOG.distance.value);}finally{Modules.apply(saved);}}
 @Test void visualHitboxIsBoundedAndConfigured(){assertEquals(2,VisualHitbox.cap(true,true));assertEquals(4,VisualHitbox.cap(false,false));assertEquals(8,VisualHitbox.cap(false,true));var saved=Modules.snapshot();try{Modules.VISUAL_HITBOX.enabled=true;Modules.VISUAL_HITBOX.thickness.set(.025);var copy=Modules.snapshot();Modules.apply(saved);Modules.apply(copy);assertTrue(Modules.VISUAL_HITBOX.enabled);assertEquals(.025,Modules.VISUAL_HITBOX.thickness.value,1e-8);}finally{Modules.apply(saved);}}
}
