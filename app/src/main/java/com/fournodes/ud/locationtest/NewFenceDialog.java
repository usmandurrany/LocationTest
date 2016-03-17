package com.fournodes.ud.locationtest;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class NewFenceDialog implements RemoteDevice {
    private Context context;
    private LatLng center;
    private Dialog dialog;
    private int radius;
    private List<Fence> mGeofenceList;
    private GoogleMap map;
    private CheckBox chkEntry;
    private CheckBox chkExit;
    private CheckBox chkRoaming;
    private Database db;
    private EditText edtTitle;
    private EditText edtDescription;
    private Spinner selectDevice;
    private DeviceLocator deviceLocator;


    public NewFenceDialog(Context context, LatLng center, int radius, List<Fence> mGeofenceList, GoogleMap map) {
        this.context = context;
        this.center = center;
        this.radius = radius;
        this.map=map;
        this.mGeofenceList=mGeofenceList;
        dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_new_fence);
    }

    public void details(){
        Button btnCancelFence = (Button) dialog.findViewById(R.id.btnCancelFence);
        Button btnSaveFence = (Button) dialog.findViewById(R.id.btnSaveFence);
        edtTitle = (EditText) dialog.findViewById(R.id.edtTitle);
        edtDescription = (EditText) dialog.findViewById(R.id.edtDescription);
        chkEntry = (CheckBox) dialog.findViewById(R.id.chkEntry);
        chkRoaming = (CheckBox) dialog.findViewById(R.id.chkRoaming);
        chkExit = (CheckBox) dialog.findViewById(R.id.chkExit);
        selectDevice = (Spinner) dialog.findViewById(R.id.selectDevice);

        deviceLocator = new DeviceLocator();
        deviceLocator.delegate=this;
        deviceLocator.execute("device_list");

        btnCancelFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnSaveFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtTitle.getText().length()>0 && !edtTitle.getText().toString().equals("My Device") && edtDescription.getText().length()>0 && (chkEntry.isChecked() || chkExit.isChecked() || chkRoaming.isChecked())){
                    if (getFenceId(edtTitle.getText().toString())==-1) {
                        db = new Database(context);
                        Fence fence = createFence(center, radius);
                        mGeofenceList.add(fence);

                  //((MainActivity)context).updateFenceCount();
                    LocationServices.GeofencingApi.addGeofences(
                            LocationService.mGoogleApiClient,
                            getGeofencingRequest(fence.getArea()),
                            fence.getPendingIntent());

                        dialog.dismiss();
                    }else
                        Toast.makeText(context, "You already have a fence called "+edtTitle.getText().toString(), Toast.LENGTH_SHORT).show();
                }
                else if (edtTitle.getText().toString().equals("My Device"))
                    Toast.makeText(context, "Title can not be My Device", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();

    }
    public Fence createFence(LatLng center, int radius) {
        if (radius == 0)
            radius = 1000;

        Marker centerMarker = map.addMarker(new MarkerOptions()
                .position(center)
                .snippet(edtDescription.getText().toString())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        .title(edtTitle.getText().toString()));

        Marker end = map.addMarker(new MarkerOptions()
                .position(toRadiusLatLng(center, radius))
                .draggable(true)
                .snippet(edtDescription.getText().toString())
                .alpha(0.4f)
                .title(edtTitle.getText().toString()));

        Circle circle = map.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .fillColor(0)
                .strokeColor(Color.parseColor("#000000"))
                .strokeWidth(3f)
        );

        Geofence geofence = new Geofence.Builder()
                .setRequestId(edtTitle.getText().toString())
                .setCircularRegion(
                        center.latitude,
                        center.longitude,
                        radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(getTransitionType())
                .setLoiteringDelay(500)
                .build();


        Fence fence = new Fence();
        fence.setArea(geofence);
        fence.setVisibleArea(circle);
        fence.setCenterMarker(centerMarker);
        fence.setEndMarker(end);
        fence.setRadius(radius);
        fence.setTitle(edtTitle.getText().toString());
        fence.setDescription(edtDescription.getText().toString());
        fence.setNotifyEntry(chkEntry.isChecked());
        fence.setNotifyExit(chkExit.isChecked());
        fence.setNotifyRoaming(chkRoaming.isChecked());
        fence.setNotifyDevice(selectDevice.getSelectedItem().toString());

        int id=(int)db.saveFence(fence);
        fence.setId(id);
        fence.setPendingIntent(getGeofencePendingIntent(id));

        return fence;

    }
    private PendingIntent getGeofencePendingIntent(int id) {
        // Reuse the PendingIntent if we already have it.

        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra("device",selectDevice.getSelectedItem().toString());
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(context, id, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }
    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / 6371009) / //Radius of earth in meters 6371009
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    public int getTransitionType(){
        // 1 - Entry
        // 2 - Exit
        // 4 - Dwell

        if (chkEntry.isChecked() && chkExit.isChecked() && chkRoaming.isChecked())
            return 1 | 2 | 4;
        else if (chkEntry.isChecked() && chkExit.isChecked())
            return 1 | 2;
        else if (chkEntry.isChecked() && chkRoaming.isChecked())
            return 1 | 4;
        else if(chkExit.isChecked() && chkRoaming.isChecked())
            return 2 | 4;
        else if (chkEntry.isChecked())
            return 1;
        else if (chkExit.isChecked())
            return 2;
        else if (chkRoaming.isChecked())
            return 4;
        else
            return -1;
    }

    public int getFenceId(String title) {
        for (int i = 0; i < mGeofenceList.size(); i++) {
            if (mGeofenceList.get(i).getTitle().equals(title))
                return mGeofenceList.indexOf(mGeofenceList.get(i));
        }
        return -1; //Error
    }

    @Override
    public void liveLocationUpdate(String lat, String lng, String device) {

    }

    @Override
    public void locationHistory(JSONArray location, String device) {

    }

    @Override
    public void deviceList(JSONArray devices) {
        String[] temp = new String[devices.length()];
        for(int i = 0; i < devices.length();i++){
            try {
                temp[i]=devices.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    selectDevice.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_list_item_1,temp));

    }
}
