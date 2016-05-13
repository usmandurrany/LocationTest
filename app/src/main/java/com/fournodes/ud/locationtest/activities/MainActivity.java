package com.fournodes.ud.locationtest.activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.adapters.FragmentAdapter;
import com.fournodes.ud.locationtest.dialogs.UserRegisterDialog;
import com.fournodes.ud.locationtest.fragments.MainFragment;
import com.fournodes.ud.locationtest.fragments.MapFragment;
import com.fournodes.ud.locationtest.gcm.GCMInitiate;
import com.fournodes.ud.locationtest.interfaces.MainFragmentInterface;
import com.fournodes.ud.locationtest.interfaces.MapFragmentInterface;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.interfaces.ServiceMessage;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.fournodes.ud.locationtest.services.LocationService;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends FragmentActivity implements RequestResult, ServiceMessage {
    public MainFragmentInterface mainDelegate;
    public MapFragmentInterface mapDelegate;

    private static final String TAG = "Main Activity";


    private ViewPager viewPager;
    private FragmentAdapter fragmentAdapter;
    private LocationService locationService;
    private Runnable check;
    private Handler handler;


    @Override
    protected void onResume() {
        super.onResume();
        Intent notificationIntent = getIntent();
        if (notificationIntent != null) {
            String action = notificationIntent.getAction();
            if (action != null && action.equals("showNotificationOnMap")) {
                Bundle showNotificationOnMap = new Bundle();
                showNotificationOnMap.putString("action", notificationIntent.getAction());
                showNotificationOnMap.putString("latitude", notificationIntent.getStringExtra("latitude"));
                showNotificationOnMap.putString("longitude", notificationIntent.getStringExtra("longitude"));
                showNotificationOnMap.putString("time", notificationIntent.getStringExtra("time"));
                showNotificationOnMap.putString("user", notificationIntent.getStringExtra("user"));
                showNotificationOnMap.putString("message", notificationIntent.getStringExtra("message"));
                List<Fragment> fragments = new ArrayList<>();

                MapFragment mapFragment = new MapFragment();
                mapFragment.setArguments(showNotificationOnMap);

                fragments.add(new MainFragment());
                fragments.add(mapFragment);

                fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), fragments);

                viewPager.setAdapter(fragmentAdapter);
                viewPager.setCurrentItem(1, true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        new SharedPrefs(this).initialize();

        if (SharedPrefs.getUserId() == null)
            new UserRegisterDialog(this).show();
        else
            Log.e("User Id: ", SharedPrefs.getUserId());

        //SharedPrefs.setPendingEventCount(0);

        handler = new Handler();
        check = new Runnable() {
            @Override
            public void run() {
                if (LocationService.isServiceRunning) {
                    locationService = LocationService.getServiceObject();
                    locationService.delegate = MainActivity.this;
                    if (mainDelegate != null)
                        mainDelegate.serviceStarted();

                }
                else {
                    startService(new Intent(MainActivity.this, LocationService.class));
                    handler.postDelayed(check, 2000);
                }
            }
        };
        handler.postDelayed(check, 2000);

        if (SharedPrefs.getDeviceGcmId() == null && SharedPrefs.getUserId() == null) {
            new GCMInitiate(this).run();
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_settings));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_map));


        viewPager = (ViewPager) findViewById(R.id.viewPager);
        List<Fragment> fragments = new ArrayList<>();


        fragments.add(new MainFragment());
        fragments.add(new MapFragment());

        fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), fragments);

        viewPager.setAdapter(fragmentAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void liveLocationUpdate(String lat, String lng, String time, String track_id) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            try {
                if (mapDelegate != null)
                    mapDelegate.trackUser(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), time, track_id);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void locationHistory(List<Coordinate> coordinates) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.viewLocationHistory(coordinates);
        }
    }


    @Override
    public void onSuccess(String result) {

    }

    @Override
    public void onFailure() {

    }

    @Override
    public void userList(List<User> users) {
    }

    @Override
    public void trackEnabled() {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.trackEnabled();
        }

    }

    @Override
    public void trackDisabled() {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.trackDisabled();
        }
    }

    @Override
    public void serviceStarted() {
        if (mainDelegate != null)
            mainDelegate.serviceStarted();
    }

    @Override
    public void serviceStopped() {
        if (mainDelegate != null)
            mainDelegate.serviceStopped();
    }

    @Override
    public void fenceTriggered(String data) {
        if (mainDelegate != null)
            mainDelegate.fenceTriggered(data);
    }

    @Override
    public void locationUpdated(String lat, String lng, String time) {
        if (mainDelegate != null)
            mainDelegate.locationUpdated(lat, lng, time);
    }

    @Override
    public void listenerLocation(Location location) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.simulate(location);
        }
    }

    @Override
    public void updateServer(String result) {
        if (mainDelegate != null)
            mainDelegate.updateServer(result);
    }


    @Override
    public void activeFenceList(List<Fence> fenceListActive, String className) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.activeFenceList(fenceListActive, TAG);
        }
    }

    @Override
    public void allFenceList(List<Fence> fenceListAll) {

    }
}