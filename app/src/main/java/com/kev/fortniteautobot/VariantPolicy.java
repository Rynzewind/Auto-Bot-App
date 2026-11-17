package com.kev.fortniteautobot;
/** Choose one screen; keep a near-tied choice stable while confirming it. */
public final class VariantPolicy {
    public static int choose(double[]scores,double[]thresholds,int preferred){
        if(scores.length!=thresholds.length)throw new IllegalArgumentException("Mismatched variants");
        int best=-1;double first=-1;
        for(int i=0;i<scores.length;i++){double s=scores[i];if(!Double.isFinite(s)||s<thresholds[i])continue;if(s>first){first=s;best=i;}}
        if(preferred>=0&&preferred<scores.length&&Double.isFinite(scores[preferred])&&scores[preferred]>=thresholds[preferred]&&first-scores[preferred]<=0.0300001)return preferred;
        return best;
    }
}
