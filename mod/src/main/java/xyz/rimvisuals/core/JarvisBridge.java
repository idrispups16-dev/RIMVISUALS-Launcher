package xyz.rimvisuals.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Wires the loopback {@link JarvisServer} to Minecraft: game-thread execution, FPS and screens. */
public final class JarvisBridge {
    private JarvisBridge() {}

    public static boolean running() { return JarvisServer.running(); }

    public static void start() {
        if (JarvisServer.running()) return;
        JarvisCommands.gameThread = new JarvisCommands.GameThread() {
            public <T> T call(Supplier<T> job) throws Exception {
                CompletableFuture<T> f = new CompletableFuture<>();
                MinecraftClient.getInstance().execute(() -> { try { f.complete(job.get()); } catch (Throwable t) { f.completeExceptionally(t); } });
                return f.get(3, TimeUnit.SECONDS);
            }
        };
        JarvisCommands.fps = () -> MinecraftClient.getInstance().getCurrentFps();
        JarvisCommands.openScreen = JarvisBridge::openScreen;
        JarvisServer.start(FabricLoader.getInstance().getConfigDir().resolve("rimvisuals"));
    }

    public static void stop() { JarvisServer.stop(); }

    private static boolean openScreen(String target) {
        var mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen)) return false;
        if (target.equals("hud_editor")) mc.setScreen(new xyz.rimvisuals.gui.HudEditorScreen(mc.currentScreen));
        else xyz.rimvisuals.RimVisualsClient.openMenu(mc);
        return true;
    }
}
