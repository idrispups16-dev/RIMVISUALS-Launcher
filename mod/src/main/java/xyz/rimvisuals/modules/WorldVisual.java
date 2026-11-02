package xyz.rimvisuals.modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.*;
import xyz.rimvisuals.core.*;
/** Bounded camera-relative geometry; uses vanilla depth-tested render layers and owns no GPU resources. */
public final class WorldVisual extends xyz.rimvisuals.core.Module {
 public enum Kind {HAT,BLOCK}
 private final Kind kind;
 public final Setting.Color color=add(new Setting.Color("Color",0xa78bfa));
 public final Setting.Num opacity=add(new Setting.Num("Opacity",.35,.05,1,.05)),radius=add(new Setting.Num("Hat radius",.65,.3,1,.05)),height=add(new Setting.Num("Hat height",.3,.1,.6,.05));
 public final Setting.Bool others=add(new Setting.Bool("Other players",false));
 private static final float[] SIN=new float[33],COS=new float[33];
 private static final int[][] FACES={{0,2,3,1},{4,5,7,6},{0,1,5,4},{2,6,7,3},{0,4,6,2},{1,3,7,5}};
 static{for(int i=0;i<=32;i++){SIN[i]=(float)Math.sin(i*Math.PI/16);COS[i]=(float)Math.cos(i*Math.PI/16);}}
 public WorldVisual(String id,String name,Kind kind){super(id,name,kind==Kind.HAT?"Depth-tested conical player hat; third-person self, optional nearby players. Safe Mode suppresses geometry":"Translucent selected-block shape bounding box; no interaction or hitbox changes. Safe Mode suppresses geometry",Category.VISUALS);this.kind=kind;enabled=false;radius.hidden=height.hidden=others.hidden=kind!=Kind.HAT;}
 public static int segments(boolean ultra){return ultra?32:16;}
 public void draw(MatrixStack matrices,OrderedRenderCommandQueue queue,Vec3d camera){
  if(!enabled||Modules.PERFORMANCE.safe.value)return;var mc=MinecraftClient.getInstance();if(mc.world==null||mc.player==null)return;
  long start=PerformanceMonitor.begin();
  try {
   if(kind==Kind.BLOCK){if(!(mc.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;var pos=hit.getBlockPos();var shape=mc.world.getBlockState(pos).getOutlineShape(mc.world,pos);if(shape.isEmpty())return;Box box=shape.getBoundingBox().offset(pos).expand(.002).offset(-camera.x,-camera.y,-camera.z);box(matrices,queue,box);}
   else {int count=0,cap=Modules.PERFORMANCE.preset.index==2?8:4;for(var player:mc.world.getPlayers()){
    if(player==mc.player&&mc.options.getPerspective().isFirstPerson())continue;
    if(player!=mc.player&&!others.value)continue;if(player.squaredDistanceTo(mc.player)>32*32)continue;
    Vec3d p=player.getLerpedPos(mc.getRenderTickCounter().getTickProgress(false));hat(matrices,queue,p.x-camera.x,p.y+player.getHeight()+.1-camera.y,p.z-camera.z);if(++count>=cap)break;
   }}
  }finally{PerformanceMonitor.end(name,start);}
 }
 private void hat(MatrixStack m,OrderedRenderCommandQueue q,double x,double y,double z){
  int step=Modules.PERFORMANCE.preset.index==2?1:2,col=Gfx.withAlpha(color.rgb,(float)opacity.value);float rad=(float)radius.value,h=(float)height.value;
  m.push();m.translate(x,y,z);q.submitCustom(m,RenderLayers.debugQuads(),(entry,v)->{var matrix=entry.getPositionMatrix();for(int i=0;i<32;i+=step){v.vertex(matrix,0,h,0).color(col);v.vertex(matrix,COS[i+step]*rad,0,SIN[i+step]*rad).color(col);v.vertex(matrix,COS[i]*rad,0,SIN[i]*rad).color(col);v.vertex(matrix,0,h,0).color(col);}});m.pop();
 }
 private void box(MatrixStack m,OrderedRenderCommandQueue q,Box b){int col=Gfx.withAlpha(color.rgb,(float)opacity.value);
  q.submitCustom(m,RenderLayers.debugQuads(),(entry,v)->{var matrix=entry.getPositionMatrix();for(int[] face:FACES)for(int i:face)v.vertex(matrix,(float)((i&1)==0?b.minX:b.maxX),(float)((i&2)==0?b.minY:b.maxY),(float)((i&4)==0?b.minZ:b.maxZ)).color(col);});
 }
}
