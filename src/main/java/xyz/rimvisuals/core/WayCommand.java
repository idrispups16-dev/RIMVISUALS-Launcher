package xyz.rimvisuals.core;
public record WayCommand(String name,double x,double y,double z) {
 public static WayCommand parse(String raw){
  var p=raw.trim().split("\\s+");
  if(p.length!=5||!p[0].equalsIgnoreCase(".way")||p[1].isBlank()||p[1].length()>40)throw new IllegalArgumentException(".way name X Y Z");
  try {double x=Double.parseDouble(p[2]),y=Double.parseDouble(p[3]),z=Double.parseDouble(p[4]);
   if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000||Math.abs(y)>4096)throw new NumberFormatException();
   return new WayCommand(p[1],x,y,z);
  }catch(NumberFormatException e){throw new IllegalArgumentException("Coordinates must be finite and inside world bounds");}
 }
}
