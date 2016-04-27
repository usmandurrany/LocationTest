package com.fournodes.ud.locationtest.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.support.v7.app.NotificationCompat;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.activities.MainActivity;

public class BackpackTrackReceiver extends BroadcastReceiver {
    public BackpackTrackReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("eu.faircode.backpacktrack2.PROXIMITY_ENTER")) {
            sendNotification(context, "Entered", "Waypoint " + String.valueOf(intent.getLongExtra("Waypoint", -1)));
        }
        else if (intent.getAction().equals("eu.faircode.backpacktrack2.PROXIMITY_EXIT")) {
            sendNotification(context, "Exited", "Waypoint " + String.valueOf(intent.getLongExtra("Waypoint", -1)));
        }

    }

    private void sendNotification(Context context, String title, String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(title)
                .setContentText(notificationDetails)
                .setContentIntent(piActivityIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

}
