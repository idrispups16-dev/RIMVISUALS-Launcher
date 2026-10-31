package xyz.rimvisuals;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.slf4j.LoggerFactory;
import xyz.rimvisuals.gui.ClickGui;
/** Opt-in development startup/GUI smoke test. Never active in normal installations. */
public final class SmokeTest {
 public static int frames;private static int ticks;private static boolean audited;
 public static void tick(MinecraftClient client){if(!Boolean.getBoolean("rimvisuals.smoke"))return;
  if(!audited){audited=true;MixinEnvironment.getCurrentEnvironment().audit();}
  ++ticks;
  if(ticks>40 && client.getOverlay()==null && !(client.currentScreen instanceof ClickGui)){client.setScreen(new ClickGui());LoggerFactory.getLogger("rimvisuals").info("SMOKE: opening GUI after loading overlay");}
  if(ticks>100&&frames>5){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI rendering; no world test");client.scheduleStop();}
 }
}
