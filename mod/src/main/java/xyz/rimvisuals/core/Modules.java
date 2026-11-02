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
    public static final WaterColor WATER_COLOR=new WaterColor();
    public static final LavaColor LAVA_COLOR=new LavaColor();
    public static final WeatherColor WEATHER_COLOR=new WeatherColor();
    public static final BiomeColors BIOME_COLORS=new BiomeColors();
    public static final EntityOutline ENTITY_OUTLINE=new EntityOutline();
    public static final HandGlow HAND_GLOW=new HandGlow();
    public static final TargetHighlight TARGET_HIGHLIGHT=new TargetHighlight();
    public static final AnchorFade ANCHOR_FADE=new AnchorFade();
    public static final ExplosionEffects EXPLOSION_EFFECTS=new ExplosionEffects();
    public static final CooldownsHud COOLDOWNS_HUD=new CooldownsHud();
    public static final NotificationsHud NOTIFICATIONS=new NotificationsHud();
    public static final TotemFade TOTEM_FADE=new TotemFade();
    public static final Fullbright FULLBRIGHT=new Fullbright();
    public static final NightVisionFade NIGHT_VISION_FADE=new NightVisionFade();
    public static final GoldenAppleGlow GOLDEN_APPLE_GLOW=new GoldenAppleGlow();
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
    public static final PerformanceHud PERFORMANCE_HUD = new PerformanceHud();
    public static final LocalMarksHud LOCAL_MARKS = new LocalMarksHud();
    public static final PlayerStatusHud TOTEM_BAR = new PlayerStatusHud("totem_bar","Totem Bar",PlayerStatusHud.Kind.TOTEMS,145);
    public static final PlayerStatusHud SATURATION_HUD = new PlayerStatusHud("saturation_hud","Saturation HUD",PlayerStatusHud.Kind.SATURATION,180);
    public static final PlayerStatusHud LOW_HP_HUD = new PlayerStatusHud("low_hp_hud","Low HP",PlayerStatusHud.Kind.LOW_HP,220);
    public static final PlayerStatusHud TNT_TIMER = new PlayerStatusHud("tnt_timer","TNT Timer",PlayerStatusHud.Kind.TNT,260);
    public static final Zoom ZOOM = new Zoom();
    public static final Crosshair CROSSHAIR=new Crosshair();
    public static final AspectRatio ASPECT_RATIO=new AspectRatio();
    public static final Atmosphere ATMOSPHERE=new Atmosphere();
    public static final ItemPhysics ITEM_PHYSICS=new ItemPhysics();
    public static final WorldVisual CHINA_HAT=new WorldVisual("china_hat","China Hat",WorldVisual.Kind.HAT);
    public static final WorldVisual BLOCK_OVERLAY=new WorldVisual("block_overlay","Block Overlay",WorldVisual.Kind.BLOCK);
    public static final SoundController SOUND_CONTROLLER=new SoundController();
    public static final ShulkerPreview SHULKER_PREVIEW=new ShulkerPreview();
    public static final ItemSwap ITEM_SWAP=new ItemSwap();
    public static final UtilityHud HOTKEYS_HUD=new UtilityHud("hotkeys_hud","Assigned Keys HUD",true,300);
    public static final UtilityHud COMPASS_HUD=new UtilityHud("compass_hud","Compass HUD",false,340);
    public static final List<Module> ALL = List.of(INFO, WATERMARK, PULSE, PARTICLES, UI, PERFORMANCE, VIEWMODEL, SWING, HAND_LIGHTING, CRYSTAL_SCALE, CRYSTAL_FADE, TOTEM_SCALE, HIT_COLOR, HIT_PARTICLES, CRIT_PARTICLES, DAMAGE_PARTICLES, PEARL_TRAILS, PROJECTILE_TRAILS, SWORD_TRAILS, SHARPNESS_PARTICLES, INVENTORY_HUD, HIT_MARKER, KILL_EFFECTS, DAMAGE_TINT, CPS_HUD, KEYSTROKES_HUD, POTION_HUD, SPEED_HUD, SESSION_HUD, HAND_COLOR, CUSTOM_FOG, HAND_GLOW, WATER_COLOR, LAVA_COLOR, WEATHER_COLOR, BIOME_COLORS, ENTITY_OUTLINE, TARGET_HIGHLIGHT, TOTEM_FADE, ANCHOR_FADE, EXPLOSION_EFFECTS, COOLDOWNS_HUD, NOTIFICATIONS, FULLBRIGHT, NIGHT_VISION_FADE, GOLDEN_APPLE_GLOW, CUSTOM_SKY, HURT_CAMERA, ARMOR_HUD, TARGET_HUD, PERFORMANCE_HUD, LOCAL_MARKS, TOTEM_BAR, SATURATION_HUD, LOW_HP_HUD, TNT_TIMER, ZOOM, CROSSHAIR, ASPECT_RATIO, ATMOSPHERE, ITEM_PHYSICS, CHINA_HAT, BLOCK_OVERLAY, SOUND_CONTROLLER, SHULKER_PREVIEW, ITEM_SWAP, HOTKEYS_HUD, COMPASS_HUD);
    public static final List<HudModule> HUD = ALL.stream().filter(HudModule.class::isInstance).map(HudModule.class::cast).toList();

    private static final Properties DEFAULTS=snapshot();
    public static void reset(Module m){if(m.toggleable)m.enabled=Boolean.parseBoolean(DEFAULTS.getProperty(m.id+".enabled"));for(Setting st:m.settings)st.load(DEFAULTS.getProperty(m.id+"."+st.key()));PERFORMANCE.seedPreset();VIEWMODEL.seedPreset();}
    private Modules() {}

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("rimvisuals"); }
    private static Path profileDir() { return dir().resolve("profiles"); }

    public static Properties snapshot() {
        Properties p = new Properties();
        for (Module m : ALL) {
            if (m.toggleable) p.setProperty(m.id + ".enabled", String.valueOf(m.enabled));
            for (Setting s : m.settings) p.setProperty(m.id + "." + s.key(), s.save());
        }
        Keybinds.snapshot(p);
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
        Keybinds.load(p);
    }

    private static boolean write(Path file, Properties p) {
        try {ConfigStore.write(file,p);return true;}
        catch(IOException e){LOG.warn("Could not write {}",file,e);return false;}
    }

    private static Properties read(Path file) {
        Properties p = new Properties();
        if (!Files.exists(file)) return p;
        try (var in = Files.newInputStream(file)) { p.load(in); }
        catch (IOException e) { LOG.warn("Could not read {}", file, e); }
        return p;
    }

    public static boolean save() { Properties s = snapshot(); if(!write(dir().resolve("config.properties"), s)) return false; lastSaved = s; return true; }
    public static void load() {
        boolean existed = Files.exists(dir().resolve("config.properties"));
        apply(read(dir().resolve("config.properties")));
        PERFORMANCE.seedPreset(); VIEWMODEL.seedPreset();
        if (existed) backupNow(); // one rolling backup per session of what was loaded
        lastSaved = snapshot();
    }

    public static ConfigStore store() { return new ConfigStore(dir()); }
    public static String cleanName(String n) { return ConfigStore.clean(n); }

    public static boolean createProfile(String name) {return store().create(cleanName(name),snapshot());}
    public static boolean saveProfile(String name) { String n = cleanName(name); return !n.isEmpty() && store().save(n, snapshot()); }
    public static boolean loadProfile(String name) {
        var p = store().load(cleanName(name));
        if (p.isEmpty()) return false;
        if (!backupNow()) return false;
        var previous=snapshot();
        apply(p.get());
        if(save())return true;
        apply(previous);return false;
    }
    public static boolean deleteProfile(String name) { return store().delete(cleanName(name)); }
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
        if (!backupNow()) return false;
        var previous=snapshot();
        apply(p.get());
        if(save())return true;
        apply(previous);return false;
    }

    public static boolean applyPreset(String name) {
        if (!Presets.apply(name)) return false;
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
        if (!UI.autoSave.value || ++autosaveTicks % 600 != 0) return;
        if (lastSaved != null && lastSaved.equals(snapshot())) return;
        save();
    }
}
