package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.core.AttackSwingClock;
import static org.junit.jupiter.api.Assertions.*;
class AttackSwingClockTest {
 @Test void attackPlaysWithoutAnyVanillaMiningProgress(){var c=new AttackSwingClock();assertEquals(-1,c.progress(1000));c.start(1000,300_000_000L);assertEquals(0,c.progress(1000));assertEquals(.5,c.progress(150_001_000L),.0001);assertEquals(-1,c.progress(300_001_000L));}
 @Test void rapidClicksDoNotKeepTheVisualAtZero(){var c=new AttackSwingClock();c.start(1000,300_000_000L);c.start(10_001_000L,300_000_000L);assertTrue(c.progress(100_001_000L)>.3);c.start(200_001_000L,300_000_000L);assertEquals(0,c.progress(200_001_000L));}
 @Test void resetDoesNotCarryAnAttackAcrossWorlds(){var c=new AttackSwingClock();c.start(1000,300_000_000L);c.clear();assertEquals(-1,c.progress(2000));}
}
