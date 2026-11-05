package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Version contract: every vanilla one-argument swing reaches our two-argument hook. */
class SwingHookTest {
 @Test void allVanillaSwingRequestsReachTheObservedOverload() throws Exception {
  var calls=new ArrayList<String>();var methods=new HashSet<String>();
  try(var in=getClass().getResourceAsStream("/net/minecraft/entity/LivingEntity.class")){
   assertNotNull(in);
   new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
    public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){
     methods.add(n+d);
     if(!n.equals("swingHand")||!d.equals("(Lnet/minecraft/util/Hand;)V"))return null;
     return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){calls.add(name+desc);}};
    }
   },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  }
  assertTrue(methods.contains("swingHand(Lnet/minecraft/util/Hand;Z)V"));
  assertTrue(calls.contains("swingHand(Lnet/minecraft/util/Hand;Z)V"),"the simple swing must delegate to the hook, regardless of held item");
 }
}
