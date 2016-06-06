package com.fournodes.ud.locationtest.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.services.GeofenceTransitionsIntentService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Usman on 20/4/2016.
 */
public class DistanceCalculator {
    private static final String TAG = "DistanceCalculator";

    public DistanceCalculator() {}

    public static List<Fence> updateDistanceFromFences(Context context, Location location1, List<Fence> fenceList, boolean updateDb) {
        int newCenterDistance;
        int newEdgeDistance;
        int oldEdgeDistance;
        List<Fence> fenceListActive = new ArrayList<>();
        Database db = new Database(context);
        for (Fence fence : fenceList) {
            //Old distance from edge for logging purpose
            oldEdgeDistance = fence.getDistanceFromEdge();

            //Location object for fence center coordinate
            Location fenceCenter = new Location("");
            fenceCenter.setLatitude(fence.getCenterLat());
            fenceCenter.setLongitude(fence.getCenterLng());

            //Distances from fence center and edge
            newCenterDistance = calcHaversine(location1, fenceCenter);
            newEdgeDistance = (int) (newCenterDistance - fence.getRadius());

            //Fence perimeter value in meters
            int fencePerimeterInMeters = (int) (((float) SharedPrefs.getFencePerimeterPercentage() / 100) * fence.getRadius());

            //Update the distance from edge in fence object
            fence.setDistanceFromEdge(newEdgeDistance);

            // Fence will be active if its edge distance is less than user vicinity in meters
            if (newEdgeDistance <= SharedPrefs.getVicinity() + fencePerimeterInMeters) {
                fence.setIsActive(1);
                fenceListActive.add(fence);
                FileLogger.e(TAG, "Fence: " + fence.getTitle());
                FileLogger.e(TAG, "Last event: " + String.valueOf(fence.getLastEvent()));
                FileLogger.e(TAG, "Old Distance: " + String.valueOf(oldEdgeDistance));
                FileLogger.e(TAG, "New Distance: " + String.valueOf(newEdgeDistance));
                FileLogger.e(TAG, "Is Active: " + String.valueOf(fence.getIsActive()));
            }

            // Enter will trigger once user is inside the fence
            if (newCenterDistance <= fence.getRadius() && fence.getLastEvent() != 1 && fence.getIsActive() == 1) {
                Intent triggerFence = new Intent(context, GeofenceTransitionsIntentService.class);
                triggerFence.putExtra("latitude", location1.getLatitude());
                triggerFence.putExtra("longitude", location1.getLongitude());
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
                triggerFence.putExtra("id", fence.getFenceId());
                context.startService(triggerFence);
            }
            // Exit will trigger once user is outside the fence
            else if (newCenterDistance >= fence.getRadius() && fence.getLastEvent() != 2 && fence.getIsActive() == 1) {
                Intent triggerFence = new Intent(context, GeofenceTransitionsIntentService.class);
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
                triggerFence.putExtra("id", fence.getFenceId());
                context.startService(triggerFence);
            }

            //Fence deactivate condition is the opposite of active condition
            if (newEdgeDistance >= SharedPrefs.getVicinity())
                fence.setIsActive(0);

            //Update the fence in database if update flag is true
            if (updateDb)
                db.updateFenceDistance(fence);

        }

        // Sort the active fence list
        if (fenceListActive.size() > 0)
            Collections.sort(fenceListActive);
        else
            FileLogger.e(TAG, "No active fences.");

        return fenceListActive;
    }

    public static int calcDistanceFromLocation(Location location1, Location location2) {
        return calcHaversine(location1, location2);
    }

    private static int calcHaversine(Location location1, Location location2) {
        long eRadius = 6378137;
        double distanceLat = Math.toRadians(location2.getLatitude() - location1.getLatitude());
        double distanceLng = Math.toRadians(location2.getLongitude() - location1.getLongitude());

        double a = Math.sin(distanceLat / 2) * Math.sin(distanceLat / 2) +
                Math.cos(Math.toRadians(location1.getLatitude())) * Math.cos(Math.toRadians(location2.getLatitude())) *
                        Math.sin(distanceLng / 2) * Math.sin(distanceLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) (eRadius * c);

    }

}
