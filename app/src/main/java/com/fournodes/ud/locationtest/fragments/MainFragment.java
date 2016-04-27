package com.fournodes.ud.locationtest.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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

import java.io.File;
import java.util.List;

import io.fabric.sdk.android.Fabric;


public class MainFragment extends Fragment implements MainFragmentInterface {

    private TextView txtLog;
    private boolean isServiceRunning = false;
    private Button btnService;
    private ScrollView lytScrollLog;


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
        /********************** Fragment Code ********************/
        CustomFrameLayout floatingMarker = (CustomFrameLayout) getView().findViewById(R.id.floatingMarker);

        btnService = (Button) getView().findViewById(R.id.btnService);
        Button btnShareLogFile = (Button) getView().findViewById(R.id.btnShareLogFile);
        Button btnClearLogFile = (Button) getView().findViewById(R.id.btnClearLogFile);
        Button btnResetAll = (Button) getView().findViewById(R.id.btnRemoveAll);
        Button btnSetVicinity = (Button) getView().findViewById(R.id.btnSetVicinity);
        Button btnSetDistanceThreshold = (Button) getView().findViewById(R.id.btnSetDistanceThreshold);

        final EditText edtVicinity = (EditText) getView().findViewById(R.id.edtVicinity);
        final EditText edtDistanceThreshold = (EditText) getView().findViewById(R.id.edtDistanceThreshold);

        edtVicinity.setText(String.valueOf(SharedPrefs.getVicinity()));
        edtDistanceThreshold.setText(String.valueOf(SharedPrefs.getDistanceThreshold()));


        txtLog = (TextView) getView().findViewById(R.id.txtLog);
        lytScrollLog = (ScrollView) getView().findViewById(R.id.lytScrollLog);


        if (LocationService.isRunning) {
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
                File file = new File("sdcard/" + FileLogger.LOG_FILE_NAME);
                if (file.exists() && !LocationService.isRunning)
                    file.delete();
                else if (LocationService.isRunning)
                    Toast.makeText(getActivity(), "Cant clear log while service is running", Toast.LENGTH_SHORT).show();
                else if (!file.exists())
                    Toast.makeText(getActivity(), "File doesn't exists", Toast.LENGTH_SHORT).show();
            }
        });

        btnShareLogFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("sdcard/" + FileLogger.LOG_FILE_NAME)));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });


        btnResetAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Database db = new Database(getContext());
                db.resetAll();
                serviceMessage("calcDistance");
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
    public void activeFenceList(List<Fence> fenceListActive) {

    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {

    }
}
