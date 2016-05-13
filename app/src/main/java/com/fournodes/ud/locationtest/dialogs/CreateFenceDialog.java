package com.fournodes.ud.locationtest.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fournodes.ud.locationtest.MultiSpinner;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.adapters.UserAdapter;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class CreateFenceDialog implements RequestResult, MultiSpinner.MultiSpinnerListener {
    private Activity activity;
    private LatLng center;
    private Dialog dialog;
    private int radius;
    private List<Fence> mGeofenceList;
    private GoogleMap map;
    private Database db;
    private EditText edtTitle;
    private EditText edtDescription;
    private MultiSpinner selectDevice;
    private Fence fence;
    private IncomingApi incomingApi;
    private Button btnSaveFence;
    private JSONObject assignment;


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
        btnSaveFence = (Button) dialog.findViewById(R.id.btnSaveFence);
        edtTitle = (EditText) dialog.findViewById(R.id.edtTitle);
        edtDescription = (EditText) dialog.findViewById(R.id.edtDescription);
        selectDevice = (MultiSpinner) dialog.findViewById(R.id.selectDevice);



        String payload = "user_id=" + SharedPrefs.getUserId();
        incomingApi = new IncomingApi(null, "user_list", payload, 0);
        incomingApi.delegate = this;
        incomingApi.execute();

        btnCancelFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnSaveFence.setEnabled(false);
        btnSaveFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtTitle.getText().length() > 0 && !edtTitle.getText().toString().equals("My Device") && edtDescription.getText().length() > 0) {
                    if (getFenceId(edtTitle.getText().toString()) == -1) {
                        if (createFence(center, radius) == null) {
                            Toast.makeText(activity, "Something went work, try again", Toast.LENGTH_SHORT).show();
                        }
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
        try {
            if (radius == 0)
                radius = 1000;

            Marker centerMarker = map.addMarker(new MarkerOptions()
                    .position(center)
                    .snippet(edtDescription.getText().toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(edtTitle.getText().toString()));

            Marker edgeMarker = map.addMarker(new MarkerOptions()
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
            fence.setTitle(edtTitle.getText().toString());
            fence.setDescription(edtDescription.getText().toString());
            fence.setCenterLat(centerMarker.getPosition().latitude);
            fence.setCenterLng(centerMarker.getPosition().longitude);
            fence.setEdgeLat(edgeMarker.getPosition().latitude);
            fence.setEdgeLng(edgeMarker.getPosition().longitude);
            fence.setRadius(radius);
            fence.setCenterMarker(centerMarker);
            fence.setEdgeMarker(edgeMarker);
            fence.setCircle(circle);
            fence.setNotifyId(Integer.parseInt(SharedPrefs.getUserId()));
            fence.setOnDevice(0);
            fence.setAssignment(assignment.toString());


            StringBuilder payload = new StringBuilder();
            payload
                    .append("title=").append(URLEncoder.encode(fence.getTitle(), "UTF-8"))
                    .append("&description=").append(URLEncoder.encode(fence.getDescription(), "UTF-8"))
                    .append("&center_latitude=").append(center.latitude)
                    .append("&center_longitude=").append(center.longitude)
                    .append("&radius=").append(radius)
                    .append("&edge_latitude=").append(toRadiusLatLng(center, radius).latitude)
                    .append("&edge_longitude=").append(toRadiusLatLng(center, radius).longitude)
                    .append("&user_id=").append(SharedPrefs.getUserId())
                    .append("&assignment_data=").append(assignment);


            incomingApi = new IncomingApi(null, "create_fence", payload.toString(), 0);
            incomingApi.delegate = this;
            incomingApi.execute();
            return fence;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
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
    public void liveLocationUpdate(String lat, String lng, String time, String track_id) {

    }

    @Override
    public void locationHistory(List<Coordinate> coordinates) {

    }


    @Override
    public void userList(List<User> users) {
        UserAdapter userAdapter = new UserAdapter(activity, null, R.layout.list_item_user_transition_type, users);
        selectDevice.setUserAdapter(userAdapter, this);
    }

    @Override
    public void trackEnabled() {

    }

    @Override
    public void trackDisabled() {

    }

    @Override
    public void onSuccess(String result) {
        if (fence != null) {
            fence.setFenceId(Integer.parseInt(result));
            db.saveFenceInformation(fence);
            mGeofenceList.add(fence);
            dialog.dismiss();
        }
    }

    @Override
    public void onFailure() {
        Toast.makeText(activity, "Fence not created. Try again", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onItemsSelected(JSONObject temp, List<String> selectedUserIds, List<String> selectedUserNames) {
        this.assignment = temp;
        btnSaveFence.setEnabled(true);
        FileLogger.e("data", temp.toString());


    }
}
