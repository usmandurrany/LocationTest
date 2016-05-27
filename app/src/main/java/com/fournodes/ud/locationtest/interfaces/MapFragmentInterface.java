package com.fournodes.ud.locationtest.interfaces;

import android.location.Location;

import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by Usman on 24/3/2016.
 */
public interface MapFragmentInterface extends FenceListInterface{
    void moveToFence(LatLng fence);
    void viewLiveLocation(LatLng coordinates, String track_id);
    void viewLocationHistory(List<Coordinate> coordinates);
    void simulate(Location location);
    void trackUser(LatLng coordinates, String time, String track_id);
    void trackDisabled();
    void trackEnabled();
    void trackLost();
    void userList(List<User> userList);

    }
