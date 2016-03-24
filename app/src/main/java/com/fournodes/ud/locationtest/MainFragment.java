package com.fournodes.ud.locationtest;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.support.v7.widget.AppCompatCheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;


public class MainFragment extends Fragment implements FragmentInterface {

    private TextView txtLastUpdated;
    private TextView txtLat;
    private TextView txtLong;
    private List<Fence> mGeofenceList;
    private boolean isServiceRunning = false;
    private boolean isMyLocEnabled = false;
    private EditText edtUpdateInterval;
    private EditText edtMinDisplacement;
    private EditText edtUpdateServer;
    private Button btnService;


    public MainFragment() {}


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) getActivity()).delegate = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for getContext() fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).delegate = this;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /********************** Fragment Code ********************/
        CustomFrameLayout floatingMarker = (CustomFrameLayout) getView().findViewById(R.id.floatingMarker);
        btnService = (Button) getView().findViewById(R.id.btnService);
        Button btnSetUpdateInterval = (Button) getView().findViewById(R.id.btnSetUpdateInterval);
        Button btnSetDisplacement = (Button) getView().findViewById(R.id.btnSetDisplacement);
        Button btnSetUpdateServer = (Button) getView().findViewById(R.id.btnSetUpdateServer);
        final AppCompatCheckBox chkPolling = (AppCompatCheckBox) getView().findViewById(R.id.chkPolling);
        final AppCompatCheckBox chkUpdateServer = (AppCompatCheckBox) getView().findViewById(R.id.chkUpdateServer);
        final AppCompatCheckBox chkActiveMode = (AppCompatCheckBox) getView().findViewById(R.id.chkActiveMode);
        txtLat = (TextView) getView().findViewById(R.id.txtLat);
        txtLong = (TextView) getView().findViewById(R.id.txtLong);
        txtLastUpdated = (TextView) getView().findViewById(R.id.txtLastUpdated);

        edtUpdateInterval = (EditText) getView().findViewById(R.id.edtUpdateInterval);
        edtMinDisplacement = (EditText) getView().findViewById(R.id.edtDisplacement);
        edtUpdateServer = (EditText) getView().findViewById(R.id.edtUpdateServer);

        edtUpdateInterval.setText(String.valueOf(SharedPrefs.getLocUpdateInterval()));
        edtMinDisplacement.setText(String.valueOf(SharedPrefs.getMinDisplacement()));
        edtUpdateServer.setText(String.valueOf(SharedPrefs.getUpdateServerInterval()));


        if(LocationService.isRunning){
            isServiceRunning = true;
            btnService.setText("Stop Service");
        }

        btnSetUpdateServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtUpdateServer.getText().length()>0 &&(Integer.parseInt(edtUpdateServer.getText().toString()) >= 60000)) {
                    SharedPrefs.setUpdateServerInterval(Integer.parseInt(edtUpdateServer.getText().toString()));
                    Toast.makeText(getContext(), "Value updated to "+String.valueOf(SharedPrefs.getUpdateServerInterval()), Toast.LENGTH_SHORT).show();

                }
                else if (Integer.parseInt(edtUpdateServer.getText().toString()) < 60000)
                    Toast.makeText(getContext(), "Minimum interval for server update is 60000 (1 Minute)", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getContext(), "Field is empty", Toast.LENGTH_SHORT).show();
            }
        });



        btnService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServiceRunning) {
                    getActivity().startService(new Intent(getContext(), LocationService.class));
                    btnService.setText("Stop Service");
                    isServiceRunning = true;
                } else if (isServiceRunning) {
                    LocationService.mNotificationManager.cancel(0);
                    getActivity().stopService(new Intent(getContext(), LocationService.class));
                    isServiceRunning = false;
                    btnService.setText("Start Service");

                }

            }
        });

        btnSetUpdateInterval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtUpdateInterval.getText().length() > 0){
                    SharedPrefs.setLocUpdateInterval(Integer.parseInt(edtUpdateInterval.getText().toString()));
                    Toast.makeText(getContext(), "Value Updated to " + String.valueOf(SharedPrefs.getLocUpdateInterval()), Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(getContext(), "Field is empty", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetDisplacement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtMinDisplacement.getText().length() > 0) {
                    SharedPrefs.setMinDisplacement(Integer.parseInt(edtMinDisplacement.getText().toString()));
                    Toast.makeText(getContext(), "Value Updated to " + String.valueOf(SharedPrefs.getMinDisplacement()), Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(getContext(), "Field is empty", Toast.LENGTH_SHORT).show();
            }
        });

        if (SharedPrefs.isPollingEnabled())
            chkPolling.setChecked(true);
        if (SharedPrefs.isUpdateServerEnabled())
            chkUpdateServer.setChecked(true);


        if (chkPolling.isChecked())
            chkUpdateServer.setEnabled(true);
        else
            chkUpdateServer.setEnabled(false);

        chkUpdateServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    SharedPrefs.setUpdateServerEnabled(true);
                }else
                {
                    SharedPrefs.setUpdateServerEnabled(false);
                }

            }
        });

        chkPolling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    SharedPrefs.setPollingEnabled(true);
                    serviceMessage("trackEnabled");

                    chkUpdateServer.setEnabled(true);
                    chkActiveMode.setEnabled(true);

                }else
                {
                    serviceMessage("trackDisabled");
                    SharedPrefs.setPollingEnabled(false);
                    chkUpdateServer.setEnabled(false);
                    chkUpdateServer.setChecked(false);
                    chkActiveMode.setChecked(false);
                    chkActiveMode.setEnabled(false);

                }
            }
        });
        chkActiveMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && LocationService.isRunning){
                    serviceMessage("switchToActiveMode");
                }else if (!isChecked)
                    serviceMessage("switchToPassiveMode");
                else
                    Toast.makeText(getContext(), "Service is not running", Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void mapDragStart() {

    }

    @Override
    public void mapDragStop() {

    }

    @Override
    public void moveToFence(LatLng fence) {

    }

    @Override
    public void viewLiveLocation(LatLng coordinates, String track_id) {

    }

    @Override
    public void viewLocationHistory(JSONArray location) {

    }

    @Override
    public void serviceStarted() {
        btnService.setText("Stop Service");
        isServiceRunning = true;

    }

    @Override
    public void serviceStopped() {
        btnService.setText("Start Service");
        isServiceRunning = false;

    }

    @Override
    public void locationUpdated(String lat, String lng, String time) {
        txtLastUpdated.append(time);
        txtLat.append(lat);
        txtLong.append(lng);
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }


}
