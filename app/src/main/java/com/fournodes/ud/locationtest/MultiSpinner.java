package com.fournodes.ud.locationtest;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.fournodes.ud.locationtest.adapters.UserAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Usman on 10/5/2016.
 */
public class MultiSpinner extends Spinner implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

    private String[] items;
    private boolean[] selected;
    private String defaultText;
    private MultiSpinnerListener listener;
    private UserAdapter userAdapter;
    private List<String> selectedUserIds;

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
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (isChecked){
            selectedUserIds.add(String.valueOf(userAdapter.getItemId(which)));
            selected[which] = true;
        }else{
            selectedUserIds.remove(selectedUserIds.indexOf(String.valueOf(userAdapter.getItemId(which))));
            selected[which] = false;}
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // refresh text on spinner
        StringBuffer spinnerBuffer = new StringBuffer();
        boolean someUnselected = false;
        for (int i = 0; i < items.length; i++) {
            if (selected[i] == true) {
                spinnerBuffer.append(items[i]);
                spinnerBuffer.append(", ");
            }
            else {
                someUnselected = true;
            }
        }
        String spinnerText;
        if (someUnselected) {
            spinnerText = spinnerBuffer.toString();
            if (spinnerText.length() > 2)
                spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
        }
        else {
            spinnerText = defaultText;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{spinnerText});
        setAdapter(adapter);
        listener.onItemsSelected(selected,selectedUserIds);
    }

    @Override
    public boolean performClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(items, selected, this);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setOnCancelListener(this);
        builder.show();
        return true;
    }


    public void setUserAdapter(UserAdapter userAdapter, MultiSpinnerListener listener) {
        this.userAdapter = userAdapter;
        this.listener = listener;
        this.items = userAdapter.getNames();
        selectedUserIds = new ArrayList<>();

        selected = new boolean[userAdapter.getCount()];
        Arrays.fill(selected, false);

        // Show default text on spinner
        ArrayAdapter<String> defaultAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, new String[]{"Select User"});
        setAdapter(defaultAdapter);
    }

    public interface MultiSpinnerListener {
        public void onItemsSelected(boolean[] selected, List<String> selectedUserIds);
    }
}