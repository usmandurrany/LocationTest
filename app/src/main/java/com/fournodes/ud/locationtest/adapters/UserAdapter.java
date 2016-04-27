package com.fournodes.ud.locationtest.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.TrackApi;
import com.fournodes.ud.locationtest.dialogs.UserListDialog;
import com.fournodes.ud.locationtest.objects.User;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usman on 21/3/2016.
 */
public class UserAdapter extends BaseAdapter {
    private List<User> userList;
    private int layout;
    private Activity activity;
    private UserListDialog dialog;

    public UserAdapter(Activity activity, UserListDialog dialog, int layout, JSONArray array) {
        userList = new ArrayList<>();
        this.layout = layout;
        this.activity = activity;
        this.dialog = dialog;
        try {
            for (int i = 0; i < array.length(); i++) {
                User user = new User();
                user.id = array.getJSONObject(i).getInt("user_id");
                user.name = array.getJSONObject(i).getString("user_name");
                user.email = array.getJSONObject(i).getString("user_email");
                user.picture = array.getJSONObject(i).getString("user_picture");
                userList.add(user);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        viewHolder holder;
        if (convertView == null) {
            holder = new viewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(layout, null);
            holder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            holder.txtEmail = (TextView) convertView.findViewById(R.id.txtEmail);
            if (layout == R.layout.list_item_user_action) {
                holder.btnHistory = (ImageButton) convertView.findViewById(R.id.btnHistory);
                holder.btnTrack = (ImageButton) convertView.findViewById(R.id.btnTrack);
            }
            convertView.setTag(holder);

        }
        else {
            holder = (viewHolder) convertView.getTag();
        }
        holder.txtName.setText(userList.get(position).name);
        holder.txtEmail.setText(userList.get(position).email);

        if (layout == R.layout.list_item_user_action) {
            holder.btnHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TrackApi trackApi = new TrackApi();
                    trackApi.delegate = ((MainActivity) activity);
                    trackApi.execute("user_id=" + SharedPrefs.getUserId() + "&track_id=" + userList.get(position).id, "location_history");
                    if (userList.get(position).id == Integer.parseInt(SharedPrefs.getUserId()))
                        dialog.showFabDeleteHistory();
                    dialog.close();

                }
            });
            holder.btnTrack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    TrackApi trackApi = new TrackApi();
                    trackApi.delegate = ((MainActivity) activity);
                    trackApi.execute("user_id=" + SharedPrefs.getUserId() + "&track_id=" + userList.get(position).id, "track_user");
                    dialog.showFabStopTrack();
                    dialog.close();
                }
            });
        }


        return convertView;
    }

    public View getMultiSelectionView() {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return userList.get(position).id;
    }

    @Override
    public Object getItem(int position) {
        return userList.get(position);
    }

    @Override
    public int getCount() {
        return userList.size();
    }


    public static class viewHolder {
        TextView txtName;
        TextView txtEmail;
        ImageButton btnTrack;
        ImageButton btnHistory;
    }
}
