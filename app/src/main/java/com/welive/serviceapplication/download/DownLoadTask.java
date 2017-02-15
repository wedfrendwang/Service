package com.welive.serviceapplication.download;


import android.os.AsyncTask;
import android.os.Environment;

import com.welive.serviceapplication.listener.DownloadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by welive on 2017/2/13.
 * 下载代码
 */

public class DownLoadTask extends AsyncTask<String ,Integer,Integer> {

    public static final int TYPE_SUCCESS = 0;

    public static final int TYPE_FAILED = 1;

    public static final int TYPE_PAUSED = 2;

    public static final int TYPE_CANCELED = 3;

    private DownloadListener downloadListener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    //记录上次下载进度
    private int lastProgress;

    public DownLoadTask(DownloadListener listener) {
        this.downloadListener = listener;
    }

    /**
     * 在进行doInBackground方法之前进行调用
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * 在doInBackground完成之后进行调用
     * @param integer
     */
    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        switch (integer){
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            case TYPE_PAUSED:
                downloadListener.onPaused();
                break;
            case TYPE_CANCELED:
                downloadListener.onCanceled();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
        }
    }

    /**
     * 更新方法进行进度的提示
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress = values[0];
        if(progress > lastProgress){
            downloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    /**
     * 子线程进行耗时操作
     * @param params
     * @return
     */
    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream = null;
        //
        RandomAccessFile savedFile = null;
        File file = null;

        try {
            long downloadedLength = 0; //记录已下载文件长度
            String downloadUrl = params[0];
            //下载地址
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //存储地址设为手机默认的download文件夹
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            file = new File(directory+fileName);

            if(file.exists()){//该文件已经存在
                //取出已经下载的大小
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) return TYPE_FAILED;
            else if (contentLength == downloadedLength) return TYPE_SUCCESS;
            //如果两个判断均不满足，则表示还没有下载完成
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().addHeader("RANGE","bytes="+downloadedLength+"-").url(downloadUrl).build();
            Response response = client.newCall(request).execute();
            if(response != null){
                inputStream = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);//跳过已经下载过的章节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = inputStream.read(b))!=-1){
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else{
                        total += len;
                        savedFile.write(b,0,len);
                        int progress = (int)(((total+downloadedLength)*100)/contentLength);
                        //调用onProgressUpdate
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }
                if (savedFile != null){
                    savedFile.close();
                }
                if(isCanceled && file != null){
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }


    /**
     * 查询需要下载数据的总量
     * @param downloadUrl
     * @return
     * @throws IOException
     */
    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }


    /**
     * 主动点击暂停事件
     */
    public void pauseDownload(){
        if(isPaused){isPaused = false;}else if(!isPaused){isPaused = true;}
    }

    public void cancelDownLoad(){
        isCanceled = true;
    }

}
