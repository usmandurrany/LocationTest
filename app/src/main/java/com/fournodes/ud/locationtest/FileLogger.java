/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.fournodes.ud.locationtest;


import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileLogger {
    private static final String TAG = "FileLogger";
    private static File logFile;

    private static boolean createFile(){
        try {
            if (logFile == null)
                logFile = new File("sdcard/location_log.txt");

            return logFile.exists() ? logFile.exists() : logFile.createNewFile();

        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void e(String TAG, String message) {
        if (createFile()) {
            try {
                //BufferedWriter for performance, true to set append to file flag
                String formattedDate = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(formattedDate + ": " + TAG + ": " + message);
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.e(TAG, message);
            }
        }else Log.e(FileLogger.TAG,"Unable to create log file");
    }

}
