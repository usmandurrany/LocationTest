package com.fournodes.ud.locationtest;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.text.DateFormat;

/**
 * Created by Usman on 10/4/2016.
 */
public class SharedLocationListener implements LocationListener {
    public LocationUpdateListener delegate;
    private String className;
    private int locationScore = 0;
    private Location bestLocation;

    public SharedLocationListener(String className) {
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
        FileLogger.e(className, "Accuracy: " + String.valueOf(location.getAccuracy()));
        FileLogger.e(className, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));

        if (className.equals("Location Service")) {
            delegate.gpsLocation(location,0);
        }
        else {
            calculateLocationScore(accuracy);

            if (bestLocation.getAccuracy() > accuracy) {
                bestLocation = location;
                delegate.gpsLocation(location, locationScore);
                if (locationScore >= 10) {
                    // Stop any further location updates
                    delegate.removeGpsLocationUpdates();
                    // Stop timeout handler from firing
                    delegate.removeGpsTimeoutHandler();
                    // Return the best location
                    delegate.gpsBestLocation(bestLocation, locationScore);
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
        FileLogger.e(className, "Location score: " + String.valueOf(locationScore));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
