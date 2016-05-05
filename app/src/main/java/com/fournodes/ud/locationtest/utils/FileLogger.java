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
    public static File logFile;

    private static Calendar c = Calendar.getInstance();
    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
    private static final String LOG_FILE_PREFIX = "location_log_";
    public static final String LOG_FILE_NAME = LOG_FILE_PREFIX + df.format(c.getTime());
    public static final String LOG_FILE_EXT = ".txt";

    public static BufferedWriter buf;


    private static void openFile() {
        try {

            logFile = new File("sdcard/" + LOG_FILE_NAME + LOG_FILE_EXT);

            if (!logFile.exists())
                logFile.createNewFile();

            //BufferedWriter for performance, true to set append to file flag
            buf = new BufferedWriter(new FileWriter(logFile, true));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean dateChanged() {
        String[] fileName = LOG_FILE_NAME.split("_");
        if (Long.parseLong(fileName[2]) < Long.parseLong(df.format(c.getTime()))) {
            deleteOldFile();
            return true;
        }
        else
            return false;
    }

    private static boolean deleteOldFile() {
        long yesterday = Long.parseLong(df.format(c.getTime())) - 1;
        String oldLogFileName = LOG_FILE_PREFIX + String.valueOf(yesterday) + LOG_FILE_EXT;
        File oldLogFile = new File("sdcard/" + oldLogFileName);
        return oldLogFile.exists() && oldLogFile.delete();
    }

    public static boolean deleteFile(){
        File deleteFile = new File("sdcard/" + LOG_FILE_NAME + LOG_FILE_EXT);
        return deleteFile.exists() && deleteFile.delete();
    }

    public static void e(String TAG, String message) {
        if (logFile == null || buf == null || dateChanged())
            openFile();

        try {
            String formattedDate = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            buf.append(formattedDate).append(": ").append(TAG).append(": ").append(message);
            buf.newLine();
            buf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
           // if (TAG.equals("Location Service") || TAG.equals("RequestLocUpdateThread"))
            Log.e(TAG, message);
        }

    }

    public static void closeFile() {
        if (logFile != null && buf != null) {
            try {
                buf.append("Logging Stopped");
                buf.newLine();
                buf.close();
                buf = null;
                logFile = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
