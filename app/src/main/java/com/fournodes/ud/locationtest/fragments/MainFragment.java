package com.fournodes.ud.locationtest.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.CustomFrameLayout;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.interfaces.MainFragmentInterface;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.services.LocationService;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;

import java.util.List;

import io.fabric.sdk.android.Fabric;


public class MainFragment extends Fragment implements MainFragmentInterface {

    private TextView txtLog;
    private boolean isServiceRunning = false;
    private Button btnService;
    private ScrollView lytScrollLog;
    private TextView txtCameraPermission;


    public MainFragment() { }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) getActivity()).mainDelegate = this;
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
        Fabric.with(getContext(), new Crashlytics());
        ((MainActivity) getActivity()).mainDelegate = this;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CustomFrameLayout floatingMarker = (CustomFrameLayout) getView().findViewById(R.id.floatingMarker);

        btnService = (Button) getView().findViewById(R.id.btnService);
        Button btnShareLogFile = (Button) getView().findViewById(R.id.btnShareLogFile);
        Button btnClearLogFile = (Button) getView().findViewById(R.id.btnClearLogFile);
        Button btnResetAll = (Button) getView().findViewById(R.id.btnRemoveAll);
        Button btnSetVicinity = (Button) getView().findViewById(R.id.btnSetVicinity);
        Button btnSetDistanceThreshold = (Button) getView().findViewById(R.id.btnSetDistanceThreshold);
        Button btnSetFencePerimeterPercentage = (Button) getView().findViewById(R.id.btnSetFencePerimeterPercentage);
        txtCameraPermission = (TextView) getView().findViewById(R.id.txtCameraPermission);
        txtCameraPermission.append(String.valueOf(isPermissionGranted()));

        final EditText edtVicinity = (EditText) getView().findViewById(R.id.edtVicinity);
        final EditText edtDistanceThreshold = (EditText) getView().findViewById(R.id.edtDistanceThreshold);
        final EditText edtFencePerimeterPercentage = (EditText) getView().findViewById(R.id.edtFencePerimeterPercentage);

        edtVicinity.setText(String.valueOf(SharedPrefs.getVicinity()));
        edtDistanceThreshold.setText(String.valueOf(SharedPrefs.getDistanceThreshold()));
        edtFencePerimeterPercentage.setText(String.valueOf(SharedPrefs.getFencePerimeterPercentage()));


        txtLog = (TextView) getView().findViewById(R.id.txtLog);
        lytScrollLog = (ScrollView) getView().findViewById(R.id.lytScrollLog);


        if (LocationService.isServiceRunning) {
            isServiceRunning = true;
            btnService.setText("Stop Service");
        }


        btnService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServiceRunning) {
                    getActivity().startService(new Intent(getContext(), LocationService.class));
                    btnService.setText("Stop Service");
                    isServiceRunning = true;
                }
                else if (isServiceRunning) {
                    getActivity().stopService(new Intent(getContext(), LocationService.class));
                    isServiceRunning = false;
                    btnService.setText("Start Service");

                }

            }
        });

        btnClearLogFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FileLogger.deleteFile())
                    Toast.makeText(getActivity(), "Log cleared", Toast.LENGTH_SHORT).show();
                else if (LocationService.isServiceRunning)
                    Toast.makeText(getActivity(), "Cant clear log while service is running", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "File doesn't exists", Toast.LENGTH_SHORT).show();
            }
        });

        btnShareLogFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(FileLogger.logFile));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });


        btnResetAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SharedPrefs.getLastDeviceLatitude() != null) {
                    Database db = new Database(getContext());
                    db.resetAll();
                    serviceMessage("calcDistance");
                }
                else
                    Toast.makeText(getContext(), "Can't reset, Location not availble. Try again later", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetVicinity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPrefs.setVicinity(Integer.parseInt(edtVicinity.getText().toString()));
                Toast.makeText(getContext(), "Vicinity value set", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetDistanceThreshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPrefs.setDistanceThreshold(Integer.parseInt(edtDistanceThreshold.getText().toString()));
                Toast.makeText(getContext(), "Distance threshold value set", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetFencePerimeterPercentage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPrefs.setFencePerimeterPercentage(Integer.parseInt(edtFencePerimeterPercentage.getText().toString()));
                Toast.makeText(getContext(), "Fence perimeter percentage value set", Toast.LENGTH_SHORT).show();

            }
        });

    }


    public boolean isPermissionGranted() {


        Log.e("Camera Permission", String.valueOf(PermissionChecker.checkCallingOrSelfPermission(getContext(), Manifest.permission.CAMERA)));

        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            return false;

        } else
            return true;


    }

    @Override
    public void mapDragStart() {

    }

    @Override
    public void mapDragStop() {

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
    public void fenceTriggered(String data) {
        StringBuilder fenceLog = new StringBuilder()
                .append("Fence: ").append(data).append("\n");
        txtLog.append(fenceLog.toString());
        lytScrollLog.scrollTo(0, View.FOCUS_DOWN);

    }

    @Override
    public void locationUpdated(String lat, String lng, String time) {
        StringBuilder locationLog = new StringBuilder()
                .append("Time: ").append(time).append("\n")
                .append("Lat: ").append(lat).append(" - ")
                .append("Lng: ").append(lng).append("\n");
        txtLog.append(locationLog.toString());
        lytScrollLog.scrollTo(0, View.FOCUS_DOWN);

    }

    @Override
    public void listenerLocation(Location location) {

    }

    @Override
    public void updateServer(String result) {
        StringBuilder updateLog = new StringBuilder()
                .append("Network: ").append(result).append("\n");
        txtLog.append(updateLog.toString());
        lytScrollLog.scrollTo(0, View.FOCUS_DOWN);
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }


    @Override
    public void activeFenceList(List<Fence> fenceListActive, String className) {

    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {

    }
}
