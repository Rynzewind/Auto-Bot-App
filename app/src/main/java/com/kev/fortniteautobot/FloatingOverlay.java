package com.kev.fortniteautobot;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
public final class FloatingOverlay extends LinearLayout {
    private final CaptureService service;
    private final WindowManager wm;
    private final WindowManager.LayoutParams position;
    private final View[] icons=new View[3];
    private Target target;
    private TextView countdown;
    private boolean moved,wasPaused,longPress,closed;
    private float startX,startY;
    private int oldX,oldY;
    private final Runnable poll=new Runnable(){public void run(){if(closed)return;for(View v:icons)v.invalidate();long left=service.automation.countdownUntil-android.os.SystemClock.elapsedRealtime();boolean counting=left>0&&!service.paused&&!service.teaching()&&App.isGame()&&!service.debugHidden;countdown.setText(counting?String.format(java.util.Locale.US,"%.1fs",left/1000.0):"");countdown.setVisibility(counting?VISIBLE:INVISIBLE);countdown.setContentDescription(service.automation.countdownLabel+" countdown "+countdown.getText());if(getVisibility()==VISIBLE){Rect b=App.bounds(service);int[]at=new int[2];getLocationOnScreen(at);service.controlArea=new double[]{(double)at[0]/b.width(),(double)at[1]/b.height(),(double)getWidth()/b.width(),(double)getHeight()/b.height()};}else service.controlArea=null;App.MAIN.postDelayed(this,100);}};
    private final Runnable held=()->{if(!moved){longPress=true;menu();}};
    public FloatingOverlay(CaptureService s){
        super(s);service=s;wm=(WindowManager)s.getSystemService(CaptureService.WINDOW_SERVICE);setOrientation(LinearLayout.VERTICAL);setPadding(dp(3),dp(2),dp(3),dp(3));setElevation(dp(8));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(0xee101c2c);bg.setCornerRadius(dp(14));setBackground(bg);
        position=new WindowManager.LayoutParams(dp(66),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);position.gravity=Gravity.TOP|Gravity.LEFT;position.x=App.prefs(s).getInt("overlay_x",dp(14));position.y=App.prefs(s).getInt("overlay_y",dp(50));
        TextView grip=new TextView(s);grip.setText("⠿");grip.setTextColor(0xffa3b8d2);grip.setGravity(Gravity.CENTER);grip.setTextSize(18);grip.setContentDescription("Drag to move. Drop on the bottom X to remove. Tap for settings.");addView(grip,new LayoutParams(-1,dp(24)));grip.setOnTouchListener(this::drag);
        countdown=new TextView(s);countdown.setTextColor(0xff66ff88);countdown.setTextSize(15);countdown.setTypeface(Typeface.DEFAULT_BOLD);countdown.setGravity(Gravity.CENTER);countdown.setVisibility(INVISIBLE);addView(countdown,new LayoutParams(-1,dp(26)));
        for(int i=0;i<3;i++){icons[i]=new RuleIcon(i);addView(icons[i],new LayoutParams(-1,dp(62)));}
        wm.addView(this,position);post(()->{clamp();poll.run();});if(service.diagnostics==null)try{service.diagnostics=new DebugOverlay(service);}catch(Exception e){App.toast(service,"Debug overlay could not open: "+e.getMessage());}
    }
    private int dp(float n){return App.dp(service,n);}
    public void visible(boolean visible){if(!closed)setVisibility(visible?View.VISIBLE:View.INVISIBLE);if(!visible)service.controlArea=null;}
    private void clamp(){Rect b=App.bounds(service);position.x=MatchMath.clamp(position.x,b.width(),getWidth());position.y=MatchMath.clamp(position.y,b.height(),getHeight());try{wm.updateViewLayout(this,position);}catch(Exception ignored){}}
    private boolean onTarget(){Rect b=App.bounds(service);return MatchMath.drop(position.x+getWidth()/2f,position.y+getHeight()/2f,b.width(),b.height(),getResources().getDisplayMetrics().density);}
    private boolean drag(View v,MotionEvent e){Rect b=App.bounds(service);switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:startX=e.getRawX();startY=e.getRawY();oldX=position.x;oldY=position.y;moved=false;longPress=false;wasPaused=service.paused;App.MAIN.postDelayed(held,ViewConfiguration.getLongPressTimeout());return true;
        case MotionEvent.ACTION_MOVE:
            if(longPress)return true;
            if(!moved&&Math.hypot(e.getRawX()-startX,e.getRawY()-startY)>dp(7)){moved=true;App.MAIN.removeCallbacks(held);service.pause(true,"Moving overlay");showTarget();}
            if(moved){position.x=MatchMath.clamp(oldX+Math.round(e.getRawX()-startX),b.width(),getWidth());position.y=MatchMath.clamp(oldY+Math.round(e.getRawY()-startY),b.height(),getHeight());try{wm.updateViewLayout(this,position);}catch(Exception ignored){}if(target!=null){target.hot=onTarget();target.invalidate();}}return true;
        case MotionEvent.ACTION_UP:App.MAIN.removeCallbacks(held);hideTarget();
            if(moved&&onTarget()){service.pause(true,"Stopped by dropping on remove");close();service.stopSelf();return true;}
            if(moved){App.prefs(service).edit().putInt("overlay_x",position.x).putInt("overlay_y",position.y).apply();service.paused=wasPaused;service.interrupt("Overlay moved");}
            else if(!longPress)menu();return true;
        case MotionEvent.ACTION_CANCEL:App.MAIN.removeCallbacks(held);hideTarget();return true;
    }return true;}
    private void menu(){boolean previouslyPaused=service.paused;service.pause(true,"Options open");boolean[]restore={true};
        StringBuilder scores=new StringBuilder(App.status);for(String id:App.IDS){Double score=service.automation.scores.get(id);scores.append("\n").append(App.name(id)).append(": ").append(service.automation.states.get(id));if(score!=null)scores.append(" • ").append(Math.round(score*100)).append('%');}
        LinearLayout options=new LinearLayout(service);options.setOrientation(LinearLayout.VERTICAL);App.text(service,options,scores.toString(),12);Switch live=new Switch(service);live.setText("Live zones and tap diagnostics");live.setTextColor(Color.WHITE);live.setChecked(service.debugEnabled());live.setOnCheckedChangeListener((v,on)->{App.prefs(service).edit().putBoolean("liveDebug",on).apply();service.setDebugSession(on);});options.addView(live);Switch boxes=new Switch(service);boxes.setText("Show button-picture match boxes");boxes.setTextColor(Color.WHITE);boxes.setChecked(App.prefs(service).getBoolean("buttonBoxes",false));boxes.setOnCheckedChangeListener((v,on)->App.prefs(service).edit().putBoolean("buttonBoxes",on).apply());options.addView(boxes);App.button(service,options,"Move debug panel",()->{int n=App.prefs(service).getInt("debugCorner",-1)+1;App.prefs(service).edit().putInt("debugCorner",n>3?-1:n).apply();});
        AlertDialog d=new AlertDialog.Builder(service).setTitle("Auto Bot").setView(options).setPositiveButton(previouslyPaused?"Resume":"Pause",(dialog,which)->{restore[0]=false;service.pause(!previouslyPaused,previouslyPaused?"Watching selected rules":"Paused");}).setNeutralButton("Setup",(dialog,which)->{restore[0]=false;App.settings(service);}).setNegativeButton("Back",null).create();
        d.setOnDismissListener(dialog->{if(restore[0])service.paused=previouslyPaused;});d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);d.show();
    }
    private void showTarget(){if(target!=null)return;target=new Target();WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(112),dp(120),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.y=dp(16);try{wm.addView(target,p);}catch(Exception e){target=null;}}
    private void hideTarget(){if(target!=null){try{wm.removeView(target);}catch(Exception ignored){}target=null;}}
    private void editRule(String id){try{RuleStore store=new RuleStore(service);java.util.List<RuleStore.Variant>screens=store.variants(id);if(screens.size()==1){service.setupZone(id);return;}service.pause(true,"Choose a screen variant to edit");String[]names=new String[screens.size()];for(int i=0;i<screens.size();i++)names[i]=screens.get(i).name;AlertDialog d=new AlertDialog.Builder(service).setTitle(App.name(id)+" — screen to edit").setItems(names,(dialog,n)->{try{new RuleStore(service).select(id,screens.get(n).key);service.setupZone(id);}catch(Exception e){App.toast(service,e.getMessage());}}).setNegativeButton("Cancel",null).create();d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);d.show();}catch(Exception e){App.toast(service,e.getMessage());}}
    public void close(){if(closed)return;closed=true;service.controlArea=null;App.MAIN.removeCallbacks(poll);App.MAIN.removeCallbacks(held);hideTarget();try{wm.removeView(this);}catch(Exception ignored){}}
    private final class Target extends View{
        boolean hot;final Paint p=new Paint(3);Target(){super(service);}
        @Override protected void onDraw(Canvas c){c.save();c.scale(getWidth()/112f,getHeight()/120f);p.setStyle(Paint.Style.FILL);p.setColor(hot?0xffd84455:0xe5304058);c.drawCircle(56,52,hot?45:40,p);p.setColor(Color.WHITE);p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(44,40,68,64,p);c.drawLine(68,40,44,64,p);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(12);c.drawText(hot?"Release to remove":"Remove",56,110,p);c.restore();}
    }
    private final class RuleIcon extends View {
        final int index;final String id;final Paint p=new Paint(3);
        RuleIcon(int n){super(service);index=n;id=App.IDS[n];setClickable(true);setOnLongClickListener(v->{editRule(id);return true;});setOnClickListener(v->{
            boolean selected=Boolean.TRUE.equals(service.automation.enabled.get(id));boolean resume=selected&&service.paused;
            try{new RuleStore(service).enabled(id,resume||!selected);service.reload();if(resume)service.paused=false;}catch(Exception e){App.toast(service,e.getMessage());}
        });}
        @Override protected void onDraw(Canvas c){
            boolean selected=Boolean.TRUE.equals(service.automation.enabled.get(id)),configured=Boolean.TRUE.equals(service.automation.configured.get(id)),error="ERROR".equals(service.automation.states.get(id));boolean on=selected&&configured&&!service.paused&&!error;
            String badge=!selected?"OFF":!configured?"SETUP":error?"ERROR":service.paused?"PAUSED":"ON";
            setContentDescription(App.name(id)+" "+badge+". Hold to set up.");c.save();c.scale(getWidth()/60f,getHeight()/62f);p.setStyle(Paint.Style.FILL);p.setColor(on?0xff17664d:0xff223145);c.drawRoundRect(1,1,59,61,10,10,p);p.setColor(error?0xffff8494:on?0xff8affc1:selected?0xffffd280:0xff8998ac);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(index==0){p.setStyle(Paint.Style.STROKE);Path tick=new Path();tick.moveTo(17,18);tick.lineTo(26,27);tick.lineTo(44,9);c.drawPath(tick,p);}
            else if(index==1){p.setStyle(Paint.Style.STROKE);c.drawRoundRect(17,6,43,30,4,4,p);c.drawRect(21,10,39,21,p);c.drawLine(30,10,30,21,p);c.drawLine(21,26,23,26,p);c.drawLine(37,26,39,26,p);c.drawLine(21,31,21,34,p);c.drawLine(39,31,39,34,p);}
            else {p.setStyle(Paint.Style.FILL);c.drawCircle(30,10,5,p);c.drawCircle(17,15,4,p);c.drawCircle(43,15,4,p);c.drawRoundRect(23,17,37,33,5,5,p);c.drawRoundRect(10,21,21,33,4,4,p);c.drawRoundRect(39,21,50,33,4,4,p);}
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(11);c.drawText(index==0?"Ready":index==1?"Leave":"Party",30,45,p);p.setTextSize(8);c.drawText(badge,30,56,p);c.restore();
        }
    }
}
