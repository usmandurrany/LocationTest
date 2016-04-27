package com.fournodes.ud.locationtest.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.Constants;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.apis.LocationUpdateApi;
import com.fournodes.ud.locationtest.interfaces.FenceListInterface;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.interfaces.ServiceMessage;
import com.fournodes.ud.locationtest.listeners.SharedLocationListener;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.threads.EventVerifierThread;
import com.fournodes.ud.locationtest.threads.LocationRequestThread;
import com.fournodes.ud.locationtest.threads.RecalculateDistanceThread;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.DistanceCalculator;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;

import java.text.DateFormat;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationUpdateListener, FenceListInterface {
    private static final String TAG = "Location Service";

    private static LocationService self = null;
    public ServiceMessage delegate;
    public static GoogleApiClient mGoogleApiClient;

    private Runnable update;
    private Handler updateServer;
    private Handler locationRequestHandler;
    private Runnable locationRequest;

    public static boolean isRunning = false;
    public static boolean isGoogleApiConnected = false;
    public boolean isModeActive = false;

    private long lastLocationTime = System.currentTimeMillis();

    private EventVerifierThread eventVerifierThread;
    private LocationRequestThread locationRequestThread;
    private RecalculateDistanceThread reCalcDistance;
    private Database db;

    private LocationManager locationManager;
    private SharedLocationListener sharedLocationListener;

    private List<Fence> fenceListActive;
    private Location location;

    private boolean isSimulationRunning = false;

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
                    if (SharedPrefs.getLocationRequestInterval() > 15) {
                        locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                        FileLogger.e(TAG, "Changing request interval to 15 seconds");
                        SharedPrefs.setLocationRequestInterval(15);
                        SharedPrefs.setIsMoving(true);
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "slowMovement": {
                    if (SharedPrefs.getLocationRequestInterval() > 60) {
                        locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                        FileLogger.e(TAG, "Changing request interval to 60 seconds");
                        SharedPrefs.setLocationRequestInterval(60);
                        SharedPrefs.setIsMoving(true);
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "noMovement": {
                    if (SharedPrefs.getLocationRequestInterval() != 900 && !isSimulationRunning) {
                        locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                        FileLogger.e(TAG, "Changing request interval to 900 seconds");
                        SharedPrefs.setLocationRequestInterval(900);
                        SharedPrefs.setIsMoving(false);
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "runEventVerifier": {
                    runEventVerifier();
                    break;
                }
                case "updateFenceListActive": {
                    if (db == null)
                        db = new Database(context);
                    fenceListActive = db.onDeviceFence("getActive");
                    break;
                }
                case "calcDistance": {
                    if (reCalcDistance == null || (reCalcDistance.getState() == Thread.State.TERMINATED && !reCalcDistance.isAlive())) {
                        reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);
                        reCalcDistance.delegate = LocationService.this;
                        reCalcDistance.start();
                    }
                    break;
                }
                case "getFenceListActive": {
                    if (delegate != null)
                        delegate.activeFenceList(fenceListActive);
                    break;
                }
                case "simulationStopped": {
                    isSimulationRunning = false;
                    switchToPassiveMode();
                    break;
                }
                case "simulationStarted": {
                    isSimulationRunning = true;
                    stopLocationUpdates();
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

    public static LocationService getServiceObject() {return self;}

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        createLocRequestHandler();
        createServerUpdateHandler();

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        sharedLocationListener = new SharedLocationListener(TAG);
        eventVerifierThread = new EventVerifierThread(getApplicationContext());

        // Initialize thread for first run
        // Fence list will be null
        locationRequestThread = new LocationRequestThread(getApplicationContext(), fenceListActive, locationRequestHandler, locationRequest);

        db = new Database(getApplicationContext());

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Service Started");

        lastLocationTime = SharedPrefs.getLocLastUpdateMillis();
        isRunning = true;
        self = this;

        if (delegate != null)
            delegate.serviceStarted();
        if (SharedPrefs.pref == null)
            new SharedPrefs(getApplicationContext()).initialize();


        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(ActivityRecognition.API)
                    .build();

            // Connect to Google client
            mGoogleApiClient.connect();
        }

        // Request current location
        locationRequestHandler.post(locationRequest);

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
        //switchToPassiveMode();

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
        sharedLocationListener.delegate = this;
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, sharedLocationListener);
        isModeActive = false;
    }

    protected void stopLocationUpdates() {
        FileLogger.e(TAG, "Stopping listener");
        locationManager.removeUpdates(sharedLocationListener);
    }

    public void switchToPassiveMode() {
        if (!isSimulationRunning) {
            FileLogger.e(TAG, "Starting listener");
            createPassiveLocationRequest();
        }else
            FileLogger.e(TAG,"Simulation is running cant start listener");
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {}

    @Override
    public void lmLocation(Location location, int locationScore) {
        this.location = location;
        if (SharedPrefs.pref == null)
            new SharedPrefs(getApplicationContext()).initialize();

        Location lastLocation = new Location("");
        lastLocation.setLatitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLatitude()));
        lastLocation.setLongitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLongitude()));

        double displacement = DistanceCalculator.calcDistanceFromLocation(location, lastLocation);

        FileLogger.e(TAG, "Displacement since last recalculation: " + String.valueOf(displacement));

        if (displacement >= SharedPrefs.getDistanceThreshold()) {
            if (reCalcDistance == null || (reCalcDistance.getState() == Thread.State.TERMINATED && !reCalcDistance.isAlive()))
                reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);

            reCalcDistance.delegate = this;
            reCalcDistance.start();

        }
        else {
            fenceListActive = DistanceCalculator.updateDistanceFromFences(getApplicationContext(), location, fenceListActive, false);
        }
        lastLocationTime = System.currentTimeMillis();
        SharedPrefs.setLocLastUpdateMillis(lastLocationTime);

        String locationTime = String.valueOf(DateFormat.getTimeInstance().format(location.getTime()));
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


        runEventVerifier();
    }


    @Override
    public void lmRemoveUpdates() {}

    @Override
    public void lmRemoveTimeoutHandler() {}


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

        locationRequestHandler = new Handler();
        locationRequest = new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.pref == null)
                    new SharedPrefs(getApplicationContext()).initialize();

                long lastLocElapsedTime = (System.currentTimeMillis() - lastLocationTime) / 1000; // In Seconds


                FileLogger.e(TAG, "Checking last update");

                if (locationRequestThread.getState() == Thread.State.NEW) {
                    FileLogger.e(TAG, "Location request first run");
                    locationRequestThread.delegate = LocationService.this;
                    locationRequestThread.start();
                    isModeActive = true;
                    //locationRequestHandler.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }
                else if (isRunning && isGoogleApiConnected && locationRequestThread.getState() == Thread.State.TERMINATED) {
                    FileLogger.e(TAG, "Requesting location update");
                    FileLogger.e(TAG, "Next check after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                    stopLocationUpdates();
                    locationRequestThread = new LocationRequestThread(getApplicationContext(), fenceListActive, locationRequestHandler, locationRequest);
                    locationRequestThread.delegate = LocationService.this;
                    locationRequestThread.start();
                    isModeActive = true;
                    //locationRequestHandler.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }
                else if (!isRunning) {
                    FileLogger.e(TAG, "Service not running. Stopping self");
                    locationRequestHandler.removeCallbacksAndMessages(null);
                }
               /* else {
                    FileLogger.e(TAG, "Update found. Next check after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
                    locationRequestHandler.postDelayed(this, SharedPrefs.getLocationRequestInterval() * 1000);
                }*/
            }
        };
    }


    public void runEventVerifier() {
        if (eventVerifierThread.getState() == Thread.State.NEW) {
            FileLogger.e(TAG, "Event verifier first run");
            eventVerifierThread.start();
        }
        else if (eventVerifierThread.getState() == Thread.State.TERMINATED && SharedPrefs.getPendingEventCount() > 0) { //&& (System.currentTimeMillis() - lastEventVerifiedTime > 5000)) {
            FileLogger.e(TAG, "Pending events: " + String.valueOf(SharedPrefs.getPendingEventCount()));
            FileLogger.e(TAG, "Starting event verifier");
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
    public void activeFenceList(List<Fence> fenceListActive) {
        this.fenceListActive = fenceListActive;
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {

    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        isGoogleApiConnected = false;
        isRunning = false;
        isModeActive = false;

        if (locationRequestHandler != null)
            locationRequestHandler.removeCallbacksAndMessages(null);


        Log.e(TAG, "Stopping listener");
        locationManager.removeUpdates(sharedLocationListener);
        locationManager = null;

        if (delegate != null)
            delegate.serviceStopped();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.e(TAG, "Service Destroyed");
        super.onDestroy();
    }


}
