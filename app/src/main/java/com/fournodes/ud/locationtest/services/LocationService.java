package com.fournodes.ud.locationtest.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
    private RecalculateDistanceThread reCalcDistance;
    private Database db;
    private LocationManager locationManager;
    private SharedLocationListener mainLocationListener;
    public List<Fence> fenceListActive;
    private boolean isSimulationRunning = false;
    private Handler liveLocationUpdate;
    private Runnable locationUpdate;
    private Runnable liveUpdateTimeout;
    private SharedLocationListener liveLocationListener;
    private RequestLocationHandler requestLocationHandler;
    private RequestLocationRunnable requestLocationRunnable;
    private Queue<Float> speedList;
    private float avgSpeed = 0;
    private PowerManager.WakeLock wakeLock;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private int inFenceCoordinateCount = 0;
    private boolean isInsideFence = false;
    private boolean nPointsActivityDetection = false;
    private PendingIntent npActivityDetectionPI;
    private int timeInSec;
/*    private int expoLocReqCount = 0;
    private int expoTimeInSec = 5;*/


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FileLogger.e(TAG, "Command received: " + intent.getStringExtra("message"));
            switch (intent.getStringExtra("message")) {

                case "locationRequestSuccess":
                    isLocationRequestRunning = false;
                    break;

                case "locationRequestFailed":
                    isLocationRequestRunning = false;
                    SharedPrefs.setLocationRequestInterval(5);
                    requestLocationRunnable.setValues(TAG);
                    requestLocationHandler.runAfterSeconds(requestLocationRunnable, 5);
                    break;

                case "GCMReceiver":
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("body"));
                    break;

                case "fastMovement":
                case "slowMovement":
                    //long delay = SharedPrefs.getLocationRequestInterval() * 1000;
                    //if (((requestLocationRunnable.schedulingTime + delay) - System.currentTimeMillis() > 5000)
                    if (SharedPrefs.getLocationRequestInterval() > 5) {
                        if (nPointsActivityDetection) {
                            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, npActivityDetectionPI);
                            FileLogger.e(TAG, "N points activity detection complete");

                        }
                        FileLogger.e("DetectedActivitiesIS", "Movement detected, interval changed to 5 seconds");
                        SharedPrefs.setIsMoving(true);
                        SharedPrefs.setLocationRequestInterval(5);
                        requestLocationHandler.clearQueue(requestLocationRunnable);
                        requestLocationRunnable.setValues("DetectedActivitiesIS");
                        requestLocationHandler.runAfterSeconds(requestLocationRunnable, 5);
                    }
                    break;


                case "noMovement":
                    if (SharedPrefs.getLocationRequestInterval() != 900 && SharedPrefs.isMoving()){//&& !isSimulationRunning) {
                        if (nPointsActivityDetection) {
                            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, npActivityDetectionPI);
                            FileLogger.e(TAG, "N points activity detection complete");
                        }
                        SharedPrefs.setIsMoving(false);
                        FileLogger.e(TAG, "No movement detected, location request stopped");
                        //FileLogger.e("DetectedActivitiesIS", "No movement detected, interval increased to 900 seconds");
                        SharedPrefs.setLocationRequestInterval(900);
                        requestLocationHandler.clearQueue(requestLocationRunnable);
                        //requestLocationRunnable.setValues("DetectedActivitiesIS");
                        //requestLocationHandler.runAfterSeconds(requestLocationRunnable, 900);

                        avgSpeed = 0;
                    }
                    break;

                case "runEventVerifier":
                    runEventVerifier();
                    break;

                case "calcDistance":
                    Location location = new Location("");
                    location.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
                    location.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));

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
                case "quit":
                    stopSelf();
                    break;

                case "startLocationRequestRunnable":
                    requestLocationRunnable.run();
                    break;
                default:
                    if (delegate != null)
                        delegate.fenceTriggered(intent.getStringExtra("message"));
                    break;

            }
        }
    };

    private void runEventVerifier() {

        if (isServiceRunning && !isVerifierRunning) {
            FileLogger.e(TAG, "Pending events: " + String.valueOf(db.getAllPendingEvents().size()));
            FileLogger.e(TAG, "Starting event verifier");
            EventVerifierThread eventVerifierThread = new EventVerifierThread(getApplicationContext(), fenceListActive);
            eventVerifierThread.delegate = LocationService.this;
            isVerifierRunning = true;
            eventVerifierThread.start();
        }
        else if (isServiceRunning && isVerifierRunning)
            FileLogger.e(TAG, "Event verifier is already running");
        else
            FileLogger.e(TAG, "Service not running, terminating");

    }

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

        // Initialize live location check handler
        liveLocationUpdateHandler();

        // Initialize passive location listener
        createPassiveLocationRequest();

        // Initialize database
        db = new Database(getApplicationContext());

        // Initialize location request handler
        requestLocationHandler = new RequestLocationHandler();

        // Initialize location request runnable inner class
        requestLocationRunnable = new RequestLocationRunnable();

        // Initialize speed list
        speedList = new LinkedList<>();

        // Initialize power manager
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Initialize wake lock
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // Initialize alarm manager
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Initialize pending intent for alarm
        alarmIntent = PendingIntent.getService(this, 0, new Intent(this, AlarmManagerLocationRequestIS.class), PendingIntent.FLAG_CANCEL_CURRENT);

        // Initialize pending intent for log uploader
        PendingIntent logUploadIntent = PendingIntent.getService(this, 0, new Intent(this, LogUploadIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);

        // Set alarm to upload log

        // Set the alarm to start at approximately 2:00 p.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 5);
        if (calendar.getTime().before(Calendar.getInstance().getTime())) {
            FileLogger.e(TAG, "Upload time has passed scheduling for tomorrow");
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, logUploadIntent);

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

        ActivityRecognition.ActivityRecognitionApi
                .requestActivityUpdates(mGoogleApiClient, Long.MAX_VALUE, getActivityDetectionPendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
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

        //Location sent from listener will have score of 0
        //Location sent from location request thread will have score of 10
        //Perform calculation only if location request thread is not running or if the location score is 10 or above
        if (!isLocationRequestRunning || locationScore >= 10) {

            // Initialize shared prefs if null
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();

            if (location.getSpeed() < 3 && !isInsideFence)
                FileLogger.e(TAG, "Current speed is lower than 3m/s and is discarded");
            else if (location.getSpeed() >= 3 || (location.getSpeed() < 3 && isInsideFence)) {
                if (avgSpeed < location.getSpeed()) {
                    speedList.clear();
                    speedList.add(location.getSpeed());
                }
                else {
                    if (speedList.size() == 10) {
                        speedList.remove();
                        speedList.add(location.getSpeed());
                    }
                    else {
                        speedList.add(location.getSpeed());
                    }
                }

                avgSpeed = calcAverageSpeed(speedList);
            }
            FileLogger.e(TAG, "Current speed: " + String.valueOf(location.getSpeed()));
            FileLogger.e(TAG, "Average speed: " + String.valueOf(avgSpeed));

            db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
            SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
            SharedPrefs.setLastLocUpdateTime(String.valueOf(location.getTime()));
            SharedPrefs.setLastLocationAccuracy(location.getAccuracy());
            SharedPrefs.setLastLocationProvider(location.getProvider());

            // Create location of coordinates from last 100m ago
            Location last100mLocation = new Location("");
            last100mLocation.setLatitude(Double.parseDouble(SharedPrefs.getLast100mLatitude()));
            last100mLocation.setLongitude(Double.parseDouble(SharedPrefs.getLast100mLongitude()));

            int distance = DistanceCalculator.calcDistanceFromLocation(location, last100mLocation);

            if ((distance >= 100 && !isLocationRequestRunning)
                    || locationScore == 10
                    || (fenceListActive != null && fenceListActive.size() > 0 && fenceListActive.get(0).getDistanceFromEdge() > 0 && fenceListActive.get(0).getDistanceFromEdge() < 100)) {

                // Replace last 100m coordinates with this location
                SharedPrefs.setLast100mLatitude(String.valueOf(location.getLatitude()));
                SharedPrefs.setLast100mLongitude(String.valueOf(location.getLongitude()));

                performCalculations(location);

                inFenceCoordinateCount = 0; //reset
            }
            if (distance < 100 && location.getSpeed() < 10.0){//||isInsideFence) {
                inFenceCoordinateCount++;
            if (inFenceCoordinateCount >= 10 && !nPointsActivityDetection) {
                nPointsActivityDetection = true;

                Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
                npActivityDetectionPI = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                ActivityRecognition.ActivityRecognitionApi
                        .requestActivityUpdates(mGoogleApiClient, 1000, npActivityDetectionPI)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess())
                                    FileLogger.e(TAG, "N points activity detection started");
                                else
                                    FileLogger.e(TAG, "N points activity detection failed. " + status.getStatusCode());
                            }
                        });
                inFenceCoordinateCount = 0;
            }
        }
        // Send location to MapFragment for simulation
        if (delegate != null)
            delegate.simulationData(location, avgSpeed, timeInSec, fenceListActive);
    }
}

    public void performCalculations(Location location) {
        if (SharedPrefs.getLastLocationProvider() != null && fenceListActive != null) {

            // Get the last recalculation script run location
            Location lastReCalcLocation = new Location("");
            lastReCalcLocation.setLatitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLatitude()));
            lastReCalcLocation.setLongitude(Double.parseDouble(SharedPrefs.getReCalcDistanceAtLongitude()));

            // Calculate displacement from recalculation script run location to current location
            int displacement = DistanceCalculator.calcDistanceFromLocation(location, lastReCalcLocation);
            FileLogger.e(TAG, "Displacement since last recalculation: " + String.valueOf(displacement));

            // If displacement is above or equal to specified value, run recalculation script at this location
            if (displacement >= SharedPrefs.getDistanceThreshold()) {
                if (reCalcDistance == null || (reCalcDistance.getState() == Thread.State.TERMINATED && !reCalcDistance.isAlive())) {
                    reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);
                    reCalcDistance.delegate = this;
                    reCalcDistance.start();
                }
            }
            // Otherwise just update the distances of all the active fences in memory
            else
                activeFenceList(DistanceCalculator.updateDistanceFromFences(getApplicationContext(), location, fenceListActive, false), TAG);

        }
        else {
            //Consider this to be the first run of the application so calculate distances of all fences
            //Save the current location as the last 100m location
            SharedPrefs.setLast100mLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLast100mLongitude(String.valueOf(location.getLongitude()));

            reCalcDistance = new RecalculateDistanceThread(getApplicationContext(), location);
            reCalcDistance.delegate = this;
            reCalcDistance.start();
        }
    }

    @Override
    public void lmRemoveUpdates() {}

    @Override
    public void lmRemoveTimeoutHandler() {}


    @Override
    public void activeFenceList(List<Fence> fenceListActive, String className) {
        this.fenceListActive = fenceListActive;

        // Don't perform any calculation if fence list is returned from event verifier
        //if (!className.equals("EventVerifierThread")) {
        if (fenceListActive.size() > 0)
            timeInSec = timeToFencePerimeter(fenceListActive.get(0));
        else {
            // No active fences
            timeInSec = (int) (SharedPrefs.getDistanceThreshold() / (avgSpeed == 0 ? 1 : avgSpeed));
            if (timeInSec > 120)
                timeInSec = 120;
        }

        if (SharedPrefs.isMoving()) {
            SharedPrefs.setLocationRequestInterval(timeInSec);
            requestLocationHandler.clearQueue(requestLocationRunnable);
            requestLocationRunnable.setValues(TAG);
            if (timeInSec > 60) {
                FileLogger.e(TAG, "Scheduling using alarm manager");

                alarmManager.cancel(alarmIntent);

                if (Build.VERSION.SDK_INT < 19)
                    alarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + (timeInSec * 1000)), alarmIntent);
                else if (Build.VERSION.SDK_INT >= 19)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + (timeInSec * 1000)), alarmIntent);

            }
            else {
                FileLogger.e(TAG, "Scheduling using handler");
                requestLocationHandler.runAfterSeconds(requestLocationRunnable, timeInSec);
            }
        }
        else if (!SharedPrefs.isMoving()){// && SharedPrefs.getLocationRequestInterval() < 900) {
            FileLogger.e(TAG, "Not moving, location request not scheduled");
            SharedPrefs.setLocationRequestInterval(900);
            /*requestLocationHandler.clearQueue(requestLocationRunnable);
            requestLocationRunnable.setValues(TAG);

            if (Build.VERSION.SDK_INT < 19)
                alarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + (900 * 1000)), alarmIntent);
            else if (Build.VERSION.SDK_INT >= 19)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + (900 * 1000)), alarmIntent);*/
        }

        FileLogger.e(TAG, "Activity: " + (SharedPrefs.isMoving() ? "Moving" : "Still") + ". Next request after " + String.valueOf(SharedPrefs.getLocationRequestInterval()) + " seconds");
        // }
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {}

    private float calcAverageSpeed(Queue<Float> speedList) {
        float sumSpeed = 0;
        for (Float speed : speedList)
            sumSpeed += speed;
        return sumSpeed / speedList.size();
    }

    // Time is in seconds
    private int timeToFencePerimeter(Fence fence) {

        float radius = fence.getRadius();
        float fencePerimeterPercentage = SharedPrefs.getFencePerimeterPercentage();
        float fencePerimeterInMeters = (fencePerimeterPercentage / 100) * radius;
        float distanceFromPerimeter = fence.getDistanceFromEdge() - fencePerimeterInMeters;

        int timeInSec = Math.round(Math.abs(distanceFromPerimeter) / (avgSpeed == 0 ? 1 : avgSpeed));


        // Enter distance
        if (distanceFromPerimeter > 0 || fence.getDistanceFromEdge() > 0) {
            isInsideFence = false;

            if (avgSpeed < 3.0) {
                FileLogger.e(TAG, "Average speed below 3 m/s.");
                return 5;

            }
            else if (timeInSec < 1) {
                timeInSec = 1;
                FileLogger.e(TAG, "Time to enter fence: " + String.valueOf(timeInSec));
                return timeInSec;
            }
            else {
                FileLogger.e(TAG, "Time to enter fence: " + String.valueOf(timeInSec));
                return timeInSec;
            }

        }
        // Exit distance
        else if (fence.getDistanceFromEdge() < 0) {
            isInsideFence = true;

            if (timeInSec > 90)
                timeInSec = 90;
            else if (timeInSec < 5)
                timeInSec = 5;

            FileLogger.e(TAG, "Time to exit fence: " + String.valueOf(timeInSec));
            return timeInSec;
        }
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
                liveLocationUpdate.removeCallbacks(liveUpdateTimeout);
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
        liveUpdateTimeout = new Runnable() {
            @Override
            public void run() {
                FileLogger.e(TAG, "Failed acquire location form GPS. Retry after 5 mins");
                locationManager.removeUpdates(liveLocationListener);
                //liveLocationUpdate.postDelayed(locationUpdate, 300000);

            }
        };
        locationUpdate = new Runnable() {
            @Override
            public void run() {
                FileLogger.e("LiveLocationCheck", "Live location checker running");
                long lastLocationTime = Long.parseLong(SharedPrefs.getLastLocUpdateTime());
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLocationTime > 5000) {
                    FileLogger.e("LiveLocationCheck", "Last location is stale, requesting new one");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, liveLocationListener);
                    liveLocationUpdate.postDelayed(liveUpdateTimeout, 60000);
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
    public long schedulingTime;

    public void setValues(String scheduledBy) {
        if (wakeLock != null && !wakeLock.isHeld() &&
                SharedPrefs.isMoving() && fenceListActive != null && fenceListActive.size() > 0) {
            wakeLock.acquire();
        }

        // Initialize shared prefs if null
        if (SharedPrefs.pref == null) {
            new SharedPrefs(getApplicationContext()).initialize();
        }


        id = SharedPrefs.getLocationRequestRunnableId() + 1;
        SharedPrefs.setLocationRequestRunnableId(id);

            /*FileLogger.e(TAG, "Scheduled Id: " + String.valueOf(id));
            FileLogger.e(TAG, "Scheduled by: " + scheduledBy);
            FileLogger.e(TAG, "Scheduled after: " + SharedPrefs.getLocationRequestInterval() + " seconds");*/

        schedulingTime = System.currentTimeMillis();
        this.scheduledBy = scheduledBy;
    }


    @Override
    public void run() {
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();

        // Initialize shared prefs if null
        if (SharedPrefs.pref == null) {
            new SharedPrefs(getApplicationContext()).initialize();
        }

        FileLogger.e(TAG, "Runnable Id: " + String.valueOf(id));
        FileLogger.e(TAG, "Called by: " + scheduledBy);
        FileLogger.e(TAG, "Scheduled delay: " + SharedPrefs.getLocationRequestInterval() + " seconds");
        FileLogger.e(TAG, "Actual delay: " + String.valueOf((System.currentTimeMillis() - schedulingTime) / 1000) + " seconds");

        if (isServiceRunning && !isLocationRequestRunning) {
            LocationRequestThread locationRequestThread = new LocationRequestThread(getApplicationContext(), self);
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
