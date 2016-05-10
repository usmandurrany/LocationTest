package com.fournodes.ud.locationtest.interfaces;

import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.User;

import java.util.List;

/**
 * Created by Usman on 18/2/2016.
 */
public interface RequestResult {
    void onSuccess(String result);
    void onFailure();
    void userList(List<User> users);
    void trackEnabled();
    void trackDisabled();
    void liveLocationUpdate(String lat, String lng, String time, String trackId);
    void locationHistory(List<Coordinate> coordinates);
}
