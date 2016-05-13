package com.fournodes.ud.locationtest.fragments;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.adapters.MultilineInfoWindowAdapter;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.dialogs.CreateFenceDialog;
import com.fournodes.ud.locationtest.dialogs.FenceListDialog;
import com.fournodes.ud.locationtest.dialogs.UserListDialog;
import com.fournodes.ud.locationtest.interfaces.MapFragmentInterface;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.fournodes.ud.locationtest.services.LocationService;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.DistanceCalculator;
import com.fournodes.ud.locationtest.utils.LatLngInterpolator;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MapFragment extends Fragment implements OnMapReadyCallback,
        ResultCallback, MapFragmentInterface, View.OnClickListener, RequestResult {
    MapView mMapView;
    private GoogleMap map;
    private FloatingActionButton fabAddFence;
    private FloatingActionMenu fabMenu;
    private FloatingActionButton fabShowFences;
    private FloatingActionButton fabMyLoc;
    private FloatingActionButton fabStopTrack;
    private FloatingActionButton fabTrackDevice;
    private FloatingActionButton fabDeleteHistory;
    private FloatingActionButton fabToggleSimulation;
    private Marker markerSelected;
    private Marker currPos;
    private Marker trackPos;

    private int dragMarkerID;
    private List<Fence> mGeofenceList;
    private Database db;

    private Handler updateLocation;
    private Runnable update;
    private Polyline polyline;

    private boolean isSimulationRunning = false;
    private Location currentLocation;
    private Circle curPosArea;
    private int distanceSinceLastRecalc = 0;
    private float speedAtLocation = 0;
    private String[] activeFences;
    private TextView txtInfo;
    private TextView txtTime;

    private String track_id;

    private List<Fence> fenceListActive;
    private Bundle arguments;
    private long startTime;
    private long currentTime;
    private int elapsedTime;
    private Handler timerHandler;
    private Runnable timer;



    public MapFragment() {}

    public void onSaveInstanceState(Bundle outState) {
        //This MUST be done before saving any of your own or your base class's variables
        final Bundle mapViewSaveState = new Bundle(outState);
        mMapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
        //Add any other variables here.
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        new SharedPrefs(getContext()).initialize();
        db = new Database(getContext());

        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.getMapAsync(this);

        if (savedInstanceState != null) {
            Bundle mapViewSavedInstanceState = savedInstanceState.getBundle("mapViewSaveState");
            mMapView.onCreate(mapViewSavedInstanceState);
        }
        else {
            mMapView.onCreate(savedInstanceState);
            mMapView.onResume();

        }

        mGeofenceList = new ArrayList<>();


        fabMenu = (FloatingActionMenu) v.findViewById(R.id.fabMenu);
        fabShowFences = (FloatingActionButton) v.findViewById(R.id.fabShowFences);
        fabMyLoc = (FloatingActionButton) v.findViewById(R.id.fabMyLoc);
        fabTrackDevice = (FloatingActionButton) v.findViewById(R.id.fabTrackDevice);
        fabAddFence = (FloatingActionButton) v.findViewById(R.id.fabAddFence);
        fabStopTrack = (FloatingActionButton) v.findViewById(R.id.fabStopTrack);
        fabDeleteHistory = (FloatingActionButton) v.findViewById(R.id.fabDeleteHistory);
        fabToggleSimulation = (FloatingActionButton) v.findViewById(R.id.fabToggleSimulation);

        txtInfo = (TextView) v.findViewById(R.id.txtInfo);
        txtTime = (TextView) v.findViewById(R.id.txtTime);


        fabMyLoc.setOnClickListener(this);
        fabShowFences.setOnClickListener(this);
        fabAddFence.setOnClickListener(this);
        fabTrackDevice.setOnClickListener(this);
        fabStopTrack.setOnClickListener(this);
        fabDeleteHistory.setOnClickListener(this);
        fabToggleSimulation.setOnClickListener(this);


        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        arguments = getArguments();
        //int argsSize = (arguments != null ? arguments.size() : -1);
        //Toast.makeText(getContext(), String.valueOf(argsSize), Toast.LENGTH_SHORT).show();

        ((MainActivity) getActivity()).mapDelegate = this;
        Fabric.with(getContext(), new Crashlytics());
        updateLocation = new Handler();
        update = new Runnable() {
            @Override
            public void run() {
                String payload = "user_id=" + SharedPrefs.getUserId() + "&track_id=" + track_id + "&live_session_id=" + SharedPrefs.getLiveSessionId();
                IncomingApi incomingApi = new IncomingApi(null, "track_user", payload, 0);
                incomingApi.delegate = ((MainActivity) getActivity());
                incomingApi.execute();
                if (SharedPrefs.isTrackingEnabled())
                    updateLocation.postDelayed(this, 5000);
            }
        };
    }


    @Override
    public void onResult(@NonNull Result result) { }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;

        mGeofenceList = db.drawOffDeviceFences(map);

        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.getTitle().equals("My Device")) {
                    marker.showInfoWindow();
                    markerSelected = marker;
                }

                return true;
            }
        });
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                dragMarkerID = getFenceId(marker.getTitle());

            }

            @Override
            public void onMarkerDrag(Marker marker) {


            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng center = mGeofenceList.get(dragMarkerID).getCenterMarker().getPosition();

                float radius = toRadiusMeters(mGeofenceList.get(dragMarkerID).getCenterMarker().getPosition(),
                        marker.getPosition());


                mGeofenceList.get(dragMarkerID).getCircle().remove();


                Circle circle = map.addCircle(new CircleOptions()
                        .center(center)
                        .radius(radius)
                        .fillColor(0)
                        .strokeColor(Color.parseColor("#000000"))
                        .strokeWidth(3f)
                );

                mGeofenceList.get(dragMarkerID).setEdgeMarker(marker);
                mGeofenceList.get(dragMarkerID).setRadius(radius);
                mGeofenceList.get(dragMarkerID).setCircle(circle);

                db = new Database(getContext());
                db.updateFenceInformation(mGeofenceList.get(dragMarkerID));

                StringBuilder payload = new StringBuilder();
                payload
                        .append("title=").append(mGeofenceList.get(dragMarkerID).getTitle())
                        .append("&description=").append(mGeofenceList.get(dragMarkerID).getDescription())
                        .append("&center_latitude=").append(center.latitude)
                        .append("&center_longitude=").append(center.longitude)
                        .append("&radius=").append(radius)
                        .append("&edge_latitude=").append(toRadiusLatLng(center, radius).latitude)
                        .append("&edge_longitude=").append(toRadiusLatLng(center, radius).longitude)
                        .append("&user_id=").append(SharedPrefs.getUserId())
                        .append("&fence_id=").append(mGeofenceList.get(dragMarkerID).getFenceId())
                        .append("&assignment_data=").append(mGeofenceList.get(dragMarkerID).getAssignment());


                IncomingApi incomingApi = new IncomingApi(null, "edit_fence", payload.toString(), 0);
                incomingApi.delegate = MapFragment.this;
                incomingApi.execute();


            }
        });
        map.setInfoWindowAdapter(new MultilineInfoWindowAdapter(getContext()));
        notificationAction();


    }

    private void serviceMessage(String message) {
        Log.d("Main Fragment", "Broadcasting message");
        Intent intent = new Intent("LOCATION_TEST_SERVICE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    public int getFenceId(String title) {
        for (int i = 0; i < mGeofenceList.size(); i++) {
            if (mGeofenceList.get(i).getTitle().equals(title))
                return mGeofenceList.indexOf(mGeofenceList.get(i));
        }
        return -1; //Error
    }

    @Override
    public void moveToFence(LatLng fence) {
        moveToLocation(fence, 15);
    }

    @Override
    public void viewLiveLocation(LatLng coordinates, final String track_id) {
        if (map != null) {

            if (currPos != null) {

                currPos.setSnippet("Lat: " + String.valueOf(coordinates.latitude)
                        + "\nLng: " + String.valueOf(coordinates.longitude));
                animateMarker(currPos, coordinates, false);
                curPosArea.remove();
                curPosArea = map.addCircle(new CircleOptions()
                        .center(coordinates)
                        .radius(SharedPrefs.getVicinity())
                        .fillColor(Color.parseColor("#9903A9F4"))
                        .strokeColor(Color.parseColor("#000000"))
                        .strokeWidth(3f));
            }
            else {
                currPos = map.addMarker(new MarkerOptions()
                        .position(coordinates)
                        .title("Current Coordinates")
                        .snippet("Lat: " + String.valueOf(coordinates.latitude)
                                + "\nLng: " + String.valueOf(coordinates.longitude)));
                curPosArea = map.addCircle(new CircleOptions()
                        .center(coordinates)
                        .radius(SharedPrefs.getVicinity())
                        .fillColor(Color.parseColor("#9903A9F4"))
                        .strokeColor(Color.parseColor("#000000"))
                        .strokeWidth(3f));
            }
            float fencePerimeterInMeters = 0;

            if (fenceListActive != null && fenceListActive.size() > 0) {
                fencePerimeterInMeters = ((float) SharedPrefs.getFencePerimeterPercentage() / 100)
                        * fenceListActive.get(0).getRadius();
            }

            int speedInKmph = (int) (speedAtLocation * 18) / 5;
            txtInfo.setText("Recalculation After (m): " + String.valueOf(SharedPrefs.getDistanceThreshold() - distanceSinceLastRecalc)
                    + "\nActivity type: " + (SharedPrefs.isMoving() ? "Moving" : "Still")
                    + "\nActive fences: " + Arrays.toString(activeFences)
                    + "\nNearest fence: " + activeFences[0]
                    + "\nNearest perimeter distance (m): " + (fenceListActive != null && fenceListActive.size() > 0
                    ? fenceListActive.get(0).getDistanceFromUser() - (fenceListActive.get(0).getRadius() - fencePerimeterInMeters) : "N/A")
                    + "\nCurrent speed: " + String.valueOf(speedAtLocation) + " m/s - " + String.valueOf(speedInKmph) + " km/h"
                    + "\nLocation request after (s): " + String.valueOf(SharedPrefs.getLocationRequestInterval()));

            moveToLocation(coordinates, 15);


        }
    }

    @Override
    public void viewLocationHistory(List<Coordinate> coordinates) {
        if (map != null) {
            //map.clear();

            PolylineOptions lineOptions = new PolylineOptions();
            for (int i = 0; i < coordinates.size(); i++) {

                lineOptions.add(new LatLng(coordinates.get(i).latitude, coordinates.get(i).longitude));

            }
            polyline = map.addPolyline(lineOptions);
            moveToLocation(new LatLng(coordinates.get(0).latitude, coordinates.get(0).longitude), 15);


        }
    }

    @Override
    public void simulate(Location location) {
        if (isSimulationRunning) {
            startTime = System.currentTimeMillis();

            currentLocation = location;

            if (currentLocation == null) {
                currentLocation = new Location("");
                currentLocation.setLatitude(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()));
                currentLocation.setLongitude(Double.parseDouble(SharedPrefs.getLastDeviceLongitude()));
                currentLocation.setSpeed(0);
            }

            Double reCalcDistanceAtLatitude = Double.parseDouble(SharedPrefs.getReCalcDistanceAtLatitude());
            Double reCalcDistanceAtLongitude = Double.parseDouble(SharedPrefs.getReCalcDistanceAtLongitude());

            Location reCalcDistanceLocation = new Location("");
            reCalcDistanceLocation.setLatitude(reCalcDistanceAtLatitude);
            reCalcDistanceLocation.setLongitude(reCalcDistanceAtLongitude);

            distanceSinceLastRecalc = DistanceCalculator.calcDistanceFromLocation(reCalcDistanceLocation, currentLocation);
            speedAtLocation = (int) Math.ceil(currentLocation.getSpeed());

            serviceMessage("getFenceListActive");
        }
    }

    @Override
    public void trackUser(LatLng coordinates, String time, final String track_id) {
        this.track_id = track_id;

        if (map != null) {
            if (trackPos != null) {

                trackPos.setSnippet("Lat: " + String.valueOf(coordinates.latitude)
                        + "\nLng: " + String.valueOf(coordinates.longitude));
                animateMarkerToICS(trackPos, coordinates, new LatLngInterpolator.Spherical());
                //animateMarker(trackPos, coordinates, false);

            }
            else {
                trackPos = map.addMarker(new MarkerOptions()
                        .position(coordinates)
                        .title(track_id)
                        .snippet("Lat: " + String.valueOf(coordinates.latitude)
                                + "\nLng: " + String.valueOf(coordinates.longitude)));
                moveToLocation(coordinates, 15);
            }

        }


        // long lastLocationTime = Long.parseLong(time);
        // long currentTime = System.currentTimeMillis();
        long timeDiff = Long.parseLong(time);

        if (timeDiff < 60) {// Second
            txtInfo.setText("Was here " + String.valueOf(timeDiff) + " seconds ago.");
        }
        else if (timeDiff > 60 && timeDiff < 3600) { // Minute
            txtInfo.setText("Was here " + String.valueOf(timeDiff / 60) + " minutes ago.");
        }
        else if (timeDiff > 3600 && timeDiff < 86400) { // Hour
            txtInfo.setText("Was here " + String.valueOf(timeDiff / 3600) + " hours ago.");
        }
        else if (timeDiff > 86400) { // Day
            txtInfo.setText("Was here " + String.valueOf(timeDiff / 86400) + " days ago.");
        }
        else {
            txtInfo.setText("Was here " + String.valueOf(timeDiff));
        }
    }

    @Override
    public void trackDisabled() {
        SharedPrefs.setTrackingEnabled(false);
        updateLocation.removeCallbacksAndMessages(null);
        trackPos.remove();
        trackPos = null;
        fabStopTrack.setVisibility(View.GONE);
    }

    @Override
    public void liveLocationUpdate(String lat, String lng, String time, String trackId) {

    }

    @Override
    public void locationHistory(List<Coordinate> coordinates) {

    }

    public void notificationAction() {
        if (map != null) {
            if (arguments != null && arguments.getString("action") != null) {

                String action = arguments.getString("action");
                switch (action) {
                    case "showNotificationOnMap":
                        track_id = arguments.getString("userId");
                        LatLng coordinates = new LatLng(Double.parseDouble(arguments.getString("latitude")),
                                Double.parseDouble(arguments.getString("longitude")));
                        Marker userMarker = map.addMarker(new MarkerOptions()
                                .position(coordinates)
                                .title(arguments.getString("user"))
                                .snippet(arguments.getString("message")));
                        userMarker.showInfoWindow();
                        moveToLocation(coordinates, 15);

                        String payload = "user_id=" + SharedPrefs.getUserId() + "&track_id=" + track_id;
                        IncomingApi incomingApi = new IncomingApi(null, "enable_track", payload, 0);
                        incomingApi.delegate = ((MainActivity) getActivity());
                        incomingApi.execute();

                        break;
                }
            }
        }


    }

    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / 6371009) / //Radius of earth in meters 6371009
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    private static float toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }


    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).mapDelegate = this;
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateLocation != null) {
            updateLocation.removeCallbacksAndMessages(update);
        }
        if (isSimulationRunning)
            stopSimulation();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    static void animateMarkerToICS(Marker marker, LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(4000);
        animator.start();
    }


    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = map.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 4000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 10);
                }
                else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    }
                    else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private void moveToLocation(LatLng location, int zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)      // Sets the center of the map to Mountain View
                .zoom(zoom)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabAddFence:
                if (LocationService.isGoogleApiConnected && markerSelected == null) {
                    CreateFenceDialog newFenceDialog = new CreateFenceDialog(
                            getActivity(),
                            map.getCameraPosition().target,
                            150,
                            mGeofenceList, map);
                    newFenceDialog.details();
                }
                else
                    Toast.makeText(getContext(), "Service is not running", Toast.LENGTH_SHORT).show();

               /* BottomSheetDialogFragment bottomSheetDialogFragment = new CreateFenceBottomSheet();
                bottomSheetDialogFragment.show(getFragmentManager(), bottomSheetDialogFragment.getTag());
*/

                break;
            case R.id.fabMyLoc:
                map.setMyLocationEnabled(true);
                moveToLocation(new LatLng(Double.parseDouble(SharedPrefs.getLastDeviceLatitude()),
                        Double.parseDouble(SharedPrefs.getLastDeviceLongitude())), 15);
                break;
            case R.id.fabShowFences:
                FenceListDialog fenceListDialog = new FenceListDialog(getContext(), mGeofenceList);
                fenceListDialog.delegate = MapFragment.this;
                fenceListDialog.show();
                break;
            case R.id.fabTrackDevice:
                UserListDialog userListDialog = new UserListDialog(getActivity(), this);
                userListDialog.show();
                break;
            case R.id.fabStopTrack:
                String payload = "user_id=" + SharedPrefs.getUserId() + "&track_id=" + track_id + "&live_session_id=" + SharedPrefs.getLiveSessionId();
                IncomingApi incomingApi = new IncomingApi(null, "disable_track", payload, 0);
                incomingApi.delegate = ((MainActivity) getActivity());
                incomingApi.execute();
                break;
            case R.id.fabDeleteHistory:
                fabDeleteHistory.setVisibility(View.GONE);
                if (polyline != null)
                    polyline.remove();

                payload = "user_id=" + SharedPrefs.getUserId();
                IncomingApi notificationApi = new IncomingApi(null, "delete_history", payload, 0);
                notificationApi.execute();
                break;
            case R.id.fabToggleSimulation:
                if (isSimulationRunning) {
                    if (map != null) {
                        map.clear();
                        db.drawOffDeviceFences(map);
                    }
                    stopSimulation();
                    timerHandler.removeCallbacks(timer);
                }
                else {
                    timerHandler = new Handler();
                    timer= new Runnable() {
                        @Override
                        public void run() {
                            currentTime=System.currentTimeMillis();
                            elapsedTime=(int)(currentTime-startTime)/1000;
                            txtTime.setText("Elapsed Time (s): " + String.valueOf(elapsedTime));
                            timerHandler.postDelayed(this,1000);
                        }
                    };
                    timerHandler.post(timer);
                    startTime = System.currentTimeMillis();

                    isSimulationRunning = true;
                    serviceMessage("simulationStarted");
                    if (map != null) {
                        map.clear();
                        Database db = new Database(getContext());
                        List<Fence> fenceListAll = db.onDeviceFence("getAll");
                        for (Fence fence : fenceListAll) {

                            float fencePerimeterInMeters = ((float) SharedPrefs.getFencePerimeterPercentage() / 100) * fence.getRadius();

                            map.addCircle(new CircleOptions()
                                    .center(new LatLng(fence.getCenterLat(), fence.getCenterLng()))
                                    .radius(fence.getRadius() + fencePerimeterInMeters)
                                    .strokeColor(Color.parseColor("#000000"))
                                    .strokeWidth(3f));

                            map.addCircle(new CircleOptions()
                                    .center(new LatLng(fence.getCenterLat(), fence.getCenterLng()))
                                    .radius(fence.getRadius())
                                    .fillColor(Color.parseColor("#9903A9F4"))
                                    .strokeColor(Color.parseColor("#9903A9F4"))
                                    .strokeWidth(2f));


                        }
                    }
                    simulate(null);

                }
                break;
        }
        fabMenu.close(true);
    }

    private void stopSimulation() {

        isSimulationRunning = false;
        if (curPosArea != null) {
            curPosArea.remove();
            currPos.remove();
        }
        txtInfo.setText("");
        serviceMessage("simulationStopped");
    }


    @Override
    public void onSuccess(String result) {}

    @Override
    public void onFailure() {}

    @Override
    public void userList(List<User> users) {
    }

    @Override
    public void trackEnabled() {
        SharedPrefs.setTrackingEnabled(true);
        updateLocation.postDelayed(update, 5000);
        showFabStopTrack();
    }

    public void showFabStopTrack() {
        fabStopTrack.setVisibility(View.VISIBLE);
        if (polyline != null)
            polyline.remove();
    }

    public void showFabDeleteHistory() {
        fabDeleteHistory.setVisibility(View.VISIBLE);
        if (updateLocation != null && update != null)
            updateLocation.removeCallbacks(update);
    }

    @Override
    public void activeFenceList(List<Fence> fenceListActive, String className) {
        this.fenceListActive = fenceListActive;
        if (fenceListActive != null && fenceListActive.size() > 0) {
            activeFences = new String[fenceListActive.size()];
            for (int i = 0; i < fenceListActive.size(); i++) {
                activeFences[i] = fenceListActive.get(i).getTitle();
            }
        }
        else {
            activeFences = new String[1];
            activeFences[0] = "No fences active";
        }

        viewLiveLocation(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), SharedPrefs.getUserId());
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {

    }


}
