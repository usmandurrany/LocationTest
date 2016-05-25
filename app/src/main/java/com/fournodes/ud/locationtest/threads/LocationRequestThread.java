package com.fournodes.ud.locationtest.threads;

/**
 * Created by Usman on 10/4/2016.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.services.LocationService;
import com.fournodes.ud.locationtest.utils.FileLogger;

import java.io.File;

/**
 * Created by Usman on 9/4/2016.
 */
public class LocationRequestThread extends HandlerThread implements LocationUpdateListener {
    private static final String TAG = "RequestLocUpdateThread";
    public LocationUpdateListener delegate;

    private Context context;
    private boolean isGpsEnabled;
    private boolean isNetworkEnabled;
    private LocationManager locationManager;
    private Handler locationUpdateTimeout;
    private Runnable timeout;
    private Location bestLocation;
    private Location networkLocation;
    private SharedLocationListener locationListener;
    private LocationService locationService;

    public LocationRequestThread(Context context, LocationService locationService) {
        super(TAG);
        this.context = context;
        this.locationService = locationService;
        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();
        if (SharedPrefs.getLocationPollTimeout() < 60000)
            SharedPrefs.setLocationPollTimeout(60000);
        if (locationService.fenceListActive != null && locationService.fenceListActive.size() == 0)
            FileLogger.e(TAG,"No fences active polling only network for location");
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

                if (bestLocation == null && networkLocation != null) {
                    FileLogger.e(TAG, "GPS fix failed, using network location");
                    locationService.lmLocation(networkLocation, 10);
                    quit();
                }
                else if (bestLocation == null && networkLocation == null) {
                    FileLogger.e(TAG, "Location not available. Will retry");
                    quit();
                }
            }
        };
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (locationService.fenceListActive == null || locationService.fenceListActive.size() > 0) {
            if (isGpsEnabled) {
                //FileLogger.e(TAG, "GPS available, waiting for fix");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, getLooper());
            }
            else {
                notifyLocationDisabled("GPS");
                FileLogger.e(TAG, "GPS unavailable");
            }
        }
        if (isNetworkEnabled) {
            //FileLogger.e(TAG, "Network available, requesting location");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, getLooper());
        }
        else {
            notifyLocationDisabled("Network");
            FileLogger.e(TAG, "Network unavailable");
        }

        if (!isGpsEnabled && !isNetworkEnabled) {
            notifyLocationDisabled("Location");
            quit();
        }
        else
            locationUpdateTimeout.postDelayed(timeout, SharedPrefs.getLocationPollTimeout());


    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {
        this.bestLocation = bestLocation;
        locationService.lmLocation(bestLocation, 10);
        quit();
    }

    @Override
    public void lmLocation(Location location, int locationScore) {
        this.networkLocation = location;
        if (locationService.fenceListActive != null && locationService.fenceListActive.size() == 0){
            locationUpdateTimeout.removeCallbacks(timeout);
            locationService.lmLocation(networkLocation, 10);
            quit();
        }

    }

    @Override
    public void lmRemoveUpdates() {
        //FileLogger.e(TAG, "Stopping location manager location updates");
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

    private void notifyLocationDisabled(String provider) {
        // Create an explicit content Intent that starts the main Activity.
        Intent locationSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        PendingIntent piActivityIntent = PendingIntent.getActivity(context, 0, locationSettingIntent, 0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(provider + " Disabled")
                .setContentText("Click to change location settings")
                .setContentIntent(piActivityIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


    @Override
    public boolean quit() {
        FileLogger.e(TAG, "Thread killed");
        // Only switch to passive if this thread was successful in obtaining location
        if (bestLocation != null || networkLocation != null) {
/*            if (SharedPrefs.getLocationPollTimeout() > 5000) {
                SharedPrefs.setLocationPollTimeout(5000);
                FileLogger.e(TAG, "Resetting location timeout value to 5 seconds");
            }*/
            serviceMessage("locationRequestSuccess");
        }
        else if (bestLocation == null && networkLocation == null && (isNetworkEnabled || isGpsEnabled)) {
/*            if (SharedPrefs.getLocationPollTimeout() < 60000) {
                SharedPrefs.setLocationPollTimeout(60000);
                FileLogger.e(TAG, "Increasing location timeout value to 60 seconds");
            }*/
            serviceMessage("locationRequestFailed");
        }
        else {
            SharedPrefs.setIsLocationEnabled(false);
            serviceMessage("quit");
        }
        return super.quit();
    }


}
