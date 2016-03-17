package com.fournodes.ud.locationtest;

/**
 * Created by Usman on 16/3/2016.
 */
public interface Messenger {
    void serviceStarted();
    void serviceStopped();
    void locationUpdated();
    void startLocationUpdate();
    void stopLocationUpdate();
}
