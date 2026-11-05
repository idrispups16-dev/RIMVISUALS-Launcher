package xyz.rimvisuals;
import xyz.rimvisuals.core.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Dev-only checks of the actual transformed third-party hooks, without joining a server. */
public final class HwIntegrationChecks {
 public static void run(){var snapshot=Modules.snapshot();try{
  var h=Modules.HW_HELPER;h.enabled=true;h.auctionPrice.value=true;h.zakoHealth.value=true;ServerHelpers.updateConnection("hub.holyworld.me");
  if(!HwIntegrations.auction()||!HwIntegrations.health())throw new IllegalStateException("HW integration enable failed");
  var saved=Modules.snapshot();h.auctionPrice.value=false;h.zakoHealth.value=false;Modules.apply(saved);
  if(!h.auctionPrice.value||!h.zakoHealth.value)throw new IllegalStateException("HW integration CFG failed");
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("auctionprice")){
   var cls=Class.forName("vupsenn.auctionprice.v12111fabric.Mode");
   var method=java.util.Arrays.stream(cls.getDeclaredMethods()).filter(m->m.getName().contains("rimAuction")).findFirst().orElseThrow();method.setAccessible(true);
   h.auctionPrice.value=false;var off=new CallbackInfoReturnable<Boolean>("test",true);method.invoke(null,off);if(!off.isCancelled()||!off.getReturnValue())throw new IllegalStateException("Auction off hook failed");
   h.auctionPrice.value=true;var on=new CallbackInfoReturnable<Boolean>("test",true);method.invoke(null,on);if(on.isCancelled())throw new IllegalStateException("Auction on must preserve original server checks");
  }
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("zakohealthindicator")){
   var cfg=Class.forName("me.zyouime.zakohealthindicator.config.ModConfig");
   var cls=Class.forName("me.zyouime.zakohealthindicator.render.HealthHudRender");
   var method=java.util.Arrays.stream(cls.getDeclaredMethods()).filter(m->m.getName().contains("rimEnabled")).findFirst().orElseThrow();method.setAccessible(true);
   h.zakoHealth.value=false;if(HwIntegrations.health())throw new IllegalStateException("Zako off hook failed");
   h.zakoHealth.value=true;if(!HwIntegrations.health()||method.getReturnType()!=boolean.class)throw new IllegalStateException("Zako on hook failed");
  }
  h.enabled=false;if(HwIntegrations.auction()||HwIntegrations.health())throw new IllegalStateException("Parent off failed");
  org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_HW_ADDONS_OK: CFG, switches and available transformed optional hooks; no public-server test");
 }catch(ReflectiveOperationException e){throw new IllegalStateException("HW add-on hook failed",e);}finally{ServerHelpers.clear();Modules.apply(snapshot);}}
}

