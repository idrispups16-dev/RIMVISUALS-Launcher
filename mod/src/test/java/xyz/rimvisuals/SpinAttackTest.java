package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import xyz.rimvisuals.modules.SwingAnimations;
import static org.junit.jupiter.api.Assertions.*;
class SpinAttackTest {
 @Test void attackingAnimatesAnItemWhenVanillaProgressIsZero(){var s=new SwingAnimations();s.enabled=true;s.style.index=4;s.attack(true,1000);var m=new MatrixStack();s.apply(m,1,0,true,150_001_000L);assertFalse(m.peek().getPositionMatrix().equals(new Matrix4f(),.00001f));}
 @Test void spinDoesNotReturnToRestAtTheMiddleOfTheHit(){
  var swing=new SwingAnimations();swing.enabled=true;swing.style.index=4;swing.easing.index=0;
  var matrix=new MatrixStack();assertTrue(swing.apply(matrix,1,.5f,true));
  assertFalse(matrix.peek().getPositionMatrix().equals(new Matrix4f(),.00001f),"a 360 degree peak hides the custom pose exactly at mid-hit");
 }
}
