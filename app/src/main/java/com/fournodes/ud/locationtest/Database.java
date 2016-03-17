package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.google.android.gms.location.Geofence;
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
    private static final String TAG = "Datanase";
    private Context context;
    public static final String DATABASE_NAME = "LocationTest";
    public static final int DATABASE_VERSION = 3;

    public static final String COLUMN_ID = "id";

    public static final String TABLE_GEOFENCE = "geofence";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CENTER_LAT = "center_lat";
    public static final String COLUMN_CENTER_LNG = "center_lng";
    public static final String COLUMN_END_LAT = "end_lat";
    public static final String COLUMN_END_LNG = "end_lng";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_TRANSITION_TYPE = "transition";
    public static final String COLUMN_NOTIFY_DEVICE = "notify_device";

    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_LOC_LAT = "loc_lat";
    public static final String COLUMN_LOC_LNG = "loc_lng";
    public static final String COLUMN_DISPLACEMENT = "displacement";
    public static final String COLUMN_TIME = "time";



    // Database creation sql statement
    private static final String SQL_CREATE_TABLE_GEOFENCE = "create table if not exists "
            + TABLE_GEOFENCE + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_CENTER_LAT + " double not null, "
            + COLUMN_CENTER_LNG + " double not null, "
            + COLUMN_END_LAT + " double not null, "
            + COLUMN_END_LNG + " double not null, "
            + COLUMN_RADIUS + " float not null,"
            + COLUMN_NOTIFY_DEVICE + " text not null,"
            + COLUMN_TRANSITION_TYPE + " integer not null);";

    private static final String SQL_CREATE_TABLE_LOC = "create table if not exists "
            + TABLE_LOCATION + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LOC_LAT + " double not null, "
            + COLUMN_LOC_LNG + " double not null, "
            + COLUMN_DISPLACEMENT + " float not null,"
            + COLUMN_TIME + " bigint not null);";


    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(SQL_CREATE_TABLE_GEOFENCE);
        database.execSQL(SQL_CREATE_TABLE_LOC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.e(TAG,"Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_GEOFENCE);
        onCreate(database);
    }
    public long saveFence(Fence fence){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE,fence.getTitle());
        values.put(COLUMN_DESCRIPTION,fence.getDescription());
        values.put(COLUMN_CENTER_LAT,fence.getCenterMarker().getPosition().latitude);
        values.put(COLUMN_CENTER_LNG,fence.getCenterMarker().getPosition().longitude);
        values.put(COLUMN_END_LAT,fence.getEndMarker().getPosition().latitude);
        values.put(COLUMN_END_LNG,fence.getEndMarker().getPosition().longitude);
        values.put(COLUMN_RADIUS,fence.getRadius());
        values.put(COLUMN_NOTIFY_DEVICE,fence.getNotifyDevice());
        values.put(COLUMN_TRANSITION_TYPE,fence.getTransitionType());
        long rowId=db.insert(TABLE_GEOFENCE,null,values);
        db.close();
        return rowId;
    }

    public List<Fence> getFences(GoogleMap map){
     SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_GEOFENCE,null,null,null,null,null,null,null);

        List<Fence> mFenceList = new ArrayList<>();

        while(cursor.moveToNext()){
            int id =cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            LatLng center = new LatLng( cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LAT)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_CENTER_LNG)));
            LatLng end = new LatLng( cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LAT)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_END_LNG)));

            float radius = cursor.getFloat(cursor.getColumnIndex(COLUMN_RADIUS));

            int transition = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSITION_TYPE));


            Fence fence = new Fence();
            fence.setId(id);
            fence.setRadius(radius);
            fence.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            fence.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            fence.setNotifyDevice(cursor.getString(cursor.getColumnIndex(COLUMN_NOTIFY_DEVICE)));

            fence.setCenterMarker(
                    map.addMarker(new MarkerOptions()
                            .position(center)
                            .snippet(fence.getDescription())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(fence.getTitle())));

            fence.setEndMarker(
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

            fence.setArea(new Geofence.Builder()
                    .setRequestId(String.valueOf(id))
                    .setCircularRegion(
                            center.latitude,
                            center.longitude,
                            radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(transition)
                    .setLoiteringDelay(120000) // 2 mins
                    .build());

            fence.setPendingIntent(getGeofencePendingIntent(id,fence.getNotifyDevice()));
            mFenceList.add(fence);
        }
        cursor.close();
        db.close();
        return mFenceList;

    }

    private PendingIntent getGeofencePendingIntent(int intentId, String device) {
        // Reuse the PendingIntent if we already have it.

        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra("device", device);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(context, intentId, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }


    public boolean removeFence(int id){
        SQLiteDatabase db = getWritableDatabase();
        int result =db.delete(TABLE_GEOFENCE, COLUMN_ID + "=" + id, null);
        db.close();
        return result > 0;
    }

    public long saveLocation(double lat, double lng, long time){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        Float displacement;
        String lastLat =SharedPrefs.getLastDeviceLatitude();
        String lastLng =SharedPrefs.getLastDeviceLongitude();

        if (lastLat == null && lastLng == null )
            displacement = 0f;
        else{
            displacement= toRadiusMeters(new LatLng(Double.parseDouble(lastLat),Double.parseDouble(lastLng)),
                    new LatLng(lat,lng));
        }

        values.put(COLUMN_LOC_LAT,lat);
        values.put(COLUMN_LOC_LNG,lng);
        values.put(COLUMN_DISPLACEMENT,displacement);
        values.put(COLUMN_TIME,time);
        long rowId=db.insert(TABLE_LOCATION,null,values);
        db.close();
        return rowId;
    }
    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    public JSONArray getLocEntries(long time){
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION,null,COLUMN_TIME+"<?",new String[]{String.valueOf(time)},null,null,null);
        List<JSONObject> rowList = new ArrayList<>();
        while (cursor.moveToNext()){
            HashMap<String,Double> row = new HashMap<>();
            row.put("latitude",cursor.getDouble(cursor.getColumnIndex(COLUMN_LOC_LAT)));
            row.put("longitude",cursor.getDouble(cursor.getColumnIndex(COLUMN_LOC_LNG)));
            row.put("displacement",cursor.getDouble(cursor.getColumnIndex(COLUMN_DISPLACEMENT)));
            row.put("time",cursor.getDouble(cursor.getColumnIndex(COLUMN_TIME)));
            rowList.add(new JSONObject(row));
        }
        cursor.close();
        db.close();
        return new JSONArray(rowList);
    }

    public boolean removeLocEntries(long time){
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_LOCATION,COLUMN_TIME+"<?",new String[]{String.valueOf(time)});
        db.close();
        return result > 0;
    }
    public int getLocEntriesCount(){
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION,null,null,null,null,null,null);
        if (cursor != null)
            return cursor.getCount();
        else
            return -1; //Error
    }

    public void updateFence(Fence fence){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RADIUS,fence.getRadius());
        values.put(COLUMN_END_LAT,fence.getEndMarker().getPosition().latitude);
        values.put(COLUMN_END_LNG,fence.getEndMarker().getPosition().longitude);
        db.update(TABLE_GEOFENCE,values,COLUMN_ID+"=?",new String[]{String.valueOf(fence.getId())});
        db.close();
    }
}
