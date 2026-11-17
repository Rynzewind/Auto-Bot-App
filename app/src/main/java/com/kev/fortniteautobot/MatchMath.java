package com.kev.fortniteautobot;
public final class MatchMath {
    public static boolean hasDetail(int[]v){if(v.length==0)return false;double mean=0;for(int n:v)mean+=n;mean/=v.length;double variation=0;for(int n:v)variation+=(n-mean)*(n-mean);return variation>=1000;}
    public static boolean unit(double n){return Double.isFinite(n)&&n>=0&&n<=1;}
    public static boolean region(double[]r){return r!=null&&r.length==4&&unit(r[0])&&unit(r[1])&&Double.isFinite(r[2])&&Double.isFinite(r[3])&&r[2]>0&&r[3]>0&&r[0]+r[2]<=1.000001&&r[1]+r[3]<=1.000001;}
    public static double compare(int[]a,int[]b){
        if(a.length!=b.length||a.length==0)return 0;
        double ma=0,mb=0;for(int i=0;i<a.length;i++){ma+=a[i];mb+=b[i];}ma/=a.length;mb/=b.length;
        double aa=0,bb=0,cross=0,error=0;
        for(int i=0;i<a.length;i++){double x=a[i]-ma,y=b[i]-mb;aa+=x*x;bb+=y*y;cross+=x*y;error+=Math.abs(a[i]-b[i]);}
        if(aa<1000||bb<1000)return 0;
        return Math.max(0,Math.min(1,Math.max(0,cross/Math.sqrt(aa*bb))*(1-error/a.length/255)));
    }
    public static boolean drop(float cx,float cy,int width,int height,float density){double dx=cx-width/2.0,dy=cy-(height-84*density);return dx*dx+dy*dy<=56*56*density*density;}
    public static int clamp(int n,int size,int extent){return Math.max(0,Math.min(n,Math.max(0,size-extent)));}
}
