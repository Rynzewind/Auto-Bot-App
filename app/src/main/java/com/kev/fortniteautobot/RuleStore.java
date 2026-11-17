package com.kev.fortniteautobot;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public final class RuleStore {
    private static final Object LOCK=new Object();
    public static final class Variant {public final String key,name;Variant(String k,String n){key=k;name=n;}@Override public String toString(){return name;}}
    public static final class Step {public double x,y;public double[]rect;public String file;public long wait;public int[]pixels;}
    public static final class Rule {
        public String id,key="default",name="Screen 1",error="";public boolean enabled=true;public double threshold;public double[]region;public double aspect;public int rotation;public List<Step>steps=new ArrayList<>();public int[]pixels;
        public String identity(){return id+":"+key;}
        public String label(){return App.name(id)+" / "+name;}
        public boolean configured(){return error.isEmpty()&&pixels!=null&&MatchMath.region(region)&&aspect>1&&!steps.isEmpty();}
    }
    private final Context context;private final File override;private final Map<String,String>selection=new HashMap<>();
    public RuleStore(Context c){this(c,(File)null);}
    public RuleStore(Context c,File base){context=c;override=base;}
    public File root(){File f=override==null?new File(context.getFilesDir(),"rules"):override;f.mkdirs();return f;}
    public File dir(String id){App.index(id);File f=new File(root(),id);f.mkdirs();return f;}
    public File file(String id,String name)throws IOException{if(name.length()>128||!name.matches("[a-zA-Z0-9_.-]+")||name.equals(".")||name.equals(".."))throw new IOException("Invalid picture name");return new File(dir(id),name);}
    private JSONObject blank(String id,String key,String name)throws JSONException{return new JSONObject().put("variantKey",key).put("variantName",name).put("threshold",id.equals("leave")?0.65:0.90).put("steps",new JSONArray());}
    private JSONObject read(String id)throws Exception{
        File f=new File(dir(id),"rule.json");if(f.length()>2*1024*1024)throw new IOException("Setup document is too large");JSONObject raw=f.isFile()?new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8)):new JSONObject().put("enabled",true);
        if(!raw.has("variants")){JSONObject profile=f.isFile()?new JSONObject(raw.toString()):blank(id,"default","Screen 1");profile.remove("enabled");profile.put("variantKey","default").put("variantName","Screen 1");raw=new JSONObject().put("enabled",raw.optBoolean("enabled",true)).put("variants",new JSONArray().put(profile));}
        JSONArray variants=raw.getJSONArray("variants");if(variants.length()<1||variants.length()>16)throw new IOException("Use 1–16 screen variants per rule");Set<String>keys=new HashSet<>();
        for(int i=0;i<variants.length();i++){JSONObject p=variants.getJSONObject(i);String key=p.getString("variantKey"),name=p.optString("variantName","Screen "+(i+1));if(!key.matches("[a-zA-Z0-9_-]{1,64}")||!keys.add(key)||name.length()>40||name.trim().isEmpty())throw new IOException("Invalid or duplicate screen key/name");}
        return raw;
    }
    private void write(String id,JSONObject document)throws Exception{File to=new File(dir(id),"rule.json"),tmp=new File(dir(id),"rule-"+UUID.randomUUID()+".tmp");Files.write(tmp.toPath(),document.toString(2).getBytes(StandardCharsets.UTF_8));Files.move(tmp.toPath(),to.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    public JSONObject document(String id)throws Exception{synchronized(LOCK){return read(id);}}
    private String chosen(String id,JSONArray variants)throws JSONException{
        String key=selection.containsKey(id)?selection.get(id):App.prefs(context).getString("screen_"+id,"default");
        for(int i=0;i<variants.length();i++)if(variants.getJSONObject(i).getString("variantKey").equals(key)){selection.put(id,key);return key;}
        if(selection.containsKey(id))throw new JSONException("Selected screen was removed; recording cancelled");key=variants.getJSONObject(0).getString("variantKey");selection.put(id,key);return key;
    }
    public String selectedKey(String id)throws Exception{synchronized(LOCK){return chosen(id,read(id).getJSONArray("variants"));}}
    public List<Variant> variants(String id)throws Exception{synchronized(LOCK){List<Variant>out=new ArrayList<>();JSONArray a=read(id).getJSONArray("variants");for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);out.add(new Variant(p.getString("variantKey"),p.optString("variantName","Screen "+(i+1))));}return out;}}
    public void select(String id,String key)throws Exception{synchronized(LOCK){boolean found=false;for(Variant v:variants(id))if(v.key.equals(key))found=true;if(!found)throw new IOException("Screen no longer exists");selection.put(id,key);App.prefs(context).edit().putString("screen_"+id,key).apply();}}
    public void add(String id,String name)throws Exception{synchronized(LOCK){JSONObject d=read(id);JSONArray a=d.getJSONArray("variants");if(a.length()>=16)throw new IOException("Maximum 16 screen variants");String key="v"+UUID.randomUUID().toString().replace("-","");a.put(blank(id,key,cleanName(name)));write(id,d);select(id,key);}}
    private String cleanName(String name)throws IOException{String n=name==null?"":name.trim();if(n.isEmpty()||n.length()>40)throw new IOException("Use a screen name of 1–40 characters");return n;}
    public void rename(String id,String name)throws Exception{JSONObject p=json(id).put("variantName",cleanName(name));save(id,p);}
    public void removeSelected(String id)throws Exception{synchronized(LOCK){JSONObject d=read(id);JSONArray a=d.getJSONArray("variants");if(a.length()==1)throw new IOException("Keep at least one screen variant");String key=chosen(id,a);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("variantKey").equals(key)){a.remove(i);break;}write(id,d);select(id,a.getJSONObject(0).getString("variantKey"));}}
    public JSONObject json(String id)throws Exception{synchronized(LOCK){JSONObject d=read(id);JSONArray a=d.getJSONArray("variants");String key=chosen(id,a);for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if(p.getString("variantKey").equals(key))return new JSONObject(p.toString()).put("enabled",d.optBoolean("enabled",true));}throw new IOException("Screen missing");}}
    public void save(String id,JSONObject profile)throws Exception{synchronized(LOCK){JSONObject d=read(id);JSONArray a=d.getJSONArray("variants");String key=profile.optString("variantKey",chosen(id,a));JSONObject p=new JSONObject(profile.toString());p.remove("enabled");p.put("variantKey",key);boolean found=false;for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("variantKey").equals(key)){a.put(i,p);found=true;break;}if(!found)throw new IOException("Screen was removed; recording not saved");write(id,d);}}
    public void enabled(String id,boolean value)throws Exception{synchronized(LOCK){JSONObject d=read(id);d.put("enabled",value);write(id,d);}}
    static double[] array(JSONArray a)throws JSONException{if(a.length()!=4)throw new JSONException("Invalid region");double[]r=new double[4];for(int i=0;i<4;i++)r[i]=a.getDouble(i);if(!MatchMath.region(r))throw new JSONException("Region outside display");return r;}
    public Rule load(String id){try{return parse(id,json(id));}catch(Exception e){Rule r=new Rule();r.id=id;r.error=e.getMessage();return r;}}
    public List<Rule> all(String id){List<Rule>out=new ArrayList<>();try{JSONObject d=document(id);JSONArray a=d.getJSONArray("variants");for(int i=0;i<a.length();i++)out.add(parse(id,new JSONObject(a.getJSONObject(i).toString()).put("enabled",d.optBoolean("enabled",true))));}catch(Exception e){Rule r=new Rule();r.id=id;r.error=e.getMessage()==null?"Invalid setup":e.getMessage();out.add(r);}return out;}
    private Rule parse(String id,JSONObject j){Rule r=new Rule();r.id=id;r.threshold=id.equals("leave")?0.65:0.90;
        try{r.key=j.optString("variantKey","default");r.name=j.optString("variantName","Screen 1");r.enabled=j.optBoolean("enabled",true);r.threshold=j.optDouble("threshold",r.threshold);if(!Double.isFinite(r.threshold)||r.threshold<0.5||r.threshold>0.99)throw new IOException("Threshold must be 50–99%");if(!j.has("region"))return r;
            r.region=array(j.getJSONArray("region"));r.aspect=j.getDouble("aspect");r.rotation=j.getInt("rotation");if(!Double.isFinite(r.aspect)||r.aspect<=1||r.rotation<0||r.rotation>3)throw new IOException("Invalid display geometry");Bitmap b=App.decode(file(id,j.getString("watch")));r.pixels=App.vector(b);b.recycle();if(!MatchMath.hasDetail(r.pixels))throw new IOException("Watch picture is blank/flat; include text or an icon");
            JSONArray steps=j.optJSONArray("steps");if(steps!=null){if(steps.length()>30)throw new IOException("Maximum 30 taps per screen");for(int i=0;i<steps.length();i++){JSONObject o=steps.getJSONObject(i);Step s=new Step();s.x=o.getDouble("x");s.y=o.getDouble("y");if(!MatchMath.unit(s.x)||!MatchMath.unit(s.y))throw new IOException("Tap outside display");s.rect=array(o.getJSONArray("rect"));s.file=o.getString("file");s.wait=o.optLong("wait",600);if(s.wait<300||s.wait>15000)throw new IOException("Invalid tap timing");r.steps.add(s);}}
        }catch(Exception e){r.error=e.getMessage()==null?"Invalid setup":e.getMessage();r.pixels=null;}return r;
    }
    public void watch(String id,Bitmap frame,double[]rect,int rotation,double threshold)throws Exception{
        if(!MatchMath.region(rect)||frame.getWidth()<=frame.getHeight()||!Double.isFinite(threshold)||threshold<0.5||threshold>0.99)throw new IOException("Mark a landscape area and threshold 50–99%");
        String name="watch-"+UUID.randomUUID()+".png";Bitmap crop=App.crop(frame,rect);try{if(!MatchMath.hasDetail(App.vector(crop)))throw new IOException("Watch zone is blank/flat; include text or an icon, or check capture");App.png(crop,file(id,name));}finally{if(crop!=frame)crop.recycle();}
        JSONObject j=json(id);double aspect=(double)frame.getWidth()/frame.getHeight();if(j.has("aspect")&&(Math.abs(j.getDouble("aspect")-aspect)>0.01||j.optInt("rotation",rotation)!=rotation))j.put("steps",new JSONArray());
        j.put("watch",name).put("region",new JSONArray(rect)).put("rotation",rotation).put("aspect",aspect).put("threshold",threshold).put("sourceReset",false);save(id,j);
    }
}
