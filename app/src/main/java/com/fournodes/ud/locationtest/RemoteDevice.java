package com.fournodes.ud.locationtest;

import org.json.JSONArray;

/**
 * Created by Usman on 14/3/2016.
 */
public interface RemoteDevice {
    void liveLocationUpdate(String lat, String lng,String device);
    void locationHistory(JSONArray location,String device);
    void deviceList(JSONArray devices);

}
