package xyz.rimvisuals.core;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.rimvisuals.modules.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/** All modules, saving/loading, and profiles (config/rimvisuals/...). */
public final class Modules {
    private static final Logger LOG = LoggerFactory.getLogger("rimvisuals");

    public static final Viewmodel VIEWMODEL=new Viewmodel();
    public static final SwingAnimations SWING=new SwingAnimations();
    public static final HandLighting HAND_LIGHTING=new HandLighting();
    public static final CrystalScale CRYSTAL_SCALE=new CrystalScale();
    public static final CrystalFade CRYSTAL_FADE=new CrystalFade();
    public static final HitColor HIT_COLOR=new HitColor();
    public static final HitParticles HIT_PARTICLES=new HitParticles();
    public static final CriticalParticles CRIT_PARTICLES=new CriticalParticles();
    public static final DamageParticles DAMAGE_PARTICLES=new DamageParticles();
    public static final PearlTrails PEARL_TRAILS=new PearlTrails();
    public static final ProjectileTrails PROJECTILE_TRAILS=new ProjectileTrails();
    public static final SwordTrails SWORD_TRAILS=new SwordTrails();
    public static final SharpnessParticles SHARPNESS_PARTICLES=new SharpnessParticles();
    public static final InventoryHud INVENTORY_HUD=new InventoryHud();
    public static final HitMarker HIT_MARKER=new HitMarker();
    public static final KillEffects KILL_EFFECTS=new KillEffects();
    public static final DamageTint DAMAGE_TINT=new DamageTint();
    public static final CpsHud CPS_HUD=new CpsHud();
    public static final KeystrokesHud KEYSTROKES_HUD=new KeystrokesHud();
    public static final PotionHud POTION_HUD=new PotionHud();
    public static final SpeedHud SPEED_HUD=new SpeedHud();
    public static final SessionHud SESSION_HUD=new SessionHud();
    public static final HandColor HAND_COLOR=new HandColor();
    public static final CustomFog CUSTOM_FOG=new CustomFog();
    public static final CustomSky CUSTOM_SKY=new CustomSky();
    public static final HurtCamera HURT_CAMERA=new HurtCamera();
    public static final ArmorHud ARMOR_HUD=new ArmorHud();
    public static final TargetHud TARGET_HUD=new TargetHud();
    public static final TotemScale TOTEM_SCALE=new TotemScale();
    public static final PerformanceOptions PERFORMANCE = new PerformanceOptions();
    public static final InterfaceModule UI = new InterfaceModule();
    public static final InfoHud INFO = new InfoHud();
    public static final Watermark WATERMARK = new Watermark();
    public static final PulseGlow PULSE = new PulseGlow();
    public static final ParticleOverlay PARTICLES = new ParticleOverlay();
    public static final List<Module> ALL = List.of(INFO, WATERMARK, PULSE, PARTICLES, UI, PERFORMANCE, VIEWMODEL, SWING, HAND_LIGHTING, CRYSTAL_SCALE, CRYSTAL_FADE, TOTEM_SCALE, HIT_COLOR, HIT_PARTICLES, CRIT_PARTICLES, DAMAGE_PARTICLES, PEARL_TRAILS, PROJECTILE_TRAILS, SWORD_TRAILS, SHARPNESS_PARTICLES, INVENTORY_HUD, HIT_MARKER, KILL_EFFECTS, DAMAGE_TINT, CPS_HUD, KEYSTROKES_HUD, POTION_HUD, SPEED_HUD, SESSION_HUD, HAND_COLOR, CUSTOM_FOG, CUSTOM_SKY, HURT_CAMERA, ARMOR_HUD, TARGET_HUD);
    public static final List<HudModule> HUD = List.of(INFO, WATERMARK, ARMOR_HUD, TARGET_HUD, CPS_HUD, KEYSTROKES_HUD, POTION_HUD, SPEED_HUD, SESSION_HUD, INVENTORY_HUD);

    private static final Properties DEFAULTS=snapshot();
    public static void reset(Module m){if(m.toggleable)m.enabled=Boolean.parseBoolean(DEFAULTS.getProperty(m.id+".enabled"));for(Setting st:m.settings)st.load(DEFAULTS.getProperty(m.id+"."+st.key()));PERFORMANCE.seedPreset();VIEWMODEL.seedPreset();}
    private Modules() {}

    private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("rimvisuals"); }
    private static Path profileDir() { return dir().resolve("profiles"); }

    public static Properties snapshot() {
        Properties p = new Properties();
        for (Module m : ALL) {
            if (m.toggleable) p.setProperty(m.id + ".enabled", String.valueOf(m.enabled));
            for (Setting s : m.settings) p.setProperty(m.id + "." + s.key(), s.save());
        }
        return p;
    }

    public static void apply(Properties p) {
        for (Module m : ALL) {
            if (m.toggleable) {
                String e = p.getProperty(m.id + ".enabled");
                if (e != null) m.enabled = Boolean.parseBoolean(e);
            }
            for (Setting s : m.settings) {
                String v = p.getProperty(m.id + "." + s.key());
                if (v != null) s.load(v);
            }
        }
        PERFORMANCE.seedPreset(); VIEWMODEL.seedPreset();
    }

    private static void write(Path file, Properties p) {
        try {
            Files.createDirectories(file.getParent());
            try (var out = Files.newOutputStream(file)) { p.store(out, "RIMVISUALS"); }
        } catch (IOException e) { LOG.warn("Could not write {}", file, e); }
    }

    private static Properties read(Path file) {
        Properties p = new Properties();
        if (!Files.exists(file)) return p;
        try (var in = Files.newInputStream(file)) { p.load(in); }
        catch (IOException e) { LOG.warn("Could not read {}", file, e); }
        return p;
    }

    public static void save() { Properties s = snapshot(); write(dir().resolve("config.properties"), s); lastSaved = s; }
    public static void load() {
        boolean existed = Files.exists(dir().resolve("config.properties"));
        apply(read(dir().resolve("config.properties")));
        PERFORMANCE.seedPreset(); VIEWMODEL.seedPreset();
        if (existed) backupNow(); // one rolling backup per session of what was loaded
        lastSaved = snapshot();
    }

    public static ConfigStore store() { return new ConfigStore(dir()); }
    public static String cleanName(String n) { return ConfigStore.clean(n); }

    public static boolean saveProfile(String name) { String n = cleanName(name); return !n.isEmpty() && store().save(n, snapshot()); }
    public static boolean loadProfile(String name) {
        var p = store().load(cleanName(name));
        if (p.isEmpty()) return false;
        apply(p.get());
        save();
        return true;
    }
    public static void deleteProfile(String name) { store().delete(cleanName(name)); }
    public static List<String> listProfiles() { return store().list(); }
    public static boolean renameProfile(String from, String to) { return store().rename(cleanName(from), cleanName(to)); }
    public static boolean duplicateProfile(String from, String to) { return store().duplicate(cleanName(from), cleanName(to)); }

    /** Writes exports/NAME.json and returns its path, or null on failure. */
    public static String exportProfile(String name) {
        try { return store().exportJson(cleanName(name)).toString(); } catch (IOException e) { LOG.warn("Export failed", e); return null; }
    }
    public static List<String> importProfiles() { return store().importFolder(); }

    public static boolean backupNow() {
        try { store().backup(snapshot()); return true; } catch (IOException e) { LOG.warn("Backup failed", e); return false; }
    }
    public static boolean restoreLatestBackup() {
        var p = store().latestBackup();
        if (p.isEmpty()) return false;
        apply(p.get());
        save();
        return true;
    }

    public static boolean applyPreset(String name) {
        if (!Presets.apply(name)) return false;
        saveProfile(name);
        save();
        return true;
    }

    public static boolean bindServer(String address, String profile) { return store().bindServer(address, cleanName(profile)); }
    public static boolean unbindServer(String address) { return store().unbindServer(address); }
    public static java.util.Optional<String> serverProfile(String address) { return store().serverProfile(address); }
    /** Loads the configuration bound to this server, if any. Returns its name. */
    public static java.util.Optional<String> applyServerProfile(String address) {
        var name = serverProfile(address);
        if (name.isPresent() && loadProfile(name.get())) return name;
        return java.util.Optional.empty();
    }

    private static Properties lastSaved;
    private static int autosaveTicks;
    /** Called every client tick; every 30 s writes config.properties if any setting changed. */
    public static void autosaveTick() {
        if (++autosaveTicks % 600 != 0) return;
        if (lastSaved != null && lastSaved.equals(snapshot())) return;
        save();
    }
}
