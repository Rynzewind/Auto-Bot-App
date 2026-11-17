package com.kev.fortniteautobot;
import android.content.Context;
import android.graphics.*;
import org.json.*;
/** Small numbered points; they are independent of watch zones and never alter stored bitmaps. */
public final class TapMarkers {
    public static void all(Canvas c,Paint paint,JSONArray steps,RectF image,Context context){if(steps==null)return;for(int i=0;i<steps.length();i++)try{one(c,paint,steps.getJSONObject(i),i+1,image,context);}catch(Exception ignored){}}
    public static void one(Canvas c,Paint paint,JSONObject step,int number,RectF image,Context context){
        try{double nx=step.getDouble("x"),ny=step.getDouble("y");if(!MatchMath.unit(nx)||!MatchMath.unit(ny))return;
            float x=TapCoordinates.preview(nx,image.left,image.width()),y=TapCoordinates.preview(ny,image.top,image.height());
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff061322);c.drawCircle(x,y,App.dp(context,7.5f),paint);paint.setColor(0xff38caff);c.drawCircle(x,y,App.dp(context,6),paint);
            paint.setColor(Color.BLACK);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(App.dp(context,8));paint.setTextAlign(Paint.Align.CENTER);c.drawText(String.valueOf(number),x,y-(paint.ascent()+paint.descent())/2,paint);paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT);
        }catch(JSONException ignored){}
    }
}
