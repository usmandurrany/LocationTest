package com.fournodes.ud.locationtest;

/**
 * Created by Usman on 16/3/2016.
 */
public interface ServiceMessage {
    void serviceStarted();

    void serviceStopped();

    void fenceTriggered(String data);

    void locationUpdated(String lat, String lng, String time);

    void updateServer(String result);

}
