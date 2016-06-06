package com.fournodes.ud.locationtest.interfaces;

import android.location.Location;

import com.fournodes.ud.locationtest.objects.Fence;

import java.util.List;

/**
 * Created by Usman on 16/3/2016.
 */
public interface ServiceMessage extends FenceListInterface{
    void serviceStarted();
    void serviceStopped();
    void fenceTriggered(String data);
    void locationUpdated(String lat, String lng, String time);
    void simulationData(Location location, float avgSpeed, int timeInSec, List<Fence> fenceListActive);
    void updateServer(String result);
}
