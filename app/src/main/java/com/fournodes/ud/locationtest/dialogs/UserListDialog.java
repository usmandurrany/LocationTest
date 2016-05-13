package com.fournodes.ud.locationtest.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.widget.ListView;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.adapters.UserAdapter;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.fragments.MapFragment;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.User;

import java.util.List;

/**
 * Created by Usman on 14/3/2016.
 */
public class UserListDialog implements RequestResult {
    private Dialog dialog;
    private Activity activity;
    private ListView lstDevices;
    private IncomingApi incomingApi;
    private MapFragment fragment;

    public UserListDialog(Activity activity, MapFragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.list_view_common);

    }

    public void show() {
        String payload = "user_id=" + SharedPrefs.getUserId();
        incomingApi = new IncomingApi(null, "user_list", payload, 0);
        incomingApi.delegate = this;
        incomingApi.execute();
        lstDevices = (ListView) dialog.findViewById(R.id.lstCommon);

    }


    @Override
    public void liveLocationUpdate(String lat, String lng, String time, String track_id) {

    }

    @Override
    public void locationHistory(List<Coordinate> coordinates) {

    }


    @Override
    public void userList(List<User> users) {
        lstDevices.setAdapter(new UserAdapter(activity, this, R.layout.list_item_user_action, users));
        dialog.show();
    }

    @Override
    public void onSuccess(String result) {

    }

    @Override
    public void onFailure() {

    }


    @Override
    public void trackEnabled() {

    }

    @Override
    public void trackDisabled() {

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
