/*
package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.utils.FileLogger;
import com.pathsense.android.sdk.location.PathsenseActivityRecognitionReceiver;
import com.pathsense.android.sdk.location.PathsenseDetectedActivities;
import com.pathsense.android.sdk.location.PathsenseDetectedActivity;
import com.pathsense.android.sdk.location.PathsenseDetectedActivityEnum;

public class PathsenseActivityDetection extends PathsenseActivityRecognitionReceiver {
    private static final String TAG = "PathsenseActivityDetection";
    private Context context;

    public PathsenseActivityDetection() {}

    @Override
    protected void onDetectedActivities(Context context, PathsenseDetectedActivities pathsenseDetectedActivities) {
        this.context = context;
        PathsenseDetectedActivity detectedActivity = pathsenseDetectedActivities.getMostProbableActivity();
        FileLogger.e(TAG,detectedActivity.getDetectedActivity().toString() + " Confidence: " + String.valueOf(detectedActivity.getConfidence()));

        if (detectedActivity.getDetectedActivity() == PathsenseDetectedActivityEnum.DRIVING)
            serviceMessage("fastMovement");
        else if (detectedActivity.getDetectedActivity() == PathsenseDetectedActivityEnum.WALKING)
            serviceMessage("slowMovement");
        else if (detectedActivity.getDetectedActivity() == PathsenseDetectedActivityEnum.STILL)
            serviceMessage("noMovement");



    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
*/
