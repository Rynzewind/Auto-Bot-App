package com.kev.fortniteautobot;
public final class DebugGeometry {
    public static double[] protect(double[]r,int width,int height){double margin=3.0/Math.min(960,width);return new double[]{r[0]*width-margin*width,r[1]*height-margin*width,(r[0]+r[2])*width+margin*width,(r[1]+r[3])*height+margin*width};}
    public static int corner(int width,int height,int w,int h,java.util.List<double[]>areas){
        double best=Double.POSITIVE_INFINITY;int chosen=0;
        for(int i=0;i<4;i++){int x=(i==0||i==1)?Math.max(0,width-w):0,y=(i==1||i==2)?Math.max(0,height-h):0;double overlap=0;
            for(double[]r:areas)overlap+=Math.max(0,Math.min(x+w,r[2])-Math.max(x,r[0]))*Math.max(0,Math.min(y+h,r[3])-Math.max(y,r[1]));
            if(overlap<best){best=overlap;chosen=i;}
        }return chosen;
    }
}
