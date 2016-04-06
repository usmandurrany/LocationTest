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
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.fournodes.ud.locationtest.network.NotificationApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by Usman on 31/3/2016.
 */
public class EventVerifier extends Service {
    private static final String TAG = "Event Verifier";
    private Context context;
    public static boolean isRunning = false;
    private boolean gps_enabled = false;
    private Database db;
    private LocationManager lm;
    private Location location;
    private Handler timeout;
    private Runnable check;
    private List<GeofenceEvent> pendingEvents;
    private LocationUpdateThread locationUpdateThread;
    private Looper locationUpdateLooper;
    private LocationUpdateListener locationUpdateListener;
    private int locationScore=0;
    private boolean isChecked = false;

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

    public void start() {
        pendingEvents = db.getAllPendingEvents();

        if (pendingEvents.size() <= 0) {
            isRunning = false;
            stopSelf();
            FileLogger.e(TAG, "No events pending, terminating");
        } else {
            if (SharedPrefs.pref == null)
                new SharedPrefs(context).initialize();

            lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (gps_enabled) {
                FileLogger.e(TAG, "GPS available, waiting for fix");
                locationUpdateThread = new LocationUpdateThread("LocationUpdateThread");
                locationUpdateThread.start();
                locationUpdateLooper = locationUpdateThread.getLooper();
                locationUpdateListener = new LocationUpdateListener(null);
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationUpdateListener, locationUpdateLooper);
            } else
                FileLogger.e(TAG, "GPS not available");

            timeout = new Handler();
            check = new Runnable() {
                @Override
                public void run() {
                    if (locationUpdateListener.location == null) {
                        if (SharedPrefs.pref == null)
                            new SharedPrefs(context).initialize();
                        lm.removeUpdates(locationUpdateListener);
                        locationUpdateThread.quit();

                        location = new Location("");//provider name is unecessary
                        location.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));//your coords of course
                        location.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
                        FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                                " Long: " + String.valueOf(location.getLongitude()));

                        FileLogger.e(TAG, "GPS fix failed, using fused api provided location");
                        locationUpdateThread = new LocationUpdateThread("LocationUpdateThreadFallback");
                        locationUpdateThread.start();
                        locationUpdateThread.post(new Runnable() {
                            @Override
                            public void run() {
                                new LocationUpdateListener(location).verify();
                            }
                        });
                        timeout.removeCallbacks(this);
                    } else if (locationUpdateListener.location != null && !isChecked){
                        FileLogger.e(TAG,"Timeout occurred, using best available location");
                        lm.removeUpdates(locationUpdateListener);
                        locationUpdateListener.verify();
                    }
                }
            };
            timeout.postDelayed(check, 5000);
        }
    }


    public class LocationUpdateListener implements LocationListener {
        public Location location;
        public int satellites = 0;
        public float accuracy = 999f;

        public LocationUpdateListener(Location location) {
            this.location = location;
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

        @Override
        public void onLocationChanged(Location location) {
            accuracy = location.getAccuracy();
            FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                    " Long: " + String.valueOf(location.getLongitude()));
            FileLogger.e(TAG, "Acquired current location from " + location.getProvider());
            FileLogger.e(TAG, "Accuracy: " + accuracy);
            if (location.getExtras() != null) {
                FileLogger.e(TAG, "Satellites: " + String.valueOf(location.getExtras().getInt("satellites", 0)));
                satellites=location.getExtras().getInt("satellites", 0);
            }


                if (this.location == null)
                    this.location = location;
                else if (this.location.getAccuracy() > location.getAccuracy())
                    this.location = location;

                if (accuracy < 20.0)
                    locationScore += 10;
                else if (accuracy >= 20.0 && accuracy < 40.0)
                    locationScore += 5;
                else if (accuracy >= 40.0 && accuracy < 50.0)
                    locationScore += 3;
                else if (accuracy >= 50.0)
                    locationScore += 1;

            FileLogger.e(TAG,"Location Score: "+ String.valueOf(locationScore));

            if (locationScore >= 10){
                lm.removeUpdates(locationUpdateListener);
                FileLogger.e(TAG,"Best location found, stopping location updates");
                locationUpdateListener.verify();
                timeout.removeCallbacksAndMessages(check);
            }

        }

        public void verify() {
            isChecked=true;
            FileLogger.e(TAG, "Verifying pending events");

            for (GeofenceEvent event : pendingEvents) {

                Fence fence = db.getFence(String.valueOf(event.requestId));
                FileLogger.e(TAG, "Fence Lat: " + String.valueOf(fence.getCenter_lat()) + " Long: " + String.valueOf(fence.getCenter_lng()));
                float distanceFromCenter = toRadiusMeters(new LatLng(fence.getCenter_lat(), fence.getCenter_lng()), new LatLng(location.getLatitude(), location.getLongitude()));
                FileLogger.e(TAG, "Distance from fence: " + String.valueOf(distanceFromCenter));

                switch (event.transitionType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        FileLogger.e(TAG, "Event: Entered fence: " + fence.getTitle());


                            if (distanceFromCenter < fence.getRadius()) {
                                if (locationScore > 5)
                                    event.verifyCount=3;
                                else
                                    event.verifyCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                            } else {
                                if (locationScore > 5)
                                    event.retryCount=3;
                                else
                                    event.retryCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                            }

                        if (event.retryCount > 1 && event.verifyCount < 2) {
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

                            if (distanceFromCenter > fence.getRadius()) {
                                if (locationScore > 5)
                                    event.verifyCount=3;
                                else
                                    event.verifyCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Verify Count:  " + String.valueOf(event.verifyCount));
                            } else {
                                if (locationScore > 5)
                                    event.retryCount=3;
                                else
                                    event.retryCount++;
                                db.updateEvent(event);
                                FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                            }
                        if (event.retryCount > 1 && event.verifyCount < 2) {
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
            locationUpdateThread.quit();
        }

        private float toRadiusMeters(LatLng center, LatLng radius) {
            float[] result = new float[1];
            Location.distanceBetween(center.latitude, center.longitude,
                    radius.latitude, radius.longitude, result);
            return result[0];
        }

        private void sendNotification(String notificationDetails, String notify_id) {
            // Create an explicit content Intent that starts the main Activity.
            PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
            // Get a notification builder that's compatible with platform versions >= 4
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        isChecked=false;
        if (timeout!=null && check != null)
            timeout.removeCallbacksAndMessages(check);
    }
}