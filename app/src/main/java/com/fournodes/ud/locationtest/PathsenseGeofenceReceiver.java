/*
package com.fournodes.ud.locationtest;

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
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.pathsense.android.sdk.location.PathsenseGeofenceEvent;
import com.pathsense.android.sdk.location.PathsenseGeofenceEventReceiver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PathsenseGeofenceReceiver extends PathsenseGeofenceEventReceiver {
    private Context context;
    public PathsenseGeofenceReceiver() {}

    @Override
    protected void onGeofenceEvent(Context context, PathsenseGeofenceEvent pathsenseGeofenceEvent) {
        this.context=context;
        Database db = new Database(context);
        Fence fence = db.getFence(pathsenseGeofenceEvent.getGeofenceId());
        if (pathsenseGeofenceEvent.isIngress()){
            sendNotification("Event verified. Entered fence: " + fence.getTitle(), fence.getNotifyId(), String.valueOf(fence.getCenterLat()), String.valueOf(fence.getCenterLng()));

        }else
        {
            sendNotification("Event verified. Exited fence: " + fence.getTitle(), fence.getNotifyId(), String.valueOf(fence.getCenterLat()), String.valueOf(fence.getCenterLng()));

        }
    }
    private void sendNotification(String notificationDetails, int notify_id, String latitude, String longitude) {
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
                .setContentTitle("Alert")
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
        try {
            if (SharedPrefs.pref == null)
                new SharedPrefs(context).initialize();
            String payload = "user_id=" + SharedPrefs.getUserId()
                    + "&notify_id=" + notify_id
                    + "&sender=" + URLEncoder.encode(SharedPrefs.getUserName(), "UTF-8")
                    + "&trigger_time=" + System.currentTimeMillis()
                    + "&latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&message=" + URLEncoder.encode(notificationDetails, "UTF-8");
            new IncomingApi(null, "notify", payload, 6).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


}
*/
