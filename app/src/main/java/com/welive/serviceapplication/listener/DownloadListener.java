package com.welive.serviceapplication.listener;

/**
 * Created by welive on 2017/2/13.
 */

public interface DownloadListener {
    /*
    进度
     */
    void onProgress(int progress);
    /*
    成功
     */
    void onSuccess();
    /*
    失败
     */
    void onFailed();
    /*
    暂停
     */
    void onPaused();
    /*
    取消
     */
    void onCanceled();
}
