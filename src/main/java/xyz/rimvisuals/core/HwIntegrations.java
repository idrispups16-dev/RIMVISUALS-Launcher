package xyz.rimvisuals.core;
/** Optional installed mods, controlled by HW Helper without bundling third-party binaries. */
public final class HwIntegrations {
 private HwIntegrations(){}
 public static boolean enabled(boolean helperEnabled,boolean access,boolean holyWorld,boolean option){return helperEnabled&&access&&holyWorld&&option;}
 public static boolean auction(){return active(Modules.HW_HELPER.auctionPrice.value);}
 public static boolean health(){return active(Modules.HW_HELPER.zakoHealth.value);}
 private static boolean active(boolean option){var h=Modules.HW_HELPER;return enabled(h.isActive(),xyz.rimvisuals.auth.AccessGate.allowed(),ServerHelpers.active()==h,option);}
 public static String installed(String id){return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)?"installed":"JAR missing";}
}
