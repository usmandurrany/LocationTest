package com.fournodes.ud.locationtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.dialogs.FenceListDialog;
import com.fournodes.ud.locationtest.dialogs.MultiUserSelectDialog;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.fournodes.ud.locationtest.utils.Database;

import java.util.List;

/**
 * Created by Usman on 17/2/2016.
 */
public class FenceAdapter extends ArrayAdapter implements RequestResult {
    private List<Fence> fenceListOrig;
    private Context context;
    private FenceListDialog fenceListDialog;
    private int activeItemPostion;
    private int activeItemId;

    public FenceAdapter(Context context, FenceListDialog fenceListDialog, List<Fence> fenceListOrig) {
        super(context, 0, fenceListOrig);
        this.context = context;
        this.fenceListDialog = fenceListDialog;
        this.fenceListOrig = fenceListOrig;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final viewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.list_item_fence, null);
            holder = new viewHolder();
            holder.txtFence = (TextView) convertView.findViewById(R.id.txtFence);
            holder.txtFenceDesc = (TextView) convertView.findViewById(R.id.txtFenceDesc);
            holder.btnRemoveFence = (ImageButton) convertView.findViewById(R.id.btnRemoveFence);
            holder.btnEditFence = (ImageButton) convertView.findViewById(R.id.btnEditFence);
            convertView.setTag(holder);
        }
        else {
            holder = (viewHolder) convertView.getTag();
        }

        holder.txtFence.setText(fenceListOrig.get(position).getTitle());
        holder.txtFenceDesc.setText(fenceListOrig.get(position).getDescription());
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
                activeItemPostion = position;

                String payload = "user_id=" + SharedPrefs.getUserId() + "&fence_id=" + fenceListOrig.get(position).getFenceId();
                IncomingApi incomingApi = new IncomingApi(null, "remove_fence", payload, 0);
                incomingApi.delegate = FenceAdapter.this;
                incomingApi.execute();


            }
        });
        holder.btnEditFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MultiUserSelectDialog multiUserSelectDialog = new MultiUserSelectDialog(getContext(),fenceListOrig.get(position).getFenceId(),fenceListOrig.get(position).getAssignment());
                multiUserSelectDialog.delegate = new MultiUserSelectDialog.editAssignmentInterface() {
                    @Override
                    public void fenceUpdated(String assignmentData) {
                        Database db = new Database(context);
                        if (db.updateFenceAssignment(fenceListOrig.get(position).getFenceId(), assignmentData))
                            Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                        fenceListOrig.get(position).setAssignment(assignmentData);
                    }
                };
                multiUserSelectDialog.show();
            }
        });
        return convertView;
    }


    public String getTitle(int position) {
        return fenceListOrig.get(position).getTitle();
    }

    @Override
    public void onSuccess(String result) {
        Database db = new Database(getContext());
        db.removeFenceFromDatabase(fenceListOrig.get(activeItemPostion).getFenceId(),0); //Remove from database
        fenceListOrig.get(activeItemPostion).removeFence(); //Remove form map
        fenceListOrig.remove(activeItemPostion); //Remove from list
        notifyDataSetChanged();

    }

    @Override
    public void onFailure() {
        Toast.makeText(context, "Network or server error", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void userList(List<User> users) {

    }

    @Override
    public void trackEnabled() {

    }

    @Override
    public void trackDisabled() {

    }

    @Override
    public void liveLocationUpdate(String lat, String lng, String time, String trackId) {

    }

    @Override
    public void locationHistory(List<Coordinate> coordinates) {

    }

    public static class viewHolder {
        TextView txtFence;
        TextView txtFenceDesc;
        ImageButton btnRemoveFence;
        ImageButton btnEditFence;
    }
}
