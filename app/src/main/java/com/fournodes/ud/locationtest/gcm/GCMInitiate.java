package com.fournodes.ud.locationtest.gcm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by Usman on 12/30/2015.
 */
public class GCMInitiate {

    private Context context;

    public GCMInitiate(Context context) {
        this.context = context;
    }


    public void run() {
        if (checkPlayServices())
            context.startService(new Intent(context, GCMRegistrationService.class));

    }


    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog((Activity) context, resultCode, 9000)
                        .show();
                Log.w("Play Services", "Play Services Not Found");
            } else {
                Log.i("Play Services", "This device is not supported.");
            }
            return false;
        }
        return true;
    }

}

