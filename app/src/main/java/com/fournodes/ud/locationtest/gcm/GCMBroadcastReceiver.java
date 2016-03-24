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

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.Fence;
import com.fournodes.ud.locationtest.MainActivity;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.network.NotificationApi;
import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

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

        Log.e("GCM Message", data.getString("type"));
        // Log.e("sender", data.getString("sender"));
        switch (data.getString("type")) {
            case "create_fence":

                createFenceOnDevice(data);

                break;
            case "edit_fence":
               editFence(data);

                break;
            case "remove_fence":
                Database db = new Database(getApplicationContext());
                Fence fence = db.getFence(data.getString("fence_id"));
                Log.e("Fence Title", fence.getTitle());
                LocationServices.GeofencingApi.removeGeofences(LocationService.mGoogleApiClient,fence.getPendingIntent()); //Remove from GoogleApiGeofence
                db.removeFence(fence.getId());
                NotificationApi notificationApi = new NotificationApi();
                notificationApi.execute("response",
                        "response=success&user_id="+SharedPrefs.getUserId()+"&action=remove_fence&fence_id="+data.getString("fence_id"));


                break;
            case "notification":
                sendNotification(data.getString("sender"), data.getString("message"));
                break;

        }


    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private void createFenceOnDevice(Bundle data) {
        Database db = new Database(getApplicationContext());
        Geofence geofence = new Geofence.Builder()
                .setRequestId(data.getString("title"))
                .setCircularRegion(
                        Double.parseDouble(data.getString("center_latitude")),
                        Double.parseDouble(data.getString("center_longitude")),
                        Float.parseFloat(data.getString("radius")))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Integer.parseInt(data.getString("transition_type")))
                .setLoiteringDelay(500)
                .build();


        Fence fence = new Fence();
        fence.setArea(geofence);
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
        db.saveFence(fence);
        fence.setPendingIntent(getGeofencePendingIntent(fence.getId(), data.getString("user_id")));


        LocationServices.GeofencingApi.addGeofences(
                LocationService.mGoogleApiClient,
                getGeofencingRequest(fence.getArea()),
                fence.getPendingIntent());

        NotificationApi notificationApi = new NotificationApi();
        notificationApi.execute("response",
                "response=success&user_id="+SharedPrefs.getUserId()+"&action=create_fence&fence_id="+data.getString("fence_id"));

    }

    private void sendNotification(String from, String message) {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
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
        mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private PendingIntent getGeofencePendingIntent(int id, String notify_id) {
        // Reuse the PendingIntent if we already have it.

        Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
        intent.putExtra("notify_id", notify_id);
        intent.putExtra("remote", true);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(getApplicationContext(), id, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void editFence(Bundle data){
        Database db = new Database(getApplicationContext());
        Fence fence = db.getFence(data.getString("fence_id"));


        Geofence geofence = new Geofence.Builder()
                .setRequestId(data.getString("title"))
                .setCircularRegion(
                        Double.parseDouble(data.getString("center_latitude")),
                        Double.parseDouble(data.getString("center_longitude")),
                        Float.parseFloat(data.getString("radius")))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Integer.parseInt(data.getString("transition_type")))
                .setLoiteringDelay(500)
                .build();


        fence.setEdge_lat(Double.parseDouble(data.getString("edge_latitude")));
        fence.setEdge_lng(Double.parseDouble(data.getString("edge_longitude")));
        fence.setRadius(Float.parseFloat(data.getString("radius")));
        fence.setArea(geofence);

        db.updateFence(fence);

         LocationServices.GeofencingApi.addGeofences(
                LocationService.mGoogleApiClient,
                getGeofencingRequest(fence.getArea()),
                fence.getPendingIntent());

        NotificationApi notificationApi = new NotificationApi();
        notificationApi.execute("response",
                "response=success&user_id="+SharedPrefs.getUserId()+"&action=edit_fence&fence_id="+data.getString("fence_id"));
    }

}
