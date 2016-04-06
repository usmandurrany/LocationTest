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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.DetectedActivitiesIntentService;
import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.RequestResult;
import com.fournodes.ud.locationtest.ServiceMessage;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.EventVerifier;
import com.fournodes.ud.locationtest.network.LocationUpdateApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private long lastUpdateTime = System.currentTimeMillis();
    private Handler forceRequest;
    private Runnable request;
    private LocationManager locationManager;
    private Location bestLocation;
    private float accuracy;
    private int locationScore=0;
    private android.location.LocationListener activeListener;


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
                    Log.e(TAG, "All Location Updates Stopped");
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
        if (SharedPrefs.pref == null)
            new SharedPrefs(getApplicationContext()).initialize();

        lastUpdateTime = SharedPrefs.getLocLastUpdateMillis();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .build();
        }
        mGoogleApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));
    }

    protected void createPassiveLocationRequest() {
        passiveLocationRequest = new LocationRequest();
        passiveLocationRequest.setFastestInterval(1000);
        passiveLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);

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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsEnabled){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isModeActive && bestLocation == null){
                        FileLogger.e(TAG,"GPS fix failed, fallback to fused api");
                        locationManager.removeUpdates(activeListener);
                        activeLocationFallback();
                    }
                }
            },5000);
            locationScore = 0;
            activeListener = new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (SharedPrefs.pref == null)
                        new SharedPrefs(getApplicationContext()).initialize();

                    accuracy = location.getAccuracy();
                    lastUpdateTime = System.currentTimeMillis();
                    SharedPrefs.setLocLastUpdateMillis(lastUpdateTime);

                    FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                            " Long: " + String.valueOf(location.getLongitude()));
                    FileLogger.e(TAG, "Acquired current location from " + location.getProvider());
                    FileLogger.e(TAG, "Accuracy: " + accuracy);

                    if (location.getExtras() != null) {
                        FileLogger.e(TAG, "Satellites: " + String.valueOf(location.getExtras().getInt("satellites", 0)));
                    }


                    if (bestLocation == null)
                        bestLocation = location;
                    else if (bestLocation.getAccuracy() > location.getAccuracy())
                        bestLocation = location;

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
                        locationManager.removeUpdates(this);
                        locationScore=0;
                        FileLogger.e(TAG,"Best location found, stopping location updates");
                        final String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                        Database db = new Database(getApplicationContext());
                        db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
                        //Save in shared prefs after saving in db
                        SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
                        SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
                        SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);
                        switchToPassiveMode();
                        forceLocationRequest();
                        if (!EventVerifier.isRunning) {
                            FileLogger.e(TAG, "Starting event verifier");
                            startService(new Intent(getApplicationContext(), EventVerifier.class));
                        }
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0,activeListener);

        }
        else {
            activeLocationFallback();
        }

    }

    private void activeLocationFallback(){
        activeLocationRequest = new LocationRequest();
        activeLocationRequest.setFastestInterval(1000);
        activeLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(activeLocationRequest);

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
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, activeLocationRequest, this);
    }

    protected void startActiveLocationUpdates() {
        createActiveLocationRequest();
        activeLocationUpdateNotify();
        isModeActive = true;
    }

    protected void stopLocationUpdates() {
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
        if (mNotificationManager != null)
            mNotificationManager.cancel(0);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, passiveLocationRequest, this);
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

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(Bundle bundle) {
        isGoogleApiConnected = true;
        Log.e(TAG, "Google API Connected");

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Long.MAX_VALUE,
                getActivityDetectionPendingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess())
                    FileLogger.e(TAG, "Activity Recognition Started");
                else
                    FileLogger.e(TAG, "Activity Recognition Failed. " + status.getStatusCode());
            }
        });

        if (SharedPrefs.isPollingEnabled()) {
            startPassiveLocationUpdates();
            forceLocationRequest();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Google API Connection Suspended");

    }

    public void forceLocationRequest(){
        /**
         * Force location update if no location has been aquired in an interval of 60 seconds defined by forceInterval.
         * Work around for device with location history disabled.
         **/
        if (forceRequest == null && request == null) {
            FileLogger.e(TAG, "Starting force request handler");
            forceRequest = new Handler();
            request = new Runnable() {
                @Override
                public void run() {
                    FileLogger.e(TAG, "Checking last update");

                    if (SharedPrefs.pref == null) {
                        new SharedPrefs(getApplicationContext()).initialize();
                    }
                    if ((System.currentTimeMillis() - lastUpdateTime) / 1000 >= (SharedPrefs.getForceRequestTimer()) && isRunning && isGoogleApiConnected) {
                        forceRequest.postDelayed(this, SharedPrefs.getForceRequestTimer() * 1000);
                        FileLogger.e(TAG, "Force requesting location update");
                        FileLogger.e(TAG, "Next check after " + String.valueOf(SharedPrefs.getForceRequestTimer()) + " seconds");
                        switchToActiveMode();
                    } else if (!isRunning) {
                        FileLogger.e(TAG, "Force request handler terminating");
                        forceRequest.removeCallbacks(this);
                    } else {
                        FileLogger.e(TAG, "Update found. Next check after " + String.valueOf(SharedPrefs.getForceRequestTimer()) + " seconds");
                        forceRequest.postDelayed(this, SharedPrefs.getForceRequestTimer() * 1000);
                    }
                }
            };
            forceRequest.postDelayed(request, SharedPrefs.getForceRequestTimer() * 1000);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() > 200){
            switchToActiveMode();
        }else {
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();


            lastUpdateTime = System.currentTimeMillis();
            SharedPrefs.setLocLastUpdateMillis(lastUpdateTime);

            FileLogger.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                    " Long: " + String.valueOf(location.getLongitude()));
            FileLogger.e(TAG, "Accuracy: " + String.valueOf(location.getAccuracy()));
            FileLogger.e(TAG, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));

            if (location.hasAccuracy() && location.getAccuracy() <= 60) {


                final String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                Database db = new Database(getApplicationContext());
                db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
                //Save in shared prefs after saving in db
                SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
                SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
                SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);

                if (delegate != null)
                    delegate.locationUpdated(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), mLastUpdateTime);

                int rowCount = db.getLocEntriesCount();

                if (updateServer == null && update == null) {
                    updServerAfterInterval();
                } else if (rowCount >= 5 && updateServer != null && update != null) {// && !isModeActive)
                    updateServer.post(update);
                }

                if (isModeActive) {
                    switchToPassiveMode();
                }
            }

            if (!EventVerifier.isRunning) {
                FileLogger.e(TAG, "Starting event verifier");
                startService(new Intent(getApplicationContext(), EventVerifier.class));
            }
        }
        forceLocationRequest();
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
                .setContentTitle("Location Request")
                .setContentText("Requesting current location. Next run after "+ String.valueOf(SharedPrefs.getForceRequestTimer()) +" seconds")
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

}
