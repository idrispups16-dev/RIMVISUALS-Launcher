package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.auth.AccessGate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class RestoredVisualsTest {
 @Test void completeCatalogSurvivesAccountIntegration(){
  assertTrue(Modules.ALL.size()>=80,"Truncated visual catalog");
  assertTrue(Modules.ALL.containsAll(List.of(Modules.CRYSTAL_FADE,Modules.TOTEM_FADE,Modules.CUSTOM_SKY,Modules.CUSTOM_FOG,Modules.CHINA_HAT,Modules.VISUAL_HITBOX,Modules.PEARL_TRAILS,Modules.SWORD_TRAILS,Modules.HIT_PARTICLES,Modules.DAMAGE_PARTICLES,Modules.HAND_GLOW,Modules.HW_HELPER,Modules.FT_HELPER)));
  assertEquals(Modules.ALL.size(),Modules.ALL.stream().map(m->m.id).distinct().count());
  assertTrue(Modules.HUD.size()>20,"HUD registration lost");
 }
 @Test void accountExpiryNeverErasesSavedSwitches(){
  var m=new xyz.rimvisuals.core.Module("fixture","Fixture","",xyz.rimvisuals.core.Module.Category.VISUALS);
  m.enabled=true;AccessGate.configure(()->false);assertFalse(m.isActive());assertTrue(m.enabled);
  AccessGate.configure(()->true);assertTrue(m.isActive());m.enabled=false;assertFalse(m.isActive());
 }
}
