package com.kev.fortniteautobot;
import java.io.*;
import java.util.*;
import java.util.zip.*;
/** Streaming, bounded setup archive extraction; no Android dependencies. */
public final class BackupArchive {
    public static boolean allowed(String name){return name.equals("format.json")||name.matches("rules/(ready|leave|party)/[a-zA-Z0-9_-]+\\.(json|png)");}
    public static void extract(InputStream input,File stage,int countLimit,long byteLimit)throws IOException{
        long total=0;int count=0;Set<String>seen=new HashSet<>();
        try(ZipInputStream zip=new ZipInputStream(input)){ZipEntry item;byte[]buffer=new byte[8192];while((item=zip.getNextEntry())!=null){String name=item.getName();if(item.isDirectory())continue;
            if(!allowed(name))throw new IOException("Unexpected backup entry");if(!seen.add(name)||++count>countLimit)throw new IOException("Duplicate or excessive backup entries");
            File out=new File(stage,name);if(!out.getCanonicalPath().startsWith(stage.getCanonicalPath()+File.separator))throw new IOException("Invalid archive path");out.getParentFile().mkdirs();
            try(OutputStream stream=new FileOutputStream(out)){int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>byteLimit)throw new IOException("Backup is too large");stream.write(buffer,0,n);}}
        }}
    }
}
