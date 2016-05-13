package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.fournodes.ud.locationtest.adapters.UserAdapter;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Usman on 10/5/2016.
 */
public class MultiSpinner extends Spinner{

    private String[] items;
    private boolean[] selected;
    private String defaultText;
    private MultiSpinnerListener listener;
    private UserAdapter userAdapter;
    private List<String> selectedUserIds;
    private List<String> selectedUserNames;
    private  ListView lstCommon;
    private HashMap<String,String> selection;


    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }




    @Override
    public boolean performClick() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialog_layout = inflater.inflate(R.layout.list_view_common, null);
        lstCommon = (ListView) dialog_layout.findViewById(R.id.lstCommon);
        lstCommon.setAdapter(userAdapter);
        AlertDialog.Builder db = new AlertDialog.Builder(getContext());
        db.setView(dialog_layout);
        db.setTitle("Select Users");
        db.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selection = userAdapter.getSelection();
                if (selection != null) {
                    JSONObject assignmentData = new JSONObject(selection);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, new String[]{Arrays.toString(userAdapter.getSelectedNames())});
                    setAdapter(adapter);
                    listener.onItemsSelected(assignmentData, selectedUserIds, selectedUserNames);
                }else {
                    ArrayAdapter<String> defaultAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, new String[]{"Select User"});
                    setAdapter(defaultAdapter);

                }
            }
        });
        db.show();

        return true;
    }


    public void setUserAdapter(UserAdapter userAdapter, MultiSpinnerListener listener) {
        this.listener = listener;
        this.userAdapter = userAdapter;
        ArrayAdapter<String> defaultAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, new String[]{"Select User"});
        setAdapter(defaultAdapter);
    }

    public interface MultiSpinnerListener {
        void onItemsSelected(JSONObject payload, List<String> selectedUserIds, List<String> selectedUserNames);
    }
}