/*
package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
import android.content.Context;

import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.services.LocationService;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;

*/
/**
 * Created by Usman on 24/3/2016.
 *//*

public class PathsenseWrapper {
    private static final String TAG = "PathsenseWrapper";
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

    public PathsenseWrapper(Context context) {
        this.context = context;
    }

    private void getAttributes(Fence fence) {
        this.fence = fence;
        id = fence.getFenceId();
        title = fence.getTitle();
        notify_id = fence.getNotifyId();
        centerLatitude = fence.getCenterLat();
        centerLongitude = fence.getCenterLng();
        edgeLatitude = fence.getEdgeLat();
        edgeLongitude = fence.getEdgeLng();
        radius = fence.getRadius();
    }

    private GeofencingRequest getRequest(com.google.android.gms.location.Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    */
/**
     * Will also be called for edit fence as it will replace the current
     * Geofence based on the pending intent
     **//*

    public void addGeofence(Fence fence) {
        getAttributes(fence);
        if (LocationService.psLocationProviderApi !=null) {
            LocationService.psLocationProviderApi.addGeofence(String.valueOf(id),centerLatitude,centerLongitude,(int)radius, PathsenseGeofenceReceiver.class);
        }
        else
            FileLogger.e(TAG, "Service is not running");
    }

    public void removeGeofence(Fence fence) {
        getAttributes(fence);
        if (LocationService.psLocationProviderApi != null) {
            LocationService.psLocationProviderApi.removeGeofence(String.valueOf(id));
        }
        else
            FileLogger.e(TAG, "Service is not running or fence doesn't exist");

    }
    public void removeGeofences(){
        if (LocationService.psLocationProviderApi != null) {
            LocationService.psLocationProviderApi.removeGeofences();
        }
        else
            FileLogger.e(TAG, "Service is not running or fence doesn't exist");
    }
}
*/
