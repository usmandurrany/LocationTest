package com.fournodes.ud.locationtest;

import android.app.Dialog;
import android.content.Context;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class FenceListDialog {
    public MapFragmentInterface delegate;
    private Context context;
    private List<Fence> mGeofenceList;
    private String[] fenceTitle;
    private Dialog dialog;

    public FenceListDialog(Context context, List<Fence> mGeofenceList) {
        this.context = context;
        this.mGeofenceList = mGeofenceList;
        fenceTitle = new String[mGeofenceList.size()];
        dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_fence_list);

        for (int i = 0; i < mGeofenceList.size(); i++) {
            fenceTitle[i] = mGeofenceList.get(i).getTitle();
        }
    }

    public void show() {
        ListView lstFences = (ListView) dialog.findViewById(R.id.lstFences);
        final FenceAdapter fenceAdapter = new FenceAdapter(context, this, mGeofenceList);
        lstFences.setAdapter(fenceAdapter);
        dialog.show();

    }

    public void close() {
        dialog.dismiss();
    }
}
