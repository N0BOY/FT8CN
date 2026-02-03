package com.bg7yoz.ft8cn.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for backing up and restoring application settings.
 * Settings are exported to/imported from JSON files.
 * 
 * @author BGY70Z
 * @date 2024
 */
public class SettingsBackupRestore {
    private static final String TAG = "SettingsBackupRestore";
    private static final String FILE_PROVIDER_AUTHORITY = "com.bg7yoz.ft8cn.fileprovider";
    private static final String BACKUP_FILE_PREFIX = "ft8cn_settings_backup_";
    private static final String BACKUP_FILE_SUFFIX = ".json";

    /**
     * Interface for backup operation callbacks
     */
    public interface OnBackupRestoreEvents {
        /**
         * Called when backup/restore operation starts
         * @param info Information message
         */
        void onStart(String info);

        /**
         * Called when backup/restore operation completes successfully
         * @param info Information message
         * @param file The backup file (for backup operations)
         */
        void onSuccess(String info, File file);

        /**
         * Called when backup/restore operation fails
         * @param info Error message
         */
        void onFailed(String info);
    }

    /**
     * Export all settings from the database to a JSON file
     * 
     * @param context Application context
     * @param db Database instance
     * @param callback Callback for operation events
     */
    public static void exportSettings(Context context, SQLiteDatabase db, OnBackupRestoreEvents callback) {
        new ExportSettingsTask(context, db, callback).execute();
    }

    /**
     * Import settings from a JSON file to the database
     * 
     * @param context Application context
     * @param db Database instance
     * @param inputStream InputStream of the JSON file
     * @param callback Callback for operation events
     */
    public static void importSettings(Context context, SQLiteDatabase db, InputStream inputStream, OnBackupRestoreEvents callback) {
        new ImportSettingsTask(context, db, inputStream, callback).execute();
    }

    /**
     * Get a FileProvider URI for sharing the backup file
     * 
     * @param context Application context
     * @param file Backup file
     * @return FileProvider URI
     */
    public static android.net.Uri getFileProviderUri(Context context, File file) {
        return FileProvider.getUriForFile(context.getApplicationContext(), FILE_PROVIDER_AUTHORITY, file);
    }

    /**
     * AsyncTask to export settings to JSON file
     */
    private static class ExportSettingsTask extends AsyncTask<Void, Void, ExportResult> {
        private final Context context;
        private final SQLiteDatabase db;
        private final OnBackupRestoreEvents callback;

        ExportSettingsTask(Context context, SQLiteDatabase db, OnBackupRestoreEvents callback) {
            this.context = context;
            this.db = db;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callback != null) {
                callback.onStart(context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_exporting));
            }
        }

        @Override
        protected ExportResult doInBackground(Void... voids) {
            try {
                // Query all config entries
                String querySQL = "SELECT keyName, Value FROM config";
                Cursor cursor = db.rawQuery(querySQL, null);
                
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("version", GeneralVariables.VERSION);
                jsonObject.put("buildDate", GeneralVariables.BUILD_DATE);
                jsonObject.put("backupDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                
                JSONObject settings = new JSONObject();
                int count = 0;
                
                while (cursor.moveToNext()) {
                    @SuppressLint("Range")
                    String keyName = cursor.getString(cursor.getColumnIndex("keyName"));
                    @SuppressLint("Range")
                    String value = cursor.getString(cursor.getColumnIndex("Value"));
                    
                    if (keyName != null) {
                        settings.put(keyName, value != null ? value : "");
                        count++;
                    }
                }
                
                cursor.close();
                
                jsonObject.put("settings", settings);
                jsonObject.put("settingsCount", count);
                
                // Create backup file
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File tempDir = context.getExternalCacheDir();
                if (tempDir == null) {
                    return new ExportResult(null, context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_no_cache_dir));
                }
                
                File backupFile = new File(tempDir, BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_SUFFIX);
                
                // Write JSON to file
                FileOutputStream fos = new FileOutputStream(backupFile);
                fos.write(jsonObject.toString(2).getBytes("UTF-8"));
                fos.close();
                
                return new ExportResult(backupFile, null);
                
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error exporting settings: " + e.getMessage(), e);
                return new ExportResult(null, e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(ExportResult result) {
            super.onPostExecute(result);
            if (callback != null) {
                if (result.file != null && result.error == null) {
                    String message = context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_export_success);
                    callback.onSuccess(message, result.file);
                } else {
                    String errorMsg = result.error != null ? result.error : context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_export_failed);
                    callback.onFailed(errorMsg);
                }
            }
        }
    }

    /**
     * AsyncTask to import settings from JSON file
     */
    private static class ImportSettingsTask extends AsyncTask<Void, Void, ImportResult> {
        private final Context context;
        private final SQLiteDatabase db;
        private final InputStream inputStream;
        private final OnBackupRestoreEvents callback;

        ImportSettingsTask(Context context, SQLiteDatabase db, InputStream inputStream, OnBackupRestoreEvents callback) {
            this.context = context;
            this.db = db;
            this.inputStream = inputStream;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callback != null) {
                callback.onStart(context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_importing));
            }
        }

        @Override
        protected ImportResult doInBackground(Void... voids) {
            try {
                // Read JSON from input stream
                StringBuilder jsonString = new StringBuilder();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    jsonString.append(new String(buffer, 0, bytesRead, "UTF-8"));
                }
                inputStream.close();
                
                JSONObject jsonObject = new JSONObject(jsonString.toString());
                
                // Validate backup file format
                if (!jsonObject.has("settings")) {
                    return new ImportResult(0, context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_invalid_format));
                }
                
                JSONObject settings = jsonObject.getJSONObject("settings");
                
                // Import settings to database
                int count = 0;
                db.beginTransaction();
                try {
                    // Delete existing config entries (optional - we could merge instead)
                    // For now, we'll replace all settings
                    
                    // Insert/update each setting
                    for (int i = 0; i < settings.length(); i++) {
                        String keyName = settings.names().getString(i);
                        String value = settings.getString(keyName);
                        
                        // Delete existing entry
                        String deleteSQL = "DELETE FROM config WHERE keyName = ?";
                        db.execSQL(deleteSQL, new String[]{keyName});
                        
                        // Insert new entry
                        String insertSQL = "INSERT INTO config (keyName, Value) VALUES (?, ?)";
                        db.execSQL(insertSQL, new String[]{keyName, value != null ? value : ""});
                        count++;
                    }
                    
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                
                return new ImportResult(count, null);
                
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error importing settings: " + e.getMessage(), e);
                return new ImportResult(0, e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(ImportResult result) {
            super.onPostExecute(result);
            if (callback != null) {
                if (result.count > 0 && result.error == null) {
                    String message = context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_import_success, result.count);
                    callback.onSuccess(message, null);
                } else {
                    String errorMsg = result.error != null ? result.error : context.getString(com.bg7yoz.ft8cn.R.string.backup_settings_import_failed);
                    callback.onFailed(errorMsg);
                }
            }
        }
    }

    /**
     * Result class for export operation
     */
    private static class ExportResult {
        final File file;
        final String error;

        ExportResult(File file, String error) {
            this.file = file;
            this.error = error;
        }
    }

    /**
     * Result class for import operation
     */
    private static class ImportResult {
        final int count;
        final String error;

        ImportResult(int count, String error) {
            this.count = count;
            this.error = error;
        }
    }
}
