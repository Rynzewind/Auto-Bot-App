package com.kev.fortniteautobot;
import android.content.Context;
import java.nio.file.*;
import org.json.*;
public final class VariantStorageTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  java.io.File home=Files.createTempDirectory("autobot-screen-storage-").toFile();Context context=new Context(home);RuleStore store=new RuleStore(context);
  java.io.File old=new java.io.File(store.dir("ready"),"rule.json");String legacy="{\"enabled\":false,\"threshold\":0.91,\"source\":\"old.png\",\"watch\":\"old-watch.png\",\"steps\":[{\"x\":0.1,\"y\":0.2}]}";Files.write(old.toPath(),legacy.getBytes("UTF-8"));
  JSONObject original=store.json("ready");check(original.getString("variantKey").equals("default"),"legacy setup becomes Screen 1");check(original.getString("watch").equals("old-watch.png"),"legacy screenshot retained");check(new String(Files.readAllBytes(old.toPath()),"UTF-8").equals(legacy),"read-only migration does not rewrite the saved file");
  store.add("ready","Alternate lobby");String second=store.selectedKey("ready");JSONObject alternate=store.json("ready").put("source","alternate.png").put("steps",new JSONArray().put(new JSONObject().put("x",0.8).put("y",0.9)));store.save("ready",alternate);
  store.select("ready","default");JSONObject first=store.json("ready");check(first.getString("source").equals("old.png")&&first.getJSONArray("steps").getJSONObject(0).getDouble("x")==0.1,"second screen cannot overwrite first tap map");check(!first.getBoolean("enabled"),"migration retains disabled rule choice");
  RuleStore frozen=new RuleStore(context);frozen.json("ready");new RuleStore(context).select("ready",second);frozen.save("ready",frozen.json("ready").put("steps",new JSONArray().put(new JSONObject().put("x",0.3))));
  RuleStore current=new RuleStore(context);check(current.json("ready").getJSONArray("steps").getJSONObject(0).getDouble("x")==0.8,"recording stays bound to its original selected screen");
  store.enabled("ready",true);check(current.json("ready").getBoolean("enabled"),"on/off applies to the parent rule");
  check(store.document("ready").getJSONArray("variants").length()==2,"export document includes both screen variants");
  current.select("ready","default");current.removeSelected("ready");boolean rejected=false;try{frozen.json("ready");}catch(JSONException expected){rejected=true;}check(rejected,"deleted screen cannot silently redirect an in-flight save to another map");
  boolean keptLast=false;try{current.removeSelected("ready");}catch(java.io.IOException expected){keptLast=true;}check(keptLast,"cannot remove the final screen");
  System.out.println("VariantStorageTest PASS: real RuleStore migration, source/tap preservation, independent maps, frozen recording selection, parent switches, export catalog, deleted-screen cancellation.");
 }
}
