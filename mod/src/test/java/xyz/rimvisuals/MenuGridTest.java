package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.gui.MenuGrid;
import static org.junit.jupiter.api.Assertions.*;
class MenuGridTest {
 @Test void adaptsWithoutOverflowOnSmallWindows(){for(int width=160;width<=1000;width++){int n=MenuGrid.columns(width,3);assertTrue(n>=1&&n<=3);assertTrue(MenuGrid.width(width,n)*n+(n-1)*10<=width);}}
 @Test void userCanChooseOneTwoOrThreeColumns(){assertEquals(1,MenuGrid.columns(800,1));assertEquals(2,MenuGrid.columns(800,2));assertEquals(3,MenuGrid.columns(800,3));assertEquals(1,MenuGrid.columns(200,3));}
 @Test void variableHeightCardsGoIntoShortestColumn(){assertEquals(2,MenuGrid.shortest(new int[]{190,250,120}));assertEquals(0,MenuGrid.shortest(new int[]{0,0,0}));}
}
