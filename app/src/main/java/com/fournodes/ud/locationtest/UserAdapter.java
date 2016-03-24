package com.fournodes.ud.locationtest;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.fournodes.ud.locationtest.network.TrackApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usman on 21/3/2016.
 */
public class UserAdapter extends BaseAdapter {
    private List<User> userList;
    private int layout;
    private Activity activity;
    private DeviceListDialog dialog;

    public UserAdapter(Activity activity, DeviceListDialog dialog, int layout, JSONArray array) {
        userList=new ArrayList<>();
        this.layout=layout;
        this.activity=activity;
        this.dialog=dialog;
        try {
            for(int i=0; i<array.length();i++){
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
        if (convertView == null){
            holder= new viewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(layout,null);
            holder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            holder.txtEmail = (TextView) convertView.findViewById(R.id.txtEmail);
            if (layout==R.layout.list_item_user_action) {
                holder.btnHistory = (Button) convertView.findViewById(R.id.btnHistory);
                holder.btnTrack = (Button) convertView.findViewById(R.id.btnTrack);
            }
            convertView.setTag(holder);

        }
        else{
            holder = (viewHolder) convertView.getTag();
        }
            holder.txtName.setText(userList.get(position).name);
            holder.txtEmail.setText(userList.get(position).email);

            if (layout==R.layout.list_item_user_action){
            holder.btnHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TrackApi trackApi = new TrackApi();
                    trackApi.delegate= ((MainActivity) activity);
                    trackApi.execute("user_id="+SharedPrefs.getUserId()+"&track_id="+userList.get(position).id,"location_history");
                   dialog.close();
                }
            });
            holder.btnTrack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TrackApi trackApi = new TrackApi();
                    trackApi.delegate= ((MainActivity) activity);
                    trackApi.execute("user_id="+SharedPrefs.getUserId()+"&track_id="+userList.get(position).id,"track_user");
                    dialog.close();
                }
            });
        }


        return convertView;
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

    public static class viewHolder{
        TextView txtName;
        TextView txtEmail;
        Button btnTrack;
        Button btnHistory;
    }
}
