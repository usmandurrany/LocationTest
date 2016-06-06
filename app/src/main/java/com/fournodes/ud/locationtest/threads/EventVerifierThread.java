package com.fournodes.ud.locationtest.threads;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.fournodes.ud.locationtest.Constants;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.interfaces.FenceListInterface;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.objects.Event;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.DistanceCalculator;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.location.Geofence;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Usman on 9/4/2016.
 */
public class EventVerifierThread extends HandlerThread implements LocationUpdateListener {
    private static final String TAG = "EventVerifierThread";
    public FenceListInterface delegate;

    private Context context;
    private boolean isGpsEnabled;
    private LocationManager locationManager;
    private Handler locationUpdateTimeout;
    private Runnable timeout;
    private Location bestLocation;
    private int locationScore = 0;
    private SharedLocationListener locationListener;
    private Fence exitedFence;


    public EventVerifierThread(Context context) {
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

                if (bestLocation == null) {
                    gpsNotAvailableFallback();
                }
                else if (bestLocation != null) {
                    verifyEvent(bestLocation);
                }
            }
        };
        FileLogger.e(TAG, "Last location provider: " + SharedPrefs.getLastLocationProvider());
        if (SharedPrefs.getLastLocationProvider() != null && SharedPrefs.getLastLocationProvider().equals("gps")) {
            Location location = new Location(SharedPrefs.getLastLocationProvider());
            location.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
            location.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
            FileLogger.e(TAG, "Using last GPS location");
            lmBestLocation(location, 10);
        }
        else
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
            gpsNotAvailableFallback();
        }

    }

    private void gpsNotAvailableFallback() {
        FileLogger.e(TAG, "GPS not available using last known location");
        FileLogger.e(TAG, "Lat: " + String.valueOf(SharedPrefs.getLastDeviceLatitude()) + " Long: " + String.valueOf(SharedPrefs.getLastDeviceLongitude()));
        FileLogger.e(TAG, "Accuracy: " + String.valueOf(SharedPrefs.getLastLocationAccuracy()));
        FileLogger.e(TAG, "Location Time: " + SharedPrefs.getLastLocUpdateTime());
        bestLocation = new Location("");
        bestLocation.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
        bestLocation.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
        verifyEvent(bestLocation);
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {
        this.locationScore = locationScore;
        verifyEvent(bestLocation);
    }

    @Override
    public void lmLocation(Location location, int locationScore) {
        this.locationScore = locationScore;
        bestLocation = location;
    }

    public void verifyEvent(Location bestLocation) {
        FileLogger.e(TAG, "Verifying pending events");

        Database db = new Database(context);
        List<Event> pendingEvents = db.getAllPendingEvents();
        List<Fence> fenceListActive;

        for (Event event : pendingEvents) {

            // Get the fence from the database of whose event needs to be verified
            Fence fence = db.getFence(String.valueOf(event.fenceId));

            // Convert the fence center coordinates into a location object
            Location fenceLocation = new Location("");
            fenceLocation.setLatitude(fence.getCenterLat());
            fenceLocation.setLongitude(fence.getCenterLng());

            // Calculate distance from the fence's center using the users current location
            int distanceFromCenter = DistanceCalculator.calcDistanceFromLocation(fenceLocation, bestLocation);

            FileLogger.e(TAG, "Fence: " + fence.getTitle());
            FileLogger.e(TAG, "Current event: " + getTransitionType(event.eventType));
            FileLogger.e(TAG, "Last event: " + getTransitionType(fence.getLastEvent()));
            FileLogger.e(TAG, "Distance from fence: " + String.valueOf(distanceFromCenter));


            // If last event and current event are different proceed to verification otherwise discard the event
            if (fence.getLastEvent() != event.eventType) {

                switch (event.eventType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        // If device is inside the fence
                        if (distanceFromCenter < fence.getRadius()) {
                            // Check if locationScore is good enough to verify in single pass (verifyCount will be 0)
                            // Or if best of three condition is met
                            if (locationScore > 0 || (event.retryCount < 2 && event.verifyCount > 1)) {
                                event.isVerified = 1;
                                fence.setLastEvent(Geofence.GEOFENCE_TRANSITION_ENTER);

                                // Update the fence in the database so active fence list can be refreshed to account for the last verified event
                                if (db.updateFenceInformation(fence))
                                    serviceMessage("updateFenceListActive");

                                FileLogger.e(TAG, "Event verified.");

                                // Create local notification about the event as well as inform the owner of the fence through server
                                sendNotification("Event verified. Entered fence: " + fence.getTitle(), fence.getNotifyId(), String.valueOf(bestLocation.getLatitude()), String.valueOf(bestLocation.getLongitude()));

                                // Remove any further events of the same type
                                db.removeSimilarEvents(fence.getFenceId(), event.eventType);

                            }
                            // if locationScore is not good enough then run three passes to verify event using best of three
                            else {
                                event.verifyCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                            }
                        }
                        else {
                            // if locationScore is good enough but the condition wasn't satisfied then discard the event immediately
                            if (locationScore > 0 || (event.retryCount > 1 && event.verifyCount < 2)) {
                                db.removeEvent(event.id);
                                FileLogger.e(TAG, "Event not verified and discarded.");
                            }
                            // if locationScore is not good enough then don't immediately discard event
                            else {
                                event.retryCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                            }
                        }

                        break;

                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        // If device is outside the fence
                        if (distanceFromCenter > fence.getRadius()) {
                            // Check if locationScore is good enough to verify in single pass (verifyCount will be 0)
                            // Or if best of three condition is met
                            if (locationScore > 0 || (event.retryCount < 2 && event.verifyCount > 1)) {
                                event.isVerified = 1;
                                fence.setLastEvent(Geofence.GEOFENCE_TRANSITION_EXIT);

                                // Update the fence in the database so active fence list can be refreshed to account for the last verified event
                                if (db.updateFenceInformation(fence))
                                    serviceMessage("updateFenceListActive");
                                exitedFence = fence;
                                FileLogger.e(TAG, "Event verified.");

                                // Create local notification about the event as well as inform the owner of the fence through server
                                sendNotification("Event verified. Exited fence: " + fence.getTitle(), fence.getNotifyId(), String.valueOf(bestLocation.getLatitude()), String.valueOf(bestLocation.getLongitude()));

                                // Remove any further events of the same type
                                db.removeSimilarEvents(fence.getFenceId(), event.eventType);

                            }
                            // if locationScore is not good enough then run three passes to verify event using best of three
                            else {
                                event.verifyCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                            }
                        }
                        else {
                            // if locationScore is good enough but the condition wasn't satisfied then discard the event immediately
                            if (locationScore > 0 || (event.retryCount > 1 && event.verifyCount < 2)) {
                                db.removeEvent(event.id);
                                FileLogger.e(TAG, "Event not verified and discarded.");
                            }
                            // if locationScore is not good enough then dont immediately discard event
                            else {
                                event.retryCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                            }
                        }

                        break;
                }

            }

            /**
             *   If the check fails then discard the event completely
             *   This will ensure that exit never triggers without enter
             */

            else {
                db.removeEvent(event.id);
                FileLogger.e(TAG, "Event discarded due to invalidation");
            }


        }
        fenceListActive = db.onDeviceFence("getActive");
        FileLogger.e(TAG,"Active list size: " + String.valueOf(fenceListActive.size()));
        if (fenceListActive.size() > 1) {
            Collections.sort(fenceListActive);
            FileLogger.e(TAG, "Exited fence position: " + String.valueOf(fenceListActive.indexOf(exitedFence)));
            if (exitedFence != null && fenceListActive.get(0).getFenceId() == exitedFence.getFenceId()) {
                FileLogger.e(TAG, "Moving exited fence to down in list");
                fenceListActive.remove(0);
                fenceListActive.add(1, exitedFence);
            }
        }
        //Return the new active fence list to the caller
        if (delegate != null)
            delegate.activeFenceList(fenceListActive, TAG);

        FileLogger.e(TAG, "Verification complete");
        quit();

    }

    @Override
    public void lmRemoveUpdates() {
        FileLogger.e(TAG, "Stopping GPS location updates");
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void lmRemoveTimeoutHandler() {
        locationUpdateTimeout.removeCallbacksAndMessages(null);
    }


    private String getTransitionType(int transition) {
        switch (transition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entered";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exited";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "Roaming";
            default:
                return "Undefined";
        }
    }

    private void sendNotification(String notificationDetails, int notify_id, String latitude, String longitude) {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle("Alert")
                .setContentText(notificationDetails)
                .setContentIntent(piActivityIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());
        try {
            if (SharedPrefs.pref == null)
                new SharedPrefs(context).initialize();
            String payload = "user_id=" + SharedPrefs.getUserId()
                    + "&notify_id=" + notify_id
                    + "&sender=" + URLEncoder.encode(SharedPrefs.getUserName(), "UTF-8")
                    + "&trigger_time=" + System.currentTimeMillis()
                    + "&latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&message=" + URLEncoder.encode(notificationDetails, "UTF-8");
            new IncomingApi(null, "notify", payload, 6).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
        serviceMessage("verificationComplete");
        return super.quit();
    }


}
