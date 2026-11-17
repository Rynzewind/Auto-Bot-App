package com.kev.fortniteautobot;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.*;

/** Non-touchable diagnostics. Recognition areas are transparent holes, not painted pixels. */
public final class DebugOverlay extends View {
    private final CaptureService service;private final WindowManager wm;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean closed;
    private final Runnable tick=new Runnable(){public void run(){if(closed)return;setVisibility(service.debugEnabled()&&!service.debugHidden&&service.zone==null&&!service.stopped?VISIBLE:INVISIBLE);invalidate();App.MAIN.postDelayed(this,200);}};
    public DebugOverlay(CaptureService s){super(s);service=s;wm=(WindowManager)s.getSystemService(CaptureService.WINDOW_SERVICE);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;params.alpha=0.65f;params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
        wm.addView(this,params);tick.run();
    }
    private float dp(float n){return App.dp(service,n);}
    private int color(String id){return id.equals("ready")?0xff55ff99:id.equals("leave")?0xff55ddff:0xffffcc55;}
    private RectF area(double[]r){double[]a=DebugGeometry.protect(r,getWidth(),getHeight());return new RectF((float)a[0],(float)a[1],(float)a[2],(float)a[3]);}
    private String percent(double n){return Double.isFinite(n)?Math.round(n*100)+"%":"—";}
    private void box(Canvas c,double[]r,int color,String label){if(!MatchMath.region(r))return;RectF b=area(r);b.inset(-dp(2),-dp(2));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));p.setColor(color);c.drawRect(b,p);p.setStyle(Paint.Style.FILL);p.setTextSize(dp(10));c.drawText(label,Math.max(dp(2),b.left),Math.max(dp(12),b.top-dp(3)),p);}
    private void cross(Canvas c,double x,double y,int color){CaptureService.Frame f=service.latest;int[]origin=new int[2];getLocationOnScreen(origin);float px=TapCoordinates.pixel(x,f==null?getWidth():f.physicalW)-origin[0],py=TapCoordinates.pixel(y,f==null?getHeight():f.physicalH)-origin[1];p.setColor(color);p.setStrokeWidth(dp(1.2f));p.setStyle(Paint.Style.STROKE);c.drawLine(px-dp(10),py,px+dp(10),py,p);c.drawLine(px,py-dp(10),px,py+dp(10),p);c.drawCircle(px,py,dp(3),p);p.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas c){
        if(getWidth()==0||getHeight()==0)return;
        Automation.Snapshot state=service.automation.debug;List<double[]>protectedPixels=new ArrayList<>();
        c.save();for(double[]r:state.protectedAreas){double[]a=DebugGeometry.protect(r,getWidth(),getHeight());protectedPixels.add(a);c.clipOutRect((float)a[0],(float)a[1],(float)a[2],(float)a[3]);}
        for(Automation.ViewState r:state.rules)if(r.rule.enabled&&MatchMath.region(r.rule.region))box(c,r.rule.region,color(r.rule.id),r.rule.label()+" "+percent(r.score)+"/"+percent(r.rule.threshold));
        if(state.playing!=null&&state.step<state.playing.steps.size()){RuleStore.Step next=state.playing.steps.get(state.step);if(App.prefs(service).getBoolean("buttonBoxes",false))box(c,next.rect,0xffff55dd,"RECORDED PATCH "+(state.step+1));cross(c,next.x,next.y,0xffff55dd);}
        if(Double.isFinite(service.lastTapX)&&SystemClock.elapsedRealtime()-service.tapAt<5000)cross(c,service.lastTapX,service.lastTapY,0xffff7777);
        List<String>lines=new ArrayList<>();CaptureService.Frame frame=service.latest;long age=frame==null?-1:SystemClock.elapsedRealtime()-frame.at;
        lines.add("AUTO BOT DEBUG • "+(service.paused?"PAUSED":"RUNNING")+" • frame #"+service.frameNumber+" age "+age+"ms");
        lines.add("Focus: "+(App.isGame()?"Fortnite":App.foreground)+" • Accessibility "+(App.taps!=null?"ON":"OFF"));
        if(frame!=null)lines.add("Capture "+frame.physicalW+"×"+frame.physicalH+" • rotation "+frame.rotation+" • "+(service.teaching()?"TEACHING":"monitoring"));
        for(String id:App.IDS){Automation.ViewState best=null;for(Automation.ViewState view:state.rules)if(view.rule.id.equals(id)){boolean preferred=view.rule.key.equals(state.chosen.get(id));boolean priorPreferred=best!=null&&best.rule.key.equals(state.chosen.get(id));if(best==null||view.rule==state.playing||best.rule!=state.playing&&(preferred||!priorPreferred&&Double.isFinite(view.score)&&(!Double.isFinite(best.score)||view.score>best.score)))best=view;}
            if(best!=null){lines.add(App.name(id)+" "+percent(best.score)+" / "+percent(best.rule.threshold)+"  hits "+best.hits+"/3 / "+best.rule.name);lines.add("  "+best.reason);}
        }
        if(state.playing!=null&&state.step<state.playing.steps.size()){RuleStore.Step s=state.playing.steps.get(state.step);int w=frame==null?getWidth():frame.physicalW,h=frame==null?getHeight():frame.physicalH;lines.add("NEXT tap "+(state.step+1)+"/"+state.playing.steps.size()+" at "+Math.round(TapCoordinates.pixel(s.x,w))+","+Math.round(TapCoordinates.pixel(s.y,h))+" / "+state.playing.name);lines.add("Saved coordinates and timing • no tap threshold");}
        if(Double.isFinite(service.lastTapX))lines.add("TAP PIXEL "+Math.round(service.lastTapPixelX)+","+Math.round(service.lastTapPixelY)+" • "+service.tapResult);
        if(service.tapDisplayW>0)lines.add("Tap display "+service.tapDisplayW+"×"+service.tapDisplayH+"; service metrics "+service.tapContextW+"×"+service.tapContextH+" (not used)");
        lines.add(App.lastAction);lines.add(App.status);
        int w=Math.min(getWidth(),(int)dp(355)),line=(int)dp(13),h=Math.min(getHeight(),line*lines.size()+(int)dp(10));
        if(service.overlay!=null){int[]at=new int[2];service.overlay.getLocationOnScreen(at);protectedPixels.add(new double[]{at[0],at[1],at[0]+service.overlay.getWidth(),at[1]+service.overlay.getHeight()});}
        int corner=App.prefs(service).getInt("debugCorner",-1);if(corner<0)corner=DebugGeometry.corner(getWidth(),getHeight(),w,h,protectedPixels);
        float x=(corner==0||corner==1)?getWidth()-w:0,y=(corner==1||corner==2)?getHeight()-h:0;
        p.setStyle(Paint.Style.FILL);p.setColor(0xff07111e);c.drawRoundRect(x,y,x+w,y+h,dp(8),dp(8),p);p.setColor(Color.WHITE);p.setTextSize(dp(10));p.setTypeface(Typeface.MONOSPACE);
        int n=0;for(String text:lines){if(y+dp(12)+n*line>getHeight())break;String shown=text==null?"":text;while(shown.length()>1&&p.measureText(shown)>w-dp(12))shown=shown.substring(0,shown.length()-1);c.drawText(shown,x+dp(6),y+dp(12)+n++*line,p);}
        c.restore();
    }
    public void close(){closed=true;App.MAIN.removeCallbacks(tick);try{wm.removeView(this);}catch(Exception ignored){}}
}
