package com.fournodes.ud.locationtest.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.adapters.UserAdapter;
import com.fournodes.ud.locationtest.apis.FenceApi;
import com.fournodes.ud.locationtest.apis.TrackApi;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.interfaces.TrackApiResult;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class CreateFenceDialog implements TrackApiResult, RequestResult {
    private Activity activity;
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
    private TrackApi trackApi;
    private Fence fence;


    public CreateFenceDialog(Activity activity, LatLng center, int radius, List<Fence> mGeofenceList, GoogleMap map) {
        this.activity = activity;
        this.center = center;
        this.radius = radius;
        this.map = map;
        this.mGeofenceList = mGeofenceList;
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_new_fence);
        db = new Database(activity);
    }

    public void details() {
        Button btnCancelFence = (Button) dialog.findViewById(R.id.btnCancelFence);
        Button btnSaveFence = (Button) dialog.findViewById(R.id.btnSaveFence);
        edtTitle = (EditText) dialog.findViewById(R.id.edtTitle);
        edtDescription = (EditText) dialog.findViewById(R.id.edtDescription);
        chkEntry = (CheckBox) dialog.findViewById(R.id.chkEntry);
        chkRoaming = (CheckBox) dialog.findViewById(R.id.chkRoaming);
        chkExit = (CheckBox) dialog.findViewById(R.id.chkExit);
        selectDevice = (Spinner) dialog.findViewById(R.id.selectDevice);

        chkEntry.setChecked(true);
        chkExit.setChecked(true);

        trackApi = new TrackApi();
        trackApi.delegate = this;
        trackApi.execute("user_id=" + SharedPrefs.getUserId(), "user_list");

        btnCancelFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnSaveFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtTitle.getText().length() > 0 && !edtTitle.getText().toString().equals("My Device") && edtDescription.getText().length() > 0 && (chkEntry.isChecked() || chkExit.isChecked() || chkRoaming.isChecked())) {
                    if (getFenceId(edtTitle.getText().toString()) == -1) {
                        createFence(center, radius);
                    }
                    else
                        Toast.makeText(activity, "You already have a fence called " + edtTitle.getText().toString(), Toast.LENGTH_SHORT).show();
                }
                else if (edtTitle.getText().toString().equals("My Device"))
                    Toast.makeText(activity, "Title can not be My Device", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(activity, "All fields are required", Toast.LENGTH_SHORT).show();
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


        fence = new Fence();
        fence.setOnDevice(0);
        fence.setVisibleArea(circle);
        fence.setCenterMarker(centerMarker);
        fence.setEdgeMarker(end);
        fence.setRadius(radius);
        fence.setTitle(edtTitle.getText().toString());
        fence.setDescription(edtDescription.getText().toString());
        fence.setTransitionType(getTransitionType());
        fence.setUserId(SharedPrefs.getUserId());
        fence.setCreate_on(String.valueOf(selectDevice.getSelectedItemId()));

        StringBuilder fenceDetails = new StringBuilder();
        try {
            fenceDetails
                    .append("title=").append(URLEncoder.encode(fence.getTitle(), "UTF-8"))
                    .append("&description=").append(URLEncoder.encode(fence.getDescription(), "UTF-8"))
                    .append("&center_latitude=").append(center.latitude)
                    .append("&center_longitude=").append(center.longitude)
                    .append("&radius=").append(radius)
                    .append("&edge_latitude=").append(toRadiusLatLng(center, radius).latitude)
                    .append("&edge_longitude=").append(toRadiusLatLng(center, radius).longitude)
                    .append("&transition_type=").append(fence.getTransitionType())
                    .append("&user_id=").append(SharedPrefs.getUserId())
                    .append("&create_on=").append(selectDevice.getSelectedItemId());


            FenceApi fenceApi = new FenceApi();
            fenceApi.delegate = this;
            fenceApi.execute(fenceDetails.toString(), "create_fence");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fence;

    }

    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / 6371009) / //Radius of earth in meters 6371009
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    public int getFenceId(String title) {
        for (int i = 0; i < mGeofenceList.size(); i++) {
            if (mGeofenceList.get(i).getTitle().equals(title))
                return mGeofenceList.indexOf(mGeofenceList.get(i));
        }
        return -1; //Error
    }

    @Override
    public void liveLocationUpdate(String lat, String lng, String track_id) {}

    @Override
    public void locationHistory(JSONArray location) {}

    @Override
    public void userList(JSONArray users) {
        UserAdapter userAdapter = new UserAdapter(activity, null, R.layout.list_item_user, users);
        selectDevice.setAdapter(userAdapter);
    }

    @Override
    public void onSuccess(String result) {
        if (fence != null) {
            fence.setId(Integer.parseInt(result));
            db.saveFence(fence);
            mGeofenceList.add(fence);
            dialog.dismiss();
        }
    }

    @Override
    public void onFailure() {
        Toast.makeText(activity, "Fence not created. Try again", Toast.LENGTH_SHORT).show();
    }

    public int getTransitionType() {
        // 1 - Entry
        // 2 - Exit
        // 4 - Dwell

        if (chkEntry.isChecked() && chkExit.isChecked() && chkRoaming.isChecked())
            return (1 | 2 | 4);
        else if (chkEntry.isChecked() && chkExit.isChecked())
            return 1 | 2;
        else if (chkEntry.isChecked() && chkRoaming.isChecked())
            return 1 | 4;
        else if (chkExit.isChecked() && chkRoaming.isChecked())
            return 2 | 4;
        else if (chkEntry.isChecked())
            return 1;
        else if (chkExit.isChecked())
            return 2;
        else if (chkRoaming.isChecked())
            return 4;
        else
            return -1; //Error
    }
}
