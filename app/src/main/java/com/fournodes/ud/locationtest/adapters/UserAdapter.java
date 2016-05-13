package com.fournodes.ud.locationtest.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.dialogs.UserListDialog;
import com.fournodes.ud.locationtest.objects.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Usman on 21/3/2016.
 */
public class UserAdapter extends BaseAdapter {
    private List<User> userList;
    private int layout;
    private Activity activity;
    private UserListDialog dialog;
    private int[][] selection;
    private List<String> selectedNames;
    private String assignmentData;
    private JSONObject assignmentJSON;

    public UserAdapter(Activity activity, UserListDialog dialog, int layout, List<User> userList) {
        this.userList = userList;
        this.layout = layout;
        this.activity = activity;
        this.dialog = dialog;
        selection = new int[userList.size()][2];
        selectedNames = new ArrayList<>();
    }

    public UserAdapter(Activity activity, UserListDialog dialog, int layout, List<User> userList, String assignmentData) {
        this.userList = userList;
        this.layout = layout;
        this.activity = activity;
        this.dialog = dialog;
        this.assignmentData = assignmentData;
        selection = new int[userList.size()][2];
        selectedNames = new ArrayList<>();
    }


    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final viewHolder holder;
        if (convertView == null) {
            holder = new viewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(layout, null);
            holder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            holder.txtEmail = (TextView) convertView.findViewById(R.id.txtEmail);
            if (layout == R.layout.list_item_user_action) {
                holder.btnHistory = (ImageButton) convertView.findViewById(R.id.btnHistory);
                holder.btnTrack = (ImageButton) convertView.findViewById(R.id.btnTrack);
            }
            else if (layout == R.layout.list_item_user_transition_type) {
                holder.chkEnter = (CheckBox) convertView.findViewById(R.id.chkEnter);
                holder.chkExit = (CheckBox) convertView.findViewById(R.id.chkExit);
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
                    String payload = "user_id=" + SharedPrefs.getUserId() + "&track_id=" + userList.get(position).id;
                    IncomingApi incomingApi = new IncomingApi(null, "location_history", payload, 0);
                    incomingApi.delegate = ((MainActivity) activity);
                    incomingApi.execute();
                    if (userList.get(position).id == Integer.parseInt(SharedPrefs.getUserId()))
                        dialog.showFabDeleteHistory();
                    dialog.close();

                }
            });
            holder.btnTrack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String payload = "user_id=" + SharedPrefs.getUserId() + "&track_id=" + userList.get(position).id;
                    IncomingApi incomingApi = new IncomingApi(null, "enable_track", payload, 0);
                    incomingApi.delegate = ((MainActivity) activity);
                    incomingApi.execute();
                    dialog.close();
                }
            });
        }
        else if (layout == R.layout.list_item_user_transition_type) {

            if (assignmentData != null) {
                try {
                    assignmentJSON = new JSONObject(assignmentData);
                    if (assignmentJSON.has(String.valueOf(userList.get(position).id))) {
                        int transitionType = Integer.parseInt(assignmentJSON.getString(String.valueOf(userList.get(position).id)));
                        if (transitionType == 1) {
                            selection[position][0] = 1;

                            holder.chkEnter.setChecked(true);
                        }
                        else if (transitionType == 2) {
                            selection[position][1] = 2;

                            holder.chkExit.setChecked(true);
                        }
                        else if (transitionType == 3) {
                            selection[position][0] = 1;
                            selection[position][1] = 2;

                            holder.chkExit.setChecked(true);
                            holder.chkEnter.setChecked(true);
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (selectedNames.size() > 0) {
                if (selection[position][0] != 0)
                    holder.chkEnter.setChecked(true);
                if (selection[position][1] != 0)
                    holder.chkExit.setChecked(true);

            }
            holder.chkEnter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((CheckBox)v).isChecked())
                        selection[position][0] = 1;
                    else {
                        if (assignmentJSON != null && assignmentJSON.has(String.valueOf(userList.get(position).id)) && !holder.chkExit.isChecked()) {
                            selection[position][0] = 10; // User removed
                        }
                        else
                            selection[position][0] = 0;
                    }

                }
            });
            holder.chkExit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((CheckBox)v).isChecked())
                        selection[position][1] = 2;
                    else {
                        if (assignmentJSON != null && assignmentJSON.has(String.valueOf(userList.get(position).id)) && !holder.chkEnter.isChecked()) {
                            selection[position][1] = 10; // User removed
                        }
                        else
                            selection[position][1] = 0;
                    }
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


    public String[] getNames() {
        String[] names = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            names[i] = userList.get(i).name;
        }
        return names;
    }

    public String[] getSelectedNames() {
        String[] temp = new String[selectedNames.size()];
        selectedNames.toArray(temp);
        return temp;
    }

    public String getNameAtPos(int pos) {
        return userList.get(pos).name;
    }

    public int getPosAtId(int id) {
        for (User user :
                userList) {
            if (user.id == id)
                return userList.indexOf(user);
        }
        return -1;
    }

    public HashMap<String, String> getSelection() {
        HashMap<String, String> data = new HashMap<>();
        int countAdd = 0;
        int countRemove = 0;
        selectedNames.clear();
        // will iteration on ROWS i.e selection[i]
        for (int i = 0; i < selection.length; i++) {
            if (selection[i][0] != 0 || selection[i][1] != 0) {
                int orValue = selection[i][0] | selection[i][1];
                if (orValue == 1 || orValue == 2 || orValue == 3) {
                    countAdd++;
                    selectedNames.add(userList.get(i).name);

                }
                else if (orValue == 10)
                    countRemove++;

                data.put(String.valueOf(userList.get(i).id), String.valueOf(orValue));
            }
        }

        return countAdd > 0 ? data : null;
    }

    public static class viewHolder {
        TextView txtName;
        TextView txtEmail;
        ImageButton btnTrack;
        ImageButton btnHistory;

        CheckBox chkEnter;
        CheckBox chkExit;
    }
}
