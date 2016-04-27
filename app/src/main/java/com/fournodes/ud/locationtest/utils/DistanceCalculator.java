package com.fournodes.ud.locationtest.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.fournodes.ud.locationtest.Constants;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.services.GeofenceTransitionsIntentService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usman on 20/4/2016.
 */
public class DistanceCalculator {
    private static final String TAG = "DistanceCalculator";

    public DistanceCalculator() {}

    public static List<Fence> updateDistanceFromFences(Context context, Location location1, List<Fence> fenceList, boolean updateDb) {
        int newDistance;
        List<Fence> fenceListActive = new ArrayList<>();
        Database db = new Database(context);
        for (Fence fence : fenceList) {
            FileLogger.e(TAG, "Fence: " + fence.getTitle());
            FileLogger.e(TAG, "Last event: " + String.valueOf(fence.getLastEvent()));
            FileLogger.e(TAG, "Old Distance: " + String.valueOf(fence.getDistanceFrom()));
            Location fenceCenter = new Location("");
            fenceCenter.setLatitude(fence.getCenter_lat());
            fenceCenter.setLongitude(fence.getCenter_lng());
            newDistance = calcHaversine(location1, fenceCenter);
            fence.setDistanceFrom(newDistance);

            int fenceOuterPerimeter = (int) Math.ceil((10 / 100) * fence.getRadius());

            if (newDistance <= SharedPrefs.getVicinity() + fenceOuterPerimeter) {
                fence.setIsActive(1);
                fenceListActive.add(fence);
            }
            else
                fence.setIsActive(0);
            FileLogger.e(TAG, "New Distance: " + String.valueOf(newDistance));
            FileLogger.e(TAG, "Is Active: " + String.valueOf(fence.getIsActive()));


            if (newDistance <= fence.getRadius() + fenceOuterPerimeter && fence.getLastEvent() != 1 && fence.getIsActive() == 1) {
                Intent triggerFence = new Intent(context, GeofenceTransitionsIntentService.class);
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
                triggerFence.putExtra("id", fence.getId());
                context.startService(triggerFence);
            }
            else if (newDistance >= fence.getRadius() + fenceOuterPerimeter && fence.getLastEvent() != 2 && fence.getIsActive() == 1) {
                Intent triggerFence = new Intent(context, GeofenceTransitionsIntentService.class);
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
                triggerFence.putExtra("id", fence.getId());
                context.startService(triggerFence);
            }

            if (updateDb)
                db.updateFenceDistance(fence);

        }
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
