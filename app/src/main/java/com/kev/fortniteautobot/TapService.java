package com.kev.fortniteautobot;
import android.accessibilityservice.*;
import android.graphics.*;
import android.os.*;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import java.util.concurrent.atomic.AtomicBoolean;
public final class TapService extends AccessibilityService {
    public interface Done{void done(boolean ok);}
    @Override protected void onServiceConnected(){App.taps=this;}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null||e.getPackageName()==null)return;String pkg=e.getPackageName().toString();
        if(pkg.equals(getPackageName())){String cls=String.valueOf(e.getClassName());if(!cls.equals(MainActivity.class.getName())&&!cls.equals(PlayActivity.class.getName())&&!cls.equals(RuleActivity.class.getName()))return;}
        App.foreground=pkg;
        if(!App.isGame()&&App.capture!=null){CaptureService s=App.capture;s.interrupt("Fortnite lost focus");App.MAIN.post(()->{if(s.recorder!=null)s.recorder.focusLost();if(s.zone!=null)s.zone.focusLost();});}
    }
    @Override public void onInterrupt(){if(App.capture!=null)App.capture.pause(true,"Accessibility interrupted");}
    @Override public void onDestroy(){if(App.taps==this)App.taps=null;if(App.capture!=null)App.capture.pause(true,"Accessibility stopped");super.onDestroy();}
    public void tap(double x,double y,Done done){
        CaptureService capture=App.capture;CaptureService.Frame frame=capture==null?null:capture.latest;
        int width=frame==null?0:frame.physicalW,height=frame==null?0:frame.physicalH;
        AtomicBoolean returned=new AtomicBoolean(),dispatched=new AtomicBoolean();Done once=ok->{if(returned.compareAndSet(false,true)){if(capture!=null)capture.traceTap(x,y,ok?"Android completed":dispatched.get()?"Android cancelled":"Not sent / rejected",width,height);done.done(ok);}};
        if(!App.isGame()||!MatchMath.unit(x)||!MatchMath.unit(y)||capture==null||capture.stopped||frame==null||SystemClock.elapsedRealtime()-frame.at>1200||frame.rotation!=App.rotation(capture)){once.done(false);return;}
        try{Rect contextBounds=App.bounds(this);capture.tapContextW=contextBounds.width();capture.tapContextH=contextBounds.height();}catch(Exception ignored){capture.tapContextW=0;capture.tapContextH=0;}capture.traceTap(x,y,"Prepared pixel",width,height);
        Path p=new Path();p.moveTo(TapCoordinates.pixel(x,width),TapCoordinates.pixel(y,height));
        try{GestureDescription.Builder gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,70));if(Build.VERSION.SDK_INT>=30)gesture.setDisplayId(Display.DEFAULT_DISPLAY);boolean accepted=dispatchGesture(gesture.build(),new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription g){once.done(true);}
            @Override public void onCancelled(GestureDescription g){once.done(false);}
        },null);if(accepted){dispatched.set(true);if(!returned.get())capture.traceTap(x,y,"Sent to Android",width,height);}else once.done(false);}catch(Exception e){once.done(false);}
    }
}
