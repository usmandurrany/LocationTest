package com.fournodes.ud.locationtest;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
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
 * Created by Usman on 14/3/2016.
 */
public class DeviceLocator extends AsyncTask<String, String, String> {
    public RemoteDevice delegate;
    private final String TAG = "Device Locator";
    private String action;
    private String device;


    @Override
    protected String doInBackground(String... params) {
        action = params[0];
        device = (params.length>1?params[1]:"null");
        try {
            String url = SharedPrefs.SERVER_ADDRESS + "location.php?action="+ params[0]+"&device="+device;
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
           /* wr.writeBytes(postParams);
            wr.flush();
            wr.close();
            */
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            //System.out.println("Post parameters : " + postParams);
            System.out.println("Response Code : " + responseCode);

            return convertStreamToString(con.getInputStream());


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"Network Error");
            //delegate.failure();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        try {
        if (s != null){
            Log.e(TAG,s);
            if (action != null){
                switch (action){
                    case "live":
                        if (delegate!= null) {
                            JSONObject result = new JSONObject(s);
                            delegate.liveLocationUpdate(result.getString("latitude"),result.getString("longitude"),device);
                        }
                        break;
                    case "history":
                        if (delegate!= null) {
                            delegate.locationHistory(new JSONArray(s),device);
                        }
                        break;
                    case "device_list":
                        if (delegate!= null) {
                            delegate.deviceList(new JSONArray(s));
                        }
                        break;
                }
            }
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
