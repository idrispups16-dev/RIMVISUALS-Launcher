package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.HwIntegrations;
import static org.junit.jupiter.api.Assertions.*;
class HwIntegrationTest {
 @Test void eachToggleNeedsEnabledAuthorizedHolyWorldHelper(){
  assertTrue(HwIntegrations.enabled(true,true,true,true));
  assertFalse(HwIntegrations.enabled(false,true,true,true));
  assertFalse(HwIntegrations.enabled(true,false,true,true));
  assertFalse(HwIntegrations.enabled(true,true,false,true));
  assertFalse(HwIntegrations.enabled(true,true,true,false));
 }
}
