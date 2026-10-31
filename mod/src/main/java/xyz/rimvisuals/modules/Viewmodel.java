package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
public final class Viewmodel extends Module {
 public final class Pose {
  public final Setting.Num x,y,z,scale,rx,ry,rz;
  Pose(String prefix){x=add(new Setting.Num(prefix+" X",0,-2,2,.01));y=add(new Setting.Num(prefix+" Y",0,-2,2,.01));z=add(new Setting.Num(prefix+" Z",0,-2,2,.01));scale=add(new Setting.Num(prefix+" Scale",1,.1,2,.01));rx=add(new Setting.Num(prefix+" Rotation X",0,-180,180,1));ry=add(new Setting.Num(prefix+" Rotation Y",0,-180,180,1));rz=add(new Setting.Num(prefix+" Rotation Z",0,-180,180,1));}
 }
 public final Pose main=new Pose("Main hand"),off=new Pose("Off hand");
 public final Setting.Num swordZ=add(new Setting.Num("Sword Z",0,-1,1,.01)),totemZ=add(new Setting.Num("Totem Z",0,-1,1,.01)),crystalZ=add(new Setting.Num("Crystal Z",0,-1,1,.01)),foodZ=add(new Setting.Num("Food Z",0,-1,1,.01));
 public final Pose sword=new Pose("Sword"),totem=new Pose("Totem"),crystal=new Pose("Crystal"),food=new Pose("Food"),other=new Pose("Other items");
 public final Setting.Bool itemOverrides=add(new Setting.Bool("Item category transforms",true));
 public final Setting.Bool smooth=add(new Setting.Bool("Smooth transitions",true));
 public final Setting.Mode preset=add(new Setting.Mode("Preset",0,"Default","Compact","Wide","Reset to Default","Custom"));
 private int applied=-1;
 private final float[][] values={new float[]{0,0,0,1,0,0,0},new float[]{0,0,0,1,0,0,0}};
 private final long[] last={0,0};
 public Viewmodel(){super("viewmodel","Viewmodel Editor","Per-hand position, scale, rotation and item depth; cosmetic only",Category.VIEWMODEL);enabled=false;}
 public void seedPreset(){applied=preset.index;}
 public void presets(){if(applied==preset.index)return;applied=preset.index;if(applied==4)return;for(Pose p:new Pose[]{main,off}){p.x.set(applied==2?.25:0);p.y.set(applied==1?-.18:0);p.z.set(applied==1?-.2:0);p.scale.set(applied==1?.8:1);p.rx.set(0);p.ry.set(0);p.rz.set(0);}swordZ.set(0);totemZ.set(0);crystalZ.set(0);foodZ.set(0);}
 public void apply(MatrixStack m,boolean mainHand,ItemStack item){if(!enabled)return;long start=xyz.rimvisuals.core.PerformanceMonitor.begin();try{Pose p=mainHand?main:off;int i=mainHand?0:1;long now=System.nanoTime();float dt=last[i]==0?1:Math.min(.1f,(now-last[i])/1e9f);last[i]=now;float k=!smooth.value||xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value?1:(float)xyz.rimvisuals.core.VisualMath.smoothing(dt);
  double extra=item.isIn(ItemTags.SWORDS)?swordZ.value:item.isOf(Items.TOTEM_OF_UNDYING)?totemZ.value:item.isOf(Items.END_CRYSTAL)?crystalZ.value:item.contains(net.minecraft.component.DataComponentTypes.FOOD)?foodZ.value:0;
  Pose category=item.isIn(ItemTags.SWORDS)?sword:item.isOf(Items.TOTEM_OF_UNDYING)?totem:item.isOf(Items.END_CRYSTAL)?crystal:item.contains(net.minecraft.component.DataComponentTypes.FOOD)?food:other;
  boolean override=itemOverrides.value&&!item.isEmpty();
  double[] target={p.x.value+(override?category.x.value:0),p.y.value+(override?category.y.value:0),p.z.value+extra+(override?category.z.value:0),p.scale.value*(override?category.scale.value:1),p.rx.value+(override?category.rx.value:0),p.ry.value+(override?category.ry.value:0),p.rz.value+(override?category.rz.value:0)};float[] v=values[i];for(int j=0;j<v.length;j++)v[j]+=(target[j]-v[j])*k;
  m.translate(v[0],v[1],v[2]);m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(v[4]));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(v[5]));m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(v[6]));m.scale(v[3],v[3],v[3]);
 }finally{xyz.rimvisuals.core.PerformanceMonitor.end("Viewmodel",start);}}
}
