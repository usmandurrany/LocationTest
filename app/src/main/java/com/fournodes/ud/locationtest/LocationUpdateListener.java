package com.fournodes.ud.locationtest;

import android.location.Location;

/**
 * Created by Usman on 10/4/2016.
 */
public interface LocationUpdateListener {
    //Android Listener

    void lmBestLocation(Location bestLocation, int locationScore);
    void lmLocation(Location location, int locationScore);
    void lmRemoveUpdates();
    void lmRemoveTimeoutHandler();

    //GMS Listener

    void fusedLocation(Location location); // Return location to delegate
    void fusedBestLocation(Location bestLocation, int locationScore);
    void fusedRemoveUpdates();

/*    void requestLocationUpdate(); // Order delegate to request location
    void createServerUpdateHandler();// Schedule location data transfer to server
    void runEventVerifier(); // Order delegate to run event verifier*/
}
