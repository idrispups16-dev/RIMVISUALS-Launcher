package xyz.rimvisuals.auth;
import java.util.function.BooleanSupplier;
/** Session authorization never overwrites the user's saved module switches. */
public final class AccessGate {
 private static volatile BooleanSupplier access = () -> false;
 private AccessGate() {}
 public static boolean allowed(){return access.getAsBoolean();}
 public static void configure(BooleanSupplier supplier){access=java.util.Objects.requireNonNull(supplier);}
}
