package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Usman on 15/2/2016.
 */
public class SharedPrefs {
    public static SharedPreferences pref;
    private static final String SHARED_PREF_FILE = "LocationTest";
    //public static final String SERVER_ADDRESS = "http://192.168.1.110/locationtest/v2/";
    public static final String SERVER_ADDRESS = "http://www.studentspot.pk/locationtest/v2/";

    public SharedPrefs(Context context) {
        pref = context.getSharedPreferences(SharedPrefs.SHARED_PREF_FILE, 0);
    }

    private static String lastDeviceLatitude;
    private static String lastDeviceLongitude;
    private static String lastLocUpdateTime;
    private static String deviceGcmId;
    private static String userId;
    private static String userName;
    private static String userEmail;
    private static String userPicture;
    private static String last100mLatitude;
    private static String last100mLongitude;
    private static String reCalcDistanceAtLatitude;
    private static String reCalcDistanceAtLongitude;
    private static String lastLocationProvider;
    private static int locationRequestInterval;
    private static int pendingEventCount;
    private static int locationRequestRunnableId;
    private static int locationPollTimeout;
    private static int vicinity;
    private static int distanceThreshold;
    private static int fencePerimeterPercentage;
    private static int updateServerRowThreshold;
    private static int liveSessionId;
    private static float lastLocationAccuracy;
    private static boolean trackingEnabled;
    private static boolean isLive;
    private static boolean isMoving;
    private static boolean isLocationEnabled;



    public void initialize() {
        lastDeviceLatitude = pref.getString("lastDeviceLatitude", null);
        lastDeviceLongitude = pref.getString("lastDeviceLongitude", null);
        lastLocUpdateTime = pref.getString("lastLocUpdateTime", null);
        deviceGcmId = pref.getString("deviceGcmId", null);
        userId = pref.getString("userId", null);
        userName = pref.getString("userName", null);
        userEmail = pref.getString("userEmail", null);
        userPicture = pref.getString("userPicture", null);
        locationRequestInterval = pref.getInt("locationRequestInterval", 60);//60 Seconds
        pendingEventCount = pref.getInt("pendingEventCount", 0);
        lastLocationAccuracy = pref.getFloat("lastLocationAccuracy", 999f);
        reCalcDistanceAtLatitude = pref.getString("reCalcDistanceAtLatitude", null);
        reCalcDistanceAtLongitude = pref.getString("reCalcDistanceAtLongitude", null);
        isMoving = pref.getBoolean("isMoving", false);
        vicinity = pref.getInt("vicinity", 1000);
        distanceThreshold = pref.getInt("distanceThreshold", 500);
        fencePerimeterPercentage = pref.getInt("fencePerimeterPercentage", 10);
        updateServerRowThreshold = pref.getInt("updateServerRowThreshold", 5);
        lastLocationProvider = pref.getString("lastLocationProvider", null);
        liveSessionId = pref.getInt("liveSessionId", -1);
        trackingEnabled = pref.getBoolean("trackingEnabled", false);
        isLive = pref.getBoolean("isLive", false);
        last100mLatitude = pref.getString("last100mLatitude", null);
        last100mLongitude = pref.getString("last100mLongitude", null);
        locationRequestRunnableId = pref.getInt("locationRequestRunnableId", 0);
        locationPollTimeout = pref.getInt("locationPollTimeout", 5000);
        isLocationEnabled = pref.getBoolean("isLocationEnabled", true);

    }

    public static boolean isLocationEnabled() {
        return isLocationEnabled;
    }

    public static void setIsLocationEnabled(boolean isLocationEnabled) {
        pref.edit().putBoolean("isLocationEnabled", isLocationEnabled).apply();
        SharedPrefs.isLocationEnabled = isLocationEnabled;
    }

    public static int getLocationPollTimeout() {
        return locationPollTimeout;
    }


    public static void setLocationPollTimeout(int locationPollTimeout) {
        pref.edit().putInt("locationPollTimeout", locationPollTimeout).apply();
        SharedPrefs.locationPollTimeout = locationPollTimeout;
    }

    public static int getLocationRequestRunnableId() {
        return locationRequestRunnableId;
    }

    public static void setLocationRequestRunnableId(int locationRequestRunnableId) {
        pref.edit().putInt("locationRequestRunnableId", locationRequestRunnableId).apply();
        SharedPrefs.locationRequestRunnableId = locationRequestRunnableId;
    }

    public static String getLast100mLatitude() {
        return last100mLatitude;
    }

    public static void setLast100mLatitude(String last100mLatitude) {
        pref.edit().putString("last100mLatitude", last100mLatitude).apply();
        SharedPrefs.last100mLatitude = last100mLatitude;
    }

    public static String getLast100mLongitude() {
        return last100mLongitude;
    }

    public static void setLast100mLongitude(String last100mLongitude) {
        pref.edit().putString("last100mLongitude", last100mLongitude).apply();
        SharedPrefs.last100mLongitude = last100mLongitude;
    }

    public static boolean isLive() {
        return isLive;
    }

    public static void setIsLive(boolean isLive) {
        pref.edit().putBoolean("isLive", isLive).apply();
        SharedPrefs.isLive = isLive;
    }

    public static boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public static void setTrackingEnabled(boolean trackingEnabled) {
        pref.edit().putBoolean("trackingEnabled", trackingEnabled).apply();
        SharedPrefs.trackingEnabled = trackingEnabled;
    }

    public static int getLiveSessionId() {
        return liveSessionId;
    }

    public static void setLiveSessionId(int liveSessionId) {
        pref.edit().putInt("liveSessionId", liveSessionId).apply();
        SharedPrefs.liveSessionId = liveSessionId;
    }

    public static String getLastLocationProvider() {
        return lastLocationProvider;
    }

    public static void setLastLocationProvider(String lastLocationProvider) {
        pref.edit().putString("lastLocationProvider", lastLocationProvider).apply();
        SharedPrefs.lastLocationProvider = lastLocationProvider;
    }

    public static int getUpdateServerRowThreshold() {
        return updateServerRowThreshold;
    }

    public static void setUpdateServerRowThreshold(int updateServerRowThreshold) {
        pref.edit().putInt("updateServerRowThreshold", updateServerRowThreshold).apply();
        SharedPrefs.updateServerRowThreshold = updateServerRowThreshold;
    }

    public static int getFencePerimeterPercentage() {
        return fencePerimeterPercentage;
    }

    public static void setFencePerimeterPercentage(int fencePerimeterPercentage) {
        pref.edit().putInt("fencePerimeterPercentage", fencePerimeterPercentage).apply();
        SharedPrefs.fencePerimeterPercentage = fencePerimeterPercentage;
    }

    public static int getDistanceThreshold() {
        return distanceThreshold;
    }

    public static void setDistanceThreshold(int distanceThreshold) {
        pref.edit().putInt("distanceThreshold", distanceThreshold).apply();
        SharedPrefs.distanceThreshold = distanceThreshold;
    }

    public static int getVicinity() {
        return vicinity;
    }

    public static void setVicinity(int vicinity) {
        pref.edit().putInt("vicinity", vicinity).apply();
        SharedPrefs.vicinity = vicinity;
    }

    public static boolean isMoving() {
        return isMoving;
    }

    public static void setIsMoving(boolean isMoving) {
        pref.edit().putBoolean("isMoving", isMoving).apply();
        SharedPrefs.isMoving = isMoving;
    }

    public static String getReCalcDistanceAtLatitude() {
        return reCalcDistanceAtLatitude;
    }

    public static void setReCalcDistanceAtLatitude(String reCalcDistanceAtLatitude) {
        pref.edit().putString("reCalcDistanceAtLatitude", reCalcDistanceAtLatitude).apply();

        SharedPrefs.reCalcDistanceAtLatitude = reCalcDistanceAtLatitude;
    }

    public static String getReCalcDistanceAtLongitude() {
        return reCalcDistanceAtLongitude;
    }

    public static void setReCalcDistanceAtLongitude(String reCalcDistanceAtLongitude) {
        SharedPrefs.reCalcDistanceAtLongitude = reCalcDistanceAtLongitude;
        pref.edit().putString("reCalcDistanceAtLongitude", reCalcDistanceAtLongitude).apply();
    }


    public static float getLastLocationAccuracy() {
        return lastLocationAccuracy;
    }

    public static void setLastLocationAccuracy(float lastLocationAccuracy) {
        pref.edit().putFloat("lastLocationAccuracy", lastLocationAccuracy).apply();
        SharedPrefs.lastLocationAccuracy = lastLocationAccuracy;
    }

    public static int getPendingEventCount() {
        return pendingEventCount;
    }

    public static void setPendingEventCount(int pendingEventCount) {
        pref.edit().putInt("pendingEventCount", pendingEventCount).apply();
        SharedPrefs.pendingEventCount = pendingEventCount;
    }


    public static int getLocationRequestInterval() {
        return locationRequestInterval;
    }

    public static void setLocationRequestInterval(int locationRequestInterval) {
        SharedPrefs.locationRequestInterval = locationRequestInterval;
        pref.edit().putInt("locationRequestInterval", locationRequestInterval).apply();
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


    public static String getDeviceGcmId() {
        return deviceGcmId;
    }

    public static void setDeviceGcmId(String deviceGcmId) {
        pref.edit().putString("deviceGcmId", deviceGcmId).apply();
        SharedPrefs.deviceGcmId = deviceGcmId;
    }

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String userId) {
        pref.edit().putString("userId", userId).apply();
        SharedPrefs.userId = userId;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        pref.edit().putString("userName", userName).apply();

        SharedPrefs.userName = userName;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserEmail(String userEmail) {
        pref.edit().putString("userEmail", userEmail).apply();

        SharedPrefs.userEmail = userEmail;
    }

    public static String getUserPicture() {
        return userPicture;
    }

    public static void setUserPicture(String userPicture) {
        pref.edit().putString("userPicture", userPicture).apply();

        SharedPrefs.userPicture = userPicture;
    }
}
