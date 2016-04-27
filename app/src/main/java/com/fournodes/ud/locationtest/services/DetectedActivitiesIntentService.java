package com.fournodes.ud.locationtest.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by Usman on 1/4/2016.
 */
public class DetectedActivitiesIntentService extends IntentService {
    protected static final String TAG = "DetectedActivitiesIS";
    public int fastMovement;
    public int slowMovement;
    public int noMovement;

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

/*        // Restart the service incase the system stops it and it doesnt restart on its own
        if (System.currentTimeMillis() - SharedPrefs.getLocLastUpdateMillis() > (SharedPrefs.getLocationRequestInterval()*1000)*2)
            startService(new Intent(this,LocationService.class));*/

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
        if (fastMovement >= 50) {
            FileLogger.e(TAG, "Fast Movement Detected.");
            serviceMessage("fastMovement");
        }
        else if (slowMovement >= 50) {
            FileLogger.e(TAG, "Slow Movement Detected.");
            serviceMessage("slowMovement");
        }
        else if (noMovement >= 50) {
            FileLogger.e(TAG, "No Movement Detected.");
            serviceMessage("noMovement");
        }
        else
            FileLogger.e(TAG, "No enough data to change interval. Using last value of " + SharedPrefs.getLocationRequestInterval() + " seconds");
    }

    public String getActivityString(DetectedActivity detectedActivity) {
        switch (detectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
                fastMovement += detectedActivity.getConfidence();
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                fastMovement += detectedActivity.getConfidence();
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                slowMovement += detectedActivity.getConfidence();
                return "On Foot";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.STILL:
                noMovement += detectedActivity.getConfidence();
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
            case DetectedActivity.WALKING:
                return "Walking";
            default:
                return "Undefined";
        }
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}