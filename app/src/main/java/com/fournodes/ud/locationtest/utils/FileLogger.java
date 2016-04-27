/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.fournodes.ud.locationtest.utils;


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

    private static Calendar c = Calendar.getInstance();
    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
    private static final String LOG_FILE_PREFIX = "location_log_";
    public static final String LOG_FILE_NAME = LOG_FILE_PREFIX + df.format(c.getTime()) + ".txt";


    private static boolean createFile() {
        try {


            logFile = new File("sdcard/" + LOG_FILE_NAME);

            if (!logFile.exists()) {

                int yesterday = Integer.parseInt(df.format(c.getTime())) - 1;
                String oldLogFileName = LOG_FILE_PREFIX + String.valueOf(yesterday) + ".txt";
                File oldLogFile = new File("sdcard/" + oldLogFileName);
                if (oldLogFile.exists())
                    oldLogFile.delete();

                return logFile.createNewFile();
            }

            return true;


        } catch (IOException e) {
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
                buf.append(formattedDate).append(": ").append(TAG).append(": ").append(message);
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.e(TAG, message);
            }
        }
        else Log.e(FileLogger.TAG, "Unable to create log file");
    }

}
