package com.kev.fortniteautobot;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;
public final class Recorder {
    private final CaptureService service;
    private final String id;
    private final RuleStore store;
    private final String screenName;
    private final WindowManager wm;
    private final int rotation;
    private final List<JSONObject> steps=new ArrayList<>();
    private LinearLayout panel;
    private TextView label;
    private Button pause;
    private View shield;
    private boolean recording,busy,closed;
    private long generation,lastTap;
    private float downX,downY,distance;
    private long downAt;
    private int pointers;
    private int sessionW,sessionH;
    Recorder(CaptureService s,String rule){service=s;id=rule;store=new RuleStore(s);String name="Screen 1";try{name=store.json(id).optString("variantName",name);}catch(Exception ignored){}screenName=name;wm=(WindowManager)s.getSystemService(CaptureService.WINDOW_SERVICE);rotation=App.rotation(s);}
    public void open(){
        if(service.overlay!=null)service.overlay.visible(false);
        panel=new LinearLayout(service);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(10,6,10,6);panel.setBackgroundColor(0xf0152438);
        label=App.text(service,panel,App.name(id)+" / "+screenName+": arrange the starting screen, then Record",13);
        LinearLayout row=new LinearLayout(service);panel.addView(row);
        pause=App.button(service,row,"Record",()->{if(busy)return;recording=!recording;if(recording){if(!allowed()){recording=false;App.toast(service,"Show Fortnite in landscape and enable Accessibility");return;}attachShield();}else removeShield();update();});
        App.button(service,row,"Done",this::done);App.button(service,row,"Cancel",()->cancel("Recording cancelled; previous recording kept"));
        WindowManager.LayoutParams p=params(App.dp(service,370),-2);p.x=App.dp(service,20);p.y=App.dp(service,12);wm.addView(panel,p);
        final float[]origin=new float[2];final int[]at=new int[2];label.setContentDescription("Drag recording controls to move them");label.setOnTouchListener((v,e)->{if(panel==null)return true;WindowManager.LayoutParams where=(WindowManager.LayoutParams)panel.getLayoutParams();if(e.getActionMasked()==MotionEvent.ACTION_DOWN){origin[0]=e.getRawX();origin[1]=e.getRawY();at[0]=where.x;at[1]=where.y;}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){Rect b=App.bounds(service);where.x=MatchMath.clamp(at[0]+Math.round(e.getRawX()-origin[0]),b.width(),panel.getWidth());where.y=MatchMath.clamp(at[1]+Math.round(e.getRawY()-origin[1]),b.height(),panel.getHeight());wm.updateViewLayout(panel,where);}return true;});
    }
    private WindowManager.LayoutParams params(int w,int h){WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;if(android.os.Build.VERSION.SDK_INT>=30)p.setFitInsetsTypes(0);return p;}
    private boolean allowed(){CaptureService.Frame f=service.latest;Rect b=App.bounds(service);return !closed&&!service.stopped&&App.isGame()&&App.taps!=null&&f!=null&&SystemClock.elapsedRealtime()-f.at<=1200&&f.rotation==rotation&&App.rotation(service)==rotation&&f.physicalW==b.width()&&f.physicalH==b.height()&&(sessionW==0||f.physicalW==sessionW&&f.physicalH==sessionH)&&b.width()>b.height();}
    private void update(){if(panel!=null){pause.setText(recording?"Pause":"Record");label.setText(App.name(id)+" / "+screenName+" • "+steps.size()+" taps • "+(busy?"sending tap; wait":recording?"tap once, then wait for controls":"normal game input; Record to continue"));}}
    private void attachShield(){
        if(closed||!recording||busy||shield!=null)return;
        View v=new View(service);v.setBackgroundColor(Color.TRANSPARENT);
        v.setOnTouchListener((view,e)->{
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();downAt=e.getEventTime();distance=0;pointers=1;break;
                case MotionEvent.ACTION_POINTER_DOWN:pointers=Math.max(pointers,e.getPointerCount());break;
                case MotionEvent.ACTION_MOVE:distance=Math.max(distance,(float)Math.hypot(e.getRawX()-downX,e.getRawY()-downY));break;
                case MotionEvent.ACTION_UP:
                    distance=Math.max(distance,(float)Math.hypot(e.getRawX()-downX,e.getRawY()-downY));
                    if(pointers==1&&distance<App.dp(service,14)&&e.getEventTime()-downAt<600){CaptureService.Frame f=service.latest;if(f!=null)capture(TapCoordinates.normalize(e.getRawX(),f.physicalW),TapCoordinates.normalize(e.getRawY(),f.physicalH));}else App.toast(service,"Only single short taps are recorded");break;
            }return true;
        });
        wm.addView(v,params(-1,-1));shield=v;
        ViewGroup.LayoutParams p=panel.getLayoutParams();wm.removeView(panel);wm.addView(panel,p);update();
    }
    private void removeShield(){if(shield!=null){try{wm.removeView(shield);}catch(Exception ignored){}shield=null;}}
    public void focusLost(){if(closed)return;generation++;recording=false;busy=false;removeShield();service.hideDebug(false);if(panel!=null)panel.setVisibility(View.VISIBLE);update();}
    private void capture(double x,double y){
        if(!allowed()||busy||steps.size()>=30){App.toast(service,steps.size()>=30?"Maximum 30 taps":"Fortnite/capture is not ready");return;}
        if(sessionW==0){sessionW=service.latest.physicalW;sessionH=service.latest.physicalH;}busy=true;service.hideDebug(true);long ticket=++generation;long hiddenAt=SystemClock.elapsedRealtime();removeShield();panel.setVisibility(View.INVISIBLE);
        if(service.overlay!=null)service.overlay.visible(false);
        awaitFrame(x,y,ticket,hiddenAt);
    }
    private void awaitFrame(double x,double y,long ticket,long hiddenAt){
        if(closed||ticket!=generation)return;
        CaptureService.Frame f=service.latest;long now=SystemClock.elapsedRealtime();
        if(!allowed()){cancel("Recording stopped: Fortnite, capture, or display changed");return;}
        if(f.at<hiddenAt+500){if(now-hiddenAt>3000){cancel("Recording stopped: no clean capture frame");return;}App.MAIN.postDelayed(()->awaitFrame(x,y,ticket,hiddenAt),100);return;}
        service.worker.post(()->{
            try{
                Rect bounds=App.bounds(service);double rx=Math.min(0.06,(double)App.dp(service,36)/bounds.width()),ry=Math.min(0.12,(double)App.dp(service,36)/bounds.height());
                double l=Math.max(0,x-rx),t=Math.max(0,y-ry),r=Math.min(1,x+rx),b=Math.min(1,y+ry);double[]rect={l,t,r-l,b-t};
                String file="tap-"+UUID.randomUUID()+".png";Bitmap patch=App.crop(f.bitmap,rect);try{App.png(patch,store.file(id,file));}finally{if(patch!=f.bitmap)patch.recycle();}
                long wait=lastTap==0?600:Math.max(300,Math.min(15000,SystemClock.elapsedRealtime()-lastTap));
                String screen="screen-"+UUID.randomUUID()+".png";App.png(f.bitmap,store.file(id,screen));
                JSONObject step=new JSONObject().put("x",x).put("y",y).put("rect",new JSONArray(rect)).put("file",file).put("wait",wait).put("screen",screen).put("displayW",f.physicalW).put("displayH",f.physicalH).put("rawX",TapCoordinates.pixel(x,f.physicalW)).put("rawY",TapCoordinates.pixel(y,f.physicalH));
                App.MAIN.post(()->{
                    if(closed||ticket!=generation)return;if(!allowed()){cancel("Recording interrupted; old recording kept");return;}
                    App.taps.tap(x,y,ok->{if(closed||ticket!=generation)return;if(!ok){cancel("Tap failed; old recording kept");return;}steps.add(step);lastTap=SystemClock.elapsedRealtime();App.MAIN.postDelayed(()->{if(closed||ticket!=generation)return;busy=false;service.hideDebug(false);panel.setVisibility(View.VISIBLE);attachShield();update();},800);});
                });
            }catch(Exception e){App.MAIN.post(()->cancel("Could not record tap: "+e.getMessage()));}
        });
    }
    private void done(){if(busy){App.toast(service,"Wait for the current tap");return;}if(steps.isEmpty()){App.toast(service,"Record at least one tap, or Cancel");return;}
        try{store.save(id,store.json(id).put("steps",new JSONArray(steps)));cancel(steps.size()+" taps saved for "+id+"; resume from an icon");}catch(Exception e){App.toast(service,"Save failed: "+e.getMessage());}
    }
    public void cancel(String why){if(closed)return;closed=true;generation++;recording=false;removeShield();service.hideDebug(false);if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){}panel=null;}if(service.recorder==this)service.recorder=null;if(service.overlay!=null&&!service.stopped)service.overlay.visible(true);service.log(App.name(id)+" / "+screenName+": "+why);service.reload();App.status=why;App.toast(service,why);}
}
