package com.fournodes.ud.locationtest.utils;

import android.os.Handler;

import com.fournodes.ud.locationtest.SharedPrefs;

/**
 * Created by Usman on 13/5/2016.
 */
public class RequestLocationHandler extends Handler {
    private static final String TAG = "RequestLocationHandler";

    public void clearQueue(Runnable runnable){
        FileLogger.e(TAG, "Clearing queued requests");
        this.removeCallbacks(runnable);
    }

    public void run(Runnable runnable){
        this.post(runnable);
    }
    public void runAfterSeconds(Runnable runnable, int seconds){

        this.postDelayed(runnable,(seconds*1000));
    }
}
