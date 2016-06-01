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
import java.util.Locale;

public class FileLogger {
    private static final String TAG = "FileLogger";
    public static File logFile;

    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final String LOG_FILE_PREFIX = "location_log_";
    public static  String LOG_FILE_NAME;
    public static final String LOG_FILE_EXT = ".txt";

    public static BufferedWriter buf;

    private static String newFile(){
        LOG_FILE_NAME = LOG_FILE_PREFIX + df.format(Calendar.getInstance().getTime());
        return LOG_FILE_NAME + LOG_FILE_EXT;
    }

    public static void openFile() {
        try {

            logFile = new File("sdcard/" + newFile());

            if (!logFile.exists())
                logFile.createNewFile();

            //BufferedWriter for performance, true to set append to file flag
            buf = new BufferedWriter(new FileWriter(logFile, true));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteFile(){
        logFile = null;
        File deleteFile = new File("sdcard/" + LOG_FILE_NAME + LOG_FILE_EXT);
        return deleteFile.exists() && deleteFile.delete();
    }

    public static void e(String TAG, String message) {
        if (logFile == null || buf == null)
            openFile();

        try {
            String formattedDate = new SimpleDateFormat("HH:mm:ss",Locale.US).format(Calendar.getInstance().getTime());
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
