package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Usman on 24/3/2016.
 */
public class GeofenceWrapper {
    private static final String TAG = "GeofenceWrapper";
    private Context context;
    private int id;
    private String title;
    private int notify_id;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double edgeLatitude;
    private Double edgeLongitude;
    private float radius;
    private int transitionType;
    private Geofence geofence;
    private PendingIntent pendingIntent;
    private Fence fence;

    public GeofenceWrapper(Context context) {
        this.context = context;
    }
    private void getAttributes(Fence fence){
        this.fence = fence;
        id = fence.getId();
        title = fence.getTitle();
        notify_id = Integer.parseInt(fence.getUserId());
        centerLatitude = fence.getCenter_lat();
        centerLongitude = fence.getCenter_lng();
        edgeLatitude = fence.getEdge_lat();
        edgeLongitude = fence.getEdge_lng();
        radius = fence.getRadius();
        transitionType = fence.getTransitionType();
        pendingIntent = fence.getPendingIntent();
    }

    private GeofencingRequest getRequest(com.google.android.gms.location.Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra("notify_id", String.valueOf(notify_id));
        intent.putExtra("title",title);
        intent.putExtra("remote", true);  //Don't generate notification of fence on this device as it is a remote fence
        return PendingIntent.getService(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Will also be called for edit fence as it will replace the current
     * Geofence based on the pending intent
     **/
    public void create(Fence fence, ResultCallback resultCallback) {
        getAttributes(fence);

        if (LocationServices.GeofencingApi != null && LocationService.isGoogleApiConnected){
            geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(id))
                .setCircularRegion(
                        centerLatitude,
                        centerLongitude,
                        radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionType)
                .setLoiteringDelay(500)
                .build();
            pendingIntent = getPendingIntent();
            fence.setArea(geofence);
            fence.setPendingIntent(pendingIntent);
            LocationServices.GeofencingApi.addGeofences(
                    LocationService.mGoogleApiClient,
                    getRequest(geofence),
                    pendingIntent).setResultCallback(resultCallback);
        } else
            FileLogger.e(TAG,"Service is not running");
    }

    public void remove(Fence fence, ResultCallback resultCallback){
        getAttributes(fence);
        if (LocationServices.GeofencingApi != null && LocationService.isGoogleApiConnected){
            pendingIntent = getPendingIntent();
            LocationServices.GeofencingApi.removeGeofences(LocationService.mGoogleApiClient, pendingIntent).setResultCallback(resultCallback);
        }else
            FileLogger.e(TAG,"Service is not running or fence doesn't exist");

    }
}
