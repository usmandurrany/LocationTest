package com.fournodes.ud.locationtest.threads;

import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;

import com.fournodes.ud.locationtest.SharedPrefs;
import com.fournodes.ud.locationtest.interfaces.FenceListInterface;
import com.fournodes.ud.locationtest.objects.Fence;
import com.fournodes.ud.locationtest.utils.Database;
import com.fournodes.ud.locationtest.utils.DistanceCalculator;
import com.fournodes.ud.locationtest.utils.FileLogger;

import java.util.List;

/**
 * Created by Usman on 20/4/2016.
 */
public class RecalculateDistanceThread extends HandlerThread {
    public FenceListInterface delegate;
    private static final String TAG = "RecalculateDistanceThread";

    private Context context;
    private Location location;
    private List<Fence> fenceListAll;

    public RecalculateDistanceThread(Context context, Location location) {
        super(TAG);
        this.context = context;
        this.location = location;

        if (SharedPrefs.pref == null)
            new SharedPrefs(context).initialize();

            SharedPrefs.setReCalcDistanceAtLatitude(String.valueOf(location.getLatitude()));
            SharedPrefs.setReCalcDistanceAtLongitude(String.valueOf(location.getLongitude()));

    }

    @Override
    public synchronized void start() {
        super.start();
        FileLogger.e(TAG, "Thread started");
        Database db = new Database(context);
        fenceListAll = DistanceCalculator.updateDistanceFromFences(context, location, db.onDeviceFence("getAll"), true);
        if (delegate != null) {
            delegate.activeFenceList(fenceListAll, TAG);
        }
        quit();
    }

    @Override
    public boolean quit() {
        FileLogger.e(TAG, "Thread killed");
        return super.quit();
    }
}
