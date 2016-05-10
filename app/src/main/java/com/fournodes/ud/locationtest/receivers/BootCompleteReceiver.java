package com.fournodes.ud.locationtest.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.fournodes.ud.locationtest.services.LocationService;
import com.fournodes.ud.locationtest.utils.Database;

public class BootCompleteReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";


    @Override
    public void onReceive(final Context context, Intent intent) {

        context.startService(new Intent(context, LocationService.class));
        final Handler checkService = new Handler();
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (LocationService.isServiceRunning && LocationService.mGoogleApiClient != null && LocationService.isGoogleApiConnected) {
                    Database db = new Database(context);
                    db.updateEventsAfterReboot();
                    checkService.removeCallbacks(this);
                }
                else
                    checkService.postDelayed(this, 5000);
            }
        };
        checkService.post(check);
    }

}