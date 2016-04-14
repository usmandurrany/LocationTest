package com.fournodes.ud.locationtest;

import android.app.backup.FileBackupHelper;
import android.content.Context;

/**
 * Created by Usman on 13/4/2016.
 */
public class DbBackupHelper extends FileBackupHelper {

    public DbBackupHelper(Context ctx, String dbName) {
        super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
    }
}