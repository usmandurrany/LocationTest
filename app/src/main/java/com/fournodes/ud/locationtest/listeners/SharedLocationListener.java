package com.fournodes.ud.locationtest.listeners;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.utils.FileLogger;

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
        // Discard anything above 250m
        if (accuracy < 250) {

            if ((className.equals("Location Service") && !location.getProvider().equals("network")) || (className.equals("RequestLocUpdateThread") && location.getProvider().equals("network"))) {

                FileLogger.e(className, "Location obtained from " + location.getProvider());
                FileLogger.e(className, "Lat: " + String.valueOf(location.getLatitude()) + " Long: " + String.valueOf(location.getLongitude()));
                FileLogger.e(className, "Accuracy: " + String.valueOf(location.getAccuracy()));
                FileLogger.e(className, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));

                delegate.lmLocation(location, 0);
            }
            else if (className.equals("RequestLocUpdateThread") || className.equals("EventVerifierThread")) {
                calculateLocationScore(accuracy);

                if (bestLocation.getAccuracy() > accuracy) {
                    bestLocation = location;
                    if (locationScore >= 10) {

                        FileLogger.e(className, "Best location obtained from " + location.getProvider());
                        FileLogger.e(className, "Lat: " + String.valueOf(location.getLatitude()) + " Long: " + String.valueOf(location.getLongitude()));
                        FileLogger.e(className, "Accuracy: " + String.valueOf(location.getAccuracy()));
                        FileLogger.e(className, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));
                        FileLogger.e(className, "Location score: " + String.valueOf(locationScore));

                        // Stop any further location updates
                        delegate.lmRemoveUpdates();
                        // Stop timeout handler from firing
                        delegate.lmRemoveTimeoutHandler();
                        // Return the best location
                        delegate.lmBestLocation(bestLocation, locationScore);
                    }

                }
                else {
                    FileLogger.e(className, "Refining location");
                    FileLogger.e(className, "Location score: " + String.valueOf(locationScore));
                    delegate.lmLocation(location, locationScore);
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
        // FileLogger.e(className, "Location score: " + String.valueOf(locationScore));
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
