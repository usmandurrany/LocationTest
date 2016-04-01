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
import java.net.URLEncoder;
import java.util.ArrayList;
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
    private boolean isProviderGPS = false;
    private Handler timeout;
    private Runnable check;
    private List<Location> locationList;
    private int isInside = 0;
    private int isOutside = 0;
    int i = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        db = new Database(this);
        this.context = this;
        isRunning = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();
        locationList = new ArrayList<>();

        Location gApiLocation = new Location("");//provider name is unecessary
        gApiLocation.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));//your coords of course
        gApiLocation.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));

        locationList.add(gApiLocation);

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps_enabled) {
            FileLogger.e(TAG, "GPS available");
            isProviderGPS = true;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } else if (network_enabled) {
            FileLogger.e(TAG, "GPS not available switching to NETWORK PROVIDER");
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, VerifyGeofenceEvent.this);
        }

        timeout = new Handler();

        check = new Runnable() {
            @Override
            public void run() {
                if (locationList.size() == 1 && isProviderGPS) {
                    lm.removeUpdates(VerifyGeofenceEvent.this);
                    isProviderGPS = false;
                    FileLogger.e(TAG, "GPS fix failed, switching to network");
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, VerifyGeofenceEvent.this);
                    timeout.postDelayed(this,15000);
                } else if (locationList.size() == 1 && !isProviderGPS) {
                    lm.removeUpdates(VerifyGeofenceEvent.this);
                    FileLogger.e(TAG, "Location not found, terminating");
                    timeout.removeCallbacks(this);
                    isRunning=false;
                    stopSelf();

                }
            }
        };
        timeout.postDelayed(check, 5000);
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Override
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

    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

   /* @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        FileLogger.e(TAG, "Acquired current location from Location Manager");
        FileLogger.e(TAG, "Accuracy: " + location.getAccuracy());
        if (location.getExtras() != null)
            FileLogger.e(TAG, "Satellites: " + String.valueOf(location.getExtras().getInt("satellites", -1)));
        FileLogger.e(TAG, "Verifying pending events");

        List<GeofenceEvent> pendingEvents = db.getAllPendingEvents();
        for (GeofenceEvent event : pendingEvents) {

            Fence fence = db.getFence(String.valueOf(event.requestId));
            float distanceFromCenter = toRadiusMeters(new LatLng(fence.getCenter_lat(), fence.getCenter_lng()), new LatLng(location.getLatitude(), location.getLongitude()));


            switch (event.transitionType) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:

                    if (event.retryCount <= 3) {
                        if (distanceFromCenter < fence.getRadius()) {
                            event.isVerified = 1;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Event verified. Entered fence: " + fence.getTitle());
                        } else {
                            event.retryCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Event not verified. Entered fence: " + fence.getTitle());
                            FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
                        }
                    } else {
                        db.removeEvent(event.id);
                        FileLogger.e(TAG, "Event discarded. Too many retries.");

                    }

                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:

                    if (event.retryCount <= 3) {
                        if (distanceFromCenter > fence.getRadius()) {
                            event.isVerified = 1;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Event verified. Entered fence: " + fence.getTitle());
                        } else {
                            event.retryCount++;
                            db.updateEvent(event);
                            FileLogger.e(TAG, "Event not verified. Exited fence: " + fence.getTitle());
                            FileLogger.e(TAG, "Retry count: " + String.valueOf(event.retryCount));
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
        lm.removeUpdates(this);

        isRunning = false;
        FileLogger.e(TAG, "Verification complete, terminating");
        stopSelf();

    }*/

    private void sendNotification(String notificationDetails,String notify_id) {
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
