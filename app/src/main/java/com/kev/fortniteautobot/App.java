package com.kev.fortniteautobot;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
public final class App {
    public static final String GAME="com.epicgames.fortnite";
    public static final String[] IDS={"ready","leave","party"},NAMES={"Auto Ready","Auto Leave","Auto Party"};
    public static volatile CaptureService capture;
    public static volatile TapService taps;
    public static volatile String foreground="",status="Start capture to begin",lastAction="No taps sent";
    public static final Handler MAIN=new Handler(Looper.getMainLooper());
    public static boolean isGame(){return GAME.equals(foreground);}
    public static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;throw new IllegalArgumentException("Unknown rule");}
    public static String name(String id){return NAMES[index(id)];}
    public static Rect bounds(Context c){WindowManager wm=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);if(Build.VERSION.SDK_INT>=30)return wm.getMaximumWindowMetrics().getBounds();android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new Rect(0,0,m.widthPixels,m.heightPixels);}
    public static int rotation(Context c){return ((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();}
    public static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("settings",0);}
    public static void toast(Context c,String s){MAIN.post(()->Toast.makeText(c,s,Toast.LENGTH_LONG).show());}
    public static boolean launch(Context c){Intent i=c.getPackageManager().getLaunchIntentForPackage(GAME);if(i==null){toast(c,"Install the official Fortnite app first");return false;}try{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(Exception e){toast(c,"Fortnite could not open: "+e.getMessage());return false;}}
    public static void settings(Context c){c.startActivity(new Intent(c,MainActivity.class).putExtra("settings",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}
    public static void editor(Context c,String id){c.startActivity(new Intent(c,RuleActivity.class).putExtra("rule",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    public static LinearLayout body(Activity a){LinearLayout b=new LinearLayout(a);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(12,10,12,10);b.setBackgroundColor(0xff101a2a);ScrollView s=new ScrollView(a);s.setBackgroundColor(0xff101a2a);s.addView(b);insets(s);a.setContentView(s);return b;}
    public static void insets(View root){root.setOnApplyWindowInsetsListener((v,i)->{int l,t,r,b;if(Build.VERSION.SDK_INT>=30){Insets safe=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=safe.left;t=safe.top;r=safe.right;b=safe.bottom;}else{l=i.getSystemWindowInsetLeft();t=i.getSystemWindowInsetTop();r=i.getSystemWindowInsetRight();b=i.getSystemWindowInsetBottom();}int space=dp(v.getContext(),8);v.setPadding(l+space,t+space,r+space,b+space);return i;});root.requestApplyInsets();}
    public static TextView text(Context c,LinearLayout b,String s,int size){TextView t=new TextView(c);t.setText(s);t.setTextColor(0xffedf4ff);t.setTextSize(size);t.setPadding(5,7,5,7);b.addView(t);return t;}
    public static Button button(Context c,LinearLayout b,String s,Runnable r){Button v=new Button(c);v.setText(s);v.setOnClickListener(w->r.run());b.addView(v);return v;}
    public static void png(Bitmap b,File file)throws IOException{File parent=file.getParentFile();if(parent!=null)parent.mkdirs();try(FileOutputStream out=new FileOutputStream(file)){if(!b.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Picture save failed");}}
    public static Bitmap decode(File file)throws IOException{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);if(o.outWidth<1||o.outHeight<1||o.outWidth>4096||o.outHeight>4096||(long)o.outWidth*o.outHeight>4000000)throw new IOException("Invalid or oversized picture");Bitmap b=BitmapFactory.decodeFile(file.getPath());if(b==null)throw new IOException("Picture could not be read");return b;}
    public static int[] vector(Bitmap b){Bitmap s=Bitmap.createScaledBitmap(b,48,24,true);int[]v=new int[1152];s.getPixels(v,0,48,0,0,48,24);if(s!=b)s.recycle();for(int i=0;i<v.length;i++){int p=v[i];v[i]=(((p>>16)&255)*30+((p>>8)&255)*59+(p&255)*11)/100;}return v;}
    public static Bitmap crop(Bitmap b,double[]r){int x=(int)(r[0]*b.getWidth()),y=(int)(r[1]*b.getHeight());int w=Math.max(1,Math.min((int)(r[2]*b.getWidth()),b.getWidth()-x)),h=Math.max(1,Math.min((int)(r[3]*b.getHeight()),b.getHeight()-y));return Bitmap.createBitmap(b,x,y,w,h);}
}
