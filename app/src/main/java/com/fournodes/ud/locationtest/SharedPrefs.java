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

    private static int locUpdateInterval;
    private static String lastDeviceLatitude;
    private static String lastDeviceLongitude;
    private static String lastLocUpdateTime;
    private static boolean pollingEnabled;
    private static int minDisplacement;
    private static int updateServerInterval;
    private static boolean updateServerEnabled;
    private static String deviceGcmId;
    private static String userId;
    private static String userName;
    private static String userEmail;
    private static String userPicture;
    private static int locationRequestInterval;
    private static long locLastUpdateMillis;
    private static int pendingEventCount;
    private static float lastLocationAccuracy;
    private static float currentDisplacement;
    private static String reCalcDistanceAtLatitude;
    private static String reCalcDistanceAtLongitude;


    private static boolean isMoving;

    private static int vicinity;
    private static int distanceThreshold;
    private static int fencePerimeterPercentage;

    private static long locationRequestAt;

    private static int updateServerRowThreshold;
    private static String lastLocationProvider;

    private static int liveSessionId;

    private static boolean trackingEnabled;
    private static boolean isLive;


    private static String last100mLatitude;
    private static String last100mLongitude;

    private static int locationRequestRunnableId;
    private static int gpsPollTimeout;


    public void initialize() {
        locUpdateInterval = pref.getInt("locUpdateInterval", 60000); //1 Min
        lastDeviceLatitude = pref.getString("lastDeviceLatitude", null);
        lastDeviceLongitude = pref.getString("lastDeviceLongitude", null);
        lastLocUpdateTime = pref.getString("lastLocUpdateTime", null);
        minDisplacement = pref.getInt("minDisplacement", 200);// 500 Meters
        pollingEnabled = pref.getBoolean("pollingEnabled", true);
        updateServerInterval = pref.getInt("updateServerInterval", 240000); //5 Min 300000
        updateServerEnabled = pref.getBoolean("updateServerEnabled", true);
        deviceGcmId = pref.getString("deviceGcmId", null);
        userId = pref.getString("userId", null);
        userName = pref.getString("userName", null);
        userEmail = pref.getString("userEmail", null);
        userPicture = pref.getString("userPicture", null);
        locationRequestInterval = pref.getInt("locationRequestInterval", 60);//60 Seconds
        locLastUpdateMillis = pref.getLong("locLastUpdateMillis", System.currentTimeMillis());
        pendingEventCount = pref.getInt("pendingEventCount", 0);
        lastLocationAccuracy = pref.getFloat("lastLocationAccuracy", 999f);
        currentDisplacement = pref.getFloat("currentDisplacement", 0);
        reCalcDistanceAtLatitude = pref.getString("reCalcDistanceAtLatitude", null);
        reCalcDistanceAtLongitude = pref.getString("reCalcDistanceAtLongitude", null);
        isMoving = pref.getBoolean("isMoving", false);
        vicinity = pref.getInt("vicinity", 1000);
        distanceThreshold = pref.getInt("distanceThreshold", 500);
        fencePerimeterPercentage = pref.getInt("fencePerimeterPercentage", 10);
        locationRequestAt = pref.getLong("locationRequestAt", System.currentTimeMillis());
        updateServerRowThreshold = pref.getInt("updateServerRowThreshold", 5);
        lastLocationProvider = pref.getString("lastLocationProvider", null);
        liveSessionId = pref.getInt("liveSessionId", -1);
        trackingEnabled = pref.getBoolean("trackingEnabled", false);
        isLive = pref.getBoolean("isLive", false);

        last100mLatitude = pref.getString("last100mLatitude", null);
        last100mLongitude = pref.getString("last100mLongitude", null);
        locationRequestRunnableId = pref.getInt("locationRequestRunnableId", 0);
        gpsPollTimeout = pref.getInt("gpsPollTimeout", 5000);
    }

    public static int getGpsPollTimeout() {
        return gpsPollTimeout;
    }

    public static void setGpsPollTimeout(int gpsPollTimeout) {
        pref.edit().putInt("gpsPollTimeout", gpsPollTimeout).apply();
        SharedPrefs.gpsPollTimeout = gpsPollTimeout;
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

    public static long getLocationRequestAt() {
        return locationRequestAt;
    }

    public static void setLocationRequestAt(long locationRequestAt) {
        SharedPrefs.locationRequestAt = locationRequestAt;
        pref.edit().putLong("locationRequestAt", locationRequestAt).apply();
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

    public static float getCurrentDisplacement() {
        return currentDisplacement;
    }

    public static void setCurrentDisplacement(float currentDisplacement) {
        pref.edit().putFloat("currentDisplacement", currentDisplacement).apply();
        SharedPrefs.currentDisplacement = currentDisplacement;
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

    public static long getLocLastUpdateMillis() {
        return locLastUpdateMillis;
    }

    public static void setLocLastUpdateMillis(long locLastUpdateMillis) {
        pref.edit().putLong("locLastUpdateMillis", locLastUpdateMillis).apply();
        SharedPrefs.locLastUpdateMillis = locLastUpdateMillis;
    }

    public static int getLocationRequestInterval() {
        return locationRequestInterval;
    }

    public static void setLocationRequestInterval(int locationRequestInterval) {
        SharedPrefs.locationRequestInterval = locationRequestInterval;
        pref.edit().putInt("locationRequestInterval", locationRequestInterval).apply();
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
        pref.edit().putInt("updateServerInterval", updateServerInterval).apply();
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
