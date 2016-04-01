package com.fournodes.ud.locationtest.network;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.fournodes.ud.locationtest.FileLogger;
import com.fournodes.ud.locationtest.SharedPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Usman on 16/3/2016.
 */
public class NotificationApi extends AsyncTask<String, String, String> {
    private String postData;
    private String getData;
    private int retryCount = 1;

    public static final String TAG = "Network";

    public NotificationApi() {
    }

    public NotificationApi(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            getData = params[0];
            postData = params[1]+"&sent_time="+System.currentTimeMillis();
            String url = SharedPrefs.SERVER_ADDRESS + "incoming.php?type=" + params[0];
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
            con.setRequestProperty("Accept", "*/*");


            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + params[1]);
            System.out.println("Response Code : " + responseCode);

            return convertStreamToString(con.getInputStream());


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            FileLogger.e(TAG, "Command: " + getData);
            FileLogger.e(TAG, "Data: " + postData);
            FileLogger.e(TAG, "Result: Network Error");
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        try {
            if (s != null) {
                Log.e(TAG, s);
                JSONObject result = new JSONObject(s);
                FileLogger.e(TAG, "Command: " + getData);
                FileLogger.e(TAG, "Data: " + postData);
                if (result.getString("success").equals("1"))
                    FileLogger.e(TAG, "Result: Success");
                else
                    FileLogger.e(TAG, "Result: Failure");
            } else {
                FileLogger.e(TAG, "Retrying..");
                final Handler retry = new Handler();
                retry.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (retryCount != 7) {
                            new NotificationApi(retryCount++).execute(getData, postData);
                            retry.removeCallbacks(this);
                        }else{
                            FileLogger.e(TAG, "Discarded Notification");
                            retry.removeCallbacks(this);
                        }
                    }
                }, retryCount * 60000);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static String convertStreamToString(InputStream is) {
    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
