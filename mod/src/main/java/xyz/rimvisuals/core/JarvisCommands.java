package xyz.rimvisuals.core;

import com.google.gson.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * RIM JARVIS command set. This is the ONLY surface the launcher can reach: a fixed allowlist of actions that read or
 * change RIMVISUALS' own saved settings. No reflection, no scripting, no commands sent to servers, no file paths.
 * Every call runs on the game thread via {@link #gameThread}; dangerous actions need {@code "confirmed":true}.
 */
public final class JarvisCommands {
    private JarvisCommands() {}

    public interface GameThread { <T> T call(Supplier<T> job) throws Exception; }
    /** Replaced by the bridge (MinecraftClient.execute) and by tests (direct). */
    public static volatile GameThread gameThread = new GameThread() { public <T> T call(Supplier<T> job) { return job.get(); } };
    public static volatile java.util.function.BooleanSupplier access = () -> xyz.rimvisuals.auth.AccessGate.allowed();
    /** Hooks that need Minecraft classes; default no-ops keep this class testable. */
    public static volatile java.util.function.Function<String, Boolean> openScreen = s -> false;
    public static volatile java.util.function.IntSupplier fps = () -> 0;
    public static volatile java.util.function.Consumer<String> toast = s -> {};

    public static final Set<String> ACTIONS = Set.of("ping", "status", "list_modules", "get_module", "set_module", "set_setting",
            "apply_preset", "performance_mode", "save_config", "load_config", "list_configs", "open_gui", "query", "reset_module");
    /** Actions that replace several settings at once: the launcher must show a confirmation first. */
    public static final Set<String> DANGEROUS = Set.of("load_config", "reset_module", "apply_preset");
    private static final Set<String> FREE = Set.of("ping", "status");

    private static JsonObject err(String code, String msg) {
        JsonObject o = new JsonObject(); o.addProperty("ok", false); o.addProperty("error", code); o.addProperty("message", msg); return o;
    }
    private static JsonObject ok() { JsonObject o = new JsonObject(); o.addProperty("ok", true); return o; }

    private static String str(JsonObject r, String k) { var v = r.get(k); return v != null && v.isJsonPrimitive() ? v.getAsString() : ""; }

    public static Module find(String id) {
        for (Module m : Modules.ALL) if (m.id.equals(id)) return m;
        return null;
    }
    public static Setting findSetting(Module m, String key) {
        String k = key.toLowerCase(Locale.ROOT).replace(' ', '_');
        for (Setting s : m.settings) if (s.key().equals(k)) return s;
        return null;
    }

    public static JsonObject handle(JsonObject req) {
        String action = str(req, "action");
        if (!ACTIONS.contains(action)) return err("unknown_action", "action is not in the allowlist");
        if (!FREE.contains(action) && !access.getAsBoolean()) return err("access_denied", "RIMVISUALS access is not active (sign in first)");
        if (DANGEROUS.contains(action) && !(req.has("confirmed") && req.get("confirmed").isJsonPrimitive() && req.get("confirmed").getAsBoolean()))
            return err("confirmation_required", "this action needs the user's confirmation");
        try {
            return gameThread.call(() -> run(action, req));
        } catch (Exception e) {
            return err("failed", e.getClass().getSimpleName());
        }
    }

    private static JsonObject run(String action, JsonObject r) {
        switch (action) {
            case "ping": { var o = ok(); o.addProperty("pong", true); return o; }
            case "status": {
                var o = ok(); o.addProperty("access", access.getAsBoolean()); o.addProperty("modules", Modules.ALL.size());
                o.addProperty("preset", Modules.PERFORMANCE.preset.get());
                try {
                    o.addProperty("minecraft", net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minecraft")
                            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse(""));
                    var mc = net.minecraft.client.MinecraftClient.getInstance();
                    o.addProperty("player", mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername());
                } catch (Throwable t) { /* status must never fail because of optional fields */ }
                return o;
            }
            case "list_modules": {
                var o = ok(); var arr = new JsonArray();
                for (Module m : Modules.ALL) {
                    var j = new JsonObject(); j.addProperty("id", m.id); j.addProperty("name", m.name); j.addProperty("category", m.category.name());
                    j.addProperty("enabled", m.enabled); j.addProperty("toggleable", m.toggleable); arr.add(j);
                }
                o.add("modules", arr); return o;
            }
            case "get_module": {
                Module m = find(str(r, "id")); if (m == null) return err("no_module", "unknown module");
                var o = ok(); o.addProperty("id", m.id); o.addProperty("enabled", m.enabled); var arr = new JsonArray();
                for (Setting s : m.settings) { var j = describe(s); if (j != null) arr.add(j); }
                o.add("settings", arr); return o;
            }
            case "set_module": {
                Module m = find(str(r, "module")); if (m == null) m = find(str(r, "id"));
                if (m == null) return err("no_module", "unknown module");
                if (!m.toggleable) return err("not_toggleable", m.name + " cannot be switched");
                if (!r.has("enabled") || !r.get("enabled").isJsonPrimitive()) return err("bad_value", "enabled must be true/false");
                m.enabled = r.get("enabled").getAsBoolean(); persist();
                var o = ok(); o.addProperty("id", m.id); o.addProperty("enabled", m.enabled); return o;
            }
            case "set_setting": {
                Module m = find(str(r, "module")); if (m == null) return err("no_module", "unknown module");
                Setting s = findSetting(m, str(r, "setting")); if (s == null || s.hidden) return err("no_setting", "unknown setting");
                String before = s.save();
                String why = assign(s, r.get("value")); if (why != null) return err("bad_value", why);
                persist();
                var o = ok(); o.addProperty("module", m.id); o.addProperty("setting", s.key()); o.addProperty("before", before); o.addProperty("value", s.save()); return o;
            }
            case "performance_mode": {
                String n = str(r, "mode").toLowerCase(Locale.ROOT); var p = Modules.PERFORMANCE;
                int idx = switch (n) { case "performance" -> 0; case "balanced" -> 1; case "ultra" -> 2; default -> -1; };
                if (idx < 0) return err("bad_value", "mode must be Performance, Balanced or Ultra");
                p.preset.index = idx; p.applyPreset(); persist();
                var o = ok(); o.addProperty("mode", p.preset.get()); o.addProperty("particleLimit", (int) p.particleLimit.value); o.addProperty("safe", p.safe.value); return o;
            }
            case "apply_preset": {
                String n = str(r, "name");
                if (!Presets.ENABLED.containsKey(n)) return err("no_preset", "unknown preset");
                if (!Presets.apply(n)) return err("failed", "preset not applied"); persist();
                var o = ok(); o.addProperty("name", n); return o;
            }
            case "reset_module": {
                Module m = find(str(r, "module")); if (m == null) return err("no_module", "unknown module");
                Modules.reset(m); persist(); var o = ok(); o.addProperty("id", m.id); return o;
            }
            case "save_config": {
                String n = Modules.cleanName(str(r, "name")); if (n.isEmpty()) return err("bad_value", "empty configuration name");
                if (!Modules.saveProfile(n)) return err("failed", "could not save"); var o = ok(); o.addProperty("name", n); return o;
            }
            case "load_config": {
                String n = Modules.cleanName(str(r, "name"));
                if (!Modules.listProfiles().contains(n)) return err("no_config", "no such configuration");
                if (!Modules.loadProfile(n)) return err("failed", "could not load"); var o = ok(); o.addProperty("name", n); return o;
            }
            case "list_configs": { var o = ok(); var a = new JsonArray(); Modules.listProfiles().forEach(a::add); o.add("configs", a); return o; }
            case "open_gui": {
                String t = str(r, "target"); if (!t.equals("clickgui") && !t.equals("hud_editor")) return err("bad_value", "target must be clickgui or hud_editor");
                if (!openScreen.apply(t)) return err("failed", "could not open (is a world or the title screen loaded?)");
                var o = ok(); o.addProperty("target", t); return o;
            }
            case "query": return query(str(r, "what"));
            default: return err("unknown_action", action);
        }
    }

    private static JsonObject query(String what) {
        var o = ok(); o.addProperty("what", what);
        switch (what) {
            case "fps": {
                var p = Modules.PERFORMANCE; int f = fps.getAsInt();
                o.addProperty("fps", f); o.addProperty("targetFps", (int) p.targetFps.value); o.addProperty("preset", p.preset.get());
                o.addProperty("safeMode", p.safe.value); o.addProperty("particleLimit", (int) p.particleLimit.value);
                var on = new JsonArray(); for (Module m : Modules.ALL) if (m.enabled && m.toggleable && heavy(m)) on.add(m.id);
                o.add("heavyEnabled", on);
                var top = new JsonArray(); for (String s : PerformanceMonitor.top(5)) top.add(s); o.add("topCpu", top);
                o.addProperty("monitorOn", PerformanceMonitor.enabled()); return o;
            }
            case "cooldowns": {
                var a = new JsonArray(); for (var c : HelperRuntime.cooldownRows()) { var j = new JsonObject(); j.addProperty("text", c.text()); j.addProperty("progress", c.progress()); a.add(j); }
                o.add("cooldowns", a); return o;
            }
            case "events": {
                var a = new JsonArray(); HelperRuntime.eventRows().forEach(a::add); o.add("events", a); o.addProperty("status", HelperRuntime.status()); return o;
            }
            default: return err("bad_value", "what must be fps, cooldowns or events");
        }
    }

    /** Effects with per-frame or per-hit cost (used by "why did my FPS drop"). */
    static boolean heavy(Module m) {
        return switch (m.category) { case VISUALS, SHADER_HANDS -> true; default -> m.id.equals("particles") || m.id.equals("pulse_glow"); };
    }

    static JsonObject describe(Setting s) {
        var j = new JsonObject(); j.addProperty("key", s.key()); j.addProperty("name", s.name);
        if (s instanceof Setting.Bool b) { j.addProperty("type", "bool"); j.addProperty("value", b.value); }
        else if (s instanceof Setting.Num n) { j.addProperty("type", "number"); j.addProperty("value", n.value); j.addProperty("min", n.min); j.addProperty("max", n.max); j.addProperty("step", n.step); }
        else if (s instanceof Setting.Mode m) { j.addProperty("type", "mode"); j.addProperty("value", m.get()); var a = new JsonArray(); for (String x : m.modes) a.add(x); j.add("modes", a); }
        else if (s instanceof Setting.Color c) { j.addProperty("type", "color"); j.addProperty("value", "#" + c.hex()); }
        else return null; // Text settings (server prefixes etc.) are never exposed
        return j;
    }

    /** Returns null on success or a reason. Text settings are refused on purpose. */
    static String assign(Setting s, JsonElement v) {
        if (v == null || !v.isJsonPrimitive()) return "value missing";
        var p = v.getAsJsonPrimitive();
        if (s instanceof Setting.Bool b) {
            if (p.isBoolean()) { b.value = p.getAsBoolean(); return null; }
            return "expected true/false";
        }
        if (s instanceof Setting.Num n) {
            if (!p.isNumber() && !(p.isString() && p.getAsString().matches("-?\\d+(\\.\\d+)?"))) return "expected a number";
            double d = p.getAsDouble(); if (!Double.isFinite(d)) return "expected a finite number";
            n.set(d); return null;
        }
        if (s instanceof Setting.Mode m) {
            if (p.isNumber()) { int i = p.getAsInt(); if (i < 0 || i >= m.modes.length) return "mode index out of range"; m.index = i; return null; }
            for (int i = 0; i < m.modes.length; i++) if (m.modes[i].equalsIgnoreCase(p.getAsString())) { m.index = i; return null; }
            return "expected one of " + String.join(", ", m.modes);
        }
        if (s instanceof Setting.Color c) {
            String h = p.getAsString().trim(); if (h.startsWith("#")) h = h.substring(1);
            if (!h.matches("[0-9a-fA-F]{6}")) return "expected #RRGGBB";
            c.setSolid(Integer.parseInt(h, 16)); return null;
        }
        return "this setting cannot be changed remotely";
    }

    /** Changes go through the normal config file, so they behave exactly like a change made in the ClickGUI. */
    static void persist() { try { Modules.save(); } catch (RuntimeException ignored) {} }
}
