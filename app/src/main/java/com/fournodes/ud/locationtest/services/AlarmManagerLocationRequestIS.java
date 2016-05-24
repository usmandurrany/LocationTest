package com.fournodes.ud.locationtest.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class AlarmManagerLocationRequestIS extends IntentService {
private static final String TAG = "AMLocationRequestIS";
    public AlarmManagerLocationRequestIS() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
       serviceMessage("startLocationRequestRunnable");
    }
    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
