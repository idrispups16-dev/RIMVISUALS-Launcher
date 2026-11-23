package xyz.rimvisuals.core;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.util.math.Vec3d;
/** Depth-tested coordinate beacons, never changes blocks or entity collision. */
public final class WaypointRenderer {
 private WaypointRenderer(){}
 public static void draw(MatrixStack matrices,OrderedRenderCommandQueue queue,Vec3d camera){
  var mc=MinecraftClient.getInstance();if(!Modules.LOCAL_MARKS.isActive()||mc.world==null||mc.player==null)return;
  String world=LocalData.world(mc),dim=LocalData.dimension(mc);int count=0;
  for(var m:LocalData.marks())if(m.world().equals(world)&&m.dimension().equals(dim)&&++count<=32){
   double x=m.x()-camera.x,y=m.y()-camera.y,z=m.z()-camera.z;if(x*x+y*y+z*z>512*512)continue;int color=Gfx.withAlpha(m.rgb(),.8f);
   queue.submitCustom(matrices,RenderLayers.debugQuads(),(entry,v)->{var mat=entry.getPositionMatrix();float a=.12f;float xx=(float)x,yy=(float)y,zz=(float)z;
    v.vertex(mat,xx-a,yy,zz).color(color);v.vertex(mat,xx+a,yy,zz).color(color);v.vertex(mat,xx+a,yy+4,zz).color(color);v.vertex(mat,xx-a,yy+4,zz).color(color);
    v.vertex(mat,xx,yy,zz-a).color(color);v.vertex(mat,xx,yy,zz+a).color(color);v.vertex(mat,xx,yy+4,zz+a).color(color);v.vertex(mat,xx,yy+4,zz-a).color(color);
   });
  }
 }
}
