package xyz.rimvisuals.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * File-based store for named configurations (CFG), JSON import/export, rolling backups and per-server bindings.
 * Pure java.nio + Gson so it is unit tested without Minecraft. Layout under {@code root}:
 * profiles/NAME.properties, exports/NAME.json, imports/*.json, backups/config-TIMESTAMP.properties, servers.properties.
 */
public final class ConfigStore {
    public static final int MAX_BACKUPS = 10;
    static final long MAX_IMPORT_BYTES = 512 * 1024;
    static final int MAX_ENTRIES = 4000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSSSSS");

    private final Path root;

    public ConfigStore(Path root) { this.root = root; }

    public Path profiles() { return root.resolve("profiles"); }
    public Path exports() { return root.resolve("exports"); }
    public Path imports() { return root.resolve("imports"); }
    public Path backups() { return root.resolve("backups"); }
    private Path servers() { return root.resolve("servers.properties"); }

    /** Letters, digits, '_' and '-' only, at most 24 characters. */
    public static String clean(String n) {
        String s = n == null ? "" : n.replaceAll("[^A-Za-z0-9_-]", "");
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    private Path file(String name) { return profiles().resolve(clean(name) + ".properties"); }

    public boolean exists(String name) { return !clean(name).isEmpty() && Files.isRegularFile(file(name)); }

    /** Creates a new CFG; unlike Save this never overwrites a named profile. */
    public boolean create(String name, Properties p) {
        if (clean(name).isEmpty()) return false;
        Path tmp=null;
        try {Files.createDirectories(profiles());tmp=Files.createTempFile(profiles(),"new-cfg-",".tmp");
            write(tmp,p);Files.move(tmp,file(name));return true;
        }catch(IOException e){return false;}
        finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}

    }

    public boolean save(String name, Properties p) {
        if (clean(name).isEmpty()) return false;
        try { write(file(name), p); return true; } catch (IOException e) { return false; }
    }

    public Optional<Properties> load(String name) {
        if (!exists(name)) return Optional.empty();
        try { return Optional.of(read(file(name))); } catch (IOException e) { return Optional.empty(); }
    }

    public boolean delete(String name) {
        if (clean(name).isEmpty()) return false;
        try { return Files.deleteIfExists(file(name)); } catch (IOException e) { return false; }
    }

    /** Copies {@code from} to a new profile {@code to}; fails if the target exists or names are invalid. */
    public boolean duplicate(String from, String to) {
        if (!exists(from) || clean(to).isEmpty() || exists(to)) return false;
        try { Files.copy(file(from), file(to)); return true; } catch (IOException e) { return false; }
    }

    public boolean rename(String from, String to) {
        if (!exists(from) || clean(to).isEmpty() || exists(to) || clean(from).equals(clean(to))) return false;
        try {
            Properties bindings=Files.isRegularFile(servers())?read(servers()):new Properties();
            Properties changed=new Properties();changed.putAll(bindings);
            changed.replaceAll((k,v)->clean(from).equals(v)?clean(to):v);
            Files.move(file(from),file(to));
            try { if(!bindings.equals(changed))write(servers(),changed); }
            catch(IOException e){Files.move(file(to),file(from));throw e;}
            return true;
        }catch(IOException e){return false;}
    }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(profiles())) return out;
        try (Stream<Path> s = Files.list(profiles())) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".properties"))
                    .map(n -> n.substring(0, n.length() - ".properties".length())).sorted().forEach(out::add);
        } catch (IOException ignored) { }
        return out;
    }

    // ---------- JSON ----------

    /** Writes exports/NAME.json: {"schema":1,"name":..,"settings":{key:value}}. */
    public Path exportJson(String name) throws IOException {
        Properties p = load(name).orElseThrow(() -> new IOException("No such configuration"));
        JsonObject settings = new JsonObject();
        for (String k : new java.util.TreeSet<>(p.stringPropertyNames())) settings.addProperty(k, p.getProperty(k));
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("name", clean(name));
        root.add("settings", settings);
        Files.createDirectories(exports());
        Path out = exports().resolve(clean(name) + ".json");
        Files.writeString(out, root.toString(), StandardCharsets.UTF_8);
        return out;
    }

    /** Imports one JSON file as a new profile; never overwrites (adds _2, _3...). Returns the profile name. */
    public Optional<String> importJson(Path file) {
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_IMPORT_BYTES) return Optional.empty();
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("schema") && (!root.get("schema").isJsonPrimitive() || !root.get("schema").getAsString().equals("1"))) return Optional.empty();
            if (!root.has("settings") || !root.get("settings").isJsonObject()) return Optional.empty();
            JsonObject settings = root.getAsJsonObject("settings");
            if (settings.size() == 0 || settings.size() > MAX_ENTRIES) return Optional.empty();
            Properties p = new Properties();
            for (Map.Entry<String, JsonElement> e : settings.entrySet()) {
                if (!e.getKey().matches("[a-z0-9_.\\-]{1,80}") || !e.getValue().isJsonPrimitive()) return Optional.empty();
                String v = e.getValue().getAsString();
                if (v.length() > 200) return Optional.empty();
                p.setProperty(e.getKey(), v);
            }
            String base = root.has("name") && root.get("name").isJsonPrimitive() ? root.get("name").getAsString()
                    : file.getFileName().toString().replaceFirst("\\.json$", "");
            String name = clean(base);
            if (name.isEmpty()) return Optional.empty();
            String unique = name;
            for (int i = 2; exists(unique) && i < 100; i++) unique = clean(name.length() > 20 ? name.substring(0, 20) : name) + "_" + i;
            if (exists(unique)) return Optional.empty();
            return create(unique, p) ? Optional.of(unique) : Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.empty(); // malformed JSON, wrong types, unreadable file
        }
    }

    /** Imports every *.json in the imports folder; returns the created profile names. */
    public List<String> importFolder() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(imports())) return out;
        try (Stream<Path> s = Files.list(imports())) {
            s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(p -> importJson(p).ifPresent(out::add));
        } catch (IOException ignored) { }
        return out;
    }

    // ---------- backups ----------

    public Path backup(Properties current) throws IOException {
        Files.createDirectories(backups());
        Path out = backups().resolve("config-" + LocalDateTime.now().format(STAMP) + ".properties");
        write(out, current);
        List<Path> all = backupFiles();
        for (int i = 0; i < all.size() - MAX_BACKUPS; i++) Files.deleteIfExists(all.get(i));
        return out;
    }

    /** Oldest first. */
    public List<Path> backupFiles() {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(backups())) return out;
        try (Stream<Path> s = Files.list(backups())) {
            s.filter(p -> p.getFileName().toString().matches("config-[0-9-]+\\.properties")).sorted().forEach(out::add);
        } catch (IOException ignored) { }
        return out;
    }

    public Optional<Properties> latestBackup() {
        List<Path> all = backupFiles();
        if (all.isEmpty()) return Optional.empty();
        try { return Optional.of(read(all.get(all.size() - 1))); } catch (IOException e) { return Optional.empty(); }
    }

    // ---------- per-server bindings ----------

    public static String serverKey(String address) { return address == null ? "" : address.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9.:_-]", ""); }

    public boolean bindServer(String address, String profile) {
        String key = serverKey(address);
        if (key.isEmpty() || !exists(profile)) return false;
        try {
            Properties p = Files.isRegularFile(servers()) ? read(servers()) : new Properties();
            p.setProperty(key, clean(profile));
            write(servers(), p);
            return true;
        } catch (IOException e) { return false; }
    }

    public boolean unbindServer(String address) {
        String key = serverKey(address);
        try {
            if (key.isEmpty() || !Files.isRegularFile(servers())) return false;
            Properties p = read(servers());
            if (p.remove(key) == null) return false;
            write(servers(), p);
            return true;
        } catch (IOException e) { return false; }
    }

    public Optional<String> serverProfile(String address) {
        try {
            String key = serverKey(address);
            if (key.isEmpty() || !Files.isRegularFile(servers())) return Optional.empty();
            String v = read(servers()).getProperty(key);
            return v != null && exists(v) ? Optional.of(v) : Optional.empty();
        } catch (IOException e) { return Optional.empty(); }
    }

    // ---------- io ----------

    public static void write(Path file, Properties p) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp=Files.createTempFile(file.getParent(),"rim-config-",".tmp");
        try {
            try (var out=Files.newOutputStream(tmp)) {p.store(out,"RIMVISUALS");}
            try {Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally {Files.deleteIfExists(tmp);}
    }

    private static Properties read(Path file) throws IOException {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) { p.load(in); }
        return p;
    }
}
