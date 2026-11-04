package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.*;
import static org.junit.jupiter.api.Assertions.*;
class GraphiteThemeTest {
 @Test void graphiteIsAchromaticAndDoesNotRemoveLegacyThemes(){var saved=Modules.snapshot();try{
  Modules.UI.theme.index=4;
  for(int col:new int[]{Theme.accent(),Theme.accent2(),Theme.bg(),Theme.panel(),Theme.card(),Theme.text(),Theme.muted()}){
   assertEquals((col>>16)&255,(col>>8)&255);assertEquals((col>>8)&255,col&255);
  }
  Modules.UI.theme.index=0;assertNotEquals(Theme.accent()&255,(Theme.accent()>>8)&255);
 }finally{Modules.apply(saved);}}
}
