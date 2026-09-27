package com.castbridge.tv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.MediaProjection;
import android.os.*;
import android.util.DisplayMetrics;
import java.io.ByteArrayOutputStream;

public class MirrorService extends Service {
    public static final String ACTION_START="START";
    private MediaProjection projection; private VirtualDisplay display; private ImageReader reader; private CastServer server;
    private HandlerThread worker; private Handler handler; private int width,height,density;

    @Override public void onCreate(){ super.onCreate(); worker=new HandlerThread("mirror-worker"); worker.start(); handler=new Handler(worker.getLooper()); }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && intent.hasExtra("resultCode")){
            if(Build.VERSION.SDK_INT>=29) startForeground(7,notification(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            else startForeground(7,notification());
            startCapture(intent);
        }
        return START_NOT_STICKY;
    }
    private Notification notification(){
        String ch="castbridge"; NotificationManager nm=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(ch,"Screen mirroring",NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this,ch).setContentTitle("CastBridge TV").setContentText("Screen mirroring is active").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build();
    }
    private void startCapture(Intent intent){
        try {
            if(server==null){ server=new CastServer(8765); server.start(); }
            MediaProjectionManagerHolder holder=new MediaProjectionManagerHolder(this);
            Intent data;
            if(Build.VERSION.SDK_INT>=33) data=intent.getParcelableExtra("data", Intent.class);
            else data=(Intent)intent.getParcelableExtra("data");
            projection=holder.mpm.getMediaProjection(intent.getIntExtra("resultCode",Activity.RESULT_CANCELED),data);
            DisplayMetrics dm=getResources().getDisplayMetrics(); density=dm.densityDpi; width=Math.min(1280,dm.widthPixels); height=(int)((float)dm.heightPixels*width/dm.widthPixels);
            reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
            reader.setOnImageAvailableListener(r -> capture(r),handler);
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},handler);
            display=projection.createVirtualDisplay("CastBridge",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        }catch(Exception e){ stopSelf(); }
    }
    private void capture(ImageReader r){
        Image image=null; try{
            image=r.acquireLatestImage(); if(image==null)return;
            Image.Plane p=image.getPlanes()[0]; int ps=p.getPixelStride(), rs=p.getRowStride(), padding=rs-ps*width;
            Bitmap bmp=Bitmap.createBitmap(width+padding/ps,height,Bitmap.Config.ARGB_8888); bmp.copyPixelsFromBuffer(p.getBuffer());
            Bitmap scaled=bmp; if(bmp.getWidth()!=width){ scaled=Bitmap.createBitmap(bmp,0,0,width,height); bmp.recycle(); }
            ByteArrayOutputStream out=new ByteArrayOutputStream(250000); scaled.compress(Bitmap.CompressFormat.JPEG,55,out); if(scaled!=bmp)scaled.recycle();
            if(server!=null)server.broadcast(out.toByteArray());
        }catch(Exception ignored){} finally{if(image!=null)image.close();}
    }
    @Override public void onDestroy(){
        try{if(display!=null)display.release();}catch(Exception ignored){} try{if(projection!=null)projection.stop();}catch(Exception ignored){}
        try{if(reader!=null)reader.close();}catch(Exception ignored){} if(server!=null)server.stop();
        if(worker!=null)worker.quitSafely(); super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent){return null;}
    static class MediaProjectionManagerHolder{ final android.media.projection.MediaProjectionManager mpm; MediaProjectionManagerHolder(Context c){mpm=(android.media.projection.MediaProjectionManager)c.getSystemService(MEDIA_PROJECTION_SERVICE);} }
}
