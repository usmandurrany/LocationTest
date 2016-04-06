package com.fournodes.ud.locationtest;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by Usman on 4/4/2016.
 */
public class LocationUpdateThread extends HandlerThread {

    private Handler handler;

    public LocationUpdateThread(String name) {
        super(name);
    }
    public void post(Runnable task){
        if (handler == null)
            handler = new Handler(getLooper());
        handler.post(task);
    }
    public void postDelayed(Runnable task, long timeMillis){
        if (handler == null)
            handler = new Handler(getLooper());
        handler.postDelayed(task,timeMillis);
    }
}
