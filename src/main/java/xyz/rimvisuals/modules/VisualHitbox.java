package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.*;
import xyz.rimvisuals.core.*;
import java.util.*;
import java.lang.ref.WeakReference;
/** Depth-tested boxes copied from real bounds; never writes entity bounds or attack state. */
public final class VisualHitbox extends xyz.rimvisuals.core.Module {
 public final Setting.Bool players=add(new Setting.Bool("Players",true)),mobs=add(new Setting.Bool("Mobs",true)),targetOnly=add(new Setting.Bool("Selected target only",true));
 public final Setting.Color color=add(new Setting.Color("Color",0x7dd3fc));
 public final Setting.Num opacity=add(new Setting.Num("Opacity",.65,.1,1,.05)),thickness=add(new Setting.Num("Width in blocks",.015,.005,.06,.005)),distance=add(new Setting.Num("Range",24,4,64,1));
 private final List<WeakReference<LivingEntity>> candidates=new ArrayList<>();private int ticks;
 private static final int[][] EDGES={{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
 private static final int[][] FACES={{0,2,3,1},{4,5,7,6},{0,1,5,4},{2,6,7,3},{0,4,6,2},{1,3,7,5}};
 public VisualHitbox(){super("visual_hitbox","Visual Hitbox","Depth-tested visual outlines of existing player/mob bounds; width is in world units. No hitbox, reach or server changes",Category.VISUALS);enabled=false;}
 public static int cap(boolean safe,boolean ultra){return safe?2:ultra?8:4;}
 private boolean valid(LivingEntity e,MinecraftClient mc){return e!=null&&e!=mc.player&&e.isAlive()&&!e.isRemoved()&&(e instanceof PlayerEntity?players.value:mobs.value&&e instanceof net.minecraft.entity.mob.MobEntity)&&e.squaredDistanceTo(mc.player)<=distance.value*distance.value;}
 public void tick(MinecraftClient mc){if(!isActive()||mc.player==null||mc.world==null){candidates.clear();return;}if(++ticks%2!=0)return;candidates.clear();
  if(targetOnly.value){LivingEntity e=mc.targetedEntity instanceof LivingEntity l?l:CombatEvents.recentTarget(2);if(valid(e,mc))candidates.add(new WeakReference<>(e));return;}
  int cap=cap(Modules.PERFORMANCE.safe.value,Modules.PERFORMANCE.preset.index==2);
  for(var e:mc.world.getEntitiesByClass(LivingEntity.class,mc.player.getBoundingBox().expand(distance.value),e->true))if(valid(e,mc)){candidates.add(new WeakReference<>(e));if(candidates.size()>=cap)break;}
 }
 public void draw(MatrixStack m,OrderedRenderCommandQueue q,Vec3d camera){if(!isActive())return;var mc=MinecraftClient.getInstance();if(mc.player==null||mc.world==null)return;long start=PerformanceMonitor.begin();
  try{int n=0,cap=cap(Modules.PERFORMANCE.safe.value,Modules.PERFORMANCE.preset.index==2);for(var ref:candidates){var e=ref.get();if(!valid(e,mc))continue;
   Vec3d p=e.getLerpedPos(mc.getRenderTickCounter().getTickProgress(false));Box b=e.getBoundingBox().offset(p.x-e.getX()-camera.x,p.y-e.getY()-camera.y,p.z-e.getZ()-camera.z);emit(m,q,b);if(++n>=cap)break;
  }}finally{PerformanceMonitor.end("Visual Hitbox",start);}
 }
 private void emit(MatrixStack m,OrderedRenderCommandQueue q,Box b){float r=(float)thickness.value/2;int col=Gfx.withAlpha(color.rgb,(float)opacity.value);
  q.submitCustom(m,RenderLayers.debugQuads(),(entry,v)->{var matrix=entry.getPositionMatrix();for(int[] edge:EDGES){int a=edge[0],z=edge[1];double ax=(a&1)==0?b.minX:b.maxX,ay=(a&2)==0?b.minY:b.maxY,az=(a&4)==0?b.minZ:b.maxZ,bx=(z&1)==0?b.minX:b.maxX,by=(z&2)==0?b.minY:b.maxY,bz=(z&4)==0?b.minZ:b.maxZ;double x0=Math.min(ax,bx)-r,x1=Math.max(ax,bx)+r,y0=Math.min(ay,by)-r,y1=Math.max(ay,by)+r,z0=Math.min(az,bz)-r,z1=Math.max(az,bz)+r;
   for(int[] face:FACES)for(int i:face)v.vertex(matrix,(float)((i&1)==0?x0:x1),(float)((i&2)==0?y0:y1),(float)((i&4)==0?z0:z1)).color(col);
  }});
 }
}
