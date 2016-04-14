package com.fournodes.ud.locationtest;

/**
 * Created by Usman on 10/4/2016.
 */

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Usman on 9/4/2016.
 */
public class LocationRequestThread extends HandlerThread implements LocationUpdateListener {
    private static final String TAG = "RequestLocUpdateThread";
    private static final int TIMEOUT_INTERVAL = 5000; // Milliseconds

    private Context context;
    private boolean isGpsEnabled;
    private LocationManager locationManager;
    private Handler locationUpdateTimeout;
    private Runnable timeout;
    private Location bestLocation;
    private SharedLocationListener locationListener;
    private SharedGmsListener gmsLocationListener;
    private NotificationManager mNotificationManager;


    public LocationRequestThread(Context context) {
        super(TAG);
        this.context = context;

        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();
    }

    @Override
    public synchronized void start() {
        super.start();
        activeLocationUpdateNotify();
        locationListener = new SharedLocationListener(TAG);
        locationListener.delegate = this;

        FileLogger.e(TAG, "Thread started");
        //Define the handler responsible for removing location updates after specified interval
        locationUpdateTimeout = new Handler(getLooper());
        timeout = new Runnable() {
            @Override
            public void run() {

                locationManager.removeUpdates(locationListener);

                if (bestLocation == null) {
                    FileLogger.e(TAG, "GPS not available, using fused api");
                    activeLocationFallback();
                }
                else if (bestLocation != null) {
                    saveLocation(bestLocation);
                }
            }
        };
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
/*        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsEnabled) {
            FileLogger.e(TAG, "GPS available waiting for fix");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, getLooper());
            locationUpdateTimeout.postDelayed(timeout, TIMEOUT_INTERVAL);
        }
        else {
            FileLogger.e(TAG, "GPS not available");*/
        activeLocationFallback();
        //}

    }

    private void activeLocationFallback() {
        FileLogger.e(TAG, "Requesting location");

        gmsLocationListener = new SharedGmsListener(TAG);
        gmsLocationListener.delegate = this;

        LocationRequest activeLocationRequest = new LocationRequest();
        activeLocationRequest.setFastestInterval(0);
        activeLocationRequest.setInterval(500);
        activeLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(activeLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(LocationService.mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        FileLogger.e(TAG, "Location Settings Are Correct");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        FileLogger.e(TAG, "Location Settings Needs To Be Resolved");

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        FileLogger.e(TAG, "Location Settings Unresolvable");
                        break;
                }
            }
        });

        if (LocationService.isRunning && LocationService.isGoogleApiConnected)
            LocationServices.FusedLocationApi.requestLocationUpdates(LocationService.mGoogleApiClient, activeLocationRequest, gmsLocationListener, getLooper());
        else
            Toast.makeText(context, "Service is not running", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void gpsBestLocation(Location bestLocation, int locationScore) {
        FileLogger.e(TAG, "Best location found");
        saveLocation(bestLocation);
    }

    @Override
    public void gpsLocation(Location location, int locationScore) {
        bestLocation = location;
    }

    @Override
    public void removeGpsLocationUpdates() {
        FileLogger.e(TAG, "Stopping GPS location updates");
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void removeGpsTimeoutHandler() {
        locationUpdateTimeout.removeCallbacksAndMessages(null);
    }

    @Override
    public void fusedLocation(Location location) {
        bestLocation = location;
    }

    @Override
    public void fusedBestLocation(Location bestLocation, int locationScore) {
        FileLogger.e(TAG, "Best location found");
        saveLocation(bestLocation);
    }

    @Override
    public void removeFusedLocationUpdates() {
        FileLogger.e(TAG, "Stopping Fused Api location updates");
        LocationServices.FusedLocationApi.removeLocationUpdates(LocationService.mGoogleApiClient, gmsLocationListener);
    }

    public void saveLocation(Location bestLocation) {
        this.bestLocation = bestLocation;
        final String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Database db = new Database(context);
        db.saveLocation(bestLocation.getLatitude(), bestLocation.getLongitude(), System.currentTimeMillis());
        //Save in shared prefs after saving in db
        SharedPrefs.setLastDeviceLatitude(String.valueOf(bestLocation.getLatitude()));
        SharedPrefs.setLastDeviceLongitude(String.valueOf(bestLocation.getLongitude()));
        SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);
        SharedPrefs.setLastLocationAccuracy(bestLocation.getAccuracy());

        serviceMessage("switchToPassiveMode");
        quit();
    }


    private void activeLocationUpdateNotify() {

        android.support.v4.app.NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(context);


        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle("Requesting Location")
                .setContentText("Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(false);

        // Issue the notification
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
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
        if (mNotificationManager != null)
            mNotificationManager.cancel(0);
        return super.quit();
    }


}
