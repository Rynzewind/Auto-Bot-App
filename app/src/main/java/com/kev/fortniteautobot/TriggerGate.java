package com.kev.fortniteautobot;

/** One trigger per visual appearance, three fresh matches and three clear frames. */
public final class TriggerGate {
    private int seen,clear;
    private boolean latched;
    private long last=-1,lastTrigger=-10000;
    public boolean observe(boolean valid,boolean match,long at){
        return observe(valid,match,at,true);
    }
    public boolean observe(boolean valid,boolean match,long at,boolean mayTrigger){
        if(!valid){interrupt();return false;}
        if(at<=last)return false;
        if(last>=0&&at-last>1500)interrupt();
        last=at;
        if(latched){clear=match?0:clear+1;if(clear>=3){latched=false;clear=0;seen=0;}return false;}
        if(!mayTrigger){seen=0;return false;}
        seen=match?seen+1:0;
        if(seen>=3&&at-lastTrigger>=5000){seen=0;clear=0;latched=true;lastTrigger=at;return true;}
        return false;
    }
    public void interrupt(){seen=0;clear=0;}
    public int hits(){return seen;}
    public int clearFrames(){return clear;}
    public boolean latched(){return latched;}
}
