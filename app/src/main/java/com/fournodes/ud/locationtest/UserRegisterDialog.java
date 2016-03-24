package com.fournodes.ud.locationtest;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fournodes.ud.locationtest.network.RegisterApi;

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
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_user_register);
        dialog.setCancelable(false);
    }

    public void show(){
        edtName = (EditText) dialog.findViewById(R.id.edtName);
        edtEmail = (EditText) dialog.findViewById(R.id.edtEmail);
        edtPicture = (EditText) dialog.findViewById(R.id.edtPicture);

        btnRegister = (Button) dialog.findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPrefs.setUserName(edtName.getText().toString());
                SharedPrefs.setUserEmail(edtEmail.getText().toString());
                SharedPrefs.setUserPicture(edtPicture.getText().toString());
                if (SharedPrefs.getDeviceGcmId() !=null) {
                    RegisterApi registerApi = new RegisterApi();
                    registerApi.delegate=UserRegisterDialog.this;
                    registerApi.execute("email=" + SharedPrefs.getUserEmail() + "&name=" + SharedPrefs.getUserName() +
                            "&picture=" + SharedPrefs.getUserPicture());
                }
            }
        });

        dialog.show();
    }

    @Override
    public void success(String result) {
        dialog.dismiss();
    }

    @Override
    public void failure() {

    }


}

