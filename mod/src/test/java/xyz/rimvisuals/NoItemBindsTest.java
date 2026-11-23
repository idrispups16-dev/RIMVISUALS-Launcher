package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.Modules;
import static org.junit.jupiter.api.Assertions.*;
class NoItemBindsTest {
 @Test void itemUseImplementationsAreAbsentFromTheMod(){
  assertNull(getClass().getResourceAsStream("/xyz/rimvisuals/core/HelperItemBindings.class"));
  assertNull(getClass().getResourceAsStream("/xyz/rimvisuals/modules/ItemSwap.class"));
 }
 @Test void itemSwapIsNotAnAvailableModule(){assertTrue(Modules.ALL.stream().noneMatch(m->m.id.equals("item_swap")));}
}
