package com.fournodes.ud.locationtest.interfaces;

/**
 * Created by Usman on 11/3/2016.
 */
public interface MainFragmentInterface {
    void mapDragStart();
    void mapDragStop();
    void serviceStarted();
    void serviceStopped();
    void fenceTriggered(String data);
    void locationUpdated(String lat, String lng, String time);
    void updateServer(String result);
}
