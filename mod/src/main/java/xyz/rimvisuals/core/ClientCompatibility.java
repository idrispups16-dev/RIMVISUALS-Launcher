package xyz.rimvisuals.core;
/** Presence notice only; it does not pretend to certify active shader packs. */
public final class ClientCompatibility {
 public static boolean iris,sodium;
 public static void init(){var loader=net.fabricmc.loader.api.FabricLoader.getInstance();iris=loader.isModLoaded("iris");sodium=loader.isModLoaded("sodium");}
}
