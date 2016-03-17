package com.fournodes.ud.locationtest;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Usman on 14/3/2016.
 */
public class DeviceListAdapter extends ArrayAdapter {
    private String[] devices;
    private Activity activity;
    private DeviceListDialog dialog;
    public DeviceListAdapter(Activity activity, DeviceListDialog dialog, String[] devices) {
        super(activity,0);
        this.devices = devices;
        this.activity=activity;
        this.dialog=dialog;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        viewHolder holder;
        if (convertView == null){
            LayoutInflater inflatoor = LayoutInflater.from(parent.getContext());
            convertView = inflatoor.inflate(R.layout.list_item_devices, null);
            holder = new viewHolder();
            holder.txtDeviceModel = (TextView) convertView.findViewById(R.id.txtDeviceModel);
            holder.btnDeviceTrack = (Button) convertView.findViewById(R.id.btnTrack);
            holder.btnDeviceHistory = (Button) convertView.findViewById(R.id.btnHistory);
            convertView.setTag(holder);

        }
        else{
           holder = (viewHolder) convertView.getTag();
        }
         holder.txtDeviceModel.setText(devices[position]);
         holder.btnDeviceTrack.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 DeviceLocator deviceLocator = new DeviceLocator();
                 deviceLocator.delegate= ((MainActivity) activity);
                 deviceLocator.execute("live",devices[position]);
                 Log.e("Device",devices[position]);
                 dialog.close();

             }
         });

        holder.btnDeviceHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceLocator deviceLocator = new DeviceLocator();
                deviceLocator.delegate= ((MainActivity) activity);
                deviceLocator.execute("history",devices[position]);
                Log.e("Device",devices[position]);
                dialog.close();

            }
        });

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return devices[position];
    }

    @Override
    public int getCount() {
        return devices.length;
    }
    public static class viewHolder{
        TextView txtDeviceModel;
        Button btnDeviceTrack;
        Button btnDeviceHistory;
    }
}
