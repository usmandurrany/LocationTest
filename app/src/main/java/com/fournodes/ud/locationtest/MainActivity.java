package com.fournodes.ud.locationtest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.fournodes.ud.locationtest.gcm.GCMInitiate;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements TrackApiResult, ServiceMessage {
    public MainFragmentInterface mainDelegate;
    public MapFragmentInterface mapDelegate;

    private static final String TAG = "Main Activity";


    private ViewPager viewPager;
    private FragmentAdapter fragmentAdapter;
    private LocationService locationService;
    private Runnable check;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new SharedPrefs(this).initialize();
        if (SharedPrefs.getUserId() == null)
            new UserRegisterDialog(this).show();
        else
            Log.e("User Id: ", SharedPrefs.getUserId());


        startService(new Intent(this, LocationService.class));
        handler = new Handler();
        check = new Runnable() {
            @Override
            public void run() {
                if (LocationService.isRunning && LocationService.getServiceObject().delegate == null) {
                    locationService = LocationService.getServiceObject();
                    locationService.delegate = MainActivity.this;
                } else
                    handler.postDelayed(check, 2000);
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
    public void liveLocationUpdate(String lat, String lng, final String track_id) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            try {
                if (mapDelegate != null)
                    mapDelegate.viewLiveLocation(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), track_id);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void locationHistory(JSONArray location) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            if (mapDelegate != null)
                mapDelegate.viewLocationHistory(location);
        }
    }

    @Override
    public void userList(JSONArray users) {

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
    public void updateServer(String result) {
        if (mainDelegate != null)
            mainDelegate.updateServer(result);
    }


}