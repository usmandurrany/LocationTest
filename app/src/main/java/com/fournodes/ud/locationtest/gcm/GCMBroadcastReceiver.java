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

import com.fournodes.ud.locationtest.GeofenceWrapper;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.NotificationApi;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.gcm.GcmListenerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usman on 11/23/2015.
 */
public class GCMBroadcastReceiver extends GcmListenerService {
    private static final String TAG = "GCM Receiver";
    private Fence fence;
    private GeofenceWrapper geofenceWrapper;
    private Database db;
    private List<String> events;


    @Override
    public void onMessageReceived(String from, Bundle data) { // Prank received will trigger this
        if (SharedPrefs.pref != null)
            new SharedPrefs(getApplicationContext()).initialize();
        if (events == null)
            events = new ArrayList<>();

        db = new Database(getApplicationContext());
        geofenceWrapper = new GeofenceWrapper(getApplicationContext());

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
                createNotification(data.getString("sender"), data.getString("message"), data.getString("latitude"),data.getString("longitude"),data.getString("trigger_time"));
                break;
            case "enable_track":
                SharedPrefs.setUpdateServerRowThreshold(1);
                SharedPrefs.setTrackingEnabled(true);
                break;
            case "disable_track":
                SharedPrefs.setUpdateServerRowThreshold(5);
                SharedPrefs.setTrackingEnabled(false);
                break;

        }


    }


    private void createFenceOnDevice(final Bundle data) {

        fence = new Fence();
        fence.setOnDevice(1);
        fence.setTransitionType(Integer.parseInt(data.getString("transition_type")));
        fence.setCenter_lat(Double.parseDouble(data.getString("center_latitude")));
        fence.setCenter_lng(Double.parseDouble(data.getString("center_longitude")));
        fence.setEdge_lat(Double.parseDouble(data.getString("edge_latitude")));
        fence.setEdge_lng(Double.parseDouble(data.getString("edge_longitude")));
        fence.setRadius(Float.parseFloat(data.getString("radius")));
        fence.setDescription(data.getString("description"));
        fence.setTitle(data.getString("title"));
        fence.setUserId(data.getString("user_id"));
        fence.setCreate_on(data.getString("create_on"));
        fence.setId(Integer.parseInt(data.getString("fence_id")));

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Create");
        FileLogger.e(TAG, "Title: " + data.getString("title"));
        FileLogger.e(TAG, "Description: " + data.getString("description"));
        FileLogger.e(TAG, "Center: Lat: " + data.getString("center_latitude") + " Long: " + data.getString("center_longitude"));
        FileLogger.e(TAG, "Radius: " + data.getString("radius"));
        //geofenceWrapper.create(fence);
        FileLogger.e(TAG, "Result: Success");
        db.saveFence(fence);


        NotificationApi notificationApi = new NotificationApi();
        notificationApi.execute("response",
                "response=success&user_id=" + SharedPrefs.getUserId() + "&action=create_fence&fence_id=" + data.getString("fence_id"));
        serviceMessage("calcDistance");
    }

    public void editFence(final Bundle data) {


        fence = db.getFence(data.getString("fence_id"));
        fence.setEdge_lat(Double.parseDouble(data.getString("edge_latitude")));
        fence.setEdge_lng(Double.parseDouble(data.getString("edge_longitude")));
        fence.setRadius(Float.parseFloat(data.getString("radius")));
        fence.setLastEvent(2);

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Edit");
        FileLogger.e(TAG, "Radius: " + data.getString("radius"));

        geofenceWrapper.create(fence);
        FileLogger.e(TAG, "Result: Success");
        db.updateFence(fence);


        NotificationApi notificationApi = new NotificationApi();
        notificationApi.execute("response",
                "response=success&user_id=" + SharedPrefs.getUserId() + "&action=edit_fence&fence_id=" + data.getString("fence_id"));
        serviceMessage("calcDistance");

    }

    public void removeFence(final Bundle data) {
        fence = db.getFence(data.getString("fence_id"));

        FileLogger.e(TAG, "Fence: " + data.getString("fence_id"));
        FileLogger.e(TAG, "Action: Remove");
        // geofenceWrapper.remove(fence);
        FileLogger.e(TAG, "Result: Success");
        db.removeFenceFromDatabase(fence.getId());


        NotificationApi notificationApi = new NotificationApi();
        notificationApi.execute("response",
                "response=success&user_id=" + SharedPrefs.getUserId() + "&action=remove_fence&fence_id=" + data.getString("fence_id"));

        serviceMessage("calcDistance");

    }

    private void createNotification(String from, String message, String latitude,String longitude,String time) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int id = (int) System.currentTimeMillis();

        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(),id,
                new Intent(getApplicationContext(), MainActivity.class)
                .setAction("showNotificationOnMap")
                .putExtra("latitude",latitude)
                .putExtra("longitude",longitude)
                .putExtra("time",time)
                .putExtra("user",from)
                .putExtra("message",message),0);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(Color.BLUE)
                .setContentTitle(from)
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
