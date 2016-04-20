package com.fournodes.ud.locationtest.interfaces;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

/**
 * Created by Usman on 24/3/2016.
 */
public interface MapFragmentInterface {

    void moveToFence(LatLng fence);
    void viewLiveLocation(LatLng coordinates, String track_id);
    void viewLocationHistory(JSONArray location);

}
