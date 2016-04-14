package com.fournodes.ud.locationtest.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.DetectedActivitiesIntentService;
import com.fournodes.ud.locationtest.EventVerifierThread;
import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.LocationUpdateListener;
import com.fournodes.ud.locationtest.LocationRequestThread;
import com.fournodes.ud.locationtest.RequestResult;
import com.fournodes.ud.locationtest.ServiceMessage;
import com.fournodes.ud.locationtest.SharedGmsListener;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.network.LocationUpdateApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.SocketHandler;

import io.fabric.sdk.android.Fabric;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationUpdateListener {

    public ServiceMessage delegate;
    private static final String TAG = "Location Service";
    public static GoogleApiClient mGoogleApiClient;
    public static boolean isGoogleApiConnected = false;
    private Runnable update;
    private Handler updateServer;
    public static boolean isRunning = false;
    public boolean isModeActive = false;
    private long lastLocationTime = System.currentTimeMillis();
    private Handler requestLocationUpdate;
    private Runnable request;
    private EventVerifierThread eventVerifierThread;
    private LocationRequestThread locationRequestThread;
    private long lastEventVerifiedTime;

    private SharedGmsListener gmsLocationListener;

    private static LocationService self = null;

    public static LocationService getServiceObject() {
        return self;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //FileLogger.e(TAG, "Broadcast Received");
            FileLogger.e(TAG, "Command received: " + intent.getStringExtra("message"));
            switch (intent.getStringExtra("message")) {
                case "switchToPassiveMode": {
                    switchToPassiveMode();
                    runEventVerifier();
                    break;
                }
                case "trackDisabled": {
                    stopLocationUpdates();
                    break;
                }
                case "GCMReceiver": {
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("body"));
                    break;
                }
                case "fastMovement": {
                    FileLogger.e(TAG, "Changing request interval to 15 seconds");
                    requestLocationUpdate.removeCallbacksAndMessages(null);
                    requestLocationUpdate.post(request);
                    break;
                }
                case "slowMovement": {
                    FileLogger.e(TAG, "Changing request interval to 60 seconds");
                    requestLocationUpdate.removeCallbacksAndMessages(null);
                    requestLocationUpdate.post(request);
                    break;
                }
                case "noMovement": {
                    FileLogger.e(TAG, "Changing request interval to 900 seconds");
                    requestLocationUpdate.removeCallbacksAndMessages(null);
                    requestLocationUpdate.post(request);
                    break;
                }
                case "runEventVerifier": {
                    runEventVerifier();
                    break;
                }
                default: {
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("message"));
                    break;
                }
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        createLocRequestHandler();
        createServerUpdateHandler();

        locationRequestThread = new LocationRequestThread(getApplicationContext());
        eventVerifierThread = new EventVerifierThread(getApplicationContext());


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Service Started");
        gmsLocationListener = new SharedGmsListener(TAG);
        gmsLocationListener.delegate = this;

        lastLocationTime = SharedPrefs.getLocLastUpdateMillis();


        if (delegate != null) {
            delegate.serviceStarted();
        }

        if (SharedPrefs.pref == null) {
            new SharedPrefs(getApplicationContext()).initialize();
        }


        /*
        Create An Instance Of GoogleApi
         */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .build();
        }

        mGoogleApiClient.connect();

        isRunning = true;
        self = this;


        return super.onStartCommand(intent, START_STICKY, startId);
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
        requestLocationUpdate.post(request);

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

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Google API Connection Suspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google API Error " + connectionResult.toString());

    }


    protected void createPassiveLocationRequest() {
        LocationRequest passiveLocationRequest = new LocationRequest();
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
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, passiveLocationRequest, gmsLocationListener);
        isModeActive = false;

    }

    protected void stopLocationUpdates() {
        FileLogger.e(TAG, "Stopping listener");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, gmsLocationListener);
    }

    public void switchToPassiveMode() {
        FileLogger.e(TAG, "Starting listener");
        createPassiveLocationRequest();
    }


    @Override
    public void gpsBestLocation(Location bestLocation, int locationScore) {}

    @Override
    public void gpsLocation(Location location, int locationScore) {}

    @Override
    public void removeGpsLocationUpdates() {}

    @Override
    public void removeGpsTimeoutHandler() {}

    @Override
    public void fusedLocation(Location location) {
        if (location.getAccuracy() > 200) {
            FileLogger.e(TAG, "Location update inaccurate. Requesting new location");
            stopLocationUpdates();
            locationRequestThread = new LocationRequestThread(getApplicationContext());
            locationRequestThread.start();
        }
        else {
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();

            lastLocationTime = System.currentTimeMillis();
            SharedPrefs.setLocLastUpdateMillis(lastLocationTime);


            if (location.hasAccuracy() && location.getAccuracy() <= 60) {

                Database db = new Database(getApplicationContext());
                String locationTime=String.valueOf(DateFormat.getTimeInstance().format(location.getTime()));
                db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
                //Save in shared prefs after saving in db
                SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
                SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
                SharedPrefs.setLastLocUpdateTime(locationTime);
                SharedPrefs.setLastLocationAccuracy(location.getAccuracy());

                if (delegate != null)
                    delegate.locationUpdated(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), locationTime);

                int rowCount = db.getLocEntriesCount();

                if (updateServer == null && update == null)
                    createServerUpdateHandler();
                else if (rowCount >= 5 && updateServer != null && update != null)
                    updateServer.post(update);

            }
            runEventVerifier();
        }
    }

    @Override
    public void fusedBestLocation(Location bestLocation, int locationScore) {
        // Not used here because this just listens for updates regardless of accuracy
    }

    @Override
    public void removeFusedLocationUpdates() {
        // Not used here because listener is never stopped except when it runs force request
    }

    public void createServerUpdateHandler() {
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
        }

    }

    public void createLocRequestHandler() {
        FileLogger.e(TAG, "Force request handler created using interval of " + SharedPrefs.getLocationRequestInterval() + " seconds");
        requestLocationUpdate = new Handler();
        request = new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.pref == null)
                    new SharedPrefs(getApplicationContext()).initialize();

                long lastLocElapsedTime = (System.currentTimeMillis() - lastLocationTime) / 1000; // In Seconds


                FileLogger.e(TAG, "Checking last update");

                if (locationRequestThread.getState() == Thread.State.NEW) {
                    FileLogger.e(TAG, "Location request first run");
                    locationRequestThread.start();
                    isModeActive = true;
                    requestLocationUpdate.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }
                else if (lastLocElapsedTime >= SharedPrefs.getLocationRequestInterval() && isRunning && isGoogleApiConnected &&
                        locationRequestThread.getState() == Thread.State.TERMINATED) {
                    FileLogger.e(TAG, "Requesting location update");
                    FileLogger.e(TAG, "Next check after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                    stopLocationUpdates();
                    locationRequestThread = new LocationRequestThread(getApplicationContext());
                    locationRequestThread.start();
                    isModeActive = true;
                    requestLocationUpdate.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }
                else if (!isRunning) {
                    FileLogger.e(TAG, "Service not running. Stopping self");
                    requestLocationUpdate.removeCallbacksAndMessages(null);
                }
                else {
                    FileLogger.e(TAG, "Update found. Next check after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                    requestLocationUpdate.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }
            }
        };
    }


    public void runEventVerifier() {
        if (eventVerifierThread.getState() == Thread.State.NEW) {
            FileLogger.e(TAG, "Event verifier first run");
            eventVerifierThread.start();
        }
        else if (eventVerifierThread.getState() == Thread.State.TERMINATED  && SharedPrefs.getPendingEventCount() > 0) { //&& (System.currentTimeMillis() - lastEventVerifiedTime > 5000)) {
            FileLogger.e(TAG, "Pending events: " + String.valueOf(SharedPrefs.getPendingEventCount()));
            FileLogger.e(TAG, "Starting event verifier");
            lastEventVerifiedTime = System.currentTimeMillis();
            eventVerifierThread = new EventVerifierThread(getApplicationContext());
            eventVerifierThread.start();
        }
        else if (eventVerifierThread.isAlive()) {
            FileLogger.e(TAG, "Event Verifier Thread is already running");
        }
        else if (SharedPrefs.getPendingEventCount() == 0)
            FileLogger.e(TAG, "No events pending");

    }


    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        isGoogleApiConnected = false;
        isRunning = false;
        isModeActive = false;
        if (requestLocationUpdate != null)
            requestLocationUpdate.removeCallbacksAndMessages(null);


        if (delegate != null)
            delegate.serviceStopped();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.e(TAG, "Service Destroyed");
        super.onDestroy();
    }
}
