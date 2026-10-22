package com.kosratdahmad.mashxall.nativeapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Opens the original, verified offline content database read-only. */
final class DatabaseHelper {
    static final String DATABASE_ASSET = "muslim_db_v2.5.1.db";
    private final Context context;

    DatabaseHelper(Context context) { this.context = context.getApplicationContext(); }

    SQLiteDatabase open() throws Exception {
        File dbFile = new File(context.getNoBackupFilesDir(), DATABASE_ASSET);
        if (!dbFile.isFile() || dbFile.length() < 1_000_000L) {
            File tmp = new File(dbFile.getParentFile(), DATABASE_ASSET + ".tmp");
            try (InputStream in = context.getAssets().open(DATABASE_ASSET);
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.getFD().sync();
            }
            if (!tmp.isFile() || tmp.length() < 1_000_000L) {
                tmp.delete();
                throw new IllegalStateException("Offline database copy was incomplete.");
            }
            if (dbFile.exists() && !dbFile.delete()) {
                tmp.delete();
                throw new IllegalStateException("Unable to replace the old local database.");
            }
            if (!tmp.renameTo(dbFile)) {
                tmp.delete();
                throw new IllegalStateException("Unable to finish copying the offline database.");
            }
        }
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try (android.database.Cursor c = db.rawQuery("PRAGMA quick_check", null)) {
            if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) {
                db.close();
                throw new IllegalStateException("Offline database integrity check failed.");
            }
        }
        return db;
    }
}
