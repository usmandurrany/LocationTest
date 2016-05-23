package com.fournodes.ud.locationtest.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Usman on 11/23/2015.
 */
public class GCMBroadcastReceiver extends GcmListenerService {
    private static final String TAG = "GCM Receiver";
    private Fence fence;
    private Database db;


    @Override
    public void onMessageReceived(String from, Bundle data) { // Prank received will trigger this

        db = new Database(getApplicationContext());

        Log.e("GCM Message", data.getString("type"));
        switch (data.getString("type")) {
            case "create_fence":
                createFenceOnDevice(data);
                break;

            case "edit_fence":
                editFence(data);
                break;

            case "remove_fence":
                removeFence(data);
                break;
            case "notification":
                createNotification(data.getString("user_id"), data.getString("sender"), data.getString("message"), data.getString("latitude"), data.getString("longitude"), data.getString("trigger_time"));
                break;
            case "enable_track":
                if (SharedPrefs.pref != null)
                    new SharedPrefs(getApplicationContext()).initialize();
                SharedPrefs.setUpdateServerRowThreshold(1);
                SharedPrefs.setIsLive(true);
                serviceMessage("isLive");
                break;
            case "disable_track":
                if (SharedPrefs.pref != null)
                    new SharedPrefs(getApplicationContext()).initialize();
                SharedPrefs.setUpdateServerRowThreshold(5);
                SharedPrefs.setIsLive(false);
                break;
            case "edit_assignment":
                if (db.removeFenceFromDatabase(Integer.parseInt(data.getString("fence_id")), 1))
                    createFenceOnDevice(data);
                else
                    FileLogger.e(TAG, "Result: Failed");
                break;

        }


    }


    private void createFenceOnDevice(final Bundle data) {

        fence = new Fence();
        fence.setFenceId(Integer.parseInt(data.getString("fence_id")));
        fence.setTitle(data.getString("title"));
        fence.setDescription(data.getString("description"));
        fence.setCenterLat(Double.parseDouble(data.getString("center_latitude")));
        fence.setCenterLng(Double.parseDouble(data.getString("center_longitude")));
        fence.setEdgeLat(Double.parseDouble(data.getString("edge_latitude")));
        fence.setEdgeLng(Double.parseDouble(data.getString("edge_longitude")));
        fence.setRadius(Float.parseFloat(data.getString("radius")));
        fence.setNotifyId(Integer.parseInt(data.getString("user_id")));
        fence.setAssignment(data.getString("assignment_data"));
        fence.setOnDevice(1);

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Create");
        FileLogger.e(TAG, "Title: " + data.getString("title"));
        FileLogger.e(TAG, "Description: " + data.getString("description"));
        FileLogger.e(TAG, "Center: Lat: " + data.getString("center_latitude") + " Long: " + data.getString("center_longitude"));
        FileLogger.e(TAG, "Radius: " + data.getString("radius"));

/*        PathsenseWrapper pathsenseWrapper = new PathsenseWrapper(this);
        pathsenseWrapper.addGeofence(fence);*/

        if (db.saveFenceInformation(fence) > 0)
            FileLogger.e(TAG, "Result: Success");
        else
            FileLogger.e(TAG, "Result: Failed");


        String payload = "response=success&user_id=" + SharedPrefs.getUserId()
                + "&action=create_fence&fence_id=" + data.getString("fence_id");
        IncomingApi incomingApi = new IncomingApi(null, "acknowledge", payload, 0);
        incomingApi.execute();
        serviceMessage("calcDistance");
    }

    public void editFence(final Bundle data) {


        fence = db.getFence(data.getString("fence_id"));
        fence.setFenceId(Integer.parseInt(data.getString("fence_id")));
        fence.setTitle(data.getString("title"));
        fence.setDescription(data.getString("description"));
        fence.setCenterLat(Double.parseDouble(data.getString("center_latitude")));
        fence.setCenterLng(Double.parseDouble(data.getString("center_longitude")));
        fence.setEdgeLat(Double.parseDouble(data.getString("edge_latitude")));
        fence.setEdgeLng(Double.parseDouble(data.getString("edge_longitude")));
        fence.setRadius(Float.parseFloat(data.getString("radius")));
        fence.setNotifyId(Integer.parseInt(data.getString("user_id")));
        fence.setAssignment(data.getString("assignment_data"));
        fence.setOnDevice(1);

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Edit");
        FileLogger.e(TAG, "Radius: " + data.getString("radius"));


        if (db.updateFenceInformation(fence))
            FileLogger.e(TAG, "Result: Success");
        else
            FileLogger.e(TAG, "Result: Failed");


        String payload = "response=success&user_id=" + SharedPrefs.getUserId()
                + "&action=edit_fence&fence_id=" + data.getString("fence_id");
        IncomingApi incomingApi = new IncomingApi(null, "acknowledge", payload, 0);
        incomingApi.execute();
        serviceMessage("calcDistance");

    }

    public void removeFence(final Bundle data) {

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Remove");

        if (db.removeFenceFromDatabase(Integer.parseInt(data.getString("fence_id")), 1))
            FileLogger.e(TAG, "Result: Success");
        else
            FileLogger.e(TAG, "Result: Failed");


        String payload = "response=success&user_id=" + SharedPrefs.getUserId()
                + "&action=remove_fence&fence_id=" + data.getString("fence_id");
        IncomingApi incomingApi = new IncomingApi(null, "acknowledge", payload, 0);
        incomingApi.execute();
        serviceMessage("calcDistance");

    }

    private void createNotification(String fromId, String fromName, String message, String latitude, String longitude, String time) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int id = (int) System.currentTimeMillis();

        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(), id,
                new Intent(getApplicationContext(), MainActivity.class)
                        .setAction("showNotificationOnMap")
                        .putExtra("latitude", latitude)
                        .putExtra("longitude", longitude)
                        .putExtra("time", time)
                        .putExtra("user", fromName)
                        .putExtra("userId", fromId)
                        .putExtra("message", message), 0);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(Color.BLUE)
                .setContentTitle(fromName)
                .setContentText(message)
                .setContentIntent(piActivityIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(false);

        mNotificationManager.notify(id, builder.build());


    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}
