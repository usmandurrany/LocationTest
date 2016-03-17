package com.fournodes.ud.locationtest;

import android.app.PendingIntent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.GeofenceTransitionsIntentService;
import com.fournodes.ud.locationtest.service.LocationService;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback, ResultCallback, FragmentInterface, View.OnClickListener {

    MapView mMapView;
    private GoogleMap map;
    private FloatingActionButton fabAddFence;
    private FloatingActionMenu fabMenu;
    private FloatingActionButton fabShowFences;
    private FloatingActionButton fabMyLoc;
    private FloatingActionButton fabTrackDevice;
    private Marker markerSelected;
    private Marker currPos;

    private int dragMarkerID;
    private List<Fence> mGeofenceList;
    private Database db;


    public MapFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        new SharedPrefs(getContext()).initialize();
        db = new Database(getContext());
        mGeofenceList = new ArrayList<>();

        fabMenu = (FloatingActionMenu) v.findViewById(R.id.fabMenu);
        fabShowFences = (FloatingActionButton) v.findViewById(R.id.fabShowFences);
        fabMyLoc = (FloatingActionButton) v.findViewById(R.id.fabMyLoc);
        fabTrackDevice = (FloatingActionButton) v.findViewById(R.id.fabTrackDevice);
        fabAddFence = (FloatingActionButton) v.findViewById(R.id.fabAddFence);

        fabMyLoc.setOnClickListener(this);
        fabShowFences.setOnClickListener(this);
        fabAddFence.setOnClickListener(this);
        fabTrackDevice.setOnClickListener(this);

        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.getMapAsync(this);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume();

        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) getActivity()).delegate = this;
    }


    @Override
    public void onResult(@NonNull Result result) {
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;
        mGeofenceList = db.getFences(map);
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

                LocationServices.GeofencingApi.removeGeofences(
                        LocationService.mGoogleApiClient,
                        mGeofenceList.get(dragMarkerID).getPendingIntent());

                mGeofenceList.get(dragMarkerID).getVisibleArea().remove();

                Geofence geofence = new Geofence.Builder()
                        .setRequestId(mGeofenceList.get(dragMarkerID).getTitle())
                        .setCircularRegion(
                                center.latitude,
                                center.longitude,
                                radius)

                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setLoiteringDelay(500)
                        .build();


                Circle circle = map.addCircle(new CircleOptions()
                        .center(center)
                        .radius(radius)
                        .fillColor(0)
                        .strokeColor(Color.parseColor("#000000"))
                        .strokeWidth(3f)
                );

                mGeofenceList.get(dragMarkerID).setEndMarker(marker);
                mGeofenceList.get(dragMarkerID).setArea(geofence);
                mGeofenceList.get(dragMarkerID).setRadius(radius);
                mGeofenceList.get(dragMarkerID).setVisibleArea(circle);

                db = new Database(getContext());
                db.updateFence(mGeofenceList.get(dragMarkerID));

                LocationServices.GeofencingApi.addGeofences(
                        LocationService.mGoogleApiClient,
                        getGeofencingRequest(mGeofenceList.get(dragMarkerID).getArea()),
                        getGeofencePendingIntent(mGeofenceList.get(dragMarkerID).getNotifyDevice()))
                        .setResultCallback(MapFragment.this);


            }
        });
    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(String device) {
        // Reuse the PendingIntent if we already have it.

        Intent intent = new Intent(getContext(), GeofenceTransitionsIntentService.class);
        intent.putExtra("device", device);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(getContext(), mGeofenceList.size(), intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }


    public int getFenceId(String title) {
        for (int i = 0; i < mGeofenceList.size(); i++) {
            if (mGeofenceList.get(i).getTitle().equals(title))
                return mGeofenceList.indexOf(mGeofenceList.get(i));
        }
        return -1; //Error
    }

    @Override
    public void mapDragStart() {
        markerSelected = null;
    }

    @Override
    public void mapDragStop() {}

    @Override
    public void moveToFence(LatLng fence) {
        moveToLocation(fence, 15);
    }

    @Override
    public void viewLiveLocation(LatLng coordinates, final String device) {
        if (map != null) {
            //map.clear();
            if (currPos != null) {
                animateMarker(currPos, coordinates, false);
            } else {
                currPos = map.addMarker(new MarkerOptions().position(coordinates));
            }
            moveToLocation(coordinates, 15);
            Handler updateLocation = new Handler();
            Runnable update = new Runnable() {
                @Override
                public void run() {
                    DeviceLocator deviceLocator = new DeviceLocator();
                    deviceLocator.delegate = ((MainActivity) getActivity());
                    deviceLocator.execute("live", device);
                }
            };
            updateLocation.postDelayed(update, 10000);
        }
    }

    @Override
    public void viewLocationHistory(JSONArray location, String device) {
        if (map != null) {
            map.clear();
            try {
                PolylineOptions lineOptions = new PolylineOptions();
                for (int i = 0; i < location.length(); i++) {

                    lineOptions.add(new LatLng(Double.parseDouble(location.getJSONObject(i).getString("latitude")),
                            Double.parseDouble(location.getJSONObject(i).getString("longitude"))));

                }
                Polyline polyline = map.addPolyline(lineOptions);
                moveToLocation(new LatLng(Double.parseDouble(location.getJSONObject(0).getString("latitude")),
                        Double.parseDouble(location.getJSONObject(0).getString("longitude"))), 15);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void serviceStarted() {}

    @Override
    public void serviceStopped() {}

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
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = map.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

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
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
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
                    NewFenceDialog newFenceDialog = new NewFenceDialog(
                            getContext(),
                            map.getCameraPosition().target,
                            300,
                            mGeofenceList, map);
                    newFenceDialog.details();
                } else
                    Toast.makeText(getContext(), "Service is not running", Toast.LENGTH_SHORT).show();

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
                DeviceListDialog deviceListDialog = new DeviceListDialog(getActivity());
                deviceListDialog.show();
                break;
        }
        fabMenu.close(true);
    }
}
