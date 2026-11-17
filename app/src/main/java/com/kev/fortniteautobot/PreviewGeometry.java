package com.kev.fortniteautobot;
/** Coordinates exclude the preview's letterbox margins. */
public final class PreviewGeometry {
    public static float[] fit(int iw,int ih,int vw,int vh){
        if(iw<=0||ih<=0||vw<=0||vh<=0)return new float[]{0,0,0,0};
        float scale=Math.min((float)vw/iw,(float)vh/ih),w=iw*scale,h=ih*scale;
        return new float[]{(vw-w)/2,(vh-h)/2,w,h};
    }
    public static float normal(float point,float origin,float size){return size<=0?0:Math.max(0,Math.min(1,(point-origin)/size));}
    public static boolean importSize(int w,int h){return w>0&&h>0&&w<=32768&&h<=32768&&(long)w*h<=100000000;}
}
