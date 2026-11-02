package xyz.rimvisuals.core;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Local-only names and coordinate marks. No network requests, cloud accounts or friendship claims. */
public final class LocalDataStore {
 public record Mark(String id,String name,String world,String dimension,double x,double y,double z,int rgb){}
 private final Path file;
 private final TreeSet<String> friends=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
 private final List<Mark> marks=new ArrayList<>();
 public LocalDataStore(Path file)throws IOException{this.file=file;if(!Files.exists(file))return;
  try{
   if(Files.size(file)>1024*1024)throw new IOException("Local data file is too large");
   var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   if(root.get("schema").getAsInt()!=1)throw new IOException("Unsupported local data schema");
   for(var value:root.getAsJsonArray("friends")){String n=value.getAsString();if(validName(n))friends.add(n);}
   for(var value:root.getAsJsonArray("marks")){var m=new Gson().fromJson(value,Mark.class);if(m.id!=null&&m.name!=null&&m.world!=null&&m.dimension!=null&&Double.isFinite(m.x)&&Double.isFinite(m.y)&&Double.isFinite(m.z))marks.add(m);}
  }catch(RuntimeException e){throw new IOException("Invalid local data; original file preserved",e);}
 }
 public static boolean validName(String n){return n!=null&&n.matches("[A-Za-z0-9_]{3,16}");}
 public List<String> friends(){return List.copyOf(friends);}
 public List<Mark> marks(){return List.copyOf(marks);}
 public boolean addFriend(String name)throws IOException{
  if(!validName(name)||friends.size()>=1000||!friends.add(name))return false;
  try{save();return true;}catch(IOException e){friends.remove(name);throw e;}
 }
 public boolean removeFriend(String name)throws IOException{String old=friends.stream().filter(n->n.equalsIgnoreCase(name)).findFirst().orElse(null);if(old==null)return false;friends.remove(old);try{save();return true;}catch(IOException e){friends.add(old);throw e;}}
 public Mark addMark(String name,String world,String dimension,double x,double y,double z)throws IOException{
  if(name==null||name.isBlank()||name.length()>40||marks.size()>=1000||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IOException("Invalid marker");
  var m=new Mark(UUID.randomUUID().toString(),name,world,dimension,x,y,z,0x60a5fa);marks.add(m);try{save();return m;}catch(IOException e){marks.remove(m);throw e;}
 }
 public boolean removeMark(String id)throws IOException{for(int i=0;i<marks.size();i++)if(marks.get(i).id.equals(id)){var m=marks.remove(i);try{save();return true;}catch(IOException e){marks.add(i,m);throw e;}}return false;}
 public void save()throws IOException{
  var root=new JsonObject();root.addProperty("schema",1);root.add("friends",new Gson().toJsonTree(friends));root.add("marks",new Gson().toJsonTree(marks));
  Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"rim-local-",".tmp");
  try{Files.writeString(tmp,root.toString());try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
 }
}
