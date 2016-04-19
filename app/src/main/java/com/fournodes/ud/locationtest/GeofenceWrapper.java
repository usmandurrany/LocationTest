package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

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
    private LocationManager locationManager;

    public GeofenceWrapper(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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


    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra("remote", true); //Don't generate notification of fence on this device as it is a remote fence
        intent.putExtra("id",id);
        return PendingIntent.getService(context,id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Will also be called for edit fence as it will replace the current
     * Geofence based on the pending intent
     **/
    public void create(Fence fence) {
        getAttributes(fence);
        if (locationManager != null){
            pendingIntent = getPendingIntent();
            fence.setArea(null);
            fence.setPendingIntent(pendingIntent);

            locationManager.addProximityAlert(centerLatitude,centerLongitude,radius,-1,pendingIntent);
        } else
            FileLogger.e(TAG,"Location Manager is not available");
    }

    public void remove(Fence fence){
        getAttributes(fence);
        if (locationManager != null){
            pendingIntent = fence.getPendingIntent();
            if (pendingIntent == null)
                pendingIntent = getPendingIntent();
            locationManager.removeProximityAlert(pendingIntent);
        }else
            FileLogger.e(TAG,"Location Manager is not available");

    }
}
