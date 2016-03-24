package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Usman on 24/3/2016.
 */
public class GeofenceWrapper {
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

    public GeofenceWrapper(Context context, Fence fence) {
        this.context = context;
        id = fence.getId();
        title = fence.getTitle();
        notify_id = Integer.parseInt(fence.getUserId());
        centerLatitude = fence.getCenter_lat();
        centerLongitude = fence.getCenter_lng();
        edgeLatitude = fence.getEdge_lat();
        edgeLongitude = fence.getEdge_lng();
        radius = fence.getRadius();
        transitionType = fence.getTransitionType();

    }

    private GeofencingRequest getRequest(com.google.android.gms.location.Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private PendingIntent getPendingIntent(int id, String notify_id) {
        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra("notify_id", notify_id);
        intent.putExtra("remote", true);  //Don't generate notification of fence on this device as it is a remote fence
        return PendingIntent.getService(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Geofence create() {
        geofence = new Geofence.Builder()
                .setRequestId(title)
                .setCircularRegion(
                        centerLatitude,
                        centerLongitude,
                        radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionType)
                .setLoiteringDelay(500)
                .build();
        if (LocationServices.GeofencingApi != null) {
            LocationServices.GeofencingApi.addGeofences(
                    LocationService.mGoogleApiClient,
                    getRequest(geofence),
                    getPendingIntent(id, String.valueOf(notify_id)));
            return geofence;
        } else
            return null;
    }


}
