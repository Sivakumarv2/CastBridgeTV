package com.castbridge.tv;

import android.app.*;import android.content.*;import android.graphics.Color;import android.media.projection.MediaProjectionManager;import android.net.wifi.WifiManager;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.drawable.GradientDrawable;
import java.net.*;import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ=44; private TextView status,url; private Button mirror,stop;
    int blue=Color.rgb(23,105,255), ink=Color.rgb(16,24,40), muted=Color.rgb(102,112,133);
    @Override public void onCreate(Bundle b){super.onCreate(b); build();}
    TextView tv(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    GradientDrawable bg(int color,float r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(r);return g;}
    Button action(String text){Button b=new Button(this);b.setText(text);b.setTextSize(15);b.setTextColor(ink);b.setAllCaps(false);b.setBackground(bg(Color.WHITE,28));return b;}
    void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,18,22,18);root.setBackgroundColor(Color.rgb(247,249,252));
        TextView brand=tv("CastBridge",28,ink);brand.setTypeface(null,1);root.addView(brand,new LinearLayout.LayoutParams(-1,52));
        TextView sub=tv("Screen mirroring & Cast to TV",15,muted);root.addView(sub,new LinearLayout.LayoutParams(-1,32));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(22,20,22,20);card.setBackground(bg(Color.WHITE,34));
        TextView ct=tv("Connect your TV",20,ink);ct.setTypeface(null,1);card.addView(ct);
        status=tv("Ready to connect",14,muted);status.setPadding(0,8,0,2);card.addView(status);
        url=tv("TV receiver: http://"+ip()+":8765",13,blue);url.setPadding(0,10,0,8);card.addView(url);
        Button share=action("Share TV receiver link");share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,url.getText().toString());startActivity(Intent.createChooser(i,"Share receiver link"));});card.addView(share,new LinearLayout.LayoutParams(-1,54));
        root.addView(card,new LinearLayout.LayoutParams(-1,220));
        Space sp=new Space(this);root.addView(sp,new LinearLayout.LayoutParams(1,18));
        TextView h=tv("Cast tools",20,ink);h.setTypeface(null,1);root.addView(h,new LinearLayout.LayoutParams(-1,40));
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1=new LinearLayout(this);row1.setWeightSum(2);mirror=action("▣  Mirror Screen");Button media=action("▣  Cast Media");row1.addView(mirror,new LinearLayout.LayoutParams(0,68,1));row1.addView(media,new LinearLayout.LayoutParams(0,68,1));grid.addView(row1);
        LinearLayout row2=new LinearLayout(this);row2.setWeightSum(2);Button web=action("◉  Cast Web");Button remote=action("⌁  TV Remote");row2.addView(web,new LinearLayout.LayoutParams(0,68,1));row2.addView(remote,new LinearLayout.LayoutParams(0,68,1));grid.addView(row2);
        root.addView(grid,new LinearLayout.LayoutParams(-1,144));
        TextView note=tv("Tip: Open the receiver address above in your Smart TV browser. Keep the phone and TV on the same Wi‑Fi network.",13,muted);note.setPadding(6,18,6,0);root.addView(note,new LinearLayout.LayoutParams(-1,70));
        stop=action("Stop mirroring");stop.setTextColor(Color.WHITE);stop.setBackground(bg(blue,28));stop.setEnabled(false);root.addView(stop,new LinearLayout.LayoutParams(-1,56));
        setContentView(root);
        mirror.setOnClickListener(v->requestProjection());stop.setOnClickListener(v->{stopService(new Intent(this,MirrorService.class));status.setText("Mirroring stopped");stop.setEnabled(false);mirror.setEnabled(true);});
        media.setOnClickListener(v->Toast.makeText(this,"Media casting receiver is included in the TV web receiver. Full DLNA/Chromecast media support can be added next.",Toast.LENGTH_LONG).show());
        web.setOnClickListener(v->Toast.makeText(this,"Open any URL on the TV using its browser, then use the same Wi‑Fi network.",Toast.LENGTH_LONG).show());
        remote.setOnClickListener(v->Toast.makeText(this,"TV remote control depends on the TV brand/protocol and is not enabled in this base build.",Toast.LENGTH_LONG).show());
    }
    void requestProjection(){MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),REQ);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==REQ&&c==RESULT_OK&&d!=null){Intent i=new Intent(this,MirrorService.class);i.putExtra("resultCode",c);i.putExtra("data",d);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("Mirroring active • TV connected when receiver is open");stop.setEnabled(true);mirror.setEnabled(false);}}
    String ip(){try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);int a=wm.getConnectionInfo().getIpAddress();return (a&255)+"."+((a>>8)&255)+"."+((a>>16)&255)+"."+((a>>24)&255);}catch(Exception e){return "PHONE-IP";}}
}
