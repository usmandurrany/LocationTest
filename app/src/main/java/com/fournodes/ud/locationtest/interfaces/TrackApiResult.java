package com.fournodes.ud.locationtest.interfaces;

import org.json.JSONArray;

/**
 * Created by Usman on 14/3/2016.
 */
public interface TrackApiResult {
    void liveLocationUpdate(String lat, String lng, String track_id);
    void locationHistory(JSONArray location);
    void userList(JSONArray users);

}
