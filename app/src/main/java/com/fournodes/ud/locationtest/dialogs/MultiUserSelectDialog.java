package com.fournodes.ud.locationtest.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.activities.MainActivity;
import com.fournodes.ud.locationtest.adapters.UserAdapter;
import com.fournodes.ud.locationtest.apis.IncomingApi;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.objects.User;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.FileLogger;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Usman on 11/5/2016.
 */
public class MultiUserSelectDialog implements RequestResult {
    public editAssignmentInterface delegate;
    private ListView lastCommon;
    private Context context;
    private AlertDialog.Builder db;
    private UserAdapter userAdapter;
    private int fenceId;
    private String assignmentData;
    private HashMap<String,String> selection;

    public MultiUserSelectDialog(Context context, final int fenceId) {
        this.context = context;
        this.fenceId = fenceId;
        init();
    }

    public MultiUserSelectDialog(Context context, int fenceId, String assignmentData) {
        this.context = context;
        this.fenceId = fenceId;
        this.assignmentData = assignmentData;
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialog_layout = inflater.inflate(R.layout.list_view_common, null);
        lastCommon = (ListView) dialog_layout.findViewById(R.id.lstCommon);

        db = new AlertDialog.Builder(context);
        db.setView(dialog_layout);
        db.setTitle("Select Users");
        db.setPositiveButton("OK", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        selection=userAdapter.getSelection();
                        if (selection != null) {
                            JSONObject assignmentData = new JSONObject(selection);
                            FileLogger.e("data", assignmentData.toString());
                            String payload = "user_id=" + SharedPrefs.getUserId() + "&fence_id=" + fenceId + "&assignment_data=" + assignmentData;
                            IncomingApi incomingApi = new IncomingApi(null, "edit_assignment", payload, 0);
                            incomingApi.delegate = MultiUserSelectDialog.this;
                            incomingApi.execute();
                        }else
                            Toast.makeText(context, "Changes discarded. You need to assign at least one user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void show() {
        String payload = "user_id=" + SharedPrefs.getUserId();
        IncomingApi incomingApi = new IncomingApi(null, "user_list", payload, 0);
        incomingApi.delegate = this;
        incomingApi.execute();

    }

    @Override
    public void onSuccess(String result) {
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        if (assignmentData != null && delegate != null) {
            delegate.fenceUpdated(result);
        }

    }

    @Override
    public void onFailure() {

    }

    @Override
    public void userList(List<User> users) {
        if (assignmentData != null)
            userAdapter = new UserAdapter((MainActivity) context, null, R.layout.list_item_user_transition_type, users, assignmentData);
        else
            userAdapter = new UserAdapter((MainActivity) context, null, R.layout.list_item_user_transition_type, users);

        lastCommon.setAdapter(userAdapter);
        db.show();

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
    public interface editAssignmentInterface{
        void fenceUpdated(String assignmentData);
    }
}
