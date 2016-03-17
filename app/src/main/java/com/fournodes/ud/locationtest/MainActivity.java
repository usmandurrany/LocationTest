package com.fournodes.ud.locationtest;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

import com.fournodes.ud.locationtest.gcm.GCMInitiate;
import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements RemoteDevice, Messenger {
    public FragmentInterface delegate;
    private static final String TAG = "Main Activity";


    private ViewPager viewPager;
    private FragmentAdapter fragmentAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new SharedPrefs(this).initialize();
        if (SharedPrefs.getDeviceGcmId() == null && SharedPrefs.getDeviceId() == null) {
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
    public void liveLocationUpdate(String lat, String lng, String device) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            delegate.viewLiveLocation(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), device);
        }

    }

    @Override
    public void locationHistory(JSONArray location, String device) {
        if (fragmentAdapter.getItem(viewPager.getCurrentItem()) instanceof MapFragment) {
            delegate.viewLocationHistory(location, device);
        }
    }

    @Override
    public void deviceList(JSONArray devices) {

    }

    @Override
    public void serviceStarted() {
        delegate.serviceStarted();

    }

    @Override
    public void serviceStopped() {
        delegate.serviceStopped();

    }

    @Override
    public void locationUpdated() {

    }

    @Override
    public void startLocationUpdate() {

    }

    @Override
    public void stopLocationUpdate() {

    }
}