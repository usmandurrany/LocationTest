/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.fournodes.ud.locationtest;


import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileLog {
    private File logFile;

    public FileLog() {
        logFile = new File("sdcard/location_log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();


            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void e(String TAG, String message)
    {


        try
        {
            //BufferedWriter for performance, true to set append to file flag
            String formattedDate = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(formattedDate+": "+TAG+": "+message);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally {
            Log.e(TAG,message);

        }
    }

}
