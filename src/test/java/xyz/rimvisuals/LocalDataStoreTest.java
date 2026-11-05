package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.rimvisuals.core.LocalDataStore;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalDataStoreTest {
 @TempDir Path dir;
 @Test void friendsAndMarkersSurviveRestartAndDelete()throws Exception{
  var file=dir.resolve("local.json");var s=new LocalDataStore(file);
  assertTrue(s.addFriend("Test_Player"));assertFalse(s.addFriend("test_player"));assertFalse(s.addFriend("bad name!"));
  var m=s.addMark("Home","local:test","minecraft:overworld",1.5,64,-3);
  var loaded=new LocalDataStore(file);assertEquals(s.friends(),loaded.friends());assertEquals(m,loaded.marks().getFirst());
  assertTrue(loaded.removeMark(m.id()));assertTrue(loaded.removeFriend("TEST_PLAYER"));
  var again=new LocalDataStore(file);assertTrue(again.friends().isEmpty());assertTrue(again.marks().isEmpty());
 }
 @Test void corruptDataIsPreservedAndNonfiniteMarksRejected()throws Exception{
  var file=dir.resolve("local.json");Files.writeString(file,"broken");assertThrows(java.io.IOException.class,()->new LocalDataStore(file));assertEquals("broken",Files.readString(file));
  var s=new LocalDataStore(dir.resolve("valid.json"));assertThrows(java.io.IOException.class,()->s.addMark("Home","w","d",Double.NaN,1,1));assertTrue(s.marks().isEmpty());
 }
}
