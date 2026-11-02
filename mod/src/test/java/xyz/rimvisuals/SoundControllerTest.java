package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.modules.SoundController;
import net.minecraft.sound.SoundCategory;
import static org.junit.jupiter.api.Assertions.*;
class SoundControllerTest {
 @Test void categoriesMultiplyAndDisabledPreservesInput(){var s=new SoundController();s.music.set(0);assertEquals(.8f,s.scale(.8f,SoundCategory.MUSIC));s.enabled=true;s.overall.set(.5);assertEquals(0,s.scale(.8f,SoundCategory.MUSIC));assertEquals(.4f,s.scale(.8f,SoundCategory.PLAYERS),1e-6);s.weather.set(.5);assertEquals(.2f,s.scale(.8f,SoundCategory.WEATHER),1e-6);}
}
