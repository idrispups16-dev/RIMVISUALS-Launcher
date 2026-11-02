package xyz.rimvisuals.core;

import java.net.IDN;
import java.util.Locale;

/** Bounded literal parsing, with no regex supplied by servers or configs. */
public final class HelperData {
 private HelperData() {}
 public static String clean(String s) {
  if(s==null)return "";
  StringBuilder b=new StringBuilder();
  for(int i=0;i<s.length()&&i<1024&&b.length()<192;i++){
   char c=s.charAt(i);if(c=='§'&&i+1<s.length()){i++;continue;}if(c>=32&&c!=127)b.append(c);
  }return b.toString().strip();
 }
 public static String host(String address){
  if(address==null||address.length()>253)return "";
  String a=address.strip().toLowerCase(Locale.ROOT);int colon=a.indexOf(':');
  if(colon>=0){if(a.indexOf(':',colon+1)>=0)return "";String port=a.substring(colon+1);try{int n=Integer.parseInt(port);if(n<1||n>65535)return "";}catch(NumberFormatException e){return "";}a=a.substring(0,colon);}
  if(a.endsWith("."))a=a.substring(0,a.length()-1);
  try{return IDN.toASCII(a,IDN.USE_STD3_ASCII_RULES);}catch(IllegalArgumentException e){return "";}
 }
 public static boolean matches(String address,boolean ft,String aliases){
  String h=host(address);if(h.isEmpty())return false;
  String[] roots=ft?new String[]{"funtime.su"}:new String[]{"holyworld.ru","holyworld.me"};
  for(String r:roots)if(h.equals(r)||h.endsWith("."+r))return true;
  if(aliases!=null&&aliases.length()<=192)for(String r:aliases.split(",",-1))if(!host(r).isEmpty()&&h.equals(host(r)))return true;
  return false;
 }
 public static String after(String message,String prefix){
  String p=clean(prefix),s=clean(message);if(p.isEmpty()||!s.startsWith(p))return "";return s.substring(p.length()).strip();
 }
 /** Rule grammar: configured literal prefix followed by seconds or mm:ss, then optional text. */
 public static int seconds(String message,String prefix){
  String s=after(message,prefix);if(s.isEmpty())return -1;
  int end=s.indexOf(' ');String token=end<0?s:s.substring(0,end);if(token.length()>8)return -1;
  try{String[] a=token.split(":",-1);int n;
   if(a.length==1)n=Integer.parseInt(a[0]);
   else if(a.length==2){int m=Integer.parseInt(a[0]),v=Integer.parseInt(a[1]);if(m<0||v<0||v>59)return -1;n=Math.addExact(Math.multiplyExact(m,60),v);}else return -1;
   return n>=0&&n<=86400?n:-1;
  }catch(ArithmeticException|NumberFormatException e){return -1;}
 }
 public static final class Session {
  public String event="",region="",action="",timerSource="";public long until;private String last="";private long lastAt;
  public void clear(){event=region=action=timerSource=last="";until=lastAt=0;}
  public boolean accept(String raw,String eventPrefix,String timerPrefix,String regionPrefix,long now){
   String s=clean(raw);if(s.isEmpty())return false;
   if(s.equals(last)&&now-lastAt<2_000_000_000L)return false;
   last=s;lastAt=now;boolean changed=false;
   String e=after(s,eventPrefix);if(!e.isEmpty()&&!e.equals(event)){event=e;changed=true;}
   String r=after(s,regionPrefix);if(!r.isEmpty())region=r;
   int seconds=seconds(s,timerPrefix);if(seconds>=0){until=now+seconds*1_000_000_000L;timerSource=s;changed=true;}
   return changed;
  }
  public long remaining(long now){return Math.max(0,(until-now+999_999_999L)/1_000_000_000L);}
 }
}
