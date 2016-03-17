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

import com.fournodes.ud.locationtest.ApiHandler;
import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.FileLog;
import com.fournodes.ud.locationtest.MainActivity;
import com.fournodes.ud.locationtest.Messenger;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.onCompleteListener;
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

import java.text.DateFormat;
import java.util.Date;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public com.fournodes.ud.locationtest.Messenger delegate;

    private static final String TAG = "Location Service";
    public static GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    public static boolean isGoogleApiConnected = false;
    public static NotificationManager mNotificationManager;
    private Runnable update;
    private Handler updateServer;
    private FileLog fileLog;
    private String action;
    public static boolean isRunning=false;



    public LocationService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        fileLog = new FileLog();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest() {
        Log.e(TAG, "Location Request Created");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(SharedPrefs.getLocUpdateInterval());
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(SharedPrefs.getMinDisplacement());

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

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

    protected void startLocationUpdates() {
        createLocationRequest();
        Log.e(TAG,"Polling Started");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        Log.e(TAG,"Polling Stopped");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get an instance of the Notification manager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (intent.getAction() != null) {
            action = intent.getAction();
        }
        if (action != null && action.equals("STOP")) {
            stopSelf();
            mNotificationManager.cancel(0);
        }else {
            Log.e(TAG, "Service Started");
            isRunningNotify();
            mGoogleApiClient.connect();
            isRunning=true;
            if (delegate != null)
                delegate.serviceStarted();
        }
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Override
    public void onDestroy() {
        isGoogleApiConnected = false;
        isRunning=false;
        mGoogleApiClient.disconnect();
        if (delegate!=null)
            delegate.serviceStopped();
        Log.e(TAG, "Service Destroyed");
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
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Google API Connection Suspended");

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location.hasAccuracy() && location.getAccuracy() <= 50){
            if (SharedPrefs.pref == null)
                new SharedPrefs(getApplicationContext()).initialize();
            String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            Database db = new Database(getApplicationContext());
            db.saveLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
            //Save in shared prefs after saving in db
            SharedPrefs.setLastDeviceLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setLastDeviceLongitude(String.valueOf(location.getLongitude()));
            SharedPrefs.setLastLocUpdateTime(mLastUpdateTime);

            fileLog.e(TAG, "Lat: " + String.valueOf(location.getLatitude()) +
                    " Long: " + String.valueOf(location.getLongitude()));
            fileLog.e(TAG, "Accuracy: " + String.valueOf(location.getAccuracy()));
            fileLog.e(TAG, "Location Provider: " + String.valueOf(location.getProvider()));
            if(location.getExtras() != null)
                fileLog.e(TAG, "Satellites: " + location.getExtras().getString("satellites"));
            fileLog.e(TAG, "Location Time: " + String.valueOf(DateFormat.getTimeInstance().format(location.getTime())));
            fileLog.e(TAG, "System Time: " + mLastUpdateTime);

            int rowCount = db.getLocEntriesCount();
            fileLog.e(TAG, "Row Count " + String.valueOf(db.getLocEntriesCount()));


            if (updateServer == null)
                updServerAfterInterval();
            if (rowCount >= 10 && updateServer != null && update != null) {
                updateServer.post(update);
            }
            //updServerOnRowCount();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google API Error " + connectionResult.toString());

    }

    private void isRunningNotify() {
        // Create an explicit content Intent that starts the main Activity.
        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(),0,new Intent(getApplicationContext(), MainActivity.class),0);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        PendingIntent piStopService = PendingIntent.getService(getApplicationContext(),0,new Intent(getApplicationContext(),LocationService.class).setAction("STOP"),0);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle("Location Service")
                .setContentText("Location service is running in the background")
                .setContentIntent(piActivityIntent)
                .addAction(android.R.drawable.ic_dialog_alert,"Stop Service",piStopService);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(false);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
    public void updServerAfterInterval(){
        if (SharedPrefs.isUpdateServerEnabled()){
            updateServer = new Handler();

            update = new Runnable() {
                @Override
                public void run() {
                    ApiHandler apiHandler = new ApiHandler(getApplicationContext());
                    apiHandler.delegate = new onCompleteListener() {
                        @Override
                        public void success() {
                            updateServer.postDelayed(update,SharedPrefs.getUpdateServerInterval());
                            Log.e(TAG,"Update Success");

                        }

                        @Override
                        public void failure() {
                            updateServer.postDelayed(update,SharedPrefs.getUpdateServerInterval());
                            Log.e(TAG,"Update Failed");

                        }
                    };
                    apiHandler.execute(System.currentTimeMillis());
                }
            };
            updateServer.post(update);
        }

    }
    public void updServerOnRowCount(){
        if (SharedPrefs.isUpdateServerEnabled()){
            ApiHandler apiHandler = new ApiHandler(getApplicationContext());
            apiHandler.delegate = new onCompleteListener() {
                @Override
                public void success() {
                    Log.e(TAG,"Update Success On Row Count");
                }

                @Override
                public void failure() {
                    Log.e(TAG,"Update Failed On Row Count");

                }
            };
            apiHandler.execute(System.currentTimeMillis());        }

    }

}
