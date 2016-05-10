package com.fournodes.ud.locationtest.utils;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.fournodes.ud.locationtest.GeofenceWrapper;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Event;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class Database extends SQLiteOpenHelper {
    private static final String TAG = "Database";
    private Context context;
    private Handler updateServer;
    private Runnable update;

    public static final String DATABASE_NAME = "LocationTest";
    public static final String DATABASE_FILE_NAME = "LocationTest.db";
    public static final int DATABASE_VERSION = 10;


    public static final String COLUMN_ID = "id";

    public static final String TABLE_GEOFENCE = "geofence";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_SERVER_FENCE_ID = "server_fence_id";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CENTER_LAT = "center_lat";
    public static final String COLUMN_CENTER_LNG = "center_lng";
    public static final String COLUMN_END_LAT = "end_lat";
    public static final String COLUMN_END_LNG = "end_lng";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_NOTIFY_DEVICE = "notify_device";
    public static final String COLUMN_ON_DEVICE = "on_device";
    public static final String COLUMN_CREATE_ON = "create_on";
    public static final String COLUMN_TRANSITION_TYPE = "transition";
    public static final String COLUMN_LAST_EVENT = "last_event";
    public static final String COLUMN_DISTANCE_FROM = "distance_from";
    public static final String COLUMN_IS_ACTIVE = "is_active";


    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_LOC_LAT = "loc_lat";
    public static final String COLUMN_LOC_LNG = "loc_lng";
    public static final String COLUMN_DISPLACEMENT = "displacement";
    public static final String COLUMN_TIME = "time";

    public static final String TABLE_GEOFENCE_EVENT = "geofence_event";
    public static final String COLUMN_REQUEST_ID = "request_id";
    public static final String COLUMN_IS_VERIFIED = "is_verified";
    public static final String COLUMN_VERIFY_COUNT = "verify_count";
    public static final String COLUMN_RETRY_COUNT = "retry_count";


    // Database creation sql statement
    private static final String SQL_CREATE_TABLE_GEOFENCE = "create table if not exists "
            + TABLE_GEOFENCE + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_SERVER_FENCE_ID + " integer not null, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_CENTER_LAT + " double not null, "
            + COLUMN_CENTER_LNG + " double not null, "
            + COLUMN_END_LAT + " double not null, "
            + COLUMN_END_LNG + " double not null, "
            + COLUMN_RADIUS + " float not null,"
            + COLUMN_NOTIFY_DEVICE + " text not null,"
            + COLUMN_ON_DEVICE + " integer not null,"
            + COLUMN_CREATE_ON + " text not null,"
            + COLUMN_TRANSITION_TYPE + " integer not null,"
            + COLUMN_LAST_EVENT + " integer default 2,"
            + COLUMN_IS_ACTIVE + " integer default 1,"
            + COLUMN_DISTANCE_FROM + " double default 0);";

    private static final String SQL_CREATE_TABLE_LOCATION = "create table if not exists "
            + TABLE_LOCATION + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LOC_LAT + " double not null, "
            + COLUMN_LOC_LNG + " double not null, "
            + COLUMN_DISPLACEMENT + " float not null,"
            + COLUMN_TIME + " bigint not null);";


    private static final String SQL_CREATE_TABLE_GEOFENCE_EVENT = "create table if not exists "
            + TABLE_GEOFENCE_EVENT + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_REQUEST_ID + " integer not null, "
            + COLUMN_TRANSITION_TYPE + " integer not null, "
            + COLUMN_IS_VERIFIED + " integer not null, "
            + COLUMN_VERIFY_COUNT + " integer not null, "
            + COLUMN_RETRY_COUNT + " integer not null);";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(SQL_CREATE_TABLE_GEOFENCE);
        database.execSQL(SQL_CREATE_TABLE_LOCATION);
        database.execSQL(SQL_CREATE_TABLE_GEOFENCE_EVENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.e(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_GEOFENCE);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_GEOFENCE_EVENT);
        onCreate(database);
    }


    public long saveLocation(double lat, double lng, final long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        Float displacement;
        String lastLat = SharedPrefs.getLastDeviceLatitude();
        String lastLng = SharedPrefs.getLastDeviceLongitude();

        if (lastLat == null && lastLng == null)
            displacement = 0f;
        else {
            displacement = toRadiusMeters(new LatLng(Double.parseDouble(lastLat), Double.parseDouble(lastLng)),
                    new LatLng(lat, lng));
        }

        values.put(COLUMN_LOC_LAT, lat);
        values.put(COLUMN_LOC_LNG, lng);
        values.put(COLUMN_DISPLACEMENT, displacement);
        values.put(COLUMN_TIME, time);
        long rowId = db.insert(TABLE_LOCATION, null, values);
        db.close();

        int rowCount = getLocEntriesCount();

        FileLogger.e(TAG, "Row count: " + String.valueOf(rowCount));
        FileLogger.e(TAG, "Row threshold: " + String.valueOf(SharedPrefs.getUpdateServerRowThreshold()));

        if (SharedPrefs.getLastLocationProvider() == null || rowCount >= SharedPrefs.getUpdateServerRowThreshold()) {
            FileLogger.e(TAG, "Updating location on server");
            final long currentTimeMillis = System.currentTimeMillis();
            String payload = "payload="+ getLocEntries(currentTimeMillis).toString()+"&user_id="+SharedPrefs.getUserId();
            IncomingApi incomingApi = new IncomingApi(null, "location_update", payload, 0);
            incomingApi.delegate = new RequestResult() {
                @Override
                public void onSuccess(String result) {
                    removeLocEntries(currentTimeMillis); //Remove from db after successfully sending to server
                    FileLogger.e(TAG, "Location update on server successful");
                }

                @Override
                public void onFailure() {
                    FileLogger.e(TAG, "Location update on server failed");

                }

                @Override
                public void userList(List<User> users) {

                }

                @Override
                public void trackEnabled() {

                }

                @Override
                public void trackDisabled() {

                }

                @Override
                public void liveLocationUpdate(String lat, String lng, String time, String trackId) {

                }

                @Override
                public void locationHistory(List<Coordinate> coordinates) {

                }
            };
            incomingApi.execute();
        }
        return rowId;
    }


    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    public JSONArray getLocEntries(long time) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION, null, COLUMN_TIME + "<?", new String[]{String.valueOf(time)}, null, null, null);
        List<JSONObject> rowList = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, Double> row = new HashMap<>();
            row.put("latitude", cursor.getDouble(cursor.getColumnIndex(COLUMN_LOC_LAT)));
            row.put("longitude", cursor.getDouble(cursor.getColumnIndex(COLUMN_LOC_LNG)));
            row.put("displacement", cursor.getDouble(cursor.getColumnIndex(COLUMN_DISPLACEMENT)));
            row.put("time", cursor.getDouble(cursor.getColumnIndex(COLUMN_TIME)));
            rowList.add(new JSONObject(row));
        }
        cursor.close();
        db.close();
        return new JSONArray(rowList);
    }

    public boolean removeLocEntries(long time) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_LOCATION, COLUMN_TIME + "<?", new String[]{String.valueOf(time)});
        db.close();
        return result > 0;
    }

    public int getLocEntriesCount() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION, null, null, null, null, null, null);

        if (cursor != null) {
            int count = cursor.getCount();
            cursor.close();
            db.close();
            return count;
        }
        else {
            db.close();
            return -1; //Error
        }
    }

    public long saveFence(Fence fence) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_FENCE_ID, fence.getId());
        values.put(COLUMN_TITLE, fence.getTitle());
        values.put(COLUMN_DESCRIPTION, fence.getDescription());
        values.put(COLUMN_CENTER_LAT, (fence.getCenterMarker() != null ? fence.getCenterMarker().getPosition().latitude : fence.getCenter_lat()));
        values.put(COLUMN_CENTER_LNG, (fence.getCenterMarker() != null ? fence.getCenterMarker().getPosition().longitude : fence.getCenter_lng()));
        values.put(COLUMN_END_LAT, (fence.getEdgeMarker() != null ? fence.getEdgeMarker().getPosition().latitude : fence.getEdge_lat()));
        values.put(COLUMN_END_LNG, (fence.getEdgeMarker() != null ? fence.getEdgeMarker().getPosition().longitude : fence.getEdge_lng()));
        values.put(COLUMN_RADIUS, fence.getRadius());
        values.put(COLUMN_NOTIFY_DEVICE, fence.getUserId());
        values.put(COLUMN_ON_DEVICE, fence.getOnDevice());
        values.put(COLUMN_CREATE_ON, fence.getCreate_on());
        values.put(COLUMN_LAST_EVENT, fence.getLastEvent());
        values.put(COLUMN_TRANSITION_TYPE, fence.getTransitionType());
        values.put(COLUMN_IS_ACTIVE, fence.getIsActive());
        values.put(COLUMN_DISTANCE_FROM, fence.getDistanceFrom());
        long rowId = db.insert(TABLE_GEOFENCE, null, values);
        db.close();

        BackupManager bm = new BackupManager(context);
        bm.dataChanged();
        return rowId;
    }

    /* Possible actions
      * getAll
      * getActive
      * createAll
      * removeAll
     */
    public List<Fence> onDeviceFence(String action) {
        final int notificationId = (int) System.currentTimeMillis();

        List<Fence> mFenceList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();
        GeofenceWrapper geofenceWrapper = new GeofenceWrapper(context);
        String selection = null;

        if (action.equals("getActive"))
            selection = COLUMN_ON_DEVICE + " = 1 AND " + COLUMN_IS_ACTIVE + " = 1";
        else if (action.equals("getAll"))
            selection = COLUMN_ON_DEVICE + " = 1";

        final Cursor cursor = db.query(TABLE_GEOFENCE, null, selection, null, null, null, null, null);

        while (cursor.moveToNext()) {
            final int id = cursor.getInt(cursor.getColumnIndex(COLUMN_SERVER_FENCE_ID));
            final String title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE));
            Fence fence = new Fence();
            fence.setId(id);
            fence.setCenter_lat(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)));
            fence.setCenter_lng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            fence.setEdge_lat(cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LAT)));
            fence.setEdge_lng(cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LNG)));
            fence.setRadius(cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS)));
            fence.setTitle(title);
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setUserId(cursor.getString(cursor.getColumnIndex(COLUMN_NOTIFY_DEVICE)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));
            fence.setCreate_on(cursor.getString(cursor.getColumnIndex(COLUMN_CREATE_ON)));
            fence.setLastEvent(cursor.getInt(cursor.getColumnIndex(COLUMN_LAST_EVENT)));
            fence.setTransitionType(cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE)));
            fence.setDistanceFrom((int) cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE_FROM)));
            fence.setIsActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)));

/*            FileLogger.e(TAG, "Fence: " + fence.getId());
            FileLogger.e(TAG, "Action: " + action);
            FileLogger.e(TAG, "Title: " + fence.getTitle());
            FileLogger.e(TAG, "Description: " + fence.getDescription());
            FileLogger.e(TAG, "Center: Lat: " + fence.getCenter_lat() + " Long: " + fence.getCenter_lng());
            FileLogger.e(TAG, "Radius: " + fence.getRadius());*/

            if (action.equals("createAll"))
                geofenceWrapper.create(fence);
            else if (action.equals("removeAll"))
                geofenceWrapper.remove(fence);

            mFenceList.add(fence);
        }

        cursor.close();
        db.close();

/*        PendingIntent piActivityIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(Color.BLUE)
                .setContentTitle("Action: " + action)
                .setContentText("Fences affected: " + String.valueOf(mFenceList.size()))
                .setContentIntent(piActivityIntent)
                .setAutoCancel(false);

        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, builder.build());*/

        return mFenceList;
    }

    public void resetAll() {
        if (updateEventsAfterReboot())
            FileLogger.e(TAG, "Pending events removed successfully.");
        else
            FileLogger.e(TAG, "Could not remove pending events.");

        if (updateFencesAfterReboot())
            FileLogger.e(TAG, "Reset all fences successfully.");
        else
            FileLogger.e(TAG, "Could not reset fences.");

        SharedPrefs.setPendingEventCount(0);
    }


    public List<Fence> drawOffDeviceFences(GoogleMap map) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = COLUMN_ON_DEVICE + " = 0";
        Cursor cursor = db.query(TABLE_GEOFENCE, null, selection, null, null, null, null, null);

        List<Fence> mFenceList = new ArrayList<>();

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(COLUMN_SERVER_FENCE_ID));
            LatLng center = new LatLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            LatLng end = new LatLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LAT)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LNG)));

            float radius = cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS));

            int transition = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE));


            Fence fence = new Fence();
            fence.setId(id);
            fence.setRadius(radius);
            fence.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setUserId(cursor.getString(cursor.getColumnIndex(COLUMN_NOTIFY_DEVICE)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));
            fence.setCreate_on(cursor.getString(cursor.getColumnIndex(COLUMN_CREATE_ON)));
            fence.setTransitionType(transition);

            fence.setCenterMarker(
                    map.addMarker(new MarkerOptions()
                            .position(center)
                            .snippet(fence.getDescription())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(fence.getTitle())));

            fence.setEdgeMarker(
                    map.addMarker(new MarkerOptions()
                            .position(end)
                            .draggable(true)
                            .alpha(0.4f)
                            .title(fence.getTitle())));

            fence.setVisibleArea(map.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .fillColor(0)
                    .strokeColor(Color.parseColor("#000000"))
                    .strokeWidth(3f)));

            mFenceList.add(fence);
        }

        cursor.close();
        db.close();
        return mFenceList;

    }

    public Fence getFence(String requestId) {
        Fence fence = new Fence();
        SQLiteDatabase db = this.getWritableDatabase();
/*        String selection = COLUMN_CENTER_LAT + " = " + centerLat
                + " AND " + COLUMN_CENTER_LNG + " = " + centerLng;*/

        String selection = COLUMN_SERVER_FENCE_ID + " = " + requestId;
        Cursor cursor = db.query(TABLE_GEOFENCE, null, selection, null, null, null, null, null);


        while (cursor.moveToNext()) {
            fence.setId(Integer.parseInt(requestId));
            fence.setCenter_lat(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)));
            fence.setCenter_lng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            fence.setEdge_lat(cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LAT)));
            fence.setEdge_lng(cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LNG)));
            fence.setRadius(cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS)));
            fence.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setUserId(cursor.getString(cursor.getColumnIndex(COLUMN_NOTIFY_DEVICE)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));
            fence.setLastEvent(cursor.getInt(cursor.getColumnIndex(COLUMN_LAST_EVENT)));
            fence.setIsActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)));
            fence.setTransitionType(cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE)));
            fence.setDistanceFrom((int) cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE_FROM)));
        }

        cursor.close();
        db.close();
        return fence;
    }

    public boolean updateFence(Fence fence) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RADIUS, fence.getRadius());
        values.put(COLUMN_END_LAT, (fence.getEdgeMarker() != null ? fence.getEdgeMarker().getPosition().latitude : fence.getEdge_lat()));
        values.put(COLUMN_END_LNG, (fence.getEdgeMarker() != null ? fence.getEdgeMarker().getPosition().longitude : fence.getEdge_lng()));
        values.put(COLUMN_LAST_EVENT, fence.getLastEvent());
        int result = db.update(TABLE_GEOFENCE, values, COLUMN_SERVER_FENCE_ID + "=?", new String[]{String.valueOf(fence.getId())});
        db.close();
        return result > 0;
    }

    public boolean updateFenceDistance(Fence fence) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, fence.getIsActive());
        values.put(COLUMN_DISTANCE_FROM, fence.getDistanceFrom());
        int result = db.update(TABLE_GEOFENCE, values, COLUMN_SERVER_FENCE_ID + "=?", new String[]{String.valueOf(fence.getId())});
        db.close();
        return result > 0;
    }


    public boolean removeFenceFromDatabase(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_GEOFENCE, COLUMN_SERVER_FENCE_ID + "=" + id, null);
        db.close();

        BackupManager bm = new BackupManager(context);
        bm.dataChanged();
        return result > 0;
    }

    public void savePendingEvent(Event event) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REQUEST_ID, event.requestId);
        values.put(COLUMN_TRANSITION_TYPE, event.transitionType);
        values.put(COLUMN_IS_VERIFIED, event.isVerified);
        values.put(COLUMN_VERIFY_COUNT, event.verifyCount);
        values.put(COLUMN_RETRY_COUNT, event.retryCount);
        db.insert(TABLE_GEOFENCE_EVENT, null, values);
        db.close();
    }

    public List<Event> getAllPendingEvents() {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COLUMN_IS_VERIFIED + " = " + 0;
        Cursor cursor = db.query(TABLE_GEOFENCE_EVENT, null, selection, null, null, null, null);
        List<Event> pendingEvents = new ArrayList<>();
        while (cursor.moveToNext()) {
            Event event = new Event();
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.requestId = cursor.getInt(cursor.getColumnIndex(COLUMN_REQUEST_ID));
            event.transitionType = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE));
            event.isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            event.verifyCount = cursor.getInt(cursor.getColumnIndex(COLUMN_VERIFY_COUNT));
            event.retryCount = cursor.getInt(cursor.getColumnIndex(COLUMN_RETRY_COUNT));
            pendingEvents.add(event);
        }
        cursor.close();
        db.close();
        return pendingEvents;
    }

    public int getFencePendingEventCount(int requestId) {
        //COLUMN_IS_VERIFIED
        SQLiteDatabase db = getWritableDatabase();
        String selection = COLUMN_REQUEST_ID + " = " + requestId;
        Cursor cursor = db.query(TABLE_GEOFENCE_EVENT, null, selection, null, null, null, null);
        return cursor != null ? cursor.getCount() : -1;
    }

    public Event getLastVerifiedEvent(int requestId) {
        FileLogger.e(TAG, "-- Begin Pending Event Removal --");

        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_REQUEST_ID + " = " + requestId
                + " AND " + COLUMN_IS_VERIFIED + " = " + 1;
        Event event = new Event();
        Cursor cursor = db.query(TABLE_GEOFENCE_EVENT, null, selection, null, null, null, COLUMN_ID + " DESC", "1");
        while (cursor.moveToNext()) {
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.requestId = cursor.getInt(cursor.getColumnIndex(COLUMN_REQUEST_ID));
            event.transitionType = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE));
            event.isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            event.verifyCount = cursor.getInt(cursor.getColumnIndex(COLUMN_VERIFY_COUNT));
            event.retryCount = cursor.getInt(cursor.getColumnIndex(COLUMN_RETRY_COUNT));
        }
        FileLogger.e(TAG, "-- End Pending Event Removal --");

        cursor.close();
        db.close();
        return event;
    }

    public Event getLastPendingEvent(int requestId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_REQUEST_ID + " = " + requestId
                + " AND " + COLUMN_IS_VERIFIED + " = " + 0;
        Event event = new Event();
        Cursor cursor = db.query(TABLE_GEOFENCE_EVENT, null, selection, null, null, null, COLUMN_ID + " DESC", "1");
        while (cursor.moveToNext()) {
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.requestId = cursor.getInt(cursor.getColumnIndex(COLUMN_REQUEST_ID));
            event.transitionType = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE));
            event.isVerified = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VERIFIED));
            event.verifyCount = cursor.getInt(cursor.getColumnIndex(COLUMN_VERIFY_COUNT));
            event.retryCount = cursor.getInt(cursor.getColumnIndex(COLUMN_RETRY_COUNT));
        }
        cursor.close();
        db.close();
        return event;
    }

    public Boolean updateEvent(Event event) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REQUEST_ID, event.requestId);
        values.put(COLUMN_TRANSITION_TYPE, event.transitionType);
        values.put(COLUMN_IS_VERIFIED, event.isVerified);
        values.put(COLUMN_VERIFY_COUNT, event.verifyCount);
        values.put(COLUMN_RETRY_COUNT, event.retryCount);
        int result = db.update(TABLE_GEOFENCE_EVENT, values, COLUMN_ID + "=?", new String[]{String.valueOf(event.id)});
        db.close();
        return result > 0;

    }

    public Boolean updateEventsAfterReboot() {
        SQLiteDatabase db = getWritableDatabase();
        //ContentValues values = new ContentValues();
        //values.put(COLUMN_IS_VERIFIED, 2); //Special value
        //int result = db.update(TABLE_GEOFENCE_EVENT, values, null, null);
        int result = db.delete(TABLE_GEOFENCE_EVENT, null, null);
        db.close();
        return result > 0;

    }

    public Boolean updateFencesAfterReboot() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_EVENT, 2); //Reset all fences to exit
        int result = db.update(TABLE_GEOFENCE, values, null, null);
        db.close();
        return result > 0;
    }

    public Boolean removeEvent(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_GEOFENCE_EVENT, COLUMN_ID + "=? AND " + COLUMN_IS_VERIFIED + "=?", new String[]{String.valueOf(id), "0"});
        db.close();
        return result > 0;
    }

    public Boolean removeSimilarEvents(int requestId, int transitionType) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_GEOFENCE_EVENT, COLUMN_REQUEST_ID + "=? AND " + COLUMN_TRANSITION_TYPE + "=? AND " + COLUMN_IS_VERIFIED + "=? ", new String[]{String.valueOf(requestId), String.valueOf(transitionType), "0"});
        db.close();
        return result > 0;

    }


}
