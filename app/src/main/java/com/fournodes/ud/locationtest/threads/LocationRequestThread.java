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

import com.fournodes.ud.locationtest.Constants;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.FenceListInterface;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.services.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.DistanceCalculator;
import com.fournodes.ud.locationtest.utils.FileLogger;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Usman on 9/4/2016.
 */
public class LocationRequestThread extends HandlerThread implements LocationUpdateListener, FenceListInterface {
    private static final String TAG = "RequestLocUpdateThread";
    public FenceListInterface delegate;

    private Context context;
    private boolean isGpsEnabled;
    private LocationManager locationManager;
    private Handler locationUpdateTimeout;
    private Runnable timeout;
    private Location bestLocation;
    private SharedLocationListener locationListener;
    private NotificationManager mNotificationManager;
    private List<Fence> fenceListActive;
    private Handler locationRequestHandler;
    private Runnable locationRequest;


    public LocationRequestThread(Context context, List<Fence> fenceListActive, Handler locationRequestHandler, Runnable locationRequest) {
        super(TAG);
        this.context = context;
        this.fenceListActive = fenceListActive;
        this.locationRequestHandler = locationRequestHandler;
        this.locationRequest = locationRequest;
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
            locationUpdateTimeout.postDelayed(timeout, Constants.GPS_TIMEOUT_INTERVAL);
        }
        else {
            FileLogger.e(TAG, "GPS not available");
            activeLocationFallback();
        }

    }

    private void activeLocationFallback() {
        FileLogger.e(TAG, "Requesting location from network");
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, getLooper());
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {
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

    public void saveLocation(Location bestLocation) {
        this.bestLocation = bestLocation;
        Database db = new Database(context);

        if (SharedPrefs.getReCalcDistanceAtLatitude() != null && SharedPrefs.getLastDeviceLatitude() != null && fenceListActive != null) {

            Location lastLocation = new Location("");
            lastLocation.setLatitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLatitude()));
            lastLocation.setLongitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLongitude()));

            int displacement = DistanceCalculator.calcDistanceFromLocation(bestLocation, lastLocation);

            FileLogger.e(TAG, "Displacement since last recalculation: " + String.valueOf(displacement));
            FileLogger.e(TAG, "Active fences:  " + fenceListActive.size());

            if (displacement >= SharedPrefs.getDistanceThreshold()) {
                RecalculateDistanceThread reCalcDistance = new RecalculateDistanceThread(context, bestLocation);
                reCalcDistance.delegate = this;
                reCalcDistance.start();

            }
            else {
                activeFenceList(DistanceCalculator.updateDistanceFromFences(context, bestLocation, fenceListActive, false));
            }

        }
        else {
            RecalculateDistanceThread reCalcDistance = new RecalculateDistanceThread(context, bestLocation);
            reCalcDistance.delegate = this;
            reCalcDistance.start();
        }
        final String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        db.saveLocation(bestLocation.getLatitude(), bestLocation.getLongitude(), System.currentTimeMillis());
        //Save in shared prefs after saving in db
        SharedPrefs.setLastDeviceLatitude(String.valueOf(bestLocation.getLatitude()));
        SharedPrefs.setLastDeviceLongitude(String.valueOf(bestLocation.getLongitude()));
        SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);
        SharedPrefs.setLastLocationAccuracy(bestLocation.getAccuracy());
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
    public void activeFenceList(List<Fence> fenceListActive) {
        if (fenceListActive.size() > 0) {
            if (fenceListActive.size() > 1) {
                // Sort the fences in ascending order of their distances
                Collections.sort(fenceListActive);

                if (SharedPrefs.isMoving())
                    SharedPrefs.setLocationRequestInterval(timeToFenceEdge(fenceListActive.get(0)));

                FileLogger.e(TAG, "Multiple active fences. Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                locationRequestHandler.postDelayed(locationRequest, SharedPrefs.getLocationRequestInterval() * 1000);

            }
            else {
                if (SharedPrefs.isMoving())
                    SharedPrefs.setLocationRequestInterval(timeToFenceEdge(fenceListActive.get(0)));

                FileLogger.e(TAG, "Single active fence. Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                locationRequestHandler.postDelayed(locationRequest, SharedPrefs.getLocationRequestInterval() * 1000);

            }

            activeLocationUpdateNotify();
        }
        else {
            // No active fences

            int timeInSec;
            if (bestLocation.getSpeed() == 0f)
                timeInSec = 60;
            else {
                timeInSec = (int) Math.ceil(SharedPrefs.getDistanceThreshold() / bestLocation.getSpeed());
                if (timeInSec > 60)
                    timeInSec = 60;
            }
            if (SharedPrefs.isMoving())
                SharedPrefs.setLocationRequestInterval(timeInSec);

            FileLogger.e(TAG, "No active fences. Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
            locationRequestHandler.postDelayed(locationRequest, SharedPrefs.getLocationRequestInterval() * 1000);
        }

        // Return the list to the Location Service
        if (delegate != null)
            delegate.activeFenceList(fenceListActive);
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {}

    // Time is in seconds
    private int timeToFenceEdge(Fence fence) {
        if (bestLocation.getSpeed() < 3.0)
            return 5;

        int distanceFromCenter = fence.getDistanceFrom();
        int radius = fence.getRadius();
        int fencePerimeterInMeters = (int) ((float)SharedPrefs.getFencePerimeterPercentage() / 100) * radius;
        int distanceFromEdge = distanceFromCenter - fencePerimeterInMeters;
        int timeInSec;
        // Enter distance
        if (distanceFromEdge > 0) {

            timeInSec = (int) Math.ceil(distanceFromEdge / bestLocation.getSpeed());
            FileLogger.e(TAG, "Time to enter fence: " + String.valueOf(timeInSec));

            if (timeInSec < 5) {
                Intent triggerFence = new Intent(context, GeofenceTransitionsIntentService.class);
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
                triggerFence.putExtra("id", fence.getId());
                context.startService(triggerFence);
                return 5;

            }
            return timeInSec;
        }
        // Exit distance
        else if (distanceFromEdge < 0) {
            // Convert to positive int
            timeInSec = (int) Math.ceil((distanceFromEdge * -1) / bestLocation.getSpeed());
            FileLogger.e(TAG, "Time to exit fence: " + String.valueOf(timeInSec));
            if (timeInSec < 5)
                return 5;
            else
                return timeInSec;
        }
        // You are at the center of the fence
        else return 5;
    }


    @Override
    public boolean quit() {
        FileLogger.e(TAG, "Thread killed");
        serviceMessage("switchToPassiveMode");
        if (mNotificationManager != null)
            mNotificationManager.cancel(0);
        return super.quit();
    }


}
