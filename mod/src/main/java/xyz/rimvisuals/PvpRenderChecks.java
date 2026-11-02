package xyz.rimvisuals;
/** Opt-in startup contracts for existing render paths; no gameplay/world validation claim. */
public final class PvpRenderChecks {
 public static void run(){
  var saved=xyz.rimvisuals.core.Modules.snapshot();
  try{
   var fade=xyz.rimvisuals.core.Modules.TOTEM_FADE;fade.enabled=true;fade.start.set(.6);fade.endOpacity.set(.15);
   if(Math.abs(fade.alpha(1)-.15)>.00001||fade.alpha(.4f)!=1)throw new IllegalStateException("Totem fade endpoints");
   var crystal=xyz.rimvisuals.core.Modules.CRYSTAL_FADE;crystal.enabled=true;crystal.opacity.set(.8);crystal.fadeIn.set(4);
   if(Math.abs(crystal.alpha(4)-.8)>.00001||crystal.alpha(2)<=0||crystal.alpha(2)>=.8)throw new IllegalStateException("Crystal fade curve");
   var type=net.minecraft.client.render.command.OrderedRenderCommandQueue.class;
   var calls=new java.util.concurrent.atomic.AtomicInteger();
   var real=(net.minecraft.client.render.command.OrderedRenderCommandQueue)java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{if(m.getName().equals("submitCustom"))calls.incrementAndGet();return null;});
   var wrapped=xyz.rimvisuals.modules.TotemFade.fading(real,.5f);
   var submit=java.util.Arrays.stream(type.getMethods()).filter(m->{var p=m.getParameterTypes();return p.length==9&&p[0]==net.minecraft.client.util.math.MatrixStack.class&&java.util.List.class.isAssignableFrom(p[6])&&p[7]==net.minecraft.client.render.RenderLayer.class;}).findFirst().orElseThrow(()->new IllegalStateException("1.21.11 item submission shape changed"));
   submit.invoke(wrapped,new net.minecraft.client.util.math.MatrixStack(),null,15728880,0,0,null,java.util.List.of(),net.minecraft.client.render.TexturedRenderLayers.getItemTranslucentCull(),null);
   if(calls.get()!=1)throw new IllegalStateException("Totem translucency was not routed");
   org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_PVP_RENDER_OK: Crystal/Totem curves and actual 1.21.11 totem submission routing; not a world test");
  }catch(ReflectiveOperationException e){throw new IllegalStateException("PvP renderer contract failed",e);}
  finally{xyz.rimvisuals.core.Modules.apply(saved);}
 }
}
