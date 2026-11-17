package com.kev.fortniteautobot;

import android.app.*;
import android.content.*;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

/** Dedicated F launcher. It never renders Setup's rule cards. */
public final class PlayActivity extends Activity {
    private TextView status;
    private Button overlay,taps,retry;
    private boolean requesting,awaiting,halted,visible;
    private long waitingAt;
    private String setupRule;
    private final Runnable poll=new Runnable(){public void run(){if(!visible)return;prepare();App.MAIN.postDelayed(this,300);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);setupRule=getIntent().getStringExtra("setupRule");if(setupRule!=null)try{App.index(setupRule);}catch(Exception e){setupRule=null;}
        LinearLayout body=App.body(this);App.text(this,body,"Auto Bot",28);status=App.text(this,body,"Opening Fortnite…",18);
        overlay=App.button(this,body,"Allow floating overlay",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        taps=App.button(this,body,"Enable Auto Bot taps",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        retry=App.button(this,body,"Retry",()->{halted=false;awaiting=false;prepare();});
        App.button(this,body,"Open Setup",()->{App.settings(this);finish();});
        if(state!=null){requesting=state.getBoolean("requesting");awaiting=state.getBoolean("awaiting");halted=state.getBoolean("halted");setupRule=state.getString("setupRule",setupRule);waitingAt=SystemClock.elapsedRealtime();}
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);setupRule=intent.getStringExtra("setupRule");if(setupRule!=null)try{App.index(setupRule);}catch(Exception e){setupRule=null;}halted=false;prepare();}
    @Override protected void onResume(){super.onResume();visible=true;poll.run();}
    @Override protected void onPause(){visible=false;App.MAIN.removeCallbacks(poll);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("requesting",requesting);b.putBoolean("awaiting",awaiting);b.putBoolean("halted",halted);b.putString("setupRule",setupRule);}
    private void say(String text){status.setText(text);}
    private void prepare(){
        overlay.setVisibility(Settings.canDrawOverlays(this)?android.view.View.GONE:android.view.View.VISIBLE);
        taps.setVisibility(App.taps==null?android.view.View.VISIBLE:android.view.View.GONE);retry.setVisibility(halted?android.view.View.VISIBLE:android.view.View.GONE);
        if(requesting||halted)return;
        CaptureService s=App.capture;if(s!=null)s.setDebugSession(setupRule!=null);
        if(awaiting){
            CaptureService.Frame f=s==null?null:s.latest;
            if(s!=null&&!s.stopped&&s.ready&&f!=null&&SystemClock.elapsedRealtime()-f.at<1500){awaiting=false;openGame(s);return;}
            if(SystemClock.elapsedRealtime()-waitingAt>15000){awaiting=false;halted=true;say("Capture did not become ready: "+App.status);if(s!=null){s.launchWaiting=false;s.stopSelf();}return;}
            say("Starting capture…\n"+App.status);return;
        }
        if(!Settings.canDrawOverlays(this)){say("Allow the floating overlay to continue.");return;}
        if(App.taps==null){say("Enable Auto Bot taps in Accessibility. Waiting for the service to connect…");return;}
        if(getPackageManager().getLaunchIntentForPackage(App.GAME)==null){halted=true;say("The installed official Fortnite app could not be found.");return;}
        if(s!=null&&!s.stopped){s.cancelTeaching();s.launchWaiting=true;s.pause(setupRule!=null,setupRule==null?"Opening Fortnite":"Opening in-game setup");awaiting=true;waitingAt=SystemClock.elapsedRealtime();prepare();return;}
        requesting=true;say("Approve screen capture to start Auto Bot.");MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent consent=Build.VERSION.SDK_INT>=34?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):m.createScreenCaptureIntent();startActivityForResult(consent,100);
    }
    private void openGame(CaptureService s){halted=true;s.launchWaiting=false;String rule=setupRule;if(rule!=null)s.pause(true,"Opening in-game zone setup");if(App.launch(this)){if(rule!=null)App.MAIN.postDelayed(()->{if(!s.stopped)s.setupZone(rule);},500);finish();}else say("Fortnite could not be launched. Tap Retry.");}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=100)return;requesting=false;
        if(result!=RESULT_OK||data==null){halted=true;say("Capture was not approved. Tap Retry when ready.");return;}
        try{awaiting=true;waitingAt=SystemClock.elapsedRealtime();startForegroundService(new Intent(this,CaptureService.class).putExtra("result",result).putExtra("consent",data).putExtra("debugSession",setupRule!=null));say("Starting capture…");}catch(Exception e){awaiting=false;halted=true;say("Capture failed: "+e.getMessage());}
    }
}
