package com.fournodes.ud.locationtest.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.services.LocationService;

public class ProviderChangedReceiver extends BroadcastReceiver {
    private Context context;

    public ProviderChangedReceiver() {}


    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;

        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGpsEnabled || isNetworkEnabled && !LocationService.isServiceRunning) {
            SharedPrefs.setIsLocationEnabled(true);
            context.startService(new Intent(context, LocationService.class));

        }
        else if (!isGpsEnabled && !isNetworkEnabled &&LocationService.isServiceRunning) {
            SharedPrefs.setIsLocationEnabled(false);
            serviceMessage("quit");
        }
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
