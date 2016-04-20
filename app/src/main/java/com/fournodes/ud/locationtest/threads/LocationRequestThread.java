package com.fournodes.ud.locationtest.threads;

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

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.SharedPrefs;

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
                    FileLogger.e(TAG, "GPS not available");
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
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsEnabled) {
            FileLogger.e(TAG, "GPS available waiting for fix");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, getLooper());
            locationUpdateTimeout.postDelayed(timeout, TIMEOUT_INTERVAL);
        }
        else {
            FileLogger.e(TAG, "GPS not available");
        activeLocationFallback();
        }

    }

    private void activeLocationFallback() {
        FileLogger.e(TAG,"Requesting location from network");
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener,getLooper());
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {
        FileLogger.e(TAG, "Best location found");
        saveLocation(bestLocation);
    }

    @Override
    public void lmLocation(Location location, int locationScore) {
        lmRemoveUpdates();
        saveLocation(location);
    }

    @Override
    public void lmRemoveUpdates() {
        FileLogger.e(TAG, "Stopping location manager location updates");
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void lmRemoveTimeoutHandler() {
        locationUpdateTimeout.removeCallbacksAndMessages(null);
    }

    @Override
    public void fusedLocation(Location location) {}

    @Override
    public void fusedBestLocation(Location bestLocation, int locationScore) {}

    @Override
    public void fusedRemoveUpdates() {}


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
