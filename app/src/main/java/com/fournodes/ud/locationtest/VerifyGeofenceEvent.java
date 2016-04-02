package com.fournodes.ud.locationtest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.fournodes.ud.locationtest.network.NotificationApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Usman on 31/3/2016.
 */
public class VerifyGeofenceEvent extends Service implements LocationListener {
    private static final String TAG = "Verify Geofence Event";
    private Context context;
    public static boolean isRunning = false;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private Database db;
    private LocationManager lm;
    private Location location;
    private Handler timeout;
    private Runnable check;
    private List<GeofenceEvent> pendingEvents;
    /*    private List<Location> locationList;
        private int isInside = 0;
        private int isOutside = 0;
        int i = 0;
    private boolean isProviderGPS = false; */


    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
        db = new Database(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        start();
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    public void start(){
        pendingEvents = db.getAllPendingEvents();

        if (pendingEvents.size() <= 0)
        {
            isRunning = false;
            stopSelf();
            FileLogger.e(TAG,"No events pending, terminating");
        }else {
            if (SharedPrefs.pref == null)
                new SharedPrefs(context).initialize();

            lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (gps_enabled) {
                FileLogger.e(TAG, "GPS available");
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            } else
                FileLogger.e(TAG, "GPS not available");

            timeout = new Handler();
            check = new Runnable() {
                @Override
                public void run() {
                    if (location == null) {
                        if (SharedPrefs.pref == null)
                            new SharedPrefs(context).initialize();
                        lm.removeUpdates(VerifyGeofenceEvent.this);

                        location = new Location("");//provider name is unecessary
                        location.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));//your coords of course
                        location.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
                        FileLogger.e(TAG, "GPS fix failed, using GoogleApi provided location");
                        FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                                " Long: " + String.valueOf(location.getLongitude()));
                        verify();
                        timeout.removeCallbacks(this);
                    }
                }
            };
            timeout.postDelayed(check, 5000);
        }
    }

/*    @Override
    public void onLocationChanged(Location location) {
        FileLogger.e(TAG, "Acquired current location from " + location.getProvider());
        FileLogger.e(TAG, "Accuracy: " + location.getAccuracy());
        if (location.getExtras() != null)
            FileLogger.e(TAG, "Satellites: " + String.valueOf(location.getExtras().getInt("satellites", -1)));

        locationList.add(location);

        if (locationList.size() == 3) {
            lm.removeUpdates(this);
            FileLogger.e(TAG, "Verifying pending events");

            List<GeofenceEvent> pendingEvents = db.getAllPendingEvents();
            for (GeofenceEvent event : pendingEvents) {


                Fence fence = db.getFence(String.valueOf(event.requestId));


                switch (event.transitionType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        if (event.retryCount <= 3) {


                            for (Location locationSample : locationList) {
                                i++;
                                float distanceFromCenter = toRadiusMeters(new LatLng(fence.getCenter_lat(), fence.getCenter_lng()), new LatLng(locationSample.getLatitude(), locationSample.getLongitude()));

                                if (distanceFromCenter < fence.getRadius()) {
                                    FileLogger.e(TAG,"Location " +String.valueOf(i)+": Inside");

                                    isInside++;
                                }else {
                                    FileLogger.e(TAG,"Location " +String.valueOf(i)+": Outside");

                                    isOutside++;
                                }if (i >= 3) {
                                    if (isInside > 1 && isOutside < 2) {
                                        event.isVerified = 1;
                                        db.updateEvent(event);
                                        FileLogger.e(TAG, "Event verified. Entered fence: " + fence.getTitle());
                                        sendNotification("Event verified. Entered fence: " + fence.getTitle(),fence.getUserId());
                                    } else if (isOutside > 1 && isInside < 2) {
                                        event.retryCount++;
                                        db.updateEvent(event);
                                        FileLogger.e(TAG, "Event not verified. Entered fence: " + fence.getTitle());
                                        FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                                    }

                                }
                            }


                        } else {
                            db.removeEvent(event.id);
                            FileLogger.e(TAG, "Event discarded. Too many retries.");

                        }

                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:

                        if (event.retryCount <= 3) {

                            for (Location locationSample : locationList) {
                                i++;
                                float distanceFromCenter = toRadiusMeters(new LatLng(fence.getCenter_lat(), fence.getCenter_lng()), new LatLng(locationSample.getLatitude(), locationSample.getLongitude()));

                                if (distanceFromCenter > fence.getRadius()) {
                                    FileLogger.e(TAG,"Location " +String.valueOf(i)+": Outside");
                                    isOutside++;
                                } else {
                                    FileLogger.e(TAG,"Location " +String.valueOf(i)+": Inside");
                                    isInside++;
                                }
                                if (i >= 3) {
                                    if (isOutside > 1 && isInside < 2) {
                                        event.isVerified = 1;
                                        db.updateEvent(event);
                                        FileLogger.e(TAG, "Event verified. Exited fence: " + fence.getTitle());
                                        sendNotification("Event verified. Exited fence: " + fence.getTitle(),fence.getUserId());
                                    } else if (isInside > 1 && isOutside < 2) {
                                        event.retryCount++;
                                        db.updateEvent(event);
                                        FileLogger.e(TAG, "Event not verified. Exited fence: " + fence.getTitle());
                                        FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                                    }

                                }
                            }


                        } else {
                            db.removeEvent(event.id);
                            FileLogger.e(TAG, "Event discarded. Too many retries.");
                        }
                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        break;
                    default:
                        break;

                }


            }
            isRunning = false;
            FileLogger.e(TAG, "Verification complete, terminating");
            stopSelf();
        }
    }*/

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    public void onLocationChanged(Location location) {
        FileLogger.e(TAG, "Acquired current location from " + location.getProvider());
        FileLogger.e(TAG, "Accuracy: " + location.getAccuracy());
        if (location.getExtras() != null)
            FileLogger.e(TAG, "Satellites: " + String.valueOf(location.getExtras().getInt("satellites", -1)));
        this.location = location;
        verify();
        lm.removeUpdates(VerifyGeofenceEvent.this);
    }

    public void verify() {
        FileLogger.e(TAG, "Verifying pending events");

        for (GeofenceEvent event : pendingEvents) {

            Fence fence = db.getFence(String.valueOf(event.requestId));
            FileLogger.e(TAG,"Fence Lat: " + String.valueOf(fence.getCenter_lat()) + " Long: " + String.valueOf(fence.getCenter_lng()));
            float distanceFromCenter = toRadiusMeters(new LatLng(fence.getCenter_lat(), fence.getCenter_lng()), new LatLng(location.getLatitude(), location.getLongitude()));
            FileLogger.e(TAG,"Distance from fence: " + String.valueOf(distanceFromCenter));

            switch (event.transitionType) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    FileLogger.e(TAG, "Event: Entered fence: " + fence.getTitle());

                    if (event.retryCount < 2 && event.verifyCount < 2) {

                        if (distanceFromCenter < fence.getRadius()) {
                            event.verifyCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                        } else {
                            event.retryCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                        }
                    } else if (event.retryCount > 1 && event.verifyCount < 2) {
                        db.removeEvent(event.id);
                        FileLogger.e(TAG, "Event not verified and discarded.");

                    } else if (event.retryCount < 2 && event.verifyCount > 1) {
                        event.isVerified = 1;
                        fence.setLastEvent(1);
                        db.updateEvent(event);
                        db.updateFence(fence);
                        FileLogger.e(TAG, "Event verified.");
                        sendNotification("Event verified. Entered fence: " + fence.getTitle(), fence.getUserId());
                    }

                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    FileLogger.e(TAG, "Event: Exited fence: " + fence.getTitle());

                    if (event.retryCount < 2 && event.verifyCount < 2) {

                        if (distanceFromCenter > fence.getRadius()) {
                            event.verifyCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                        } else {
                            event.retryCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                        }
                    } else if (event.retryCount > 1 && event.verifyCount < 2) {
                        db.removeEvent(event.id);
                        FileLogger.e(TAG, "Event not verified and discarded.");

                    } else if (event.retryCount < 2 && event.verifyCount > 1) {
                        event.isVerified = 1;
                        fence.setLastEvent(2);
                        db.updateEvent(event);
                        db.updateFence(fence);
                        FileLogger.e(TAG, "Event verified.");
                        sendNotification("Event verified. Exited fence: " + fence.getTitle(), fence.getUserId());
                    }

                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    break;
                default:
                    break;

            }


        }
        isRunning = false;
        FileLogger.e(TAG, "Verification complete, terminating");
        stopSelf();
    }

    private void sendNotification(String notificationDetails, String notify_id) {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
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
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());
        try {
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();
            new NotificationApi().execute("notification", "user_id="
                    + SharedPrefs.getUserId() + "&notify_id=" + notify_id
                    + "&sender=" + URLEncoder.encode(SharedPrefs.getUserName(), "UTF-8")
                    + "&trigger_time=" + System.currentTimeMillis()
                    + "&message=" + URLEncoder.encode(notificationDetails, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
