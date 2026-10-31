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
    public static final ArmorHud ARMOR_HUD=new ArmorHud();
    public static final TargetHud TARGET_HUD=new TargetHud();
    public static final TotemScale TOTEM_SCALE=new TotemScale();
    public static final PerformanceOptions PERFORMANCE = new PerformanceOptions();
    public static final InterfaceModule UI = new InterfaceModule();
    public static final InfoHud INFO = new InfoHud();
    public static final Watermark WATERMARK = new Watermark();
    public static final PulseGlow PULSE = new PulseGlow();
    public static final ParticleOverlay PARTICLES = new ParticleOverlay();
    public static final List<Module> ALL = List.of(INFO, WATERMARK, PULSE, PARTICLES, UI, PERFORMANCE, VIEWMODEL, SWING, HAND_LIGHTING, CRYSTAL_SCALE, CRYSTAL_FADE, TOTEM_SCALE, HIT_COLOR, HIT_PARTICLES, CRIT_PARTICLES, DAMAGE_PARTICLES, PEARL_TRAILS, PROJECTILE_TRAILS, SWORD_TRAILS, ARMOR_HUD, TARGET_HUD);
    public static final List<HudModule> HUD = List.of(INFO, WATERMARK, ARMOR_HUD, TARGET_HUD);

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

    public static void save() { write(dir().resolve("config.properties"), snapshot()); }
    public static void load() { apply(read(dir().resolve("config.properties"))); PERFORMANCE.seedPreset(); VIEWMODEL.seedPreset(); }

    public static String cleanName(String n) {
        String s = n == null ? "" : n.replaceAll("[^A-Za-z0-9_-]", "");
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    public static boolean saveProfile(String name) {
        String n = cleanName(name);
        if (n.isEmpty()) return false;
        write(profileDir().resolve(n + ".properties"), snapshot());
        return true;
    }

    public static boolean loadProfile(String name) {
        Path f = profileDir().resolve(cleanName(name) + ".properties");
        if (!Files.exists(f)) return false;
        apply(read(f));
        save();
        return true;
    }

    public static void deleteProfile(String name) {
        try { Files.deleteIfExists(profileDir().resolve(cleanName(name) + ".properties")); }
        catch (IOException e) { LOG.warn("Could not delete profile {}", name, e); }
    }

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(profileDir())) return out;
        try (Stream<Path> s = Files.list(profileDir())) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".properties"))
                    .map(n -> n.substring(0, n.length() - ".properties".length())).sorted().forEach(out::add);
        } catch (IOException e) { LOG.warn("Could not list profiles", e); }
        return out;
    }
}
