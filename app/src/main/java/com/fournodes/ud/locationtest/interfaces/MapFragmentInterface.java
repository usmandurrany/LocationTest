package com.fournodes.ud.locationtest.interfaces;

import com.fournodes.ud.locationtest.objects.Fence;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

/**
 * Created by Usman on 24/3/2016.
 */
public interface MapFragmentInterface extends FenceListInterface{
    void moveToFence(LatLng fence);
    void viewLiveLocation(LatLng coordinates, String track_id);
    void viewLocationHistory(JSONArray location);
}
