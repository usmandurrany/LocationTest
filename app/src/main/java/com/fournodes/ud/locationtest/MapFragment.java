package com.fournodes.ud.locationtest;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.network.FenceApi;
import com.fournodes.ud.locationtest.network.NotificationApi;
import com.fournodes.ud.locationtest.network.TrackApi;
import com.fournodes.ud.locationtest.service.LocationService;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MapFragment extends Fragment implements OnMapReadyCallback, ResultCallback, MapFragmentInterface, View.OnClickListener, RequestResult {
    MapView mMapView;
    private GoogleMap map;
    private FloatingActionButton fabAddFence;
    private FloatingActionMenu fabMenu;
    private FloatingActionButton fabShowFences;
    private FloatingActionButton fabMyLoc;
    private FloatingActionButton fabStopTrack;
    private FloatingActionButton fabTrackDevice;
    private FloatingActionButton fabDeleteHistory;
    private Marker markerSelected;
    private Marker currPos;

    private int dragMarkerID;
    private List<Fence> mGeofenceList;
    private Database db;

    private Handler updateLocation;
    private Runnable update;
    private Polyline polyline;



    public MapFragment() {
    }

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

        fabMyLoc.setOnClickListener(this);
        fabShowFences.setOnClickListener(this);
        fabAddFence.setOnClickListener(this);
        fabTrackDevice.setOnClickListener(this);
        fabStopTrack.setOnClickListener(this);
        fabDeleteHistory.setOnClickListener(this);


        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) getActivity()).mapDelegate = this;
        Fabric.with(getContext(), new Crashlytics());

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


                mGeofenceList.get(dragMarkerID).getVisibleArea().remove();


                Circle circle = map.addCircle(new CircleOptions()
                        .center(center)
                        .radius(radius)
                        .fillColor(0)
                        .strokeColor(Color.parseColor("#000000"))
                        .strokeWidth(3f)
                );

                mGeofenceList.get(dragMarkerID).setEdgeMarker(marker);
                mGeofenceList.get(dragMarkerID).setRadius(radius);
                mGeofenceList.get(dragMarkerID).setVisibleArea(circle);

                db = new Database(getContext());
                db.updateFence(mGeofenceList.get(dragMarkerID));

                StringBuilder fenceDetails = new StringBuilder();
                fenceDetails
                        .append("title=").append(mGeofenceList.get(dragMarkerID).getTitle())
                        .append("&description=").append(mGeofenceList.get(dragMarkerID).getDescription())
                        .append("&center_latitude=").append(center.latitude)
                        .append("&center_longitude=").append(center.longitude)
                        .append("&radius=").append(radius)
                        .append("&edge_latitude=").append(toRadiusLatLng(center, radius).latitude)
                        .append("&edge_longitude=").append(toRadiusLatLng(center, radius).longitude)
                        .append("&transition_type=").append(mGeofenceList.get(dragMarkerID).getTransitionType())
                        .append("&user_id=").append(SharedPrefs.getUserId())
                        .append("&create_on=").append(mGeofenceList.get(dragMarkerID).getCreate_on())
                        .append("&fence_id=").append(mGeofenceList.get(dragMarkerID).getId());


                FenceApi fenceApi = new FenceApi();
                fenceApi.delegate = MapFragment.this;
                fenceApi.execute(fenceDetails.toString(), "edit_fence");


            }
        });


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
            if (updateLocation != null && update != null) {
                updateLocation.removeCallbacks(update);
                updateLocation = null;
            }
            if (currPos != null) {
                animateMarker(currPos, coordinates, false);
            }
            else {
                currPos = map.addMarker(new MarkerOptions()
                        .position(coordinates)
                        .title("Tracking Id: " + track_id)
                        .snippet("Current Position"));
            }
            moveToLocation(coordinates, 15);
            updateLocation = new Handler();
            update = new Runnable() {
                @Override
                public void run() {
                    TrackApi trackApi = new TrackApi();
                    trackApi.delegate = ((MainActivity) getActivity());
                    trackApi.execute("user_id=" + SharedPrefs.getUserId() + "&track_id=" + track_id, "track_user");
                }
            };
            updateLocation.postDelayed(update, 30000);
        }
    }

    @Override
    public void viewLocationHistory(JSONArray location) {
        if (map != null) {
            //map.clear();
            try {
                PolylineOptions lineOptions = new PolylineOptions();
                for (int i = 0; i < location.length(); i++) {

                    lineOptions.add(new LatLng(Double.parseDouble(location.getJSONObject(i).getString("latitude")),
                            Double.parseDouble(location.getJSONObject(i).getString("longitude"))));

                }
                polyline = map.addPolyline(lineOptions);
                moveToLocation(new LatLng(Double.parseDouble(location.getJSONObject(0).getString("latitude")),
                        Double.parseDouble(location.getJSONObject(0).getString("longitude"))), 15);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
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
                    CreateFence newFenceDialog = new CreateFence(
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
                fabStopTrack.setVisibility(View.GONE);
                if (updateLocation != null && update != null)
                    updateLocation.removeCallbacks(update);

                break;
            case R.id.fabDeleteHistory:
                fabDeleteHistory.setVisibility(View.GONE);
                if (polyline != null)
                    polyline.remove();

                NotificationApi notificationApi = new NotificationApi();
                notificationApi.execute("delete_history", "user_id=" + SharedPrefs.getUserId());
                break;
        }
        fabMenu.close(true);
    }

    @Override
    public void onSuccess(String result) {}

    @Override
    public void onFailure() {}

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
}
