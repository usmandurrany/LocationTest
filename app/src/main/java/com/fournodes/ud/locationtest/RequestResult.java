package com.fournodes.ud.locationtest;

/**
 * Created by Usman on 18/2/2016.
 */
public interface RequestResult {
    void onSuccess(String result);

    void onFailure();
}
