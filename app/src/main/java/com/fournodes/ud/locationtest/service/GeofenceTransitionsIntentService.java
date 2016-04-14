package com.fournodes.ud.locationtest.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.Fence;
import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.GeofenceEvent;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";
    private Database db;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new Database(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(geofencingEvent.getErrorCode());
            FileLogger.e(TAG, errorMessage);
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        String requestId = triggeringGeofences.get(0).getRequestId();
        GeofenceEvent event = new GeofenceEvent();
        event.requestId = Integer.parseInt(requestId);
        event.transitionType = geofenceTransition;
        event.isVerified = 0;
        event.verifyCount = 0;
        event.retryCount = 0;

        GeofenceEvent lastPendingEvent = db.getLastPendingEvent(event.requestId);
        Fence fence = db.getFence(requestId);
        FileLogger.e(TAG, getTransitionType(geofenceTransition) + " fence: " + fence.getTitle());

        /*
        *   Perform two checks if the first one fails it will be rechecked by event verifier
        *   1. Check if triggered event is same as last event of the fence and has pending events - Should be false
        *   2. Check if triggered event is same as the last event of the fence and same as the last pending event for the fence - Should be false
        *   3. Special case if the event is same as last event of fence but not same as the last pending event of the fence - Should be true
        */

        if ((fence.getLastEvent() != event.transitionType && lastPendingEvent.id == -1)
                || (fence.getLastEvent() != event.transitionType && lastPendingEvent.transitionType != event.transitionType)
                || (fence.getLastEvent() == event.transitionType && lastPendingEvent.transitionType != event.transitionType)) {
            FileLogger.e(TAG, "Event pending verification");
            int pendingEventsCount = SharedPrefs.getPendingEventCount();
            SharedPrefs.setPendingEventCount(pendingEventsCount + 1);
            db.savePendingEvent(event);
            serviceMessage("runEventVerifier");
        }
        else {
            FileLogger.e(TAG, "Both checks failed, event discarded");
        }

    }


    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public String getTransitionType(int transitionType) {
        switch (transitionType) {
            case 1:
                return "Entered";
            case 2:
                return "Exited";
            case 4:
                return "Roaming in";
        }
        return null;
    }

    public static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence service not available.";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many geofences!";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending Intents.";
            default:
                return "Undefined error.";
        }

    }
}
