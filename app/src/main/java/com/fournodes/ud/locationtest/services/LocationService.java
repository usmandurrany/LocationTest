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
import com.fournodes.ud.locationtest.utils.RequestLocationHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;

import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationUpdateListener, FenceListInterface {
    private static final String TAG = "Location Service";

    private static LocationService self = null;
    public ServiceMessage delegate;
    public static GoogleApiClient mGoogleApiClient;
    public static boolean isServiceRunning = false;
    public static boolean isGoogleApiConnected = false;
    public boolean isLocationRequestRunning = false;
    private boolean isVerifierRunning = false;
    private EventVerifierThread eventVerifierThread;
    private LocationRequestThread locationRequestThread;
    private RecalculateDistanceThread reCalcDistance;
    private Database db;
    private LocationManager locationManager;
    private SharedLocationListener mainLocationListener;
    private List<Fence> fenceListActive;
    private Location location;
    private boolean isSimulationRunning = false;
    private Handler liveLocationUpdate;
    private Runnable locationUpdate;
    private SharedLocationListener liveLocationListener;
    private RequestLocationHandler requestLocationHandler;
    public RequestLocationRunnable requestLocationRunnable;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //FileLogger.e(TAG, "Broadcast Received");
            //FileLogger.e(TAG, "Command received: " + intent.getStringExtra("message"));
            switch (intent.getStringExtra("message")) {
                case "switchToPassiveMode":
                    isLocationRequestRunning = false;
                    break;

                case "locationRequestThreadFailed":
                    isLocationRequestRunning = false;
                    requestLocationRunnable.setValues(TAG);
                    requestLocationHandler.run(requestLocationRunnable);
                    break;

                case "GCMReceiver":
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("body"));
                    break;

                case "fastMovement":
                case "slowMovement":
                    SharedPrefs.setIsMoving(true);
                    if (SharedPrefs.getLocationRequestInterval() > 5) {
                        FileLogger.e(TAG, "Movement detected, interval changed to 5 seconds");
                        SharedPrefs.setLocationRequestInterval(5);
                        requestLocationRunnable.setValues("DetectedActivitiesIS");
                        requestLocationHandler.clearQueue(requestLocationRunnable);
                        requestLocationHandler.runAfterSeconds(requestLocationRunnable, 5);
                    }
                    break;

                case "noMovement":
                    SharedPrefs.setIsMoving(false);
                    if (SharedPrefs.getLocationRequestInterval() != 900) { //&& !isSimulationRunning) {
                        FileLogger.e(TAG, "No movement detected, interval increased to 900 seconds");
                        SharedPrefs.setLocationRequestInterval(900);
                        requestLocationRunnable.setValues("DetectedActivitiesIS");
                        requestLocationHandler.clearQueue(requestLocationRunnable);
                        requestLocationHandler.runAfterSeconds(requestLocationRunnable, 900);
                    }
                    break;

                case "runEventVerifier":
                    runEventVerifier();
                    break;

                case "updateFenceListActive":
                    fenceListActive = db.onDeviceFence("getActive");
                    break;

                case "calcDistance":
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

                case "getFenceListActive":
                    if (delegate != null)
                        delegate.activeFenceList(fenceListActive, TAG);
                    break;

                case "simulationStopped":
                    isSimulationRunning = false;
                    break;

                case "simulationStarted":
                    isSimulationRunning = true;

                   /* SharedPrefs.setIsMoving(true);
                    requestLocationRunnable.setValues(TAG);
                    requestLocationHandler.clearQueue(requestLocationRunnable);
                    requestLocationHandler.run(requestLocationRunnable);*/

                    break;

                case "verificationComplete":
                    isVerifierRunning = false;
                    break;

                case "isLive":
                    if (liveLocationUpdate != null)
                        liveLocationUpdate.post(locationUpdate);
                    else {
                        liveLocationUpdateHandler();
                        liveLocationUpdate.post(locationUpdate);
                    }
                    break;

                default:
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("message"));
                    break;

            }
        }
    };

    public static LocationService getServiceObject() {return self;}

    @Override
    public void onCreate() {
        super.onCreate();
        FileLogger.e(TAG, "Initializing");
        // Initialize fabric
        Fabric.with(this, new Crashlytics());

        // Reset shared prefs
        if (SharedPrefs.pref == null)
            new SharedPrefs(getApplicationContext()).initialize();
        SharedPrefs.setLocationRequestRunnableId(0);

        // Initialize location manager
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        // Initialize location request handler
        //createLocationRequestRunnable();

        // Initialize live location check handler
        liveLocationUpdateHandler();

        // Initialize passive location listener
        createPassiveLocationRequest();

        // Initialize database
        db = new Database(getApplicationContext());

        // Initialize location request handler
        requestLocationHandler = new RequestLocationHandler();

        requestLocationRunnable = new RequestLocationRunnable();

        // Initialize local broadcast manager
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("LOCATION_TEST_SERVICE"));

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Service Started");

        isServiceRunning = true;
        self = this;

        // Signal delegate that service has been started
        if (delegate != null) {
            delegate.serviceStarted();
        }


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
        requestLocationRunnable.setValues(TAG);
        requestLocationHandler.run(requestLocationRunnable);

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
        FileLogger.e(TAG, "Starting listener");
        mainLocationListener = new SharedLocationListener(TAG);
        mainLocationListener.delegate = this;
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mainLocationListener);
    }


    @Override
    public void lmBestLocation(Location bestLocation, int locationScore) {}

    @Override
    public void lmLocation(Location location, int locationScore) {

        // Initialize shared prefs if null
        if (SharedPrefs.pref == null) {
            new SharedPrefs(getApplicationContext()).initialize();
        }

        // Create location of coordinates from last 100m ago
        Location last100mLocation = new Location("");
        last100mLocation.setLatitude(Double.parseDouble(SharedPrefs.getLast100mLatitude()));
        last100mLocation.setLongitude(Double.parseDouble(SharedPrefs.getLast100mLongitude()));

        db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
        SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
        SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
        SharedPrefs.setLastLocUpdateTime(String.valueOf(location.getTime()));
        SharedPrefs.setLastLocationAccuracy(location.getAccuracy());
        SharedPrefs.setLastLocationProvider(location.getProvider());


        if (DistanceCalculator.calcDistanceFromLocation(location, last100mLocation) >= 100 || isLocationRequestRunning) {
            this.location = location;

            // Replace last 100m coordinates with this location
            SharedPrefs.setLast100mLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLast100mLongitude(String.valueOf(location.getLongitude()));


            if (SharedPrefs.getReCalcDistanceAtLatitude() != null && SharedPrefs.getLastDeviceLatitude() != null && fenceListActive != null) {


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

            }
            else {
                SharedPrefs.setLast100mLatitude(String.valueOf(location.getLatitude()));
                SharedPrefs.setLast100mLongitude(String.valueOf(location.getLongitude()));

                RecalculateDistanceThread reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);
                reCalcDistance.delegate = this;
                reCalcDistance.start();
            }
        }

        // Send location to MapFragment for simulation
        if (delegate != null) {
            delegate.listenerLocation(location);
        }

    }


    @Override
    public void lmRemoveUpdates() {}

    @Override
    public void lmRemoveTimeoutHandler() {}

    //TODO: Make event verifier return list of active fences through delegate like location request thread
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
        this.fenceListActive = fenceListActive;
        int timeInSec;

        if (fenceListActive.size() > 0) {
            if (fenceListActive.size() > 1) {
                FileLogger.e(TAG, "Multiple active fences.");
                // Sort the fences in ascending order of their distances
                Collections.sort(fenceListActive);
            }
            else
                FileLogger.e(TAG, "Single active fence.");

            timeInSec = timeToFenceEdge(fenceListActive.get(0));

        }
        else {
            // No active fences
            FileLogger.e(TAG, "No active fences.");

            if (location == null || location.getSpeed() == 0f)
                timeInSec = 60;
            else {
                timeInSec = (int) Math.ceil(SharedPrefs.getDistanceThreshold() / location.getSpeed());
                if (timeInSec > 60)
                    timeInSec = 60;
            }

        }

        if (SharedPrefs.isMoving()) {
            SharedPrefs.setLocationRequestInterval(timeInSec);
            SharedPrefs.setLocationRequestAt(System.currentTimeMillis() + (timeInSec * 1000));

            requestLocationRunnable.setValues(TAG);
            requestLocationHandler.clearQueue(requestLocationRunnable);
            requestLocationHandler.runAfterSeconds(requestLocationRunnable, timeInSec);

            FileLogger.e(TAG, "Scheduling force request run after " + String.valueOf(timeInSec) + " seconds");

        }
        else if (!SharedPrefs.isMoving() && SharedPrefs.getLocationRequestInterval() < 900) {
            SharedPrefs.setLocationRequestInterval(900);
            SharedPrefs.setLocationRequestAt(System.currentTimeMillis() + (900 * 1000));

            requestLocationRunnable.setValues(TAG);
            requestLocationHandler.clearQueue(requestLocationRunnable);
            requestLocationHandler.runAfterSeconds(requestLocationRunnable, 900);

            FileLogger.e(TAG, "Scheduling force request run after " + String.valueOf(900) + " seconds");
        }
    }

    public float locationRequestTimeLeft() {
        long scheduledTime = SharedPrefs.getLocationRequestAt() + 2000; // 2 Seconds to account for any delays
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

        int distanceFromCenter = fence.getDistanceFromUser();
        int radius = (int) fence.getRadius();
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
                triggerFence.putExtra("id", fence.getFenceId());
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

    public void liveLocationUpdateHandler() {
        liveLocationListener = new SharedLocationListener(TAG);
        liveLocationListener.delegate = new LocationUpdateListener() {
            @Override
            public void lmBestLocation(Location bestLocation, int locationScore) {}

            @Override
            public void lmLocation(Location location, int locationScore) {
                locationManager.removeUpdates(liveLocationListener);
                FileLogger.e("LiveLocationCheck", "Location received");

                if (SharedPrefs.isLive()) {
                    FileLogger.e("LiveLocationCheck", "Will check again after 5 seconds");
                    liveLocationUpdate.postDelayed(locationUpdate, 5000);
                }
                else
                    FileLogger.e("LiveLocationCheck", "Live session ended, terminating");


            }

            @Override
            public void lmRemoveUpdates() {}

            @Override
            public void lmRemoveTimeoutHandler() {}
        };

        liveLocationUpdate = new Handler();
        locationUpdate = new Runnable() {
            @Override
            public void run() {
                FileLogger.e("LiveLocationCheck", "Live location checker running");
                long lastLocationTime = Long.parseLong(SharedPrefs.getLastLocUpdateTime());
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLocationTime > 5000) {
                    FileLogger.e("LiveLocationCheck", "Last location is stale, requesting new one");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, liveLocationListener);
                }
                else {
                    FileLogger.e("LiveLocationCheck", "Sending last location. Will check again after 5 seconds");
                    if (SharedPrefs.isLive()) {
                        liveLocationUpdate.postDelayed(locationUpdate, 5000);
                    }
                    else
                        FileLogger.e("LiveLocationCheck", "Live session ended, terminating");
                }


            }
        };
    }


    class RequestLocationRunnable implements Runnable {
        private static final String TAG = "RequestLocationRunnable";
        private int id;
        private String scheduledBy;
        private long schedulingTime;

        public void setValues(String scheduledBy) {

            // Initialize shared prefs if null
            if (SharedPrefs.pref == null) {
                new SharedPrefs(getApplicationContext()).initialize();
            }


            id = SharedPrefs.getLocationRequestRunnableId() + 1;
            SharedPrefs.setLocationRequestRunnableId(id);

            FileLogger.e(TAG, "Scheduled Id: " + String.valueOf(id));
            FileLogger.e(TAG, "Scheduled by: " + scheduledBy);
            FileLogger.e(TAG, "Scheduled after: " + SharedPrefs.getLocationRequestInterval() + " seconds");

            schedulingTime = System.currentTimeMillis();
            this.scheduledBy = scheduledBy;
        }


        @Override
        public void run() {

            // Initialize shared prefs if null
            if (SharedPrefs.pref == null) {
                new SharedPrefs(getApplicationContext()).initialize();
            }

            FileLogger.e(TAG, "Running Id: " + String.valueOf(id));
            FileLogger.e(TAG, "Ran by: " + scheduledBy);
            FileLogger.e(TAG, "Ran after: " + String.valueOf((System.currentTimeMillis() - schedulingTime) / 1000) + " seconds");

            if (isServiceRunning && !isLocationRequestRunning) {
                locationRequestThread = new LocationRequestThread(getApplicationContext());
                // To get list of active fences
                locationRequestThread.delegate = LocationService.this;
                locationRequestThread.start();
                // Flag to prevent passive listener from interfering
                isLocationRequestRunning = true;
            }
            else if (isServiceRunning && isLocationRequestRunning)
                FileLogger.e(TAG, "Location request already running");
            else
                FileLogger.e(TAG, "Service not running, terminating");


        }


    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        isGoogleApiConnected = false;
        isServiceRunning = false;
        isLocationRequestRunning = false;


        requestLocationHandler.clearQueue(requestLocationRunnable);

        Log.e(TAG, "Stopping listener");
        locationManager.removeUpdates(mainLocationListener);
        locationManager = null;

        if (delegate != null)
            delegate.serviceStopped();

        FileLogger.closeFile();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.e(TAG, "Service Destroyed");
        super.onDestroy();
    }


}
