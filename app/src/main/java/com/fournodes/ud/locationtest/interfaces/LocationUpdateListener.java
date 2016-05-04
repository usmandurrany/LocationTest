package com.fournodes.ud.locationtest.interfaces;

import android.location.Location;

/**
 * Created by Usman on 10/4/2016.
 */
public interface LocationUpdateListener {
    void lmBestLocation(Location bestLocation, int locationScore);
    void lmLocation(Location location, int locationScore);
    void lmRemoveUpdates();
    void lmRemoveTimeoutHandler();
}
