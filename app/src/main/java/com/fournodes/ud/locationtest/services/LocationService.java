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
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.FenceListInterface;
import com.fournodes.ud.locationtest.interfaces.LocationUpdateListener;
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
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationUpdateListener, FenceListInterface {
    private static final String TAG = "Location Service";

    private static LocationService self = null;
    public ServiceMessage delegate;
    public static GoogleApiClient mGoogleApiClient;

    private Handler locationRequestHandler;
    private Runnable locationRequest;

    public static boolean isServiceRunning = false;
    public static boolean isGoogleApiConnected = false;
    public boolean isLocationRequestRunning = false;
    private boolean isVerifierRunning = false;

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
                        SharedPrefs.setIsMoving(true);
                        FileLogger.e(TAG,"Starting force location request");
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "slowMovement": {
                    if (SharedPrefs.getLocationRequestInterval() > 60) {
                        locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                        SharedPrefs.setIsMoving(true);
                        FileLogger.e(TAG,"Starting force location request");
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "noMovement": {
                    if (SharedPrefs.getLocationRequestInterval() != 900 ){// && !isSimulationRunning) {
                        locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                        SharedPrefs.setIsMoving(false);
                        FileLogger.e(TAG,"Starting force location request");
                        locationRequestHandler.post(locationRequest);
                    }
                    break;
                }
                case "runEventVerifier": {
                    runEventVerifier();
                    break;
                }
                case "updateFenceListActive": {
                    fenceListActive = db.onDeviceFence("getActive");
                    break;
                }
                case "calcDistance": {
                    if (location == null) {
                        location = new Location("");
                        location.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
                        location.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
                    }
                    if (reCalcDistance == null || (reCalcDistance.getState() == Thread.State.TERMINATED && !reCalcDistance.isAlive())) {
                        reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);
                        reCalcDistance.delegate = LocationService.this;
                        reCalcDistance.start();
                    }
                    break;
                }
                case "getFenceListActive": {
                    if (delegate != null)
                        delegate.activeFenceList(fenceListActive, TAG);
                    break;
                }
                case "simulationStopped": {
                    isSimulationRunning = false;
                    break;
                }
                case "simulationStarted": {
                    isSimulationRunning = true;
                    locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                    SharedPrefs.setIsMoving(true);
                    locationRequestHandler.post(locationRequest);
                    break;
                }
                case "verificationComplete": {
                    isVerifierRunning = false;
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

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        sharedLocationListener = new SharedLocationListener(TAG);

        db = new Database(getApplicationContext());

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Service Started");

        lastLocationTime = SharedPrefs.getLocLastUpdateMillis();
        isServiceRunning = true;
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
        isLocationRequestRunning = false;
    }

    protected void stopLocationUpdates() {
        FileLogger.e(TAG, "Stopping listener");
        locationManager.removeUpdates(sharedLocationListener);
    }

    public void switchToPassiveMode() {
        FileLogger.e(TAG, "Starting listener");
        createPassiveLocationRequest();
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {}

    @Override
    public void lmLocation(Location location, int locationScore) {
        // Send location to MapFragment for simulation
        if (delegate != null)
            delegate.listenerLocation(location);

        Location lastLocation = new Location("");
        lastLocation.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
        lastLocation.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));

        if (DistanceCalculator.calcDistanceFromLocation(location, lastLocation) >= 100) {
            this.location = location;

            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();

            // Get the last recalculation script run location
            Location lastReCalcLocation = new Location("");
            lastReCalcLocation.setLatitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLatitude()));
            lastReCalcLocation.setLongitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLongitude()));

            // Calculate displacement from recalculation script run location to current location
            int displacement = DistanceCalculator.calcDistanceFromLocation(location, lastReCalcLocation);
            FileLogger.e(TAG, "Displacement since last recalculation: " + String.valueOf(displacement));

            // If displacement is above or equal to specified value, run recalculation script at this location
            if (displacement >= SharedPrefs.getDistanceThreshold()) {
                if (reCalcDistance == null || (reCalcDistance.getState() == Thread.State.TERMINATED && !reCalcDistance.isAlive()))
                    reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);

                reCalcDistance.delegate = this;
                reCalcDistance.start();

            }
            // Otherwise just update the distances of all the ACTIVE fences in memory
            else
                activeFenceList(DistanceCalculator.updateDistanceFromFences(getApplicationContext(), location, fenceListActive, false), TAG);

            // Save the location with time schedule server updater
            lastLocationTime = System.currentTimeMillis();
            SharedPrefs.setLocLastUpdateMillis(lastLocationTime);

            String locationTime = String.valueOf(DateFormat.getTimeInstance().format(location.getTime()));
            db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
            //Save in shared prefs after saving in db
            SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
            SharedPrefs.setLastLocUpdateTime(locationTime);
            SharedPrefs.setLastLocationAccuracy(location.getAccuracy());
            SharedPrefs.setLastLocationProvider(location.getProvider());

            if (delegate != null)
                delegate.locationUpdated(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), locationTime);

        }

    }


    @Override
    public void lmRemoveUpdates() {}

    @Override
    public void lmRemoveTimeoutHandler() {}


    public void createLocRequestHandler() {
        FileLogger.e(TAG, "Force request handler created using interval of " + SharedPrefs.getLocationRequestInterval() + " seconds");

        locationRequestHandler = new Handler();
        locationRequest = new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.pref == null)
                    new SharedPrefs(getApplicationContext()).initialize();


                FileLogger.e(TAG, "Checking last update");

                if (isServiceRunning && !isLocationRequestRunning) {
                    stopLocationUpdates();
                    FileLogger.e(TAG, "Requesting location update");
                    locationRequestThread = new LocationRequestThread(getApplicationContext(), fenceListActive, locationRequestHandler, locationRequest);
                    locationRequestThread.delegate = LocationService.this;
                    locationRequestThread.start();
                    isLocationRequestRunning = true;
                }
                else if (isServiceRunning && isLocationRequestRunning)
                    FileLogger.e(TAG, "Location request already running");
                else
                    FileLogger.e(TAG, "Service not running, terminating");

            }
        };
    }


    public void runEventVerifier() {

        if (isServiceRunning && !isVerifierRunning) {
            FileLogger.e(TAG, "Pending events: " + String.valueOf(db.getAllPendingEvents().size()));
            FileLogger.e(TAG, "Starting event verifier");
            eventVerifierThread = new EventVerifierThread(getApplicationContext());
            isVerifierRunning = true;
            eventVerifierThread.start();
        }
        else if (isServiceRunning && isVerifierRunning)
            FileLogger.e(TAG, "Event verifier is already running");
        else
            FileLogger.e(TAG, "Service not running, terminating");

    }

    @Override
    public void activeFenceList(List<Fence> fenceListActive, String className) {
        if (!className.equals("RequestLocUpdateThread")) {
            if (fenceListActive.size() > 0) {
                if (fenceListActive.size() > 1) {
                    FileLogger.e(TAG, "Multiple active fences.");
                    // Sort the fences in ascending order of their distances
                    Collections.sort(fenceListActive);
                }
                else
                    FileLogger.e(TAG, "Single active fence.");


                if ((locationRequestTimeLeft() > 0.5 || locationRequestTimeLeft() < 0.0) && SharedPrefs.isMoving()) {
                    locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                    int timeInSec = timeToFenceEdge(fenceListActive.get(0));
                    SharedPrefs.setLocationRequestInterval(timeInSec);
                    SharedPrefs.setLocationRequestAt(System.currentTimeMillis() + (timeInSec * 1000));
                    locationRequestHandler.postDelayed(locationRequest, SharedPrefs.getLocationRequestInterval() * 1000);
                }

                FileLogger.e(TAG, "Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");


            }
            else {
                // No active fences

                int timeInSec;
                if (location == null || location.getSpeed() == 0f)
                    timeInSec = 60;
                else {
                    timeInSec = (int) Math.ceil(SharedPrefs.getDistanceThreshold() / location.getSpeed());
                    if (timeInSec > 60)
                        timeInSec = 60;
                }
                if ((locationRequestTimeLeft() > 0.5 || locationRequestTimeLeft() < 0.0) && SharedPrefs.isMoving()) {
                    locationRequestHandler.removeCallbacksAndMessages(locationRequest);
                    SharedPrefs.setLocationRequestInterval(timeInSec);
                    SharedPrefs.setLocationRequestAt(System.currentTimeMillis() + (timeInSec * 1000));
                    locationRequestHandler.postDelayed(locationRequest, SharedPrefs.getLocationRequestInterval() * 1000);
                }
                FileLogger.e(TAG, "No active fences. Next run after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");

            }
        }
        this.fenceListActive = fenceListActive;
    }

    public float locationRequestTimeLeft() {
        long scheduledTime = SharedPrefs.getLocationRequestAt();
        int scheduledIntervalInMillis = SharedPrefs.getLocationRequestInterval() * 1000;
        long timeLeftInMillis = scheduledTime - System.currentTimeMillis();
        float timeLeftValue = (float) timeLeftInMillis / scheduledIntervalInMillis;
        FileLogger.e(TAG, "Next run after (ms): " + String.valueOf(timeLeftInMillis));
        FileLogger.e(TAG, "Next run after value: " + String.valueOf(timeLeftValue));
        return timeLeftValue;
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {}

    // Time is in seconds
    private int timeToFenceEdge(Fence fence) {
        if (location == null || location.getSpeed() < 3.0)
            return 5;

        int distanceFromCenter = fence.getDistanceFrom();
        int radius = fence.getRadius();
        int fencePerimeterInMeters = (int) ((float) SharedPrefs.getFencePerimeterPercentage() / 100) * radius;
        int distanceFromEdge = distanceFromCenter - (radius + fencePerimeterInMeters);
        int timeInSec;

        // Enter distance
        if (distanceFromEdge > 0) {

            timeInSec = (int) Math.ceil(distanceFromEdge / location.getSpeed());
            FileLogger.e(TAG, "Time to enter fence: " + String.valueOf(timeInSec));
            FileLogger.e(TAG, "Current speed: " + String.valueOf(location.getSpeed()));

            if (timeInSec < 5) {
                Intent triggerFence = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
                triggerFence.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
                triggerFence.putExtra("id", fence.getId());
                getApplicationContext().startService(triggerFence);
                return 5;

            }
            return timeInSec;
        }
        // Exit distance
        else if (distanceFromEdge < 0) {
            // Convert to positive int
            timeInSec = (int) Math.ceil((distanceFromEdge * -1) / location.getSpeed());
            FileLogger.e(TAG, "Time to exit fence: " + String.valueOf(timeInSec));
            FileLogger.e(TAG, "Current speed: " + String.valueOf(location.getSpeed()));

            if (timeInSec < 5)
                return 5;
            else
                return timeInSec;
        }
        // You are at the center of the fence
        else return 5;
    }


    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        isGoogleApiConnected = false;
        isServiceRunning = false;
        isLocationRequestRunning = false;


        locationRequestHandler.removeCallbacksAndMessages(locationRequest);

        Log.e(TAG, "Stopping listener");
        locationManager.removeUpdates(sharedLocationListener);
        locationManager = null;

        if (delegate != null)
            delegate.serviceStopped();

        FileLogger.closeFile();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.e(TAG, "Service Destroyed");
        super.onDestroy();
    }


}
