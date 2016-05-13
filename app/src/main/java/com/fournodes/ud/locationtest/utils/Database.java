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

    public static final String DATABASE_NAME = "LocationTest";
    public static final String DATABASE_FILE_NAME = "LocationTest.db";
    public static final int DATABASE_VERSION = 11;

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FENCE_ID = "fence_id";

    public static final String TABLE_FENCE_INFORMATION = "fence_information";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CENTER_LAT = "center_lat";
    public static final String COLUMN_CENTER_LNG = "center_lng";
    public static final String COLUMN_EDGE_LAT = "end_lat";
    public static final String COLUMN_EDGE_LNG = "end_lng";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_ASSIGNMENT = "assignment";
    public static final String COLUMN_ON_DEVICE = "on_device";


    public static final String TABLE_FENCE_PARAMETER = "fence_parameter";
    public static final String COLUMN_NOTIFY_ID = "notify_id";
    public static final String COLUMN_LAST_EVENT = "last_event";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_DISTANCE_FROM_USER = "distance_from_user";


    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_LOC_LAT = "loc_lat";
    public static final String COLUMN_LOC_LNG = "loc_lng";
    public static final String COLUMN_DISPLACEMENT = "displacement";
    public static final String COLUMN_TIME = "time";

    public static final String TABLE_FENCE_EVENT = "fence_event";
    public static final String COLUMN_IS_VERIFIED = "is_verified";
    public static final String COLUMN_VERIFY_COUNT = "verify_count";
    public static final String COLUMN_RETRY_COUNT = "retry_count";
    public static final String COLUMN_EVENT_TYPE = "event_type";


    // Database creation sql statement
    private static final String CREATE_TABLE_FENCE_INFORMATION = "create table if not exists "
            + TABLE_FENCE_INFORMATION + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_FENCE_ID + " integer not null, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_CENTER_LAT + " double not null, "
            + COLUMN_CENTER_LNG + " double not null, "
            + COLUMN_EDGE_LAT + " double not null, "
            + COLUMN_EDGE_LNG + " double not null, "
            + COLUMN_RADIUS + " float not null,"
            + COLUMN_ASSIGNMENT + " text not null,"
            + COLUMN_ON_DEVICE + " integer not null);";

    private static final String CREATE_TABLE_FENCE_PARAMETER = "create table if not exists "
            + TABLE_FENCE_PARAMETER + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_FENCE_ID + " integer not null, "
            + COLUMN_NOTIFY_ID + " integer not null,"
            + COLUMN_LAST_EVENT + " integer default 2,"
            + COLUMN_IS_ACTIVE + " integer default 1,"
            + COLUMN_DISTANCE_FROM_USER + " double default 0);";


    private static final String CREATE_TABLE_LOCATION = "create table if not exists "
            + TABLE_LOCATION + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LOC_LAT + " double not null, "
            + COLUMN_LOC_LNG + " double not null, "
            + COLUMN_DISPLACEMENT + " float not null,"
            + COLUMN_TIME + " bigint not null);";


    private static final String CREATE_TABLE_FENCE_EVENT = "create table if not exists "
            + TABLE_FENCE_EVENT + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_FENCE_ID + " integer not null, "
            + COLUMN_EVENT_TYPE + " integer not null, "
            + COLUMN_VERIFY_COUNT + " integer not null, "
            + COLUMN_RETRY_COUNT + " integer not null, "
            + COLUMN_IS_VERIFIED + " integer not null);";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(CREATE_TABLE_FENCE_INFORMATION);
        database.execSQL(CREATE_TABLE_FENCE_PARAMETER);
        database.execSQL(CREATE_TABLE_FENCE_EVENT);
        database.execSQL(CREATE_TABLE_LOCATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.e(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_FENCE_INFORMATION);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_FENCE_PARAMETER);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_FENCE_EVENT);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
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
            String payload = "payload=" + getLocEntries(currentTimeMillis).toString() + "&user_id=" + SharedPrefs.getUserId();
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

    public long saveFenceInformation(Fence fence) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FENCE_ID, fence.getFenceId());
        values.put(COLUMN_TITLE, fence.getTitle());
        values.put(COLUMN_DESCRIPTION, fence.getDescription());
        values.put(COLUMN_CENTER_LAT, (fence.getCenterLat()));
        values.put(COLUMN_CENTER_LNG, (fence.getCenterLng()));
        values.put(COLUMN_EDGE_LAT, (fence.getEdgeLat()));
        values.put(COLUMN_EDGE_LNG, (fence.getEdgeLng()));
        values.put(COLUMN_RADIUS, fence.getRadius());
        values.put(COLUMN_ASSIGNMENT, fence.getAssignment());
        values.put(COLUMN_ON_DEVICE, fence.getOnDevice());

        long rowId = db.insert(TABLE_FENCE_INFORMATION, null, values);
        db.close();

        BackupManager bm = new BackupManager(context);
        bm.dataChanged();

        if (rowId > 0 && fence.getOnDevice() == 1) {
            saveFenceParameter(fence);
            return rowId;
        }
        else {
            return -1;
        }

    }

    public long saveFenceParameter(Fence fence) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FENCE_ID, fence.getFenceId());
        values.put(COLUMN_NOTIFY_ID, fence.getNotifyId());
        values.put(COLUMN_LAST_EVENT, fence.getLastEvent());
        values.put(COLUMN_DISTANCE_FROM_USER, fence.getDistanceFromUser());
        values.put(COLUMN_IS_ACTIVE, fence.getIsActive());

        long rowId = db.insert(TABLE_FENCE_PARAMETER, null, values);
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
        List<Fence> mFenceList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();
        String selection = null;

        if (action.equals("getActive"))
            selection = TABLE_FENCE_INFORMATION+"."+COLUMN_ON_DEVICE + " = 1 AND " + TABLE_FENCE_PARAMETER+"."+COLUMN_IS_ACTIVE + " = 1";
        else if (action.equals("getAll"))
            selection = TABLE_FENCE_INFORMATION+"."+COLUMN_ON_DEVICE + " = 1";

        final Cursor cursor = db.query(TABLE_FENCE_INFORMATION + " LEFT OUTER JOIN "+TABLE_FENCE_PARAMETER+" ON "+ TABLE_FENCE_INFORMATION+"."+COLUMN_FENCE_ID+"="+TABLE_FENCE_PARAMETER+"."+COLUMN_FENCE_ID, null, selection, null, null, null, null, null);
       // final Cursor cursor = db.rawQuery("SELECT * FROM fence_information LEFT OUTER JOIN fence_parameter ON fence_information.fence_id = fence_parameter.fence_id WHERE " +selection,null);


        Log.e(TAG,String.valueOf(cursor.getCount()));
        while (cursor.moveToNext()) {
            final int id = cursor.getInt(cursor.getColumnIndex(COLUMN_FENCE_ID));
            final String title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE));
            Fence fence = new Fence();
            fence.setFenceId(id);
            fence.setCenterLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)));
            fence.setCenterLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            fence.setEdgeLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LAT)));
            fence.setEdgeLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LNG)));
            fence.setRadius(cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS)));
            fence.setTitle(title);
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setNotifyId(cursor.getInt(cursor.getColumnIndex(COLUMN_NOTIFY_ID)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));
            fence.setLastEvent(cursor.getInt(cursor.getColumnIndex(COLUMN_LAST_EVENT)));
            fence.setAssignment(cursor.getString(cursor.getColumnIndex(COLUMN_ASSIGNMENT)));
            fence.setDistanceFromUser((int) cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE_FROM_USER)));
            fence.setIsActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)));
            mFenceList.add(fence);
        }

        cursor.close();
        db.close();


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

        String selection = TABLE_FENCE_INFORMATION+"."+COLUMN_ON_DEVICE + " = 0";
        final Cursor cursor = db.query(TABLE_FENCE_INFORMATION + " LEFT OUTER JOIN "+TABLE_FENCE_PARAMETER+" ON "+ TABLE_FENCE_INFORMATION+"."+COLUMN_FENCE_ID+"="+TABLE_FENCE_PARAMETER+"."+COLUMN_FENCE_ID, null, selection, null, null, null, null, null);

        List<Fence> mFenceList = new ArrayList<>();

        while (cursor.moveToNext()) {


            Fence fence = new Fence();
            fence.setFenceId(cursor.getInt(cursor.getColumnIndex(COLUMN_FENCE_ID)));
            fence.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setCenterLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)));
            fence.setCenterLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            fence.setEdgeLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LAT)));
            fence.setEdgeLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LNG)));
            fence.setRadius(cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS)));
            fence.setAssignment(cursor.getString(cursor.getColumnIndex(COLUMN_ASSIGNMENT)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));

            LatLng latLngCenter = new LatLng(fence.getCenterLat(),fence.getCenterLng());
            LatLng latLngEdge = new LatLng(fence.getEdgeLat(),fence.getEdgeLng());

            fence.setCenterMarker(
                    map.addMarker(new MarkerOptions()
                            .position(latLngCenter)
                            .snippet(fence.getDescription())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(fence.getTitle())));

            fence.setEdgeMarker(
                    map.addMarker(new MarkerOptions()
                            .position(latLngEdge)
                            .draggable(true)
                            .alpha(0.4f)
                            .title(fence.getTitle())));

            fence.setCircle(map.addCircle(new CircleOptions()
                    .center(latLngCenter)
                    .radius(fence.getRadius())
                    .fillColor(0)
                    .strokeColor(Color.parseColor("#000000"))
                    .strokeWidth(3f)));

            mFenceList.add(fence);
        }

        cursor.close();
        db.close();
        return mFenceList;

    }

    public Fence getFence(String fenceId) {
        Fence fence = new Fence();
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = TABLE_FENCE_INFORMATION+"."+COLUMN_FENCE_ID + " = " + fenceId;
        final Cursor cursor = db.query(TABLE_FENCE_INFORMATION + " LEFT OUTER JOIN "+TABLE_FENCE_PARAMETER+" ON "+ TABLE_FENCE_INFORMATION+"."+COLUMN_FENCE_ID+"="+TABLE_FENCE_PARAMETER+"."+COLUMN_FENCE_ID, null, selection,null, null, null, null, null);
        //final Cursor cursor = db.rawQuery("SELECT * FROM fence_information LEFT OUTER JOIN fence_parameter ON fence_information.fence_id = fence_parameter.fence_id WHERE fence_information.fence_id=?",new String[]{fenceId});


        while (cursor.moveToNext()) {
            fence.setFenceId(Integer.parseInt(fenceId));
            fence.setCenterLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)));
            fence.setCenterLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            fence.setEdgeLat(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LAT)));
            fence.setEdgeLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_EDGE_LNG)));
            fence.setRadius(cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS)));
            fence.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setNotifyId(cursor.getInt(cursor.getColumnIndex(COLUMN_NOTIFY_ID)));
            fence.setOnDevice(cursor.getInt(cursor.getColumnIndex(COLUMN_ON_DEVICE)));
            fence.setLastEvent(cursor.getInt(cursor.getColumnIndex(COLUMN_LAST_EVENT)));
            fence.setIsActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)));
            fence.setAssignment(cursor.getString(cursor.getColumnIndex(COLUMN_ASSIGNMENT)));
            fence.setDistanceFromUser((int) cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE_FROM_USER)));
        }

        cursor.close();
        db.close();
        return fence;
    }

    public boolean updateFenceInformation(Fence fence) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FENCE_ID, fence.getFenceId());
        values.put(COLUMN_TITLE, fence.getTitle());
        values.put(COLUMN_DESCRIPTION, fence.getDescription());
        values.put(COLUMN_CENTER_LAT, (fence.getCenterLat()));
        values.put(COLUMN_CENTER_LNG, (fence.getCenterLng()));
        values.put(COLUMN_EDGE_LAT, (fence.getEdgeLat()));
        values.put(COLUMN_EDGE_LNG, (fence.getEdgeLng()));
        values.put(COLUMN_RADIUS, fence.getRadius());
        values.put(COLUMN_ASSIGNMENT, fence.getAssignment());
        values.put(COLUMN_ON_DEVICE, fence.getOnDevice());
        int result = db.update(TABLE_FENCE_INFORMATION, values, COLUMN_FENCE_ID + "=?", new String[]{String.valueOf(fence.getFenceId())});
        db.close();

        if (result > 0 && fence.getOnDevice() == 1) {
            updateFenceParameter(fence);
            return true;
        }
        else {
            return false;
        }

    }

    public boolean updateFenceParameter(Fence fence) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FENCE_ID, fence.getFenceId());
        values.put(COLUMN_NOTIFY_ID, fence.getNotifyId());
        values.put(COLUMN_LAST_EVENT, fence.getLastEvent());
        values.put(COLUMN_DISTANCE_FROM_USER, fence.getDistanceFromUser());
        values.put(COLUMN_IS_ACTIVE, fence.getIsActive());
        int result = db.update(TABLE_FENCE_PARAMETER, values, COLUMN_FENCE_ID + "=?", new String[]{String.valueOf(fence.getFenceId())});
        db.close();
        return result > 0;

    }

    public boolean updateFenceDistance(Fence fence) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, fence.getIsActive());
        values.put(COLUMN_DISTANCE_FROM_USER, fence.getDistanceFromUser());
        int result = db.update(TABLE_FENCE_PARAMETER, values, COLUMN_FENCE_ID + "=?", new String[]{String.valueOf(fence.getFenceId())});
        db.close();
        return result > 0;
    }

    public boolean updateFenceAssignment(int fenceId, String assignmentData) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ASSIGNMENT, assignmentData);
        int result = db.update(TABLE_FENCE_INFORMATION, values, COLUMN_FENCE_ID + "=?", new String[]{String.valueOf(fenceId)});
        db.close();
        return result > 0;
    }


    public boolean removeFenceFromDatabase(int id, int onDevice) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_FENCE_INFORMATION, COLUMN_FENCE_ID + "=" + id, null);
        if (result > 0 && onDevice == 1) {
            db.delete(TABLE_FENCE_PARAMETER, COLUMN_FENCE_ID + "=" + id, null);
            db.close();

            BackupManager bm = new BackupManager(context);
            bm.dataChanged();
            return true;
        }
        else {
            db.close();
            return false;
        }

    }

    public void savePendingEvent(Event event) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FENCE_ID, event.fenceId);
        values.put(COLUMN_EVENT_TYPE, event.eventType);
        values.put(COLUMN_IS_VERIFIED, event.isVerified);
        values.put(COLUMN_VERIFY_COUNT, event.verifyCount);
        values.put(COLUMN_RETRY_COUNT, event.retryCount);
        db.insert(TABLE_FENCE_EVENT, null, values);
        db.close();
    }

    public List<Event> getAllPendingEvents() {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COLUMN_IS_VERIFIED + " = " + 0;
        Cursor cursor = db.query(TABLE_FENCE_EVENT, null, selection, null, null, null, null);
        List<Event> pendingEvents = new ArrayList<>();
        while (cursor.moveToNext()) {
            Event event = new Event();
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.fenceId = cursor.getInt(cursor.getColumnIndex(COLUMN_FENCE_ID));
            event.eventType = cursor.getInt(cursor.getColumnIndex(COLUMN_EVENT_TYPE));
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
        String selection = COLUMN_FENCE_ID + " = " + requestId;
        Cursor cursor = db.query(TABLE_FENCE_EVENT, null, selection, null, null, null, null);
        return cursor != null ? cursor.getCount() : -1;
    }

    public Event getLastVerifiedEvent(int requestId) {
        FileLogger.e(TAG, "-- Begin Pending Event Removal --");

        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_FENCE_ID + " = " + requestId
                + " AND " + COLUMN_IS_VERIFIED + " = " + 1;
        Event event = new Event();
        Cursor cursor = db.query(TABLE_FENCE_EVENT, null, selection, null, null, null, COLUMN_ID + " DESC", "1");
        while (cursor.moveToNext()) {
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.fenceId = cursor.getInt(cursor.getColumnIndex(COLUMN_FENCE_ID));
            event.eventType = cursor.getInt(cursor.getColumnIndex(COLUMN_EVENT_TYPE));
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
        String selection = COLUMN_FENCE_ID + " = " + requestId
                + " AND " + COLUMN_IS_VERIFIED + " = " + 0;
        Event event = new Event();
        Cursor cursor = db.query(TABLE_FENCE_EVENT, null, selection, null, null, null, COLUMN_ID + " DESC", "1");
        while (cursor.moveToNext()) {
            event.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            event.fenceId = cursor.getInt(cursor.getColumnIndex(COLUMN_FENCE_ID));
            event.eventType = cursor.getInt(cursor.getColumnIndex(COLUMN_EVENT_TYPE));
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
        values.put(COLUMN_FENCE_ID, event.fenceId);
        values.put(COLUMN_EVENT_TYPE, event.eventType);
        values.put(COLUMN_IS_VERIFIED, event.isVerified);
        values.put(COLUMN_VERIFY_COUNT, event.verifyCount);
        values.put(COLUMN_RETRY_COUNT, event.retryCount);
        int result = db.update(TABLE_FENCE_EVENT, values, COLUMN_ID + "=?", new String[]{String.valueOf(event.id)});
        db.close();
        return result > 0;

    }

    public Boolean updateEventsAfterReboot() {
        SQLiteDatabase db = getWritableDatabase();
        //ContentValues values = new ContentValues();
        //values.put(COLUMN_IS_VERIFIED, 2); //Special value
        //int result = db.update(TABLE_FENCE_EVENT, values, null, null);
        int result = db.delete(TABLE_FENCE_EVENT, null, null);
        db.close();
        return result > 0;

    }

    public Boolean updateFencesAfterReboot() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_EVENT, 2); //Reset all fences to exit
        int result = db.update(TABLE_FENCE_PARAMETER, values, null, null);
        db.close();
        return result > 0;
    }

    public Boolean removeEvent(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_FENCE_EVENT, COLUMN_ID + "=? AND " + COLUMN_IS_VERIFIED + "=?", new String[]{String.valueOf(id), "0"});
        db.close();
        return result > 0;
    }

    public Boolean removeSimilarEvents(int requestId, int transitionType) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_FENCE_EVENT, COLUMN_FENCE_ID + "=? AND " + COLUMN_EVENT_TYPE + "=? AND " + COLUMN_IS_VERIFIED + "=? ", new String[]{String.valueOf(requestId), String.valueOf(transitionType), "0"});
        db.close();
        return result > 0;

    }


}
