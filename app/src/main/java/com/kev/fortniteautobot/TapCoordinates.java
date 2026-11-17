package com.kev.fortniteautobot;
/** Record, replay, and review all use the full captured display, never a service window size. */
public final class TapCoordinates {
    public static double normalize(double pixel,int extent){if(extent<=0||!Double.isFinite(pixel))throw new IllegalArgumentException("Invalid display point");return Math.max(0,Math.min(1,pixel/extent));}
    public static float pixel(double normalized,int extent){if(!MatchMath.unit(normalized)||extent<=0)throw new IllegalArgumentException("Invalid tap/display");return (float)Math.max(0,Math.min(extent-1,normalized*extent));}
    public static float preview(double normalized,float origin,float extent){if(!MatchMath.unit(normalized))throw new IllegalArgumentException("Invalid saved tap");return origin+(float)normalized*extent;}
}
