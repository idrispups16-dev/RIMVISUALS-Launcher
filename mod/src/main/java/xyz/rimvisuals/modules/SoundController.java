package xyz.rimvisuals.modules;
import net.minecraft.sound.SoundCategory;
import xyz.rimvisuals.core.*;
public final class SoundController extends xyz.rimvisuals.core.Module {
 public final Setting.Num overall=add(new Setting.Num("Overall multiplier",1,0,1,.05)),music=add(new Setting.Num("Music",1,0,1,.05)),ambient=add(new Setting.Num("Ambient",1,0,1,.05)),weather=add(new Setting.Num("Weather",1,0,1,.05)),hostile=add(new Setting.Num("Hostile mobs",1,0,1,.05));
 public SoundController(){super("sound_controller","Sound Controller","Scales sound categories at playback; Minecraft volume options remain unchanged",Category.UTILITIES);enabled=false;}
 private long signature=Long.MIN_VALUE;
 private static final SoundCategory[] CATEGORIES=SoundCategory.values();
 public void refresh(net.minecraft.client.MinecraftClient mc){long value=(isActive()?1:0);value=value*31+Double.doubleToLongBits(overall.value);value=value*31+Double.doubleToLongBits(music.value);value=value*31+Double.doubleToLongBits(ambient.value);value=value*31+Double.doubleToLongBits(weather.value);value=value*31+Double.doubleToLongBits(hostile.value);if(signature==value)return;signature=value;for(var category:CATEGORIES)if(category!=SoundCategory.MASTER)mc.getSoundManager().refreshSoundVolumes(category);}
 public float scale(float value,SoundCategory category){if(!isActive())return value;double factor=switch(category){case MUSIC->music.value;case AMBIENT->ambient.value;case WEATHER->weather.value;case HOSTILE->hostile.value;default->1;};return (float)Math.max(0,Math.min(1,value*overall.value*factor));}
}
