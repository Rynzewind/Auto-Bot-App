package com.kev.fortniteautobot;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;
public final class CaptureService extends Service {
    public static final class Frame {
        public final Bitmap bitmap;public final long at;public final int physicalW,physicalH,rotation;
        Frame(Bitmap b,long time,int w,int h,int r){bitmap=b;at=time;physicalW=w;physicalH=h;rotation=r;}
    }
    public final AtomicLong epoch=new AtomicLong();
    public volatile boolean paused=false,ready=false,stopped=false;
    public volatile Frame latest;
    public Handler worker;
    public Automation automation;
    public FloatingOverlay overlay;
    public DebugOverlay diagnostics;
    public volatile boolean debugHidden;
    public volatile boolean debugSession;
    public volatile boolean launchWaiting;
    public volatile long frameNumber,tapAt;
    public volatile double lastTapX=Double.NaN,lastTapY=Double.NaN;
    public volatile float lastTapPixelX=Float.NaN,lastTapPixelY=Float.NaN;
    public volatile int tapDisplayW,tapDisplayH,tapContextW,tapContextH;
    public volatile String tapResult="No tap dispatched";
    public volatile double[]controlArea;
    public volatile Recorder recorder;
    public volatile ZoneOverlay zone;
    private HandlerThread thread;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private int width,height,rotation,frames,black;
    private volatile long lastFrame;
    private long lastSample,hiddenAt;
    private volatile String captureId;
    private long captureDue;
    private final MediaProjection.Callback projectionCallback=new MediaProjection.Callback(){
        @Override public void onStop(){App.MAIN.post(()->{if(!stopped){App.status="Screen capture stopped";stopSelf();}});}
        @Override public void onCapturedContentResize(int w,int h){if(worker!=null)worker.post(()->{Rect b=App.bounds(CaptureService.this);if(w==b.width()&&h==b.height())resize(w,h);else{paused=true;App.status="Use capture of the entire display";}});}
    };
    private final DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener(){
        public void onDisplayAdded(int id){}public void onDisplayRemoved(int id){}
        public void onDisplayChanged(int id){if(id==0&&worker!=null)worker.post(()->{Rect b=App.bounds(CaptureService.this);resize(b.width(),b.height());});}
    };
    @Override public void onCreate(){super.onCreate();App.capture=this;thread=new HandlerThread("AutoBot frames");thread.start();worker=new Handler(thread.getLooper());automation=new Automation(this);App.MAIN.postDelayed(watchdog,1000);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if("STOP".equals(intent.getAction())){pause(true,"Stopped");stopSelf();return START_NOT_STICKY;}
        debugSession=intent.getBooleanExtra("debugSession",false);if(projection!=null)return START_NOT_STICKY;
        try{
            NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("capture","Auto Bot",NotificationManager.IMPORTANCE_LOW));
            Intent stop=new Intent(this,CaptureService.class).setAction("STOP");PendingIntent stopPi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            Intent settings=new Intent(this,MainActivity.class).putExtra("settings",true);PendingIntent open=PendingIntent.getActivity(this,2,settings,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n=new Notification.Builder(this,"capture").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Auto Bot").setContentText("Ready • Leave • Party — tap for settings").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stopPi).build()).build();
            startForeground(20,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            Intent consent=intent.getParcelableExtra("consent");int result=intent.getIntExtra("result",0);
            if(consent==null||result!=Activity.RESULT_OK)throw new IllegalStateException("Approve screen capture first");
            projection=((MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(result,consent);
            projection.registerCallback(projectionCallback,App.MAIN);
            ((DisplayManager)getSystemService(DISPLAY_SERVICE)).registerDisplayListener(displayListener,App.MAIN);
            worker.post(()->{try{Rect b=App.bounds(this);width=b.width();height=b.height();rotation=App.rotation(this);reader=newReader(width,height);display=projection.createVirtualDisplay("AutoBot",width,height,getResources().getDisplayMetrics().densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,worker);App.MAIN.post(()->{if(!stopped)try{overlay=new FloatingOverlay(this);}catch(Exception e){App.status="Overlay could not open: "+e.getMessage();stopSelf();}});App.status="Capture started; waiting for landscape frames";}catch(Exception e){App.status="Capture failed: "+e.getMessage();App.MAIN.post(this::stopSelf);}});
        }catch(Exception e){App.status="Capture could not start: "+e.getMessage();App.toast(this,App.status);stopSelf();}
        return START_NOT_STICKY;
    }
    private ImageReader newReader(int w,int h){ImageReader next=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);next.setOnImageAvailableListener(this::image,worker);return next;}
    private void resize(int w,int h){if(stopped||display==null||w<1||h<1)return;int rot=App.rotation(this);if(w==width&&h==height&&rot==rotation)return;epoch.incrementAndGet();automation.cancel("Display changed; checking calibration");ready=false;frames=0;ImageReader next=newReader(w,h);display.resize(w,h,getResources().getDisplayMetrics().densityDpi);display.setSurface(next.getSurface());ImageReader old=reader;reader=next;width=w;height=h;rotation=rot;if(old!=null)old.close();Recorder current=recorder;if(current!=null)App.MAIN.post(()->current.cancel("Recording cancelled: screen rotated"));}
    private void image(ImageReader source){
        Image image=null;try{image=source.acquireLatestImage();if(image==null||source!=reader||stopped)return;long now=SystemClock.elapsedRealtime();lastFrame=now;
            if(now-lastSample<250||paused&&recorder==null&&zone==null&&captureId==null&&ready&&!debugEnabled()&&!launchWaiting)return;lastSample=now;
            Image.Plane p=image.getPlanes()[0];int paddedW=p.getRowStride()/p.getPixelStride();Bitmap padded=Bitmap.createBitmap(paddedW,image.getHeight(),Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(p.getBuffer());Bitmap cropped=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight());int sw=Math.min(960,cropped.getWidth());Bitmap small=Bitmap.createScaledBitmap(cropped,sw,Math.max(1,Math.round((float)cropped.getHeight()*sw/cropped.getWidth())),true);if(cropped!=small)cropped.recycle();if(padded!=cropped&&padded!=small)padded.recycle();
            Frame f=new Frame(small,now,width,height,rotation);latest=f;frames++;frameNumber++;ready=frames>=2&&width>height;
            long light=0;for(int y=0;y<small.getHeight();y+=40)for(int x=0;x<small.getWidth();x+=40){int v=small.getPixel(x,y);light+=(v>>16)&255;light+=(v>>8)&255;light+=v&255;}black=light<100?black+1:0;
            if(black>=12){pause(true,"Capture is black; check game/Android capture settings");return;}
            if(captureId!=null){capturePicture(f);return;}
            automation.frame(f);
        }catch(Exception e){captureId=null;pause(true,"Frame processing stopped: "+e.getMessage());App.MAIN.post(()->{if(overlay!=null)overlay.visible(true);});}finally{if(image!=null)image.close();}
    }
    private final Runnable watchdog=new Runnable(){public void run(){if(stopped)return;if(!paused&&lastFrame>0&&SystemClock.elapsedRealtime()-lastFrame>10000)pause(true,"No fresh capture frames");App.MAIN.postDelayed(this,1000);}};
    public boolean teaching(){return recorder!=null||zone!=null||captureId!=null;}
    public boolean debugEnabled(){return debugSession&&App.prefs(this).getBoolean("liveDebug",true);}
    public void setDebugSession(boolean enabled){debugSession=enabled;hideDebug(debugHidden);}
    public void hideDebug(boolean hide){debugHidden=hide;if(diagnostics!=null)diagnostics.setVisibility(hide?android.view.View.INVISIBLE:debugEnabled()?android.view.View.VISIBLE:android.view.View.INVISIBLE);}
    public void traceTap(double x,double y,String result,int width,int height){lastTapX=x;lastTapY=y;tapAt=SystemClock.elapsedRealtime();tapResult=result;tapDisplayW=width;tapDisplayH=height;if(width>0&&height>0&&MatchMath.unit(x)&&MatchMath.unit(y)){lastTapPixelX=TapCoordinates.pixel(x,width);lastTapPixelY=TapCoordinates.pixel(y,height);}log("Android tap normalized "+x+","+y+" -> pixel "+lastTapPixelX+","+lastTapPixelY+" on capture "+width+"x"+height+"; Accessibility context "+tapContextW+"x"+tapContextH+": "+result);}
    public boolean controlsOverlap(double[]r){double[]b=controlArea;return b!=null&&r!=null&&r[0]<b[0]+b[2]&&r[0]+r[2]>b[0]&&r[1]<b[1]+b[3]&&r[1]+r[3]>b[1];}
    public void cancelTeaching(){captureId=null;if(zone!=null)zone.close("Zone setup cancelled for transfer");if(recorder!=null)recorder.cancel("Recording cancelled for setup transfer");if(overlay!=null)overlay.visible(true);}
    public void setupZone(String id){App.index(id);setDebugSession(true);captureId=null;pause(true,"Selecting "+id+" watch zone");if(recorder!=null)recorder.cancel("Recording cancelled for zone setup");if(zone!=null)zone.close("Changing rule");zone=new ZoneOverlay(this,id);try{zone.open();if(!App.isGame())App.launch(this);}catch(Exception e){zone.close("Zone controls could not open");App.toast(this,e.getMessage());}}
    public void interrupt(String why){epoch.incrementAndGet();log(why);if(worker!=null)worker.post(()->automation.cancel(why));}
    public void pause(boolean value,String why){paused=value;interrupt(why);if(!value&&worker!=null)worker.post(()->automation.reload());}
    public void reload(){interrupt("Settings changed");worker.post(()->automation.reload());}
    public void capturePicture(String id){
        App.index(id);pause(true,"Capture in 7 seconds; show the desired Fortnite screen");worker.post(()->{captureId=id;captureDue=SystemClock.elapsedRealtime()+7000;hiddenAt=0;});App.toast(this,"Show the "+id+" indicator in Fortnite. Capturing in 7 seconds.");App.launch(this);
    }
    private void capturePicture(Frame f)throws Exception{
        if(f.at<captureDue)return;
        if(hiddenAt==0){hiddenAt=f.at;App.MAIN.post(()->{if(overlay!=null)overlay.visible(false);});return;}
        if(f.at-hiddenAt<750)return;
        String id=captureId;captureId=null;
        if(!App.isGame()||!ready||App.rotation(this)!=f.rotation){App.status="Picture not saved: Fortnite must be visible in landscape";App.MAIN.post(()->{if(overlay!=null)overlay.visible(true);});return;}
        RuleStore s=new RuleStore(this);String name="source-"+UUID.randomUUID()+".png";App.png(f.bitmap,s.file(id,name));JSONObject j=s.json(id).put("source",name).put("sourceRotation",f.rotation).put("sourceReset",true);s.save(id,j);App.status="Picture saved for "+id;
        App.MAIN.post(()->{if(overlay!=null)overlay.visible(true);App.editor(this,id);});
    }
    public void record(String id){setDebugSession(true);captureId=null;pause(true,"Recording "+id);if(zone!=null)zone.close("Starting tap recording");if(recorder!=null)recorder.cancel("Previous recording cancelled");recorder=new Recorder(this,id);recorder.open();if(!App.isGame())App.launch(this);}
    public synchronized void log(String line){File f=new File(getFilesDir(),"events.log");try(FileWriter out=new FileWriter(f,f.length()<1024*1024)){out.write(java.time.Instant.now()+" "+line+"\n");}catch(Exception ignored){}}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){stopped=true;paused=true;epoch.incrementAndGet();ready=false;App.MAIN.removeCallbacks(watchdog);if(zone!=null)zone.close("Capture stopped");if(recorder!=null)recorder.cancel("Capture stopped");if(overlay!=null)overlay.close();if(diagnostics!=null)diagnostics.close();try{((DisplayManager)getSystemService(DISPLAY_SERVICE)).unregisterDisplayListener(displayListener);}catch(Exception ignored){}
        worker.post(()->{if(display!=null)display.release();if(reader!=null)reader.close();if(projection!=null){projection.unregisterCallback(projectionCallback);projection.stop();}thread.quitSafely();});if(App.capture==this)App.capture=null;latest=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
