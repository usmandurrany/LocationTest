package com.fournodes.ud.locationtest.dialogs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fournodes.ud.locationtest.R;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.apis.RegisterApi;
import com.fournodes.ud.locationtest.interfaces.RequestResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Created by Usman on 21/3/2016.
 */
public class UserRegisterDialog implements RequestResult {
    private Context context;
    private Dialog dialog;
    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPicture;
    private Button btnRegister;

    public UserRegisterDialog(Context context) {
        this.context = context;
        dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_user_register);
        dialog.setCancelable(false);


    }

    public void show() {

        edtName = (EditText) dialog.findViewById(R.id.edtName);
        edtEmail = (EditText) dialog.findViewById(R.id.edtEmail);
        edtPicture = (EditText) dialog.findViewById(R.id.edtPicture);

        Pattern emailPattern; // API level 8+
        emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                edtEmail.setText(account.name);
            }
        }

        btnRegister = (Button) dialog.findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SharedPrefs.setUserName(edtName.getText().toString());
                    SharedPrefs.setUserEmail(edtEmail.getText().toString());
                    SharedPrefs.setUserPicture(edtPicture.getText().toString());
                    if (SharedPrefs.getDeviceGcmId() != null) {
                        RegisterApi registerApi = new RegisterApi();
                        registerApi.delegate = UserRegisterDialog.this;

                        registerApi.execute("email=" + URLEncoder.encode(SharedPrefs.getUserEmail(), "UTF-8") + "&name=" + URLEncoder.encode(SharedPrefs.getUserName(), "UTF-8") +
                                "&picture=" + URLEncoder.encode(SharedPrefs.getUserPicture(), "UTF-8"));

                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.show();


    }

    @Override
    public void onSuccess(String result) {
        dialog.dismiss();
    }

    @Override
    public void onFailure() {

    }


}

