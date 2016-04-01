package com.fournodes.ud.locationtest.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.GeofenceEvent;
import com.fournodes.ud.locationtest.VerifyGeofenceEvent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.io.File;
import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";
    private Database db;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {super(TAG);}

    @Override
    public void onCreate() {
        super.onCreate();
        db = new Database(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        String requestId = triggeringGeofences.get(0).getRequestId();
        GeofenceEvent event = new GeofenceEvent();
        event.requestId = Integer.parseInt(requestId);
        event.transitionType = geofenceTransition;
        event.isVerified = 0;
        event.retryCount = 0;

        FileLogger.e(TAG, getTransitionType(geofenceTransition)+" event occurred.");

        GeofenceEvent lastVerifiedEvent = db.getLastVerifiedEvent(event.requestId);
        GeofenceEvent lastPendingEvent = db.getLastPendingEvent(event.requestId);

        if (lastVerifiedEvent.id == -1 && lastPendingEvent.id == -1) {
            FileLogger.e(TAG,"Event pending verification");
            db.savePendingEvent(event);
        }else if(lastVerifiedEvent.id == -1 && lastPendingEvent.id != -1
                && lastPendingEvent.transitionType != event.transitionType){
            FileLogger.e(TAG,"Event pending verification");
            db.savePendingEvent(event);
        }else if(lastPendingEvent.id == -1 && lastVerifiedEvent.id != -1
                && lastVerifiedEvent.transitionType != event.transitionType) {
            FileLogger.e(TAG, "Event pending verification");
            db.savePendingEvent(event);
        }else if(lastVerifiedEvent.id != -1 && lastVerifiedEvent.transitionType != event.transitionType
                && lastPendingEvent.id != -1 && lastPendingEvent.transitionType != event.transitionType){
            FileLogger.e(TAG,"Event pending verification");
            db.savePendingEvent(event);
        }else {
            FileLogger.e(TAG,"Event discarded");
        }

        if (!VerifyGeofenceEvent.isRunning) {
            FileLogger.e(TAG,"Starting event verifier");
            startService(new Intent(getApplicationContext(), VerifyGeofenceEvent.class));
        }
    }

    public String getTransitionType(int transitionType){
        switch (transitionType){
            case 1:
                return "Enter";
            case 2:
                return "Exit";
            case 3:
                return "Roaming";
        }
        return null;
    }

}


class GeofenceErrorMessages {

    public static String getErrorString(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Not available!";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many fences!";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending Intents";
            default:
                return "What?";
        }
    }


}
