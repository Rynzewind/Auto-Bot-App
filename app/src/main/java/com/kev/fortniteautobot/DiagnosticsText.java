package com.kev.fortniteautobot;
import java.util.Locale;
public final class DiagnosticsText {
    public static String snapshot(CaptureService service){
        StringBuilder text=new StringBuilder("Auto Bot diagnostics\n");text.append(java.time.Instant.now()).append("\n");
        if(service==null)return text.append("Capture is stopped.\n").toString();
        CaptureService.Frame frame=service.latest;text.append("Paused: ").append(service.paused).append("; foreground: ").append(App.foreground).append("; Accessibility: ").append(App.taps!=null).append("\n");
        if(frame!=null)text.append("Frame #").append(service.frameNumber).append(" age ").append(android.os.SystemClock.elapsedRealtime()-frame.at).append("ms; ").append(frame.physicalW).append('x').append(frame.physicalH).append(" rot ").append(frame.rotation).append("\n");
        text.append("Status: ").append(App.status).append("\nLast action: ").append(App.lastAction).append("\nLast Android tap normalized: ").append(service.lastTapX).append(',').append(service.lastTapY).append(" -> actual sent pixel ").append(service.lastTapPixelX).append(',').append(service.lastTapPixelY).append(" on capture ").append(service.tapDisplayW).append('x').append(service.tapDisplayH).append("; unused Accessibility-window metrics ").append(service.tapContextW).append('x').append(service.tapContextH).append(' ').append(service.tapResult).append("\n\nLast in-game screen diagnostics (age ").append(android.os.SystemClock.elapsedRealtime()-service.automation.lastGameAt).append("ms):\n");
        Automation.Snapshot snap=service.automation.lastGameDebug;
        for(Automation.ViewState v:snap.rules){RuleStore.Rule r=v.rule;text.append(r.label()).append(" [").append(r.key).append("] enabled=").append(r.enabled).append(" configured=").append(r.configured()).append(" score=").append(String.format(Locale.ROOT,"%.3f",v.score)).append(" threshold=").append(r.threshold).append(" hits=").append(v.hits).append("/3 latched=").append(v.latched).append(" clear=").append(v.clear).append("/3 savedRotation=").append(r.rotation).append(" aspect=").append(r.aspect).append("\n  ").append(v.reason).append("\n");}
        if(snap.playing!=null)text.append("Playing ").append(snap.playing.label()).append(" step ").append(snap.step+1).append(" saved-coordinate playback; no tap threshold\n");
        return text.toString();
    }
}
