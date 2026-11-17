package com.kev.fortniteautobot;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.UUID;
import org.json.*;

/** Calibrate directly over Fortnite; save only clean frames after hiding all guides. */
public final class ZoneOverlay {
    private final CaptureService service;
    private final String id;
    private final WindowManager wm;
    private final int rotation;
    private final RuleStore store;
    private final String screenName;
    private JSONArray numberedTaps=new JSONArray();
    private final RectF zone=new RectF();
    private float markerX=0.5f,markerY=0.5f;
    private LinearLayout panel;
    private TextView label;
    private Button mode;
    private View shield;
    private boolean selecting=true,dirty=true,busy;
    private volatile boolean closed;
    private volatile long generation;

    ZoneOverlay(CaptureService s,String rule){service=s;id=rule;store=new RuleStore(s);String name="Screen 1";try{name=store.json(id).optString("variantName",name);}catch(Exception ignored){}screenName=name;wm=(WindowManager)s.getSystemService(CaptureService.WINDOW_SERVICE);rotation=App.rotation(s);}
    public void open(){
        if(service.overlay!=null)service.overlay.visible(false);
        try{JSONObject j=store.json(id);numberedTaps=j.optJSONArray("steps");Rect b=App.bounds(service);
            if(j.has("region")&&j.optInt("rotation",-1)==rotation&&Math.abs(j.optDouble("aspect",0)-(double)b.width()/b.height())<0.01){double[]r=RuleStore.array(j.getJSONArray("region"));zone.set((float)r[0],(float)r[1],(float)(r[0]+r[2]),(float)(r[1]+r[3]));markerX=zone.centerX();markerY=zone.centerY();dirty=false;}
        }catch(Exception ignored){}
        panel=new LinearLayout(service);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(10,5,10,5);panel.setBackgroundColor(0xf0152438);
        label=App.text(service,panel,"",13);
        LinearLayout buttons=new LinearLayout(service);panel.addView(buttons);
        mode=App.button(service,buttons,"Game input",()->{if(busy)return;selecting=!selecting;if(selecting)attach();else removeShield();update("Drag controls to move them");});
        App.button(service,buttons,"Save zone",()->save(false));
        App.button(service,buttons,"Record taps",()->{if(busy)return;if(dirty||!MatchMath.region(region()))save(true);else record();});
        App.button(service,buttons,"Done",()->close("Zone setup closed; resume from an icon"));
        for(int i=0;i<buttons.getChildCount();i++)((Button)buttons.getChildAt(i)).setTextSize(11);
        WindowManager.LayoutParams p=params(App.dp(service,440),-2);p.x=App.dp(service,12);p.y=App.dp(service,12);wm.addView(panel,p);
        final float[]origin=new float[2];final int[]start=new int[2];
        label.setOnTouchListener((v,e)->{if(panel==null||busy)return true;WindowManager.LayoutParams where=(WindowManager.LayoutParams)panel.getLayoutParams();if(e.getActionMasked()==MotionEvent.ACTION_DOWN){origin[0]=e.getRawX();origin[1]=e.getRawY();start[0]=where.x;start[1]=where.y;}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){Rect b=App.bounds(service);where.x=MatchMath.clamp(start[0]+Math.round(e.getRawX()-origin[0]),b.width(),panel.getWidth());where.y=MatchMath.clamp(start[1]+Math.round(e.getRawY()-origin[1]),b.height(),panel.getHeight());wm.updateViewLayout(panel,where);}else if(e.getActionMasked()==MotionEvent.ACTION_UP)v.performClick();return true;});
        attach();update("Drag GREEN zone; tap MAGENTA crosshair");
    }
    private WindowManager.LayoutParams params(int w,int h){WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;if(android.os.Build.VERSION.SDK_INT>=30)p.setFitInsetsTypes(0);return p;}
    private double[]region(){return new double[]{zone.left,zone.top,zone.width(),zone.height()};}
    private void update(String text){if(label!=null){mode.setText(selecting?"Game input":"Select zone");label.setText(App.name(id)+" / "+screenName+" • "+(selecting?"ZONE MODE":"GAME INPUT")+"\n"+text);}}
    private void attach(){if(closed||busy||shield!=null||!selecting)return;shield=new Guides();wm.addView(shield,params(-1,-1));ViewGroup.LayoutParams p=panel.getLayoutParams();wm.removeView(panel);wm.addView(panel,p);}
    private void removeShield(){if(shield!=null){try{wm.removeView(shield);}catch(Exception ignored){}shield=null;}}
    private boolean allowed(){CaptureService.Frame f=service.latest;Rect b=App.bounds(service);return !closed&&!service.stopped&&App.isGame()&&f!=null&&f.rotation==rotation&&App.rotation(service)==rotation&&f.physicalW==b.width()&&f.physicalH==b.height()&&b.width()>b.height()&&SystemClock.elapsedRealtime()-f.at<=1200;}
    public void focusLost(){if(closed)return;generation++;busy=false;selecting=false;removeShield();if(panel!=null)panel.setVisibility(View.VISIBLE);update("Show Fortnite, then Select zone");}
    private void save(boolean thenRecord){
        if(busy||closed)return;
        if(!MatchMath.region(region())||zone.width()<0.008||zone.height()<0.008){App.toast(service,"Drag a watch zone over the game first");return;}
        if(!allowed()){App.toast(service,"Show Fortnite in the same landscape rotation with capture running");return;}
        busy=true;long ticket=++generation,hidden=SystemClock.elapsedRealtime();removeShield();panel.setVisibility(View.INVISIBLE);await(ticket,hidden,thenRecord);
    }
    private void await(long ticket,long hidden,boolean thenRecord){
        if(closed||ticket!=generation)return;
        if(!allowed()){focusLost();return;}
        CaptureService.Frame frame=service.latest;
        if(frame.at<hidden+600){if(SystemClock.elapsedRealtime()-hidden>3500){busy=false;panel.setVisibility(View.VISIBLE);attach();update("No clean frame; try Save zone again");return;}App.MAIN.postDelayed(()->await(ticket,hidden,thenRecord),100);return;}
        final double[]selected=region();
        service.worker.post(()->{
            try{
                if(closed||ticket!=generation||service.stopped)return;
                String source="source-"+UUID.randomUUID()+".png";App.png(frame.bitmap,store.file(id,source));
                JSONObject j=store.json(id).put("source",source).put("sourceRotation",rotation).put("sourceReset",true);store.save(id,j);
                store.watch(id,frame.bitmap,selected,rotation,j.optDouble("threshold",id.equals("leave")?0.65:0.90));
                App.MAIN.post(()->{if(closed||ticket!=generation)return;busy=false;dirty=false;service.reload();if(thenRecord)record();else{panel.setVisibility(View.VISIBLE);attach();update("Screen and watch zone saved. Record taps or Done.");}});
            }catch(Exception e){App.MAIN.post(()->{if(closed||ticket!=generation)return;busy=false;panel.setVisibility(View.VISIBLE);attach();update("Save failed: "+e.getMessage());});}
        });
    }
    private void record(){close("Watch saved; record the taps over Fortnite");service.record(id);}
    public void close(String why){if(closed)return;closed=true;generation++;removeShield();if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){}panel=null;}if(service.zone==this)service.zone=null;if(service.overlay!=null&&!service.stopped)service.overlay.visible(true);App.status=why;}
    private final class Guides extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float downX,downY,startX,startY;boolean dragging;
        Guides(){super(service);setContentDescription("Drag the green watch zone directly over Fortnite; tap for magenta crosshairs");}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();RectF r=new RectF(zone.left*w,zone.top*h,zone.right*w,zone.bottom*h);
            if(zone.width()>0&&zone.height()>0){paint.setStyle(Paint.Style.FILL);paint.setColor(0x332cff7f);c.drawRect(r,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xee001c0c);paint.setStrokeWidth(App.dp(service,4));c.drawRect(r,paint);paint.setColor(0xff2cff7f);paint.setStrokeWidth(App.dp(service,2));c.drawRect(r,paint);}
            float x=markerX*w,y=markerY*h;paint.setStyle(Paint.Style.STROKE);paint.setColor(0xdd210018);paint.setStrokeWidth(App.dp(service,3));c.drawLine(0,y,w,y,paint);c.drawLine(x,0,x,h,paint);paint.setColor(0xffff40d5);paint.setStrokeWidth(App.dp(service,1.2f));c.drawLine(0,y,w,y,paint);c.drawLine(x,0,x,h,paint);c.drawCircle(x,y,App.dp(service,6),paint);TapMarkers.all(c,paint,numberedTaps,new RectF(0,0,w,h),service);
        }
        @Override public boolean onTouchEvent(MotionEvent e){Rect b=App.bounds(service);float x=PreviewGeometry.normal(e.getRawX(),0,b.width()),y=PreviewGeometry.normal(e.getRawY(),0,b.height());
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();startX=x;startY=y;dragging=false;return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP){if(Math.hypot(e.getRawX()-downX,e.getRawY()-downY)>App.dp(service,6))dragging=true;markerX=x;markerY=y;if(dragging){zone.set(Math.min(startX,x),Math.min(startY,y),Math.max(startX,x),Math.max(startY,y));dirty=true;}if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();invalidate();update("Green "+Math.round(zone.width()*100)+"% × "+Math.round(zone.height()*100)+"% • magenta "+Math.round(markerX*100)+"%, "+Math.round(markerY*100)+"%");return true;}return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
