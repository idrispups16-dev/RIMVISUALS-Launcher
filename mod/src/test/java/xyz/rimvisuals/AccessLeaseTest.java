package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.auth.AccessLease;
import static org.junit.jupiter.api.Assertions.*;
class AccessLeaseTest {
 @Test void startsLockedAndExpiresWithoutNetwork(){var l=new AccessLease();assertFalse(l.active(1));l.grant(1,75);assertTrue(l.active(74_000_000_001L));assertFalse(l.active(75_000_000_001L));}
 @Test void capsServerLeaseAndRevokesImmediately(){var l=new AccessLease();l.grant(1,99999);assertFalse(l.active(75_000_000_001L));l.grant(1,75);l.clear();assertFalse(l.active(2));}
 @Test void zeroLeaseCannotEnableFeatures(){var l=new AccessLease();l.grant(1,0);assertFalse(l.active(1));l.grant(1,-1);assertFalse(l.active(1));}
}
