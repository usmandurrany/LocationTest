package com.fournodes.ud.locationtest.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.RequestResult;
import com.fournodes.ud.locationtest.ServiceMessage;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.network.LocationUpdateApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.util.Date;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public ServiceMessage delegate;
    private static final String TAG = "Location Service";
    public static GoogleApiClient mGoogleApiClient;
    private LocationRequest activeLocationRequest;
    private LocationRequest passiveLocationRequest;
    public static boolean isGoogleApiConnected = false;
    public static NotificationManager mNotificationManager;
    private Runnable update;
    private Handler updateServer;
    private String action;
    public static boolean isRunning = false;
    public boolean isModeActive = false;


    private static LocationService self = null;

    public static LocationService getServiceObject() {
        return self;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast Received");
            switch (intent.getStringExtra("message")) {
                case "switchToPassiveMode":
                    switchToPassiveMode();
                    mNotificationManager.cancel(0);

                    break;
                case "switchToActiveMode":
                    switchToActiveMode();
                    break;
                case "trackEnabled":
                    startPassiveLocationUpdates();
                    break;
                case "trackDisabled":
                    stopLocationUpdates();
                    break;
                case "GCMReceiver":
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("body"));
                    break;
                default:
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("message"));
                    break;
            }
        }
    };


    public LocationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));
    }

    protected void createPassiveLocationRequest() {
        Log.e(TAG, "Location Request Created");

        passiveLocationRequest = new LocationRequest();
        passiveLocationRequest.setInterval(60000);//1 min
        passiveLocationRequest.setFastestInterval(5000);//5 sec
        passiveLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        passiveLocationRequest.setSmallestDisplacement(10);//200 meters

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(passiveLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        Log.e(TAG, "Location Settings Are Correct");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        Log.e(TAG, "Location Settings Needs To Be Resolved");

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Log.e(TAG, "Location Settings Unresolvable");
                        break;
                }
            }
        });


    }

    public void createActiveLocationRequest() {
        Log.e(TAG, "Active Location Request Created");

        activeLocationRequest = new LocationRequest();
        activeLocationRequest.setInterval(SharedPrefs.getLocUpdateInterval());//2 min
        activeLocationRequest.setFastestInterval(5000);//5 sec
        activeLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        activeLocationRequest.setSmallestDisplacement(SharedPrefs.getMinDisplacement());//200 meters

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(passiveLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        Log.e(TAG, "Location Settings Are Correct");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        Log.e(TAG, "Location Settings Needs To Be Resolved");

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Log.e(TAG, "Location Settings Unresolvable");
                        break;
                }
            }
        });

    }

    protected void startActiveLocationUpdates() {
        createActiveLocationRequest();
        activeLocationUpdateNotify();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, activeLocationRequest, this);
        Log.e(TAG, "Active Location Update Started");
        isModeActive = true;
    }

    protected void stopLocationUpdates() {
        Log.e(TAG, "All Location Updates Stopped");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

    }

    public void switchToPassiveMode() {
        Log.e(TAG, "Switching To Passive Mode");
        stopLocationUpdates();
        startPassiveLocationUpdates();
    }

    public void switchToActiveMode() {
        Log.e(TAG, "Switching To Active Mode");
        stopLocationUpdates();
        startActiveLocationUpdates();
    }

    private void startPassiveLocationUpdates() {
        createPassiveLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, passiveLocationRequest, this);
        Log.e(TAG, "Passive Location Update Started");
        isModeActive = false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get an instance of the Notification manager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (intent != null) {
            action = intent.getAction();
        }
        if (action != null && action.equals("switchToPassiveMode")) {
            if (activeLocationRequest != null)
                switchToPassiveMode();

            mNotificationManager.cancel(0);
        } else {
            Log.e(TAG, "Service Started");
            mGoogleApiClient.connect();
            isRunning = true;
            if (delegate != null)
                delegate.serviceStarted();
        }
        self = this;
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Override
    public void onDestroy() {
        isGoogleApiConnected = false;
        isRunning = false;
        mGoogleApiClient.disconnect();
        if (delegate != null)
            delegate.serviceStopped();
        Log.e(TAG, "Service Destroyed");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        isGoogleApiConnected = true;
        Log.e(TAG, "Google API Connected");
        if (SharedPrefs.isPollingEnabled()) {
            startPassiveLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Google API Connection Suspended");

    }

    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    public void onLocationChanged(Location location) {
/*        float displacement = toRadiusMeters(new LatLng(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()),Double.parseDouble(SharedPrefs.getLastDeviceLongitude())),
                new LatLng(location.getLatitude(),location.getLongitude()));*/

        FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                " Long: " + String.valueOf(location.getLongitude()));
        FileLogger.e(TAG, "Accuracy: " + String.valueOf(location.getAccuracy()));
        FileLogger.e(TAG, "Location Provider: " + String.valueOf(location.getProvider()));
        FileLogger.e(TAG, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));

        if (location.hasAccuracy() && location.getAccuracy() <= 60) {
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();
            String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            Database db = new Database(getApplicationContext());
            db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
            //Save in shared prefs after saving in db
            SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
            SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);

            if (delegate != null)
                delegate.locationUpdated(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), mLastUpdateTime);

            int rowCount = db.getLocEntriesCount();

            if (updateServer == null && update == null)
                updServerAfterInterval();
            else if (rowCount >= 5 && updateServer != null && update != null && !isModeActive)
                updateServer.post(update);
            else if (isModeActive && updateServer != null && update != null)
                updateServer.post(update);


            //updServerOnRowCount();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google API Error " + connectionResult.toString());

    }

    private void activeLocationUpdateNotify() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        PendingIntent piSwitchToPassiveMode = PendingIntent.getService(getApplicationContext(), 0, new Intent(getApplicationContext(), LocationService.class).setAction("switchToPassiveMode"), 0);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle("Tracking Active")
                .setContentText("Sending location data to server. Click to stop")
                .setContentIntent(piSwitchToPassiveMode);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(false);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


    public void updServerAfterInterval() {
        if (SharedPrefs.isUpdateServerEnabled()) {
            updateServer = new Handler();

            update = new Runnable() {
                @Override
                public void run() {
                    LocationUpdateApi locationUpdateApi = new LocationUpdateApi(getApplicationContext());
                    locationUpdateApi.delegate = new RequestResult() {
                        @Override
                        public void onSuccess(String result) {
                            updateServer.postDelayed(update, SharedPrefs.getUpdateServerInterval());
                            if (delegate != null)
                                delegate.updateServer("Update Server Successful");
                        }

                        @Override
                        public void onFailure() {
                            updateServer.postDelayed(update, SharedPrefs.getUpdateServerInterval());
                            if (delegate != null)
                                delegate.updateServer("Update Server Failed");


                        }
                    };
                    locationUpdateApi.execute(System.currentTimeMillis());
                }
            };
            updateServer.post(update);
        }

    }

    public void updServerOnRowCount() {
        if (SharedPrefs.isUpdateServerEnabled()) {
            LocationUpdateApi locationUpdateApi = new LocationUpdateApi(getApplicationContext());
            locationUpdateApi.delegate = new RequestResult() {
                @Override
                public void onSuccess(String result) {
                    Log.e(TAG, "Update Success On Row Count");
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "Update Failed On Row Count");

                }
            };
            locationUpdateApi.execute(System.currentTimeMillis());
        }

    }

}
