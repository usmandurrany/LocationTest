package com.fournodes.ud.locationtest;

import android.app.Activity;
import android.app.Dialog;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Usman on 14/3/2016.
 */
public class DeviceListDialog implements RemoteDevice {
private Dialog dialog;
    private Activity activity;
    private ListView lstDevices;
    private DeviceLocator deviceLocator;
    public DeviceListDialog(Activity activity) {
        this.activity = activity;
        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.list_devices);

    }
    public void show(){
        deviceLocator = new DeviceLocator();
        deviceLocator.delegate=this;
        deviceLocator.execute("device_list");
        lstDevices = (ListView) dialog.findViewById(R.id.lstDevices);

    }

    @Override
    public void liveLocationUpdate(String lat, String lng,String device) {

    }

    @Override
    public void locationHistory(JSONArray location,String device) {

    }

    @Override
    public void deviceList(final JSONArray devices) {
        String[] temp = new String[devices.length()];
        for(int i = 0; i < devices.length();i++){
            try {
                temp[i]=devices.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        lstDevices.setAdapter(new DeviceListAdapter(activity,this,temp));
        dialog.show();
    }
    public void close(){
        dialog.dismiss();
    }

}
