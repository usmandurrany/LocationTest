package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Usman on 15/2/2016.
 */
public class SharedPrefs {
    public static SharedPreferences pref;
    private static final String SHARED_PREF_FILE = "LocationTest";
    //public static final String SERVER_ADDRESS = "http://192.168.1.110/locationtest/";
    public static final String SERVER_ADDRESS = "http://studentspot.pk/locationtest/";

    public SharedPrefs(Context context) {
        pref = context.getSharedPreferences(SharedPrefs.SHARED_PREF_FILE, 0);
    }

    private static int locUpdateInterval;
    private static String lastDeviceLatitude;
    private static String lastDeviceLongitude;
    private static String lastLocUpdateTime;
    private static boolean pollingEnabled;
    private static int minDisplacement;
    private static int updateServerInterval;
    private static boolean updateServerEnabled;
    private static String deviceGcmId;
    private static String deviceId;



    public void initialize() {
        locUpdateInterval = pref.getInt("locUpdateInterval", 60000); //1 Min
        lastDeviceLatitude = pref.getString("lastDeviceLatitude", null);
        lastDeviceLongitude = pref.getString("lastDeviceLongitude", null);
        lastLocUpdateTime = pref.getString("lastLocUpdateTime", null);
        minDisplacement = pref.getInt("minDisplacement", 500);// 500 Meters
        pollingEnabled = pref.getBoolean("pollingEnabled", false);
        updateServerInterval = pref.getInt("updateServerInterval", 300000); //5 Min
        updateServerEnabled = pref.getBoolean("updateServerEnabled", false);
        deviceGcmId = pref.getString("deviceGcmId",null);
        deviceId = pref.getString("deviceId",null);

    }

    public static int getLocUpdateInterval() {
        return locUpdateInterval;
    }

    public static void setLocUpdateInterval(int locUpdateInterval) {
        pref.edit().putInt("locUpdateInterval", locUpdateInterval).apply();
        SharedPrefs.locUpdateInterval = locUpdateInterval;
    }

    public static String getLastDeviceLongitude() {
        return lastDeviceLongitude;
    }

    public static void setLastDeviceLongitude(String lastDeviceLongitude) {
        pref.edit().putString("lastDeviceLongitude", lastDeviceLongitude).apply();
        SharedPrefs.lastDeviceLongitude = lastDeviceLongitude;
    }

    public static String getLastDeviceLatitude() {
        return lastDeviceLatitude;
    }

    public static void setLastDeviceLatitude(String lastDeviceLatitude) {
        pref.edit().putString("lastDeviceLatitude", lastDeviceLatitude).apply();
        SharedPrefs.lastDeviceLatitude = lastDeviceLatitude;
    }

    public static String getLastLocUpdateTime() {
        return lastLocUpdateTime;
    }

    public static void setLastLocUpdateTime(String lastLocUpdateTime) {
        pref.edit().putString("lastLocUpdateTime", lastLocUpdateTime).apply();
        SharedPrefs.lastLocUpdateTime = lastLocUpdateTime;
    }

    public static boolean isPollingEnabled() {
        return pollingEnabled;
    }

    public static void setPollingEnabled(boolean pollingEnabled) {
        pref.edit().putBoolean("pollingEnabled", pollingEnabled).apply();
        SharedPrefs.pollingEnabled = pollingEnabled;
    }

    public static int getMinDisplacement() {
        return minDisplacement;
    }

    public static void setMinDisplacement(int minDisplacement) {
        pref.edit().putInt("minDisplacement", minDisplacement).apply();
        SharedPrefs.minDisplacement = minDisplacement;
    }

    public static int getUpdateServerInterval() {
        return updateServerInterval;
    }

    public static void setUpdateServerInterval(int updateServerInterval) {
        pref.edit().putInt("updateServerInterval",updateServerInterval).apply();
        SharedPrefs.updateServerInterval = updateServerInterval;
    }

    public static boolean isUpdateServerEnabled() {
        return updateServerEnabled;
    }

    public static void setUpdateServerEnabled(boolean updateServerEnabled) {
        pref.edit().putBoolean("updateServerEnabled", updateServerEnabled).apply();
        SharedPrefs.updateServerEnabled = updateServerEnabled;
    }

    public static String getDeviceGcmId() {
        return deviceGcmId;
    }

    public static void setDeviceGcmId(String deviceGcmId) {
        pref.edit().putString("deviceGcmId", deviceGcmId).apply();
        SharedPrefs.deviceGcmId = deviceGcmId;
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public static void setDeviceId(String deviceId) {
        pref.edit().putString("deviceId", deviceId).apply();
        SharedPrefs.deviceId = deviceId;
    }
}
