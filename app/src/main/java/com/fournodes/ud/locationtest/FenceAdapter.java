package com.fournodes.ud.locationtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fournodes.ud.locationtest.service.LocationService;
import com.google.android.gms.location.LocationServices;

import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class FenceAdapter extends ArrayAdapter {
    private  List<Fence> fenceListOrig;
    private Context context;
    private FenceListDialog fenceListDialog;
    public FenceAdapter(Context context, FenceListDialog fenceListDialog, List<Fence> fenceListOrig) {
        super(context, 0, fenceListOrig);
        this.context=context;
        this.fenceListDialog=fenceListDialog;
        this.fenceListOrig=fenceListOrig;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        viewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.list_item_fence, null);
            holder = new viewHolder();
            holder.txtFence = (TextView) convertView.findViewById(R.id.txtFence);
            holder.btnRemoveFence = (Button) convertView.findViewById(R.id.btnRemoveFence);
            convertView.setTag(holder);
        } else {
            holder = (viewHolder) convertView.getTag();
        }

            holder.txtFence.setText(fenceListOrig.get(position).getTitle());
        holder.txtFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fenceListDialog.delegate.moveToFence(fenceListOrig.get(position).getCenterMarker().getPosition());
                fenceListDialog.close();
            }
        });
            holder.btnRemoveFence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (LocationService.isGoogleApiConnected) {
                        Database db =new Database(getContext());
                        LocationServices.GeofencingApi.removeGeofences(LocationService.mGoogleApiClient,
                                fenceListOrig.get(position).getPendingIntent()); //Remove from GoogleApiGeofence
                        fenceListOrig.get(position).removeFence(); //Remove form map
                        db.removeFence(fenceListOrig.get(position).getId()); //Remove from database
                        fenceListOrig.remove(position); //Remove from list
                        notifyDataSetChanged();
                    }else
                        Toast.makeText(getContext(), "Service not running", Toast.LENGTH_SHORT).show();
                }
            });
        return convertView;
    }


  public String getTitle(int position) {
      return fenceListOrig.get(position).getTitle();
  }

    public static class viewHolder {
        TextView txtFence;
        Button btnRemoveFence;
    }
}
