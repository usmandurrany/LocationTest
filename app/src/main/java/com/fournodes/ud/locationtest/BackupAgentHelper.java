package com.fournodes.ud.locationtest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.io.IOException;

/**
 * Created by Usman on 13/4/2016.
 */
public class BackupAgentHelper extends android.app.backup.BackupAgentHelper {
    static final String SHARED_PREFS_BACKUP_KEY = "SharedPrefs";
    static final String DATABASE_BACKUP_KEY = "Database";


    @Override
    public void onCreate() {
        addHelper(DATABASE_BACKUP_KEY, new DbBackupHelper(this, Database.DATABASE_FILE_NAME));
       // SharedPreferencesBackupHelper sharedPrefsBackup = new SharedPreferencesBackupHelper()

    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        createNotification("LocationTest","Data restored");

    }

    @Override
    public void onFullBackup(FullBackupDataOutput data) throws IOException {
        super.onFullBackup(data);
       // createNotification("LocationTest","Data backup complete");

    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);
      //  createNotification("LocationTest","Data backup complete");
    }

    private void createNotification(String from, String message) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        PendingIntent piActivityIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(Color.BLUE)
                .setContentTitle(from)
                .setContentText(message)
                .setContentIntent(piActivityIntent)
                .setAutoCancel(false);

        mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());


    }
}
