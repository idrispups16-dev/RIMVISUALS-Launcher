package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
/** Inspect bytecode without booting Minecraft: a version-specific deferred-item hook contract. */
class GuiRenderHookTest {
 @Test void reportDeferredItemSubmissionCalls() throws Exception {
  var calls=new ArrayList<String>();
  try(var in=getClass().getResourceAsStream("/net/minecraft/client/gui/render/GuiRenderer.class")) {
   assertNotNull(in,"named Minecraft client must be on test classpath");
   new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
    @Override public MethodVisitor visitMethod(int a,String name,String desc,String signature,String[] exceptions){
     if(!name.equals("prepareItem"))return null;
     return new MethodVisitor(Opcodes.ASM9){
      @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){calls.add(owner+"."+n+d);}
     };
    }
   },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  }
  assertFalse(calls.isEmpty(),"prepareItem mapping disappeared");
  System.out.println("RIM_GUI_ITEM_CALLS: "+String.join("; ",calls));
 }
}
