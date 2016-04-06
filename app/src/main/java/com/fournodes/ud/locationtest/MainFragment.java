package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.LocationService;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;


public class MainFragment extends Fragment implements MainFragmentInterface {

    private TextView txtLastUpdated;
    private TextView txtLog;
    private TextView txtLong;
    private List<Fence> mGeofenceList;
    private boolean isServiceRunning = false;
    private boolean isMyLocEnabled = false;
    private EditText edtUpdateInterval;
    private EditText edtMinDisplacement;
    private EditText edtUpdateServer;
    private Button btnService;
    private Button btnShareLogFile;
    private Button btnClearLogFile;
    private Button btnInspectDb;
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
        ((MainActivity) getActivity()).mainDelegate = this;

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
        txtLog = (TextView) getView().findViewById(R.id.txtLog);


        edtUpdateInterval = (EditText) getView().findViewById(R.id.edtUpdateInterval);
        edtMinDisplacement = (EditText) getView().findViewById(R.id.edtDisplacement);
        edtUpdateServer = (EditText) getView().findViewById(R.id.edtUpdateServer);

        edtUpdateInterval.setText(String.valueOf(SharedPrefs.getLocUpdateInterval()));
        edtMinDisplacement.setText(String.valueOf(SharedPrefs.getMinDisplacement()));
        edtUpdateServer.setText(String.valueOf(SharedPrefs.getUpdateServerInterval()));

        btnShareLogFile = (Button) getView().findViewById(R.id.btnShareLogFile);
        btnClearLogFile = (Button) getView().findViewById(R.id.btnClearLogFile);
        btnInspectDb = (Button) getView().findViewById(R.id.btnInspectDb);
        lytScrollLog = (ScrollView) getView().findViewById(R.id.lytScrollLog);


        if (LocationService.isRunning) {
            isServiceRunning = true;
            btnService.setText("Stop Service");
        }

        btnSetUpdateServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtUpdateServer.getText().length() > 0 && (Integer.parseInt(edtUpdateServer.getText().toString()) >= 60000)) {
                    SharedPrefs.setUpdateServerInterval(Integer.parseInt(edtUpdateServer.getText().toString()));
                    Toast.makeText(getContext(), "Value updated to " + String.valueOf(SharedPrefs.getUpdateServerInterval()), Toast.LENGTH_SHORT).show();

                } else if (Integer.parseInt(edtUpdateServer.getText().toString()) < 60000)
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

        btnClearLogFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File("sdcard/location_log.txt");
                if (file.exists() && !LocationService.isRunning)
                    file.delete();
                else if(LocationService.isRunning)
                    Toast.makeText(getActivity(), "Cant clear log while service is running", Toast.LENGTH_SHORT).show();
                else if(!file.exists())
                    Toast.makeText(getActivity(), "File doesn't exists", Toast.LENGTH_SHORT).show();
            }
        });

        btnShareLogFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("sdcard/location_log.txt")));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

        btnInspectDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btnSetUpdateInterval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtUpdateInterval.getText().length() > 0) {
                    SharedPrefs.setLocUpdateInterval(Integer.parseInt(edtUpdateInterval.getText().toString()));
                    Toast.makeText(getContext(), "Value Updated to " + String.valueOf(SharedPrefs.getLocUpdateInterval()), Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getContext(), "Field is empty", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetDisplacement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtMinDisplacement.getText().length() > 0) {
                    SharedPrefs.setMinDisplacement(Integer.parseInt(edtMinDisplacement.getText().toString()));
                    Toast.makeText(getContext(), "Value Updated to " + String.valueOf(SharedPrefs.getMinDisplacement()), Toast.LENGTH_SHORT).show();
                } else
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
                if (isChecked) {
                    SharedPrefs.setUpdateServerEnabled(true);
                } else {
                    SharedPrefs.setUpdateServerEnabled(false);
                }

            }
        });

        chkPolling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    SharedPrefs.setPollingEnabled(true);
                    serviceMessage("trackEnabled");

                    chkUpdateServer.setEnabled(true);
                    chkActiveMode.setEnabled(true);

                } else {
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
                if (isChecked && LocationService.isRunning) {
                    serviceMessage("switchToActiveMode");
                } else if (!isChecked)
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
        lytScrollLog.scrollTo(0,View.FOCUS_DOWN);

    }

    @Override
    public void locationUpdated(String lat, String lng, String time) {
        StringBuilder locationLog = new StringBuilder()
                .append("Time: ").append(time).append("\n")
                .append("Lat: ").append(lat).append(" - ")
                .append("Lng: ").append(lng).append("\n");
        txtLog.append(locationLog.toString());
        lytScrollLog.scrollTo(0,View.FOCUS_DOWN);

    }

    @Override
    public void updateServer(String result) {
        StringBuilder updateLog = new StringBuilder()
                .append("Network: ").append(result).append("\n");
        txtLog.append(updateLog.toString());
        lytScrollLog.scrollTo(0,View.FOCUS_DOWN);
    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }


}
