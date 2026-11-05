package xyz.rimvisuals.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** All HTTP and disk work runs outside the Minecraft render/tick thread. */
public final class DeviceAuth {
    public static final DeviceAuth INSTANCE = new DeviceAuth();
    public static final String SITE = "https://rimvisuals.vercel.app";
    private static final Gson JSON = new Gson();
    private final AccessLease lease = new AccessLease();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rimvisuals-auth"); t.setDaemon(true); return t;
    });
    private final HttpClient http = AuthConnection.client();
    private long lastFailureLog;
    private String lastFailureCode = "";
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("rimvisuals/mod-session.json");
    private volatile String message = "Войдите через сайт, чтобы включить RIMVISUALS.";
    private volatile String code = "";
    private volatile boolean busy;
    private volatile boolean locallySignedOut;
    private String api, key, token, verifier, candidate, challenge;
    private long expires, nextPoll, nextValidation;
    private boolean initialized;
    private volatile long socialRevision;
    public long socialRevision(){return socialRevision;}

    private DeviceAuth() {}
    public boolean active() { return !locallySignedOut && lease.active(System.nanoTime()); }
    public String message() { return message; }
    public String code() { return code; }
    public boolean busy() { return busy; }
    public String browserUrl() { return SITE + "/account.html" + (code.isEmpty() ? "#device" : "?device=" + code + "#device"); }

    public synchronized void init() {
        if (initialized) return; initialized = true;
        worker.execute(() -> {
            try {
                if (Files.exists(file)) {
                    JsonObject saved = JSON.fromJson(Files.readString(file), JsonObject.class);
                    String value = saved.get("token").getAsString();
                    if (value.matches("[a-f0-9]{64}")) token = value;
                }
            } catch (Exception ignored) { message = "Сеанс не прочитан. Войдите заново."; }
        });
        worker.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.SECONDS);
    }
    public void start() {
        if (busy) return;
        busy = true;
        worker.execute(() -> {
            try {
                configure();
                verifier = random(); candidate = random();
                JsonObject result = rpc("mod_device_start", Map.of("p_verifier_hash", hash(verifier), "p_session_hash", hash(candidate), "p_device_name", "Minecraft / " + platform()));
                if (!status(result).equals("pending")) throw new IllegalStateException("request denied");
                challenge = result.get("challenge_id").getAsString(); code = result.get("code").getAsString();
                expires = System.nanoTime() + 300_000_000_000L; nextPoll = 0;
                message = "Откройте сайт и подтвердите этот код в своём аккаунте.";
            } catch (Exception e) { challenge = null; code = ""; message = AuthConnection.message(e); reportFailure("start",e); }
            finally { busy = false; }
        });
    }
    private void tick() {
        try {
            if (!busy && challenge == null && token != null && System.nanoTime() >= nextValidation) validate();
            if (challenge != null && System.nanoTime() >= nextPoll) {
                if (System.nanoTime() >= expires) { challenge = null; code = ""; message = "Код истёк. Получите новый код."; return; }
                nextPoll = System.nanoTime() + 5_000_000_000L;
                JsonObject result = rpc("mod_device_poll", Map.of("p_challenge_id", challenge, "p_verifier", verifier));
                String status = status(result);
                if (status.equals("approved")) {
                    locallySignedOut = false;
                    socialRevision++;token = candidate; challenge = null; code = ""; nextValidation = 0;
                    save(); validate();
                } else if (status.equals("no_access") || status.equals("invalid") || status.equals("expired")) {
                    // If the successful claim response was lost, the candidate may already be active.
                    socialRevision++;token = candidate; challenge = null; code = ""; validate();
                    if (lease.active(System.nanoTime())) { locallySignedOut = false; save(); }
                }
            }
        } catch (Exception e) {
            message = active() ? "Соединение потеряно. Повторная проверка…" : AuthConnection.message(e);
            reportFailure("poll/validate",e);
        }
    }
    private void validate() throws Exception {
        nextValidation = System.nanoTime() + 30_000_000_000L;
        configure();
        // Start lease at request time so network delays cannot extend server authorization.
        long started = System.nanoTime();
        JsonObject result = rpc("mod_session_validate", Map.of("p_token", token));
        if (status(result).equals("active")) {
            lease.grant(started, result.get("lease_seconds").getAsInt());
            message = "Вход выполнен. RIMVISUALS готов к работе.";
        } else {
            lease.clear(); token = null; Files.deleteIfExists(file);
            message = "Сеанс отключён или доступ не выдан. Подтвердите вход на сайте.";
        }
    }
    public void logout() {
        socialRevision++;locallySignedOut = true;
        lease.clear(); message = "Вы вышли. Для отзыва сеанса откройте кабинет на сайте.";
        worker.execute(() -> { lease.clear(); token = null; challenge = null; code = ""; try { Files.deleteIfExists(file); } catch (Exception ignored) {} });
    }
    public void social(String action, JsonObject payload, java.util.function.Consumer<JsonObject> callback) {
        worker.execute(() -> {
            JsonObject result;
            try { if(!active()||token==null)throw new IllegalStateException(); configure();
                result=rpc("mod_social",Map.of("p_token",token,"p_action",action,"p_payload",payload));
            }catch(Exception e){result=new JsonObject();result.addProperty("status","unavailable");}
            callback.accept(result);
        });
    }
    private void configure() throws Exception {
        if (api != null) return;
        JsonObject config = request(HttpRequest.newBuilder(URI.create(SITE + "/api/public-config")).timeout(Duration.ofSeconds(25)).GET().build());
        if (!config.get("authEnabled").getAsBoolean()) throw new IllegalStateException("auth unavailable");
        String url = config.get("supabaseUrl").getAsString();
        if (!url.equals("https://pixqnuuvpxxxwaxjntpl.supabase.co")) throw new IllegalStateException("unexpected auth server");
        key = config.get("supabaseAnonKey").getAsString(); api = url + "/rest/v1/rpc/";
    }
    private JsonObject rpc(String name, Map<String, ?> args) throws Exception {
        return request(HttpRequest.newBuilder(URI.create(api + name)).timeout(Duration.ofSeconds(25))
            .header("apikey", key).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(args))).build());
    }
    private JsonObject request(HttpRequest request) throws Exception {
        try { return AuthConnection.request(http,request); }
        catch(Exception e){reportFailure(request.uri().getHost()+request.uri().getPath(),e);throw e;}
    }
    private void reportFailure(String stage,Exception e){
        String code=AuthConnection.code(e);long now=System.nanoTime();
        if(code.equals(lastFailureCode)&&now-lastFailureLog<30_000_000_000L)return;
        lastFailureCode=code;lastFailureLog=now;
        org.slf4j.LoggerFactory.getLogger("rimvisuals-auth").warn("RIM_AUTH_ERROR stage={} code={} type={}",stage,code,e.getClass().getSimpleName());
    }
    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JSON.toJson(Map.of("token", token)));
            try { Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); } catch (UnsupportedOperationException ignored) {}
        } catch (Exception e) { message = "Вход выполнен, но сеанс не сохранён на диске."; }
    }
    private static String status(JsonObject result) { return result.get("status").getAsString(); }
    private static String random() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    private static String hash(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    private static String platform() { String os = System.getProperty("os.name", "Device"); return os.length() > 30 ? os.substring(0,30) : os; }
}

