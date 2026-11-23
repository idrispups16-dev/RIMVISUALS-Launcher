package xyz.rimvisuals.core;
import net.minecraft.client.MinecraftClient;
import java.io.IOException;
import java.util.List;
/** Initialized once. Never reads files from a render callback. */
public final class LocalData {
 private static LocalDataStore store;
 public static String error="";
 private static List<LocalDataStore.Mark> marks=List.of();
 public static void init(){try{store=new LocalDataStore(Modules.dir().resolve("local-data.json"));refresh();}catch(IOException e){error=e.getMessage();}}
 public static LocalDataStore store(){return store;}
 public static void refresh(){if(store!=null)marks=store.marks();}
 public static List<LocalDataStore.Mark> marks(){var all=new java.util.ArrayList<>(SocialClient.markers());all.addAll(marks);return java.util.List.copyOf(all);}
 public static String world(MinecraftClient mc){var server=mc.getCurrentServerEntry();if(server!=null)return server.address.toLowerCase(java.util.Locale.ROOT);return mc.getServer()!=null?"local-folder:"+mc.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).toAbsolutePath().normalize().getFileName().toString():"";}
 public static String dimension(MinecraftClient mc){return mc.world==null?"":mc.world.getRegistryKey().getValue().toString();}
}
