package com.fournodes.ud.locationtest.network;

import android.content.Context;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.util.Log;

import com.fournodes.ud.locationtest.Database;
import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.RequestResult;

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
 * Created by Usman on 18/2/2016.
 */
public class LocationUpdateApi extends AsyncTask<Long,String,String> {
    private static final String TAG= "Api Handler";
    public RequestResult delegate;
    private long time;
    private Database db;
    private Context context;
    private JSONArray payload;

    public LocationUpdateApi(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Long... longs) {
        time = longs[0]; // Time sent by LocationService.updServerAfterInterval()
        db = new Database(context);
        payload = db.getLocEntries(time);
        if (payload.length()>0) {
            TrafficStats.setThreadStatsTag(0xF00D);

            try {
                String url = SharedPrefs.SERVER_ADDRESS + "incoming.php?type=location_update";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setConnectTimeout(15000);
                con.setReadTimeout(15000);
                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                con.setRequestProperty("Accept", "*/*");

                String postParams = "payload=" + payload.toString();

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams+"&user_id="+SharedPrefs.getUserId());
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + postParams);
                System.out.println("Response Code : " + responseCode);

                return convertStreamToString(con.getInputStream());


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"Network Error");
                delegate.failure();
            }
            finally {
                TrafficStats.clearThreadStatsTag();

            }
        }else
             this.cancel(true);

    //Log.e("Server Resp",result);
    return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null){
            try{
                JSONObject response = new JSONObject(result);
                if (response.getString("result").equals("1")){
                    db.removeLocEntries(time); //Remove from db after successfully sending to server
                    delegate.success(null);
                }else
                    delegate.failure();

            }catch (JSONException e){e.printStackTrace();}
        }else {
            Log.e(TAG,"Result is null");
        }
        super.onPostExecute(result);
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
