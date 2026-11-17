package com.kev.fortniteautobot;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.io.*;
public class MainActivity extends Activity {
    private boolean launchWanted,requesting,awaiting,visible;
    private long awaitingAt;
    private TextView status;
    private String setupRule;
    private final Runnable refresh=new Runnable(){public void run(){if(!visible)return;updateStatus();CaptureService s=App.capture;
        if(awaiting&&s!=null&&s.ready&&!s.stopped){awaiting=false;openGame();}
        else if(awaiting&&SystemClock.elapsedRealtime()-awaitingAt>15000){awaiting=false;launchWanted=false;App.status="Landscape capture did not become ready. Tap Open Fortnite to retry.";if(s!=null)s.stopSelf();}
        App.MAIN.postDelayed(this,400);
    }};
    @Override public void onCreate(Bundle state){super.onCreate(state);launchWanted=false;setupRule=getIntent().getStringExtra("setupRule");if(setupRule!=null)try{App.index(setupRule);}catch(Exception e){setupRule=null;}render();if(state!=null){requesting=state.getBoolean("requesting");awaiting=state.getBoolean("awaiting");awaitingAt=SystemClock.elapsedRealtime();setupRule=state.getString("setupRule",setupRule);launchWanted=state.getBoolean("launchWanted",launchWanted);}if(launchWanted)App.MAIN.post(this::prepare);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);launchWanted=false;awaiting=false;setupRule=null;render();}
    @Override protected void onResume(){super.onResume();if(App.capture!=null)App.capture.setDebugSession(true);visible=true;refresh.run();if(launchWanted&&!requesting&&!awaiting)App.MAIN.post(this::prepare);}
    @Override protected void onPause(){visible=false;App.MAIN.removeCallbacks(refresh);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("requesting",requesting);b.putBoolean("awaiting",awaiting);b.putBoolean("launchWanted",launchWanted);b.putString("setupRule",setupRule);}
    private void openGame(){CaptureService s=App.capture;String rule=setupRule;if(rule!=null&&s!=null)s.pause(true,"Opening in-game zone setup");if(App.launch(this)){if(rule!=null&&s!=null)App.MAIN.postDelayed(()->{if(!s.stopped)s.setupZone(rule);},500);finish();}}
    private void render(){
        LinearLayout body=App.body(this);App.text(this,body,"Auto Bot Setup",27);App.text(this,body,"Ready, Leave and Party — multiple screen layouts, one floating control.",15);
        status=App.text(this,body,"Checking permissions…",16);
        LinearLayout row=new LinearLayout(this);body.addView(row);
        App.button(this,row,"Open Fortnite",()->{launchWanted=true;prepare();});
        App.button(this,row,"Overlay permission",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        App.button(this,row,"Accessibility",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        if(Build.VERSION.SDK_INT>=33)App.button(this,row,"Notifications",()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},90));
        Switch live=new Switch(this);live.setText("Setup diagnostics (F launcher starts without debug)");live.setTextColor(android.graphics.Color.WHITE);live.setChecked(App.prefs(this).getBoolean("liveDebug",true));live.setOnCheckedChangeListener((v,on)->{App.prefs(this).edit().putBoolean("liveDebug",on).apply();if(App.capture!=null)App.capture.setDebugSession(on);});body.addView(live);
        App.text(this,body,"Delay after trigger: applies to Ready, Leave and Party. Green countdown appears above the overlay icons; recorded tap timing follows.",14);
        Spinner delay=new Spinner(this);String[] delays=new String[11];for(int i=0;i<delays.length;i++)delays[i]=i+" seconds";
        delay.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,delays));delay.setSelection(Math.max(0,Math.min(10,App.prefs(this).getInt("triggerDelayMs",3000)/1000)));body.addView(delay);
        delay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int n,long id){App.prefs(MainActivity.this).edit().putInt("triggerDelayMs",n*1000).apply();}});
        for(String id:App.IDS){
            RuleStore.Rule rule=new RuleStore(this).load(id);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(12,8,12,8);card.setBackgroundColor(0xff192a40);LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);spacing.setMargins(0,8,0,8);body.addView(card,spacing);
            Switch on=new Switch(this);on.setText(App.name(id));on.setTextColor(0xffeaf5ff);on.setTextSize(20);on.setChecked(rule.enabled);card.addView(on);
            on.setOnCheckedChangeListener((v,enabled)->{try{new RuleStore(this).enabled(id,enabled);if(App.capture!=null)App.capture.reload();}catch(Exception e){App.toast(this,e.getMessage());}});
            java.util.List<RuleStore.Rule>screens=new RuleStore(this).all(id);int complete=0;for(RuleStore.Rule screen:screens)if(screen.configured())complete++;App.text(this,card,complete+" / "+screens.size()+" screen variants configured. Each screen has its own watch picture and taps.",14);
            App.button(this,card,"Set up / edit "+id,()->{if(App.capture!=null)App.capture.pause(true,"Editing setup");App.editor(this,id);});
        }
        App.text(this,body,"The F shortcut opens Fortnite using these settings. Green icons are on; unconfigured rules wait without tapping. Tap the overlay grip for options; hold an icon to edit it.",14);
        LinearLayout backup=new LinearLayout(this);body.addView(backup);
        App.button(this,backup,"Export all setup",()->{pauseForBackup();startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Auto-Bot-Setup.zip"),301);});
        App.button(this,backup,"Import setup",()->{pauseForBackup();startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE),302);});
        App.button(this,backup,"Export debug log",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Auto-Bot-Diagnostics.txt"),303));
        App.button(this,backup,"Stop capture",()->{if(App.capture!=null){App.capture.pause(true,"Stopped");App.capture.stopSelf();}updateStatus();});
        updateStatus();
    }
    private void pauseForBackup(){if(App.capture!=null){App.capture.pause(true,"Setup transfer; automation paused");App.capture.cancelTeaching();}}
    private void updateStatus(){if(status==null)return;CaptureService s=App.capture;status.setText((Settings.canDrawOverlays(this)?"✓":"○")+" Overlay  "+(App.taps!=null?"✓":"○")+" Accessibility  "+(s!=null&&s.ready?"✓":"○")+" Capture\n"+App.status+"\n"+App.lastAction);}
    private void prepare(){
        if(!launchWanted||requesting||awaiting)return;
        if(!Settings.canDrawOverlays(this)){App.status="Grant overlay permission, then Open Fortnite";updateStatus();return;}
        if(App.taps==null){App.status="Enable Auto Bot taps in Accessibility, then Open Fortnite";updateStatus();return;}
        if(getPackageManager().getLaunchIntentForPackage(App.GAME)==null){App.status="The official Fortnite app is not installed";updateStatus();return;}
        CaptureService s=App.capture;
        if(s!=null&&!s.stopped){s.setDebugSession(true);s.pause(setupRule!=null,setupRule==null?"Watching selected rules":"Opening zone setup");if(s.ready)openGame();else{awaiting=true;awaitingAt=SystemClock.elapsedRealtime();}return;}
        requesting=true;
        MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent capture=Build.VERSION.SDK_INT>=34?manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):manager.createScreenCaptureIntent();
        startActivityForResult(capture,100);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==100){requesting=false;if(result!=RESULT_OK||data==null){launchWanted=false;App.status="Capture was not approved. Tap Open Fortnite to retry.";updateStatus();return;}
            try{awaiting=true;awaitingAt=SystemClock.elapsedRealtime();startForegroundService(new Intent(this,CaptureService.class).putExtra("result",result).putExtra("consent",data).putExtra("debugSession",true));App.status="Starting landscape screen capture…";}catch(Exception e){awaiting=false;launchWanted=false;App.status=e.getMessage();}return;}
        if((request==301||request==302||request==303)&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();new Thread(()->{try{
            if(request==301){try(OutputStream out=getContentResolver().openOutputStream(uri)){Backup.exportSetup(this,out);}App.toast(this,"All three setups exported");}
            else if(request==302){try(InputStream in=getContentResolver().openInputStream(uri)){Backup.importSetup(this,in);}if(App.capture!=null)App.capture.reload();App.MAIN.post(this::render);App.toast(this,"All screen variants imported; automation remains paused");}
            else{try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(DiagnosticsText.snapshot(App.capture).getBytes(java.nio.charset.StandardCharsets.UTF_8));File log=new File(getFilesDir(),"events.log");if(log.isFile()){out.write("\nRecent tap/error events:\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(java.nio.file.Files.readAllBytes(log.toPath()));}}App.toast(this,"Diagnostics exported");}
        }catch(Exception e){App.toast(this,"Setup transfer failed: "+e.getMessage());}},"AutoBot backup").start();}
    }
}
