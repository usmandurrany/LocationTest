package com.fournodes.ud.locationtest.apis;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.RequestResult;
import com.fournodes.ud.locationtest.objects.Coordinate;
import com.fournodes.ud.locationtest.objects.User;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usman on 17/3/2016.
 */
public class IncomingApi extends AsyncTask<String, String, String> {
    public RequestResult delegate;
    private final String TAG = "IncomingApi";
    private String payload;
    private String type;
    private int retryCount = 0;
    private Context context;

    public IncomingApi(Context context, String type, String payload, int retryCount) {
        this.context = context;
        this.type = type;

        switch (type) {
            case "register":
            case "acknowledge":
            case "create_fence":
            case "edit_fence":
            case "remove_fence":
            case "user_list":
            case "enable_track":
            case "disable_track":
            case "track_user":
            case "location_history":
            case "delete_history":
            case "location_update":
                this.payload = payload;
                break;
            case "notify":
                this.payload = payload + "&sent_time=" + System.currentTimeMillis();
                this.retryCount = retryCount;
                break;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        if (payload.length() > 0) {
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

                //Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(payload);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + payload);
                System.out.println("Response Code : " + responseCode);

                return convertStreamToString(con.getInputStream());


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Network Error");
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String serverResponse) {
        if (serverResponse != null) {
            try {

                JSONObject response = new JSONObject(serverResponse);
                Log.e(TAG, response.toString());

                switch (type) {
                    case "register":
                        if (!response.getString("result").equals("failed")) {
                            SharedPrefs.setUserId(response.getString("result"));
                            if (delegate != null)
                                delegate.onSuccess(null);
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;

                    case "create_fence":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.onSuccess(response.getString("result"));
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;
                    case "edit_fence":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.onSuccess(response.getString("result"));
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;
                    case "remove_fence":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.onSuccess(response.getString("result"));
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;
                    case "user_list":
                        if (delegate != null) {
                            List<User> users = new ArrayList<>();
                            JSONArray usersDetails = response.getJSONArray("result");
                            for (int i = 0; i < usersDetails.length(); i++) {
                                User user = new User();
                                JSONObject userDetails = usersDetails.getJSONObject(i);
                                user.id = Integer.parseInt(userDetails.getString("user_id"));
                                user.name = userDetails.getString("user_name");
                                user.email = userDetails.getString("user_email");
                                user.picture = userDetails.getString("user_picture");
                                users.add(user);
                            }
                            delegate.userList(users);
                        }
                        break;

                    case "disable_track":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.trackDisabled();
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;

                    case "enable_track":
                    case "track_user":
                        if (!response.getString("result").equals("failed")) {
                            JSONObject result = response.getJSONObject("result");
                            if (type.equals("enable_track")) {
                                SharedPrefs.setLiveSessionId(Integer.parseInt(result.getString("live_session_id")));
                                if (delegate != null)
                                    delegate.trackEnabled();
                            }
                            delegate.liveLocationUpdate(result.getString("latitude"),
                                    result.getString("longitude"),
                                    result.getString("time"),
                                    result.getString("track_id"));
                        }

                        else if (delegate != null)
                            delegate.onFailure();
                        break;

                    case "location_history":
                        if (delegate != null) {
                            List<Coordinate> coordinates = new ArrayList<>();
                            JSONArray locationHistory = response.getJSONArray("result");
                            for (int i = 0; i < locationHistory.length(); i++) {
                                Coordinate coordinate = new Coordinate();
                                JSONObject locationDetails = locationHistory.getJSONObject(i);
                                coordinate.id = i + 1;
                                coordinate.latitude = Double.parseDouble(locationDetails.getString("latitude"));
                                coordinate.longitude = Double.parseDouble(locationDetails.getString("longitude"));
                                coordinate.time = Long.parseLong(locationDetails.getString("time"));
                                coordinates.add(coordinate);
                            }
                            delegate.locationHistory(coordinates);
                        }
                        break;

                    case "location_update":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.onSuccess(null);
                            if (SharedPrefs.isLive() && response.getInt("liveUsers") == 0) {
                                SharedPrefs.setIsLive(false);
                                SharedPrefs.setUpdateServerRowThreshold(5);
                            }
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;

                    case "acknowledge":
                        if (!response.getString("result").equals("failed")) {
                            if (delegate != null)
                                delegate.onSuccess(null);
                        }
                        else if (delegate != null)
                            delegate.onFailure();
                        break;
                    case "notify":
                        if (response.getString("success").equals("1")) {
                            if (delegate != null)
                                delegate.onSuccess(null);
                            Log.i(TAG, "Result: Success");
                        }
                        else if (delegate != null) {
                            delegate.onFailure();
                            Log.i(TAG, "Result: Failed");
                        }
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        else if (type.equals("notify")) {
            Log.i(TAG, "Retrying..");
            final Handler retry = new Handler();
            retry.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (retryCount > 0) {
                        new IncomingApi(context, type, payload, --retryCount).execute();
                        retry.removeCallbacks(this);
                    }
                    else {
                        Log.i(TAG, "Discarded Notification");
                        retry.removeCallbacks(this);
                    }
                }
            }, retryCount * 60000);

        }
        else {
            if (delegate != null)
                delegate.onFailure();
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
