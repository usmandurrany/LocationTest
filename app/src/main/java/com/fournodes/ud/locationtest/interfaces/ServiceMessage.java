package com.fournodes.ud.locationtest.interfaces;

import android.location.Location;

/**
 * Created by Usman on 16/3/2016.
 */
public interface ServiceMessage extends FenceListInterface{
    void serviceStarted();
    void serviceStopped();
    void fenceTriggered(String data);
    void locationUpdated(String lat, String lng, String time);
    void listenerLocation(Location location);
    void updateServer(String result);
}
