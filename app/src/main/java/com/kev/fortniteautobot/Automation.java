package com.kev.fortniteautobot;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Automation {
    public static final class ViewState {
        public final RuleStore.Rule rule;public final double score;public final String reason;public final int hits,clear;public final boolean latched;
        ViewState(RuleStore.Rule r,double s,String why,TriggerGate g){rule=r;score=s;reason=why;hits=g.hits();clear=g.clearFrames();latched=g.latched();}
    }
    public static final class Snapshot {
        public final List<ViewState>rules;public final List<double[]>protectedAreas;public final RuleStore.Rule playing;public final int step,hits;public final double stepScore;public final Map<String,String>chosen;
        Snapshot(List<ViewState>r,List<double[]>p,RuleStore.Rule active,int n,int matches,double score,Map<String,String>selected){rules=Collections.unmodifiableList(r);protectedAreas=p;playing=active;step=n;hits=matches;stepScore=score;chosen=Collections.unmodifiableMap(new HashMap<>(selected));}
    }
    private final CaptureService service;private final RuleStore store;
    private final Map<String,List<RuleStore.Rule>>groups=new LinkedHashMap<>();
    private final Map<String,TriggerGate>gates=new HashMap<>();
    private final Map<String,String>errors=new HashMap<>(),reasons=new HashMap<>();
    private final Map<String,String>selectedScreens=new HashMap<>();
    private final Map<String,Double>variantScores=new HashMap<>();
    private List<double[]>protectedAreas=Collections.emptyList();
    public final Map<String,String>states=new ConcurrentHashMap<>();public final Map<String,Double>scores=new ConcurrentHashMap<>();
    public final Map<String,Boolean>enabled=new ConcurrentHashMap<>(),configured=new ConcurrentHashMap<>();
    public volatile Snapshot debug=new Snapshot(new ArrayList<>(),Collections.emptyList(),null,0,0,Double.NaN,Collections.emptyMap());
    public volatile Snapshot lastGameDebug=debug;
    public volatile long lastGameAt;
    private RuleStore.Rule playing;private int step,matches;private long epoch,notBefore;private boolean busy;private double stepScore=Double.NaN;
    public volatile long countdownUntil;
    public volatile String countdownLabel="";
    private boolean lastScoredFlat;
    public Automation(CaptureService s){service=s;store=new RuleStore(s);reload();}
    public void reload(){
        cancel("Settings loaded");groups.clear();gates.clear();errors.clear();reasons.clear();variantScores.clear();selectedScreens.clear();List<double[]>areas=new ArrayList<>();
        for(String id:new String[]{"leave","party","ready"}){
            List<RuleStore.Rule>rs=store.all(id);groups.put(id,rs);boolean any=false,on=true;int count=0;
            TriggerGate groupGate=new TriggerGate();for(RuleStore.Rule r:rs){gates.put(r.identity(),groupGate);reasons.put(r.identity(),r.error.isEmpty()?"Waiting for frame":r.error);on=r.enabled;if(r.configured()){any=true;count++;}if(MatchMath.region(r.region))areas.add(r.region);for(RuleStore.Step s:r.steps)if(MatchMath.region(s.rect))areas.add(s.rect);}
            enabled.put(id,on);configured.put(id,any);states.put(id,on?(any?count+" screens configured":"Setup needed"):"OFF");
        }
        protectedAreas=Collections.unmodifiableList(areas);publish();
    }
    public void cancel(String why){countdownUntil=0;playing=null;busy=false;matches=0;stepScore=Double.NaN;for(TriggerGate g:gates.values())g.interrupt();App.status=why;}
    private String blocked(CaptureService.Frame f,RuleStore.Rule r){
        if(service.stopped)return "Capture stopped";
        if(f==null)return "No capture frame";
        long age=SystemClock.elapsedRealtime()-f.at;if(age<0||age>1200)return "Stale frame: "+age+"ms";
        if(App.taps==null)return "Accessibility service off";
        if(!App.isGame())return "Game not foreground: "+App.foreground;
        if(service.controlsOverlap(r.region))return "Floating controls cover watch zone; drag them away";
        if(((KeyguardManager)service.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked())return "Phone locked";
        if(!((PowerManager)service.getSystemService(Context.POWER_SERVICE)).isInteractive())return "Screen off";
        Rect b=App.bounds(service);if(f.physicalW!=b.width()||f.physicalH!=b.height())return "Capture/display size differs";
        if(f.physicalW<=f.physicalH)return "Landscape needed";
        if(f.rotation!=App.rotation(service)||f.rotation!=r.rotation)return "Rotation "+f.rotation+"; saved "+r.rotation;
        if(Math.abs((double)f.bitmap.getWidth()/f.bitmap.getHeight()-r.aspect)>=0.01)return "Screen aspect differs from saved picture";
        return "";
    }
    private double score(CaptureService.Frame f,double[]rect,int[]ref){Bitmap b=App.crop(f.bitmap,rect);try{int[]pixels=App.vector(b);lastScoredFlat=!MatchMath.hasDetail(pixels);return MatchMath.compare(pixels,ref);}finally{if(b!=f.bitmap)b.recycle();}}
    public void frame(CaptureService.Frame f){
        for(List<RuleStore.Rule>rs:groups.values())for(RuleStore.Rule r:rs){
            String why=!r.error.isEmpty()?r.error:r.pixels==null||!MatchMath.region(r.region)?"Needs watch picture/zone":blocked(f,r);double confidence=Double.NaN;
            if(why.isEmpty()){confidence=score(f,r.region,r.pixels);if(lastScoredFlat)why="Current watch area blank/flat: capture or wrong zone";}
            variantScores.put(r.identity(),confidence);
            if(!r.enabled)why="OFF";
            else if(!why.isEmpty()){}
            else if(errors.containsKey(r.identity()))why=errors.get(r.identity());
            else if(service.teaching())why="Setup/recording active; automatic taps paused";
            else if(service.paused)why="PAUSED (live recognition only)";
            else if(r.steps.isEmpty())why="No tap recording for this screen (watch score is live)";
            else why=confidence>=r.threshold?"Picture matches":"Watch score below threshold";
            reasons.put(r.identity(),why);
        }
        for(String id:groups.keySet()){double best=Double.NaN;for(RuleStore.Rule r:groups.get(id)){double s=variantScores.get(r.identity());if(Double.isFinite(s)&&(!Double.isFinite(best)||s>best))best=s;}if(Double.isFinite(best))scores.put(id,best);else scores.remove(id);}
        if(service.paused||service.teaching()){for(TriggerGate g:gates.values())g.interrupt();publish();return;}
        if(playing!=null){replay(f);publish();return;}
        boolean fired=false;
        for(String id:groups.keySet()){
            List<RuleStore.Rule>rs=groups.get(id);double[]values=new double[rs.size()],limits=new double[rs.size()];
            for(int i=0;i<rs.size();i++){RuleStore.Rule r=rs.get(i);values[i]=r.enabled&&r.configured()&&!errors.containsKey(r.identity())?variantScores.get(r.identity()):Double.NaN;limits[i]=r.threshold;}
            int preferred=-1;for(int i=0;i<rs.size();i++)if(rs.get(i).key.equals(selectedScreens.get(id)))preferred=i;
            int chosen=VariantPolicy.choose(values,limits,preferred);TriggerGate groupGate=gates.get(rs.get(0).identity());
            if(chosen>=0){String key=rs.get(chosen).key;if(!key.equals(selectedScreens.get(id))&&!groupGate.latched())groupGate.interrupt();selectedScreens.put(id,key);}
            boolean anyValid=false,anyMatch=false;for(int i=0;i<values.length;i++){if(Double.isFinite(values[i]))anyValid=true;if(Double.isFinite(values[i])&&values[i]>=limits[i])anyMatch=true;}
            boolean trigger=groupGate.observe(anyValid,anyMatch,f.at,!fired&&chosen>=0);
            for(int i=0;i<rs.size();i++){
                RuleStore.Rule r=rs.get(i);TriggerGate gate=gates.get(r.identity());boolean valid=Double.isFinite(values[i]),match=valid&&values[i]>=r.threshold;
                if(gate.latched()&&valid)reasons.put(r.identity(),"Waiting for all matching screens to clear "+gate.clearFrames()+"/3");
                else if(match&&i!=chosen)reasons.put(r.identity(),"Using "+rs.get(chosen).name+" once for this appearance");
                else if(match)reasons.put(r.identity(),"Checking picture "+gate.hits()+"/3");
                if(trigger&&i==chosen){playing=r;step=0;matches=0;busy=false;epoch=service.epoch.get();notBefore=f.at+Math.max(0,Math.min(10000,App.prefs(service).getInt("triggerDelayMs",3000)))+r.steps.get(0).wait;countdownLabel=r.label();countdownUntil=notBefore;stepScore=Double.NaN;App.status=r.label()+": waiting for tap 1";fired=true;}
            }
        }
        publish();
    }
    private void publish(){
        List<ViewState>views=new ArrayList<>();for(String id:groups.keySet()){
            String base="";boolean allErrors=true,hasConfigured=false;
            for(RuleStore.Rule r:groups.get(id)){String why=reasons.get(r.identity());if(playing==r)why="Playing tap "+(step+1)+"/"+r.steps.size();views.add(new ViewState(r,variantScores.containsKey(r.identity())?variantScores.get(r.identity()):Double.NaN,why==null?"Waiting":why,gates.get(r.identity())));if(r.configured()){hasConfigured=true;if(!errors.containsKey(r.identity()))allErrors=false;}if(base.isEmpty()||playing==r)base=r.name+": "+why;}
            if(hasConfigured&&allErrors)states.put(id,"ERROR");else states.put(id,Boolean.TRUE.equals(enabled.get(id))?base:"OFF");
        }
        debug=new Snapshot(views,protectedAreas,playing,step,matches,stepScore,selectedScreens);if(App.isGame()){lastGameDebug=debug;CaptureService.Frame f=service.latest;if(f!=null)lastGameAt=f.at;}
    }
    private void fail(String why){if(playing!=null){errors.put(playing.identity(),why);reasons.put(playing.identity(),why);}service.log(why);cancel(why);}
    private void replay(CaptureService.Frame frame){
        if(playing==null)return;
        if(epoch!=service.epoch.get()){cancel("Pending taps cancelled");return;}
        String why=blocked(frame,playing);if(!why.isEmpty()){fail("Replay stopped: "+why);return;}
        if(busy||frame.at<notBefore)return;
        RuleStore.Step s=playing.steps.get(step);
        App.status=playing.label()+" tap "+(step+1)+"/"+playing.steps.size()+": playing saved coordinates";
        countdownUntil=0;busy=true;long token=epoch;RuleStore.Rule expected=playing;int expectedStep=step;
        App.MAIN.post(()->{
            if(token!=service.epoch.get()||service.paused||service.teaching()||!App.isGame()||App.taps==null){service.worker.post(()->cancel("Tap cancelled: pause, setup, focus, or Accessibility changed"));return;}
            CaptureService.Frame fresh=service.latest;if(fresh==null||!blocked(fresh,expected).isEmpty()){service.worker.post(()->fail("No valid fresh frame; tap not sent"));return;}
            App.taps.tap(s.x,s.y,ok->service.worker.post(()->{
                if(token!=service.epoch.get()||playing!=expected||step!=expectedStep)return;
                busy=false;matches=0;if(!ok){fail("Android cancelled/rejected tap "+(step+1));publish();return;}
                App.lastAction=expected.label()+" tap "+(step+1)+" sent";service.log(App.lastAction);step++;
                if(step>=expected.steps.size()){playing=null;App.status=expected.label()+": recording sent; waiting for picture to clear";}
                else{notBefore=SystemClock.elapsedRealtime()+expected.steps.get(step).wait;stepScore=Double.NaN;}
                publish();
            }));
        });
    }
}
