package xyz.rimvisuals;
import org.junit.jupiter.api.extension.*;
import xyz.rimvisuals.auth.AccessGate;
/** Existing visual behavior tests use an authorized fixture, never a live account. */
public final class AuthorizedVisualsFixture implements BeforeEachCallback,AfterEachCallback {
 public void beforeEach(ExtensionContext context){AccessGate.configure(()->true);}
 public void afterEach(ExtensionContext context){AccessGate.configure(()->false);}
}
