package com.fournodes.ud.locationtest.apis;

import android.os.AsyncTask;
import android.util.Log;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.TrackApiResult;

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
public class TrackApi extends AsyncTask<String, String, String> {
    public TrackApiResult delegate;
    private final String TAG = "Track Api";
    private String type;


    @Override
    protected String doInBackground(String... params) {
        type = params[1];
        try {
            String url = SharedPrefs.SERVER_ADDRESS + "incoming.php?type=" + type;
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
            wr.writeBytes(params[0]);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + params[0]);
            System.out.println("Response Code : " + responseCode);

            return convertStreamToString(con.getInputStream());


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Network Error");
            //delegate.onFailure();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        try {
            if (s != null) {
                Log.i(TAG, s);
                if (type != null) {
                    switch (type) {
                        case "track_user":
                            if (delegate != null) {
                                JSONObject result = new JSONObject(s);
                                delegate.liveLocationUpdate(result.getString("latitude"), result.getString("longitude"), result.getString("track_id"));
                            }
                            break;
                        case "location_history":
                            if (delegate != null) {
                                delegate.locationHistory(new JSONArray(s));
                            }
                            break;
                        case "user_list":
                            if (delegate != null) {
                                delegate.userList(new JSONArray(s));
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
