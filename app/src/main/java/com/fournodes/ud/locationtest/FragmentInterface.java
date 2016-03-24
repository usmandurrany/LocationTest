package com.fournodes.ud.locationtest;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

/**
 * Created by Usman on 11/3/2016.
 */
public interface FragmentInterface {
    void mapDragStart();
    void mapDragStop();
    void moveToFence(LatLng fence);
    void viewLiveLocation(LatLng coordinates, String track_id);
    void viewLocationHistory(JSONArray location);
    void serviceStarted();
    void serviceStopped();
    void locationUpdated(String lat, String lng, String time);

}
