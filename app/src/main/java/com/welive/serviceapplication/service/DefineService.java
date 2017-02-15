package com.welive.serviceapplication.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.welive.serviceapplication.MainActivity;
import com.welive.serviceapplication.R;
import com.welive.serviceapplication.download.DownLoadTask;
import com.welive.serviceapplication.listener.DownloadListener;

import java.io.File;

/**
 * Service:进行通知以及启动线程，并进行判断
 */
public class DefineService extends Service {

    private DownLoadTask downLoadTask;

    private String downloadUrl;
    private DownloadBinder downloadBinder = new DownloadBinder();

    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            //每一次调用那么更新通知内容
            getNotificationManager().notify(1,getNotification("Downloading```",progress));
        }

        @Override
        public void onSuccess() {

            downLoadTask = null;
            //成功之后停止前台的服务通知
            stopForeground(true);
            //成功之后，将值改为小于0的值就好
            getNotificationManager().notify(1,getNotification("Download Success",-1));
            Toast.makeText(DefineService.this,"Download Success",Toast.LENGTH_SHORT).show();
            stopSelf();
        }

        @Override
        public void onFailed() {

            downLoadTask = null;
            //下载失败，关闭前台服务通知，并且开始创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            Toast.makeText(DefineService.this,"Download Failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downLoadTask = null;
            Toast.makeText(DefineService.this,"Download pause",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCanceled() {
            downLoadTask = null;
            downloadBinder.cancelDownload();
        }
    };

    public DefineService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return downloadBinder;
    }


    public class DownloadBinder extends Binder{
        /**
         * 开始启动下载任务，并调用异步线程
         * @param url
         */


        public void startDownload(String url){
            if(downLoadTask == null){
                downloadUrl = url;
                downLoadTask = new DownLoadTask(downloadListener);
                downLoadTask.execute(downloadUrl);
                startForeground(1,getNotification("Downloading```",0));
                Toast.makeText(DefineService.this,"Downloading```",Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 暂停下载
         */
        public void pauseDownload(){

            if(downLoadTask != null){
                downLoadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if(downLoadTask != null){
                downLoadTask.cancelDownLoad();
            }else{
                if(downloadUrl != null){

                    //删除已经下载的文件
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    //这个路径地址和你的存储路径地址一致
                    File file = new File(directory+fileName);
                    if(file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);//notification的取消通知，此时的通知很重要
                    stopForeground(true);
                    stopSelf();
                    Toast.makeText(DefineService.this,"cancel",Toast.LENGTH_SHORT).show();
                }
            }

        }

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }


    /**
     * 关于相应的Notification进行提示通知
     *
     * 1.使用通知的时候，创建NotificationManager  使用 getSystemService
     *
     * 2.设置Notification
     *
     * 3.最后使用NotificationManager进行Notification的调用
     */
    private NotificationManager getNotificationManager(){
        return ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
    }

    /**
     * 通知提示的基本设置
     * @param title
     * @param progress
     * @return
     */
    private Notification getNotification(String title,int progress){

        //点击通知的操作
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        //为了保证所有的通知一致，使用V7包的NotificationCompat.Builder()
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_input_add);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle(title);
        builder.setContentIntent(pendingIntent);
        if(progress>0){
            //当progress大于0
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }


}
