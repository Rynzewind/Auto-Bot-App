package com.kev.fortniteautobot;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.UUID;
import org.json.*;

public final class RuleActivity extends Activity {
    private static final int IMPORT_PICTURE=410;
    private String id,sourceName="";
    private Bitmap picture;
    private int rotation;
    private Selection preview;
    private TextView message;
    private EditText threshold;
    private boolean importing;
    private RuleStore editingStore;
    private JSONArray numberedTaps=new JSONArray();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);id=getIntent().getStringExtra("rule");
        try{App.index(id);}catch(Exception e){finish();return;}
        if(App.capture!=null){App.capture.setDebugSession(true);App.capture.pause(true,"Editing "+id);App.capture.cancelTeaching();}
        render(state);
    }
    private void render(Bundle state){
        picture=null;preview=null;sourceName="";numberedTaps=new JSONArray();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff101a2a);
        App.text(this,root,App.name(id)+" setup",23);
        message=App.text(this,root,"Import a full landscape screenshot or capture one. Drag a green watch zone; tap to move the magenta crosshair.",13);
        variants(root);
        LinearLayout pictures=new LinearLayout(this);root.addView(pictures);
        App.button(this,pictures,"Set zone over game",()->{
            if(importing)return;CaptureService s=App.capture;
            try{new RuleStore(this).select(id,editingStore.selectedKey(id));}catch(Exception e){App.toast(this,e.getMessage());return;}
            if(s==null)startActivity(new Intent(this,PlayActivity.class).putExtra("setupRule",id));else s.setupZone(id);
            finish();
        });
        App.button(this,pictures,"Import screenshot",()->{
            if(!importing)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),IMPORT_PICTURE);
        });
        App.button(this,pictures,"Back",()->{App.settings(this);finish();});
        LinearLayout actions=new LinearLayout(this);root.addView(actions);App.text(this,actions,"Match %",12);
        threshold=new EditText(this);threshold.setTextColor(Color.WHITE);threshold.setSingleLine(true);threshold.setEms(3);threshold.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);actions.addView(threshold);
        App.button(this,actions,"Save watch area",this::save);
        App.button(this,actions,"Record taps",()->{
            if(!save())return;CaptureService s=App.capture;
            if(s==null){message.setText("Start capture from Setup first");return;}
            try{new RuleStore(this).select(id,editingStore.selectedKey(id));}catch(Exception e){App.toast(this,e.getMessage());return;}s.record(id);finish();
        });
        RuleStore store=new RuleStore(this);editingStore=store;
        try{
            JSONObject j=store.json(id);threshold.setText(String.valueOf(Math.round(j.optDouble("threshold",id.equals("leave")?0.65:0.90)*100)));
            if(j.has("source")){sourceName=j.getString("source");picture=App.decode(store.file(id,sourceName));rotation=j.optInt("sourceRotation",App.rotation(this));}
            if(picture!=null){
                preview=new Selection();
                // A new draft image must not inherit a zone from a different screenshot.
                if(j.has("region")&&!j.optBoolean("sourceReset",false)){
                    double[]r=RuleStore.array(j.getJSONArray("region"));preview.rect.set((float)r[0],(float)r[1],(float)(r[0]+r[2]),(float)(r[1]+r[3]));
                    preview.markerX=preview.rect.centerX();preview.markerY=preview.rect.centerY();
                }
                root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
            }else App.text(this,root,"No picture yet. Import a PNG/JPEG screenshot or capture Fortnite in landscape.",16);
            JSONArray steps=j.optJSONArray("steps");numberedTaps=steps==null?new JSONArray():steps;int count=numberedTaps.length();
            TextView taps=App.text(this,root,count+" numbered taps • GREEN = watch zone • BLUE 1,2,3 = saved clicks. Tap here to review each screen.",12);taps.setOnClickListener(v->review());
        }catch(Exception e){message.setText("Setup issue: "+e.getMessage());}
        if(state!=null){
            if(state.containsKey("threshold"))threshold.setText(state.getString("threshold"));
            if(preview!=null&&sourceName.equals(state.getString("source"))){preview.rect.set(state.getFloat("l"),state.getFloat("t"),state.getFloat("r"),state.getFloat("b"));preview.markerX=state.getFloat("markerX",0.5f);preview.markerY=state.getFloat("markerY",0.5f);}
        }
        RuleStore.Rule checked=store.load(id);if(!checked.error.isEmpty())message.setText("Setup issue: "+checked.error);App.insets(root);setContentView(root);
    }
    private void variants(LinearLayout root){
        LinearLayout row=new LinearLayout(this);root.addView(row);App.text(this,row,"Screen",12);
        try{RuleStore store=new RuleStore(this);java.util.List<RuleStore.Variant>list=store.variants(id);String selected=store.selectedKey(id);Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,list));row.addView(spinner,new LinearLayout.LayoutParams(0,-2,1));int at=0;for(int i=0;i<list.size();i++)if(list.get(i).key.equals(selected))at=i;spinner.setSelection(at);spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?>p){}public void onItemSelected(AdapterView<?>p,View v,int position,long item){if(importing)return;try{String key=list.get(position).key;if(!new RuleStore(RuleActivity.this).selectedKey(id).equals(key)){new RuleStore(RuleActivity.this).select(id,key);render(null);}}catch(Exception e){App.toast(RuleActivity.this,e.getMessage());}}});
            App.button(this,row,"Add screen",()->nameScreen(false));App.button(this,row,"Rename",()->nameScreen(true));App.button(this,row,"Remove",()->{if(importing)return;new AlertDialog.Builder(this).setTitle("Remove this screen variant?").setMessage("Other screen variants and their recordings are kept.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{try{new RuleStore(this).removeSelected(id);if(App.capture!=null)App.capture.reload();render(null);}catch(Exception e){App.toast(this,e.getMessage());}}).show();});
        }catch(Exception e){App.text(this,row,e.getMessage(),12);}
    }
    private void nameScreen(boolean rename){if(importing)return;EditText input=new EditText(this);input.setSingleLine(true);input.setHint("e.g. Lobby / alternate party screen");if(rename)try{input.setText(new RuleStore(this).json(id).optString("variantName","Screen 1"));}catch(Exception ignored){}
        new AlertDialog.Builder(this).setTitle(rename?"Rename screen":"Add screen with its own picture and taps").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{try{RuleStore store=new RuleStore(this);if(rename)store.rename(id,input.getText().toString());else store.add(id,input.getText().toString());if(App.capture!=null)App.capture.reload();render(null);}catch(Exception e){App.toast(this,e.getMessage());}}).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=IMPORT_PICTURE||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();importing=true;RuleStore targetStore=editingStore;String selectedThreshold=threshold.getText().toString();message.setText("Importing screenshot…");
        new Thread(()->{
            Bitmap imported=null;
            try{
                imported=ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{
                    int w=info.getSize().getWidth(),h=info.getSize().getHeight();
                    if(!PreviewGeometry.importSize(w,h))throw new IllegalArgumentException("Image is too large or has invalid dimensions");
                    int target=Math.min(960,w);decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetSize(target,Math.max(1,Math.round((float)h*target/w)));
                });
                if(imported.getWidth()<=imported.getHeight())throw new IllegalArgumentException("Use an uncropped landscape screenshot of the full Fortnite display");
                RuleStore store=targetStore;String name="source-"+UUID.randomUUID()+".png";App.png(imported,store.file(id,name));
                store.save(id,store.json(id).put("source",name).put("sourceRotation",App.rotation(this)).put("sourceReset",true));
                App.MAIN.post(()->{importing=false;if(isFinishing()||isDestroyed())return;Bundle draft=new Bundle();draft.putString("threshold",selectedThreshold);render(draft);message.setText("Screenshot imported. Drag the green zone. Tap for the magenta crosshair; recorded taps stay separate.");});
            }catch(Exception e){App.MAIN.post(()->{importing=false;if(!isFinishing()&&!isDestroyed())message.setText("Import failed: "+e.getMessage());});}
            finally{if(imported!=null)imported.recycle();}
        },"AutoBot screenshot import").start();
    }
    private boolean save(){
        if(importing)return false;
        try{
            if(preview==null||picture==null||preview.rect.width()<0.008||preview.rect.height()<0.008)throw new IllegalStateException("Import/capture a picture and drag a watch rectangle first");
            RectF r=preview.rect;
            // Save from the clean bitmap, never the annotated preview canvas.
            editingStore.watch(id,picture,new double[]{r.left,r.top,r.width(),r.height()},rotation,Double.parseDouble(threshold.getText().toString())/100);
            if(App.capture!=null)App.capture.reload();message.setText("Watch area saved. Coloured guides are excluded from the saved picture. Record taps, then resume in Fortnite.");return true;
        }catch(Exception e){message.setText(e.getMessage());return false;}
    }
    private void review(){
        try{JSONArray steps=editingStore.json(id).optJSONArray("steps");if(steps==null||steps.length()==0){App.toast(this,"No taps recorded yet");return;}String[]items=new String[steps.length()];for(int i=0;i<steps.length();i++){JSONObject s=steps.getJSONObject(i);items[i]=(i+1)+". "+Math.round(s.getDouble("x")*1000)/10.0+"%, "+Math.round(s.getDouble("y")*1000)/10.0+"% • "+s.optLong("wait",600)+"ms";}
            new AlertDialog.Builder(this).setTitle("Choose a numbered tap to inspect").setItems(items,(d,n)->showTap(n)).setNegativeButton("Back",null).show();
        }catch(Exception e){message.setText(e.getMessage());}
    }
    private void showTap(int index){try{
        JSONObject profile=editingStore.json(id),step=profile.getJSONArray("steps").getJSONObject(index);boolean own=step.has("screen");String name=own?step.getString("screen"):profile.optString("source","");
        if(name.isEmpty()){App.toast(this,"No saved screenshot for this older recording; coordinates are shown in the tap list");return;}
        Bitmap shot=App.decode(editingStore.file(id,name));LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        App.text(this,content,own?"Number marks the saved tap on its own recorded screen.":"Older tap: marker is on the watch screenshot; later menu screens were not saved.",12);
        App.text(this,content,"Tap "+(index+1)+": "+Math.round(step.getDouble("x")*1000)/10.0+"%, "+Math.round(step.getDouble("y")*1000)/10.0+"%",13);
        View image=new View(this){final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas c){float[]f=PreviewGeometry.fit(shot.getWidth(),shot.getHeight(),getWidth(),getHeight());RectF display=new RectF(f[0],f[1],f[0]+f[2],f[1]+f[3]);p.setStyle(Paint.Style.FILL);c.drawBitmap(shot,null,display,p);TapMarkers.one(c,p,step,index+1,display,RuleActivity.this);}};
        content.addView(image,new LinearLayout.LayoutParams(-1,Math.min(App.dp(this,190),App.bounds(this).height()/2)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Recorded tap "+(index+1)).setView(content).setPositiveButton("Done",null).create();dialog.show();
    }catch(Exception e){message.setText("Could not show tap: "+e.getMessage());}}
    @Override protected void onSaveInstanceState(Bundle s){
        super.onSaveInstanceState(s);s.putString("source",sourceName);s.putString("threshold",threshold.getText().toString());
        if(preview!=null){s.putFloat("l",preview.rect.left);s.putFloat("t",preview.rect.top);s.putFloat("r",preview.rect.right);s.putFloat("b",preview.rect.bottom);s.putFloat("markerX",preview.markerX);s.putFloat("markerY",preview.markerY);}
    }
    private final class Selection extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF display=new RectF(),rect=new RectF();
        float startX,startY,downX,downY,markerX=0.5f,markerY=0.5f;boolean dragging;
        Selection(){super(RuleActivity.this);setContentDescription("Drag a green watch zone; tap to place the magenta crosshair");}
        @Override protected void onDraw(Canvas c){
            float[]fit=PreviewGeometry.fit(picture.getWidth(),picture.getHeight(),getWidth(),getHeight());display.set(fit[0],fit[1],fit[0]+fit[2],fit[1]+fit[3]);
            p.setStyle(Paint.Style.FILL);c.drawBitmap(picture,null,display,p);float w=display.width(),h=display.height();c.save();c.clipRect(display);
            if(rect.width()>0&&rect.height()>0){
                RectF box=new RectF(display.left+rect.left*w,display.top+rect.top*h,display.left+rect.right*w,display.top+rect.bottom*h);
                p.setColor(0x332cff7f);p.setStyle(Paint.Style.FILL);c.drawRect(box,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xdd001c0c);p.setStrokeWidth(App.dp(RuleActivity.this,4));c.drawRect(box,p);
                p.setColor(0xff2cff7f);p.setStrokeWidth(App.dp(RuleActivity.this,2));c.drawRect(box,p);p.setStyle(Paint.Style.FILL);
                for(float x:new float[]{box.left,box.right})for(float y:new float[]{box.top,box.bottom}){p.setColor(0xff001c0c);c.drawCircle(x,y,App.dp(RuleActivity.this,4),p);p.setColor(0xff2cff7f);c.drawCircle(x,y,App.dp(RuleActivity.this,2.5f),p);}
            }
            float x=display.left+markerX*w,y=display.top+markerY*h;p.setStyle(Paint.Style.STROKE);p.setColor(0xdd210018);p.setStrokeWidth(App.dp(RuleActivity.this,3));
            c.drawLine(display.left,y,display.right,y,p);c.drawLine(x,display.top,x,display.bottom,p);p.setColor(0xffff40d5);p.setStrokeWidth(App.dp(RuleActivity.this,1.2f));
            c.drawLine(display.left,y,display.right,y,p);c.drawLine(x,display.top,x,display.bottom,p);c.drawCircle(x,y,App.dp(RuleActivity.this,6),p);TapMarkers.all(c,p,numberedTaps,display,RuleActivity.this);c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(display.width()==0)return false;float x=PreviewGeometry.normal(e.getX(),display.left,display.width()),y=PreviewGeometry.normal(e.getY(),display.top,display.height());
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(!display.contains(e.getX(),e.getY()))return false;startX=x;startY=y;downX=e.getX();downY=e.getY();dragging=false;return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP){
                if(Math.hypot(e.getX()-downX,e.getY()-downY)>App.dp(RuleActivity.this,6))dragging=true;markerX=x;markerY=y;
                if(dragging)rect.set(Math.min(startX,x),Math.min(startY,y),Math.max(startX,x),Math.max(startY,y));
                if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();message.setText("Green zone: "+Math.round(rect.width()*100)+"% × "+Math.round(rect.height()*100)+"% • Magenta guide: "+Math.round(markerX*100)+"%, "+Math.round(markerY*100)+"%");invalidate();return true;
            }return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
