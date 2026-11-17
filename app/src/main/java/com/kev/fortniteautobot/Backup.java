package com.kev.fortniteautobot;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;
public final class Backup {
    private static void entry(ZipOutputStream zip,String name,byte[]bytes)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();}
    public static void exportSetup(Context c,OutputStream output)throws Exception{
        if(output==null)throw new IOException("No export destination");RuleStore store=new RuleStore(c);
        try(ZipOutputStream zip=new ZipOutputStream(output)){
            entry(zip,"format.json","{\"app\":\"Auto Bot\",\"version\":2}".getBytes(StandardCharsets.UTF_8));
            for(String id:App.IDS){JSONObject document=store.document(id);entry(zip,"rules/"+id+"/rule.json",document.toString(2).getBytes(StandardCharsets.UTF_8));Set<String>names=new LinkedHashSet<>();JSONArray variants=document.getJSONArray("variants");for(int v=0;v<variants.length();v++){JSONObject j=variants.getJSONObject(v);if(j.has("source"))names.add(j.getString("source"));if(j.has("watch"))names.add(j.getString("watch"));JSONArray steps=j.optJSONArray("steps");if(steps!=null)for(int i=0;i<steps.length();i++){JSONObject step=steps.getJSONObject(i);names.add(step.getString("file"));if(step.has("screen"))names.add(step.getString("screen"));}}for(String name:names)entry(zip,"rules/"+id+"/"+name,Files.readAllBytes(store.file(id,name).toPath()));}
        }
    }
    public static void importSetup(Context c,InputStream input)throws Exception{
        if(input==null)throw new IOException("No import file");File stage=new File(c.getFilesDir(),"import-"+UUID.randomUUID());stage.mkdirs();
        try{
            BackupArchive.extract(input,stage,2048,200*1024*1024);
            JSONObject format=new JSONObject(new String(Files.readAllBytes(new File(stage,"format.json").toPath()),StandardCharsets.UTF_8));int version=format.getInt("version");if(!"Auto Bot".equals(format.getString("app"))||(version!=1&&version!=2))throw new IOException("Unsupported backup format");
            RuleStore candidate=new RuleStore(c,new File(stage,"rules"));for(String id:App.IDS){if(!new File(candidate.dir(id),"rule.json").isFile())throw new IOException("Backup must include all three rules");for(RuleStore.Rule r:candidate.all(id))if(!r.error.isEmpty())throw new IOException(r.label()+": "+r.error);}
            File live=new File(c.getFilesDir(),"rules"),previous=new File(c.getFilesDir(),"previous-rules-"+UUID.randomUUID());boolean moved=live.exists();if(moved)Files.move(live.toPath(),previous.toPath());try{Files.move(new File(stage,"rules").toPath(),live.toPath());}catch(Exception e){if(moved)Files.move(previous.toPath(),live.toPath());throw e;}
        }finally{remove(stage);}
    }
    private static void remove(File f){File[]children=f.listFiles();if(children!=null)for(File child:children)remove(child);f.delete();}
}
