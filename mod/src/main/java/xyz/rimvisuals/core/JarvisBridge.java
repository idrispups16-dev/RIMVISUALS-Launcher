package xyz.rimvisuals.core;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Local IPC for the RIMVISUALS launcher (RIM JARVIS). A TCP server bound to 127.0.0.1 only, on a random port, guarded by a
 * random per-session token written to config/rimvisuals/jarvis-ipc.json (readable only by the same Windows user's profile
 * folder). Newline-delimited JSON; the first line must be {"auth":"<token>"}. Only {@link JarvisCommands#ACTIONS} run.
 * The server is started only if the launcher enabled it in launcher.json ("jarvis": {"enabled": true}); it closes with the game.
 */
public final class JarvisBridge {
    private static final Logger LOG = LoggerFactory.getLogger("rimvisuals-jarvis");
    private static final int MAX_LINE = 16 * 1024;
    private static volatile ServerSocket server;
    private static volatile String token = "";
    private static Path ipcFile;
    private static BufferedWriter audit;

    private JarvisBridge() {}

    public static boolean running() { return server != null && !server.isClosed(); }

    public static synchronized void start() {
        if (running()) return;
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("rimvisuals");
            Files.createDirectories(dir);
            byte[] raw = new byte[32]; new SecureRandom().nextBytes(raw);
            token = HexFormat.of().formatHex(raw);
            server = new ServerSocket(0, 4, InetAddress.getLoopbackAddress());
            ipcFile = dir.resolve("jarvis-ipc.json");
            JsonObject o = new JsonObject(); o.addProperty("schemaVersion", 1); o.addProperty("port", server.getLocalPort());
            o.addProperty("token", token); o.addProperty("pid", ProcessHandle.current().pid());
            Files.writeString(ipcFile, o.toString());
            audit = Files.newBufferedWriter(dir.resolve("jarvis.log"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            JarvisCommands.gameThread = new JarvisCommands.GameThread() {
                public <T> T call(Supplier<T> job) throws Exception {
                    CompletableFuture<T> f = new CompletableFuture<>();
                    MinecraftClient.getInstance().execute(() -> { try { f.complete(job.get()); } catch (Throwable t) { f.completeExceptionally(t); } });
                    return f.get(3, TimeUnit.SECONDS);
                }
            };
            JarvisCommands.fps = () -> MinecraftClient.getInstance().getCurrentFps();
            JarvisCommands.openScreen = JarvisBridge::openScreen;
            Thread t = new Thread(JarvisBridge::accept, "rimvisuals-jarvis-ipc"); t.setDaemon(true); t.start();
            LOG.info("RIM JARVIS IPC listening on 127.0.0.1:{}", server.getLocalPort());
        } catch (IOException | RuntimeException e) { LOG.warn("RIM JARVIS IPC not started", e); stop(); }
    }

    public static synchronized void stop() {
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        server = null; token = "";
        try { if (ipcFile != null) Files.deleteIfExists(ipcFile); } catch (IOException ignored) {}
        try { if (audit != null) audit.close(); } catch (IOException ignored) {}
        audit = null;
    }

    private static boolean openScreen(String target) {
        var mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen)) return false;
        if (target.equals("hud_editor")) mc.setScreen(new xyz.rimvisuals.gui.HudEditorScreen(mc.currentScreen));
        else xyz.rimvisuals.RimVisualsClient.openMenu(mc);
        return true;
    }

    private static void accept() {
        ServerSocket s = server;
        while (s != null && !s.isClosed()) {
            try {
                Socket c = s.accept();
                if (!c.getInetAddress().isLoopbackAddress()) { c.close(); continue; }
                Thread t = new Thread(() -> serve(c), "rimvisuals-jarvis-conn"); t.setDaemon(true); t.start();
            } catch (IOException e) { if (!s.isClosed()) LOG.debug("accept", e); return; }
        }
    }

    static boolean tokenOk(String given) {
        byte[] a = given.getBytes(StandardCharsets.UTF_8), b = token.getBytes(StandardCharsets.UTF_8);
        return !token.isEmpty() && MessageDigest.isEqual(a, b);
    }

    private static void serve(Socket c) {
        try (c; var in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(new OutputStreamWriter(c.getOutputStream(), StandardCharsets.UTF_8), true)) {
            c.setSoTimeout(15000);
            String first = readLine(in);
            boolean authed = false;
            try { authed = first != null && tokenOk(JsonParser.parseString(first).getAsJsonObject().get("auth").getAsString()); } catch (RuntimeException ignored) {}
            if (!authed) { out.println("{\"ok\":false,\"error\":\"auth\"}"); log("REJECT bad token"); return; }
            out.println("{\"ok\":true,\"hello\":\"rimvisuals-jarvis\",\"protocol\":1}");
            c.setSoTimeout(0);
            String line;
            while ((line = readLine(in)) != null) {
                JsonObject res;
                try {
                    JsonObject req = JsonParser.parseString(line).getAsJsonObject();
                    res = JarvisCommands.handle(req);
                    log(summary(req) + " -> " + (res.get("ok").getAsBoolean() ? "ok" : res.get("error").getAsString()));
                } catch (RuntimeException e) {
                    res = new JsonObject(); res.addProperty("ok", false); res.addProperty("error", "bad_request");
                }
                out.println(res);
            }
        } catch (IOException ignored) {
        }
    }

    private static String summary(JsonObject r) {
        StringBuilder sb = new StringBuilder();
        for (String k : new String[]{"action", "module", "id", "setting", "name", "mode", "target", "what", "confirmed"})
            if (r.has(k) && r.get(k).isJsonPrimitive()) sb.append(k).append('=').append(r.get(k).getAsString().replaceAll("[\\r\\n]", " ")).append(' ');
        if (r.has("value") && r.get("value").isJsonPrimitive()) sb.append("value=").append(r.get("value").getAsString().replaceAll("[\\r\\n]", " "));
        return sb.toString();
    }

    private static String readLine(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder(); int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') return sb.toString();
            if (sb.length() >= MAX_LINE) throw new IOException("line too long");
            sb.append((char) ch);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static synchronized void log(String s) {
        LOG.info("JARVIS {}", s);
        try { if (audit != null) { audit.write(java.time.Instant.now() + " " + s + "\n"); audit.flush(); } } catch (IOException ignored) {}
    }
}
