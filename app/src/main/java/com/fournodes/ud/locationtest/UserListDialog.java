package com.fournodes.ud.locationtest;

import android.app.Activity;
import android.app.Dialog;
import android.widget.ListView;

import com.fournodes.ud.locationtest.network.TrackApi;

import org.json.JSONArray;

/**
 * Created by Usman on 14/3/2016.
 */
public class UserListDialog implements TrackApiResult {
    private Dialog dialog;
    private Activity activity;
    private ListView lstDevices;
    private TrackApi trackApi;
    private MapFragment fragment;

    public UserListDialog(Activity activity, MapFragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.list_devices);

    }

    public void show() {
        trackApi = new TrackApi();
        trackApi.delegate = this;
        trackApi.execute("user_id=" + SharedPrefs.getUserId(), "user_list");
        lstDevices = (ListView) dialog.findViewById(R.id.lstDevices);

    }

    @Override
    public void liveLocationUpdate(String lat, String lng, String track_id) {

    }

    @Override
    public void locationHistory(JSONArray location) {

    }

    @Override
    public void userList(final JSONArray users) {
        lstDevices.setAdapter(new UserAdapter(activity, this, R.layout.list_item_user_action, users));
        dialog.show();
    }

    public void close() {
        dialog.dismiss();
    }

    public void showFabStopTrack() {
        fragment.showFabStopTrack();
    }
    public void showFabDeleteHistory() {
        fragment.showFabDeleteHistory();
    }

}
