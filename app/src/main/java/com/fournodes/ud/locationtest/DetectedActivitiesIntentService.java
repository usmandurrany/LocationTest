package com.fournodes.ud.locationtest;

import android.app.IntentService;
import android.content.Intent;

import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Usman on 1/4/2016.
 */
public class DetectedActivitiesIntentService extends IntentService {
    protected static final String TAG = "DetectedActivitiesIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (SharedPrefs.pref == null)
            new SharedPrefs(this).initialize();
    }

    /**
     * Handles incoming intents.
     *
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (SharedPrefs.pref == null)
            new SharedPrefs(getApplicationContext()).initialize();

        if (System.currentTimeMillis() - SharedPrefs.getLocLastUpdateMillis() > (SharedPrefs.getForceRequestTimer()*1000)*2)
            startService(new Intent(this,LocationService.class));

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        FileLogger.e(TAG, " -- Activities detected --");
        for (DetectedActivity da : detectedActivities) {
            FileLogger.e(TAG, getActivityString(da) + " Confidence: " + da.getConfidence() + "%");
        }

    }

    public static String getActivityString(DetectedActivity detectedActivity) {
        switch (detectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
                if (SharedPrefs.getForceRequestTimer()>15) {
                    FileLogger.e(TAG,"Changing force check interval to 15 seconds");
                    SharedPrefs.setForceRequestTimer(15);
                }
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
               // SharedPrefs.setForceRequestTimer(30000);
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
/*                if (detectedActivity.getConfidence() >= 80 && SharedPrefs.getForceRequestTimer()>60){
                    SharedPrefs.setForceRequestTimer(60);
                    FileLogger.e(TAG,"Changing force check interval to 1 minutes");
                }*/
                return "On Foot";
            case DetectedActivity.RUNNING:
               // SharedPrefs.setForceRequestTimer(60000);
                return "Running";
            case DetectedActivity.STILL:
                if (detectedActivity.getConfidence() >= 95 && SharedPrefs.getForceRequestTimer()<120){
                    SharedPrefs.setForceRequestTimer(120);
                    FileLogger.e(TAG,"Changing force check interval to 2 minutes");
                }
                return "Still";
            case DetectedActivity.TILTING:
                //SharedPrefs.setForceRequestTimer(120000);
                return "Tilting";
            case DetectedActivity.UNKNOWN:
               // SharedPrefs.setForceRequestTimer(10000);
                return "Unknown";
            case DetectedActivity.WALKING:
               // SharedPrefs.setForceRequestTimer(60000);
                return "Walking";
            default:
                return "Undefined";
        }
    }
}