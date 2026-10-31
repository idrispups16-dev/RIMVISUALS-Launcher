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
  if(++ticks==40)client.setScreen(new ClickGui());
  if(ticks>100&&frames>5){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI rendering; no world test");client.scheduleStop();}
 }
}
