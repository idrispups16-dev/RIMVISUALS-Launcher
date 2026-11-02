package xyz.rimvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.gui.ClickGui;

public class RimVisualsClient implements ClientModInitializer {
    public static KeyBinding menuKey, monitorKey;

    @Override
    public void onInitializeClient() {
        Modules.load();
        LauncherSettings.load();
        xyz.rimvisuals.core.CombatEvents.init();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rimvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        monitorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rimvisuals.monitor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            var entry = client.getCurrentServerEntry();
            if (entry != null) Modules.applyServerProfile(entry.address).ifPresent(n -> org.slf4j.LoggerFactory.getLogger("rimvisuals").info("Loaded server configuration {}", n));
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Modules.autosaveTick();
            xyz.rimvisuals.core.WorldColors.tick(client);
            xyz.rimvisuals.core.Notifier.tick(client);
            while (monitorKey.wasPressed()) Modules.PERFORMANCE.monitor.value = !Modules.PERFORMANCE.monitor.value;
            SmokeTest.tick(client);
            xyz.rimvisuals.core.CombatEvents.tick(client);
            xyz.rimvisuals.core.RimOverlays.tick();
            xyz.rimvisuals.core.Trails.tick(client);
            Modules.PERFORMANCE.applyPreset();
            Modules.VIEWMODEL.presets();
            LauncherSettings.tick();
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
        });
        HudRenderCallback.EVENT.register(RimHud::render);
        // Touch-friendly entry point: a RIMVISUALS button on the title and pause screens.
        ScreenEvents.AFTER_INIT.register(RimVisualsClient::addMenuButton);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Modules.save());
    }

    private static void addMenuButton(MinecraftClient client, Screen screen, int w, int h) {
        if (!(screen instanceof TitleScreen) && !(screen instanceof GameMenuScreen)) return;
        boolean mob = RimPlatform.mobile();
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("RIMVISUALS"), b -> client.setScreen(new ClickGui()))
                .dimensions(6, 6, mob ? 110 : 84, mob ? 26 : 20).build());
    }
}
