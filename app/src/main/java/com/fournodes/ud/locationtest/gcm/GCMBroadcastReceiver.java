package com.fournodes.ud.locationtest.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fournodes.ud.locationtest.MainActivity;
import com.fournodes.ud.locationtest.NotifyExternalDevice;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Usman on 11/23/2015.
 */
public class GCMBroadcastReceiver extends GcmListenerService {
    private static final String TAG = "GCMBroadcastReceiver";


    @Override
    public void onMessageReceived(String from, Bundle data) { // Prank received will trigger this
        if (SharedPrefs.pref == null) {
            new SharedPrefs(getApplicationContext());
        }

        Log.e("GCM Message", data.getString("message"));
        Log.e("sender", data.getString("sender"));
        sendNotification(data.getString("sender"),data.getString("message"));





    }
    private void sendNotification(String from,String message) {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(),0,new Intent(getApplicationContext(), MainActivity.class),0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(from)
                .setContentText(message)
                .setContentIntent(piActivityIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

}
