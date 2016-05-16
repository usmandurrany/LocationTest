package com.fournodes.ud.locationtest.threads;

/**
 * Created by Usman on 10/4/2016.
 */

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.Constants;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.utils.FileLogger;

/**
 * Created by Usman on 9/4/2016.
 */
public class LocationRequestThread extends HandlerThread implements LocationUpdateListener {
    private static final String TAG = "RequestLocUpdateThread";
    public LocationUpdateListener delegate;

    private Context context;
    private boolean isGpsEnabled;
    private LocationManager locationManager;
    private Handler locationUpdateTimeout;
    private Runnable timeout;
    private Location bestLocation;
    private SharedLocationListener locationListener;
    private boolean isFallbackActive = false;

    public LocationRequestThread(Context context) {
        super(TAG);
        this.context = context;
        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();
    }


    @Override
    public synchronized void start() {
        super.start();

        locationListener = new SharedLocationListener(TAG);
        locationListener.delegate = this;

        FileLogger.e(TAG, "Thread started");
        //Define the handler responsible for removing location updates after specified interval
        locationUpdateTimeout = new Handler(getLooper());
        timeout = new Runnable() {
            @Override
            public void run() {

                locationManager.removeUpdates(locationListener);

                if (bestLocation == null && !isFallbackActive) {
                    FileLogger.e(TAG, "GPS not available");
                    activeLocationFallback();
                }
                else if (bestLocation == null && isFallbackActive) {
                    FileLogger.e(TAG, "Location not available. Will retry");
                    quit();
                }
                else if (bestLocation != null) {
                    delegate.lmLocation(bestLocation, 10);
                }
            }
        };
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsEnabled) {
            FileLogger.e(TAG, "GPS available waiting for fix");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, getLooper());
            locationUpdateTimeout.postDelayed(timeout, SharedPrefs.getGpsPollTimeout());
        }
        else {
            FileLogger.e(TAG, "GPS location is disabled");
            activeLocationFallback();
        }

    }

    private void activeLocationFallback() {
        if (locationManager.isProviderEnabled("network")) {
            FileLogger.e(TAG, "Requesting location from network");
            isFallbackActive = true;

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, getLooper());
            locationUpdateTimeout.postDelayed(timeout, Constants.NETWORK_TIMEOUT_INTERVAL);
        }
        else {
            FileLogger.e(TAG, "Network location is disabled");
            quit();
        }
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {
        if (SharedPrefs.getGpsPollTimeout() > 5000) {
            SharedPrefs.setGpsPollTimeout(5000);
            FileLogger.e(TAG, "Resetting GPS timeout value to 5 seconds");

        }
        this.bestLocation = bestLocation;
        quit();
    }

    @Override
    public void lmLocation(Location location, int locationScore) {
        this.bestLocation = location;
        lmRemoveUpdates();
        lmRemoveTimeoutHandler();
        delegate.lmLocation(location, locationScore);
        quit();
    }

    @Override
    public void lmRemoveUpdates() {
        FileLogger.e(TAG, "Stopping location manager location updates");
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void lmRemoveTimeoutHandler() {
        locationUpdateTimeout.removeCallbacks(timeout);
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    @Override
    public boolean quit() {
        FileLogger.e(TAG, "Thread killed");
        // Only switch to passive if this thread was successful in obtaining location
        if (bestLocation != null || (bestLocation == null && !isFallbackActive))
            serviceMessage("switchToPassiveMode");
        else if (bestLocation == null && isFallbackActive) {
            if (SharedPrefs.getGpsPollTimeout() < 60000) {
                SharedPrefs.setGpsPollTimeout(60000);
                FileLogger.e(TAG, "Increasing GPS timeout value to 60 seconds");
            }
            serviceMessage("locationRequestThreadFailed");
        }

        return super.quit();
    }


}
