package com.kev.fortniteautobot;
import android.content.*;
import android.graphics.Bitmap;
public final class App {
 private static final java.util.Map<Context,SharedPreferences>prefs=new java.util.HashMap<>();
 public static int index(String id){if(!id.equals("ready")&&!id.equals("leave")&&!id.equals("party"))throw new IllegalArgumentException();return 0;}
 public static String name(String id){return id;}
 public static SharedPreferences prefs(Context c){if(!prefs.containsKey(c))prefs.put(c,new SharedPreferences());return prefs.get(c);}
 public static Bitmap decode(java.io.File f){throw new UnsupportedOperationException("Image operations are not mocked as passing");}
 public static int[]vector(Bitmap b){throw new UnsupportedOperationException();}
 public static Bitmap crop(Bitmap b,double[]r){throw new UnsupportedOperationException();}
 public static void png(Bitmap b,java.io.File f){throw new UnsupportedOperationException();}
}
