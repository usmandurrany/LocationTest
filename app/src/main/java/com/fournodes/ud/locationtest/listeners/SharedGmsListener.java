package com.fournodes.ud.locationtest.listeners;

import android.location.Location;

import com.fournodes.ud.locationtest.utils.FileLogger;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.google.android.gms.location.LocationListener;

import java.text.DateFormat;

/**
 * Created by Usman on 11/4/2016.
 */
public class SharedGmsListener implements LocationListener {
    private static final String TAG = "SharedGmsListener";
    public LocationUpdateListener delegate;
    private String className;

    private int locationScore = 0;
    private Location bestLocation;

    public SharedGmsListener(String className){
        this.className = className;
        // Initialize location object to enable accuracy check
        bestLocation = new Location("");
        bestLocation.setAccuracy(9999f);
    }

    @Override
    public void onLocationChanged(Location location) {
        float accuracy = location.getAccuracy();

        FileLogger.e(className, "Location obtained from " + location.getProvider());
        FileLogger.e(className, "Lat: " + String.valueOf(location.getLatitude()) + " Long: " + String.valueOf(location.getLongitude()));
        FileLogger.e(className, "Accuracy: " + String.valueOf(accuracy));
        FileLogger.e(className, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));

        if (className.equals("Location Service"))
            delegate.fusedLocation(location);
        else if (className.equals("RequestLocUpdateThread")){

            calculateLocationScore(accuracy);

            if (bestLocation.getAccuracy() > accuracy) {
                bestLocation = location;
                delegate.fusedLocation(location);
                if (locationScore >= 10) {
                    // Stop any further location updates
                    delegate.fusedRemoveUpdates();
                    // Return the best location
                    delegate.fusedBestLocation(bestLocation,locationScore);

                }
            }
        }
    }

    private void calculateLocationScore(float accuracy) {
        if (accuracy < 20.0)
            locationScore += 10;
        else if (accuracy >= 20.0 && accuracy < 40.0)
            locationScore += 5;
        else if (accuracy >= 40.0 && accuracy < 50.0)
            locationScore += 3;
        else if (accuracy >= 50.0)
            locationScore += 1;
        FileLogger.e(className, "Location score: "+String.valueOf(locationScore));
    }
}
