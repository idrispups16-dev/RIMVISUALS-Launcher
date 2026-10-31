package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;
/** Packed item light only: no post-processing shaders or GPU resources. */
public final class HandLighting extends Module {
 public final Setting.Bool main=add(new Setting.Bool("Main hand lighting",true)),off=add(new Setting.Bool("Off hand lighting",true));
 public final Setting.Num level=add(new Setting.Num("Light level",15,0,15,1));
 public HandLighting(){super("hand_lighting","Hand Lighting","Item illumination only. Bloom/outline/blur shaders are not implemented; no Iris pipeline hooks.",Category.SHADER_HANDS);enabled=false;}
 public int light(int original,boolean mainHand){return enabled&&(mainHand?main.value:off.value)?((int)level.value<<4)|((int)level.value<<20):original;}
}
