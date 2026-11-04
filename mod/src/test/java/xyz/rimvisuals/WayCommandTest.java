package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.WayCommand;
import xyz.rimvisuals.core.HelperItemBindings;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class WayCommandTest {
 @Test void parsesNameAndSignedCoordinates(){var w=WayCommand.parse(".way Дом -123.5 64 230");assertEquals("Дом",w.name());assertEquals(-123.5,w.x());assertEquals(64,w.y());}
 @Test void rejectsInvalidAndUnboundedCoordinates(){for(String s:List.of(".way x NaN 2 3",".way x 1 Infinity 2",".way x 30000001 2 3",".way x 1 2",".way x 1 2 3 extra"))assertThrows(IllegalArgumentException.class,()->WayCommand.parse(s));}
 @Test void matchesFormattedItemNamesWithoutGuessingEmptyRules(){assertEquals(1,HelperItemBindings.findSlot(List.of("Камень","§5ТРАПКА уровня 2","Пласт"),"трапка"));assertEquals(-1,HelperItemBindings.findSlot(List.of("Камень"),""));assertEquals(-1,HelperItemBindings.findSlot(List.of("Пласт"),"трапка"));}
}
