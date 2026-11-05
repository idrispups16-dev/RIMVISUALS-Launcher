package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class HitboxReadOnlyTest {
 @Test void overlayHasNoEntityMutationOrNetworkCalls()throws Exception{
  var forbidden=Set.of("setBoundingBox","setDimensions","setPosition","attackEntity","clickSlot","sendPacket");var calls=new ArrayList<String>();
  try(var in=getClass().getResourceAsStream("/xyz/rimvisuals/modules/VisualHitbox.class")){
   assertNotNull(in);new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(forbidden.contains(name))calls.add(owner+"."+name);}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  }assertTrue(calls.isEmpty(),calls.toString());
 }
}
