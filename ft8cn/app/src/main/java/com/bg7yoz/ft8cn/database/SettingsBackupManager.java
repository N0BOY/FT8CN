package com.bg7yoz.ft8cn.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * Manages backup and restore of application settings.
 * Settings are exported to/imported from JSON files.
 *
 * @author N0BOY
 * @date 2026-02-02
 */
public class SettingsBackupManager {
    private static final String TAG = "SettingsBackupManager";
    private static final String BACKUP_VERSION = "1.0";
    private static final String BACKUP_FILE_PREFIX = "ft8cn_settings_";
    private static final String BACKUP_FILE_EXTENSION = ".json";

    private final Context context;
    private final DatabaseOpr databaseOpr;

    public interface BackupCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public SettingsBackupManager(Context context, DatabaseOpr databaseOpr) {
        this.context = context;
        this.databaseOpr = databaseOpr;
    }

    /**
     * Export all settings to a JSON object
     */
    public JSONObject exportSettings() throws JSONException {
        JSONObject backup = new JSONObject();
        JSONObject settings = new JSONObject();

        // Add backup metadata
        backup.put("version", BACKUP_VERSION);
        backup.put("app", "FT8CN");
        backup.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        backup.put("callsign", GeneralVariables.myCallsign);

        // Read all settings from config table
        SQLiteDatabase db = databaseOpr.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT KeyName, Value FROM config", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String key = cursor.getString(0);
                    String value = cursor.getString(1);
                    if (key != null && value != null) {
                        settings.put(key, value);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Also include settings that might only be in GeneralVariables (not yet saved)
        addCurrentSettings(settings);

        backup.put("settings", settings);
        return backup;
    }

    /**
     * Add current in-memory settings to ensure nothing is missed
     */
    private void addCurrentSettings(JSONObject settings) throws JSONException {
        // Audio settings
        if (!settings.has("volumeValue")) {
            settings.put("volumeValue", String.valueOf((int)(GeneralVariables.volumePercent * 100)));
        }
        if (!settings.has("micInputGain")) {
            settings.put("micInputGain", String.valueOf((int)(GeneralVariables.micInputGain * 100)));
        }
        if (!settings.has("audioBits")) {
            settings.put("audioBits", GeneralVariables.audioOutput32Bit ? "1" : "0");
        }
        if (!settings.has("audioRate")) {
            settings.put("audioRate", String.valueOf(GeneralVariables.audioSampleRate));
        }

        // Station settings
        if (!settings.has("callsign")) {
            settings.put("callsign", GeneralVariables.myCallsign != null ? GeneralVariables.myCallsign : "");
        }
        if (!settings.has("grid")) {
            settings.put("grid", GeneralVariables.getMyMaidenheadGrid());
        }
        if (!settings.has("toModifier")) {
            settings.put("toModifier", GeneralVariables.toModifier != null ? GeneralVariables.toModifier : "");
        }
        if (!settings.has("parkNumber")) {
            settings.put("parkNumber", GeneralVariables.parkNumber != null ? GeneralVariables.parkNumber : "");
        }

        // Radio settings
        if (!settings.has("ctrMode")) {
            settings.put("ctrMode", String.valueOf(GeneralVariables.controlMode));
        }
        if (!settings.has("model")) {
            settings.put("model", String.valueOf(GeneralVariables.modelNo));
        }
        if (!settings.has("instruction")) {
            settings.put("instruction", String.valueOf(GeneralVariables.instructionSet));
        }
        if (!settings.has("baudRate")) {
            settings.put("baudRate", String.valueOf(GeneralVariables.baudRate));
        }
        if (!settings.has("civ")) {
            settings.put("civ", GeneralVariables.getCivAddressStr());
        }

        // Transmit settings
        if (!settings.has("freq")) {
            settings.put("freq", GeneralVariables.getBaseFrequencyStr());
        }
        if (!settings.has("synFreq")) {
            settings.put("synFreq", GeneralVariables.synFrequency ? "1" : "0");
        }
        if (!settings.has("fakeItSplit")) {
            settings.put("fakeItSplit", GeneralVariables.fakeItSplit ? "1" : "0");
        }
        if (!settings.has("transDelay")) {
            settings.put("transDelay", GeneralVariables.getTransmitDelayStr());
        }
        if (!settings.has("pttDelay")) {
            settings.put("pttDelay", String.valueOf(GeneralVariables.pttDelay));
        }
        if (!settings.has("transmitOffset")) {
            settings.put("transmitOffset", String.valueOf(GeneralVariables.transmitOffsetHz));
        }

        // Operating settings
        if (!settings.has("autoFollowCQ")) {
            settings.put("autoFollowCQ", GeneralVariables.autoFollowCQ ? "1" : "0");
        }
        if (!settings.has("autoCallFollow")) {
            settings.put("autoCallFollow", GeneralVariables.autoCallFollow ? "1" : "0");
        }
        if (!settings.has("callingAddsToFollowList")) {
            settings.put("callingAddsToFollowList", GeneralVariables.callingAddsToFollowList ? "1" : "0");
        }
        if (!settings.has("skipMyGridWhenResponding")) {
            settings.put("skipMyGridWhenResponding", GeneralVariables.skip_my_grid_when_responding ? "1" : "0");
        }
        if (!settings.has("launchSupervision")) {
            settings.put("launchSupervision", String.valueOf(GeneralVariables.launchSupervision));
        }
        if (!settings.has("noReplyLimit")) {
            settings.put("noReplyLimit", String.valueOf(GeneralVariables.noReplyLimit));
        }

        // Decode settings
        if (!settings.has("deepMode")) {
            settings.put("deepMode", GeneralVariables.deepDecodeMode ? "1" : "0");
        }
        if (!settings.has("msgMode")) {
            settings.put("msgMode", GeneralVariables.simpleCallItemMode ? "1" : "0");
        }
        if (!settings.has("decodeOverrunToast")) {
            settings.put("decodeOverrunToast", GeneralVariables.decode_overrun_toast ? "1" : "0");
        }

        // Cloud settings
        if (!settings.has("enableCloudlog")) {
            settings.put("enableCloudlog", GeneralVariables.enableCloudlog ? "1" : "0");
        }
        if (!settings.has("enableQRZ")) {
            settings.put("enableQRZ", GeneralVariables.enableQRZ ? "1" : "0");
        }
        if (!settings.has("enablePskReporterReceive")) {
            settings.put("enablePskReporterReceive", GeneralVariables.enablePskReporterReceive ? "1" : "0");
        }

        // Time settings
        if (!settings.has("ntpServer")) {
            settings.put("ntpServer", GeneralVariables.ntpServer != null ? GeneralVariables.ntpServer : "");
        }
        if (!settings.has("enableGpsTimeSync")) {
            settings.put("enableGpsTimeSync", GeneralVariables.enableGpsTimeSync ? "1" : "0");
        }

        // Logging settings
        if (!settings.has("saveSWL")) {
            settings.put("saveSWL", GeneralVariables.saveSWLMessage ? "1" : "0");
        }
        if (!settings.has("saveSWLQSO")) {
            settings.put("saveSWLQSO", GeneralVariables.saveSWL_QSO ? "1" : "0");
        }

        // Monitoring settings
        if (!settings.has("swrSwitch")) {
            settings.put("swrSwitch", GeneralVariables.swr_switch_on ? "1" : "0");
        }
        if (!settings.has("alcSwitch")) {
            settings.put("alcSwitch", GeneralVariables.alc_switch_on ? "1" : "0");
        }
    }

    /**
     * Generate default backup filename
     */
    public String getDefaultBackupFilename() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String callsign = GeneralVariables.myCallsign != null && !GeneralVariables.myCallsign.isEmpty()
                ? GeneralVariables.myCallsign + "_" : "";
        return BACKUP_FILE_PREFIX + callsign + timestamp + BACKUP_FILE_EXTENSION;
    }

    /**
     * Write backup to output stream (for SAF)
     */
    public void writeBackupToStream(OutputStream outputStream, BackupCallback callback) {
        try {
            JSONObject backup = exportSettings();
            String jsonString = backup.toString(2); // Pretty print with 2 space indent
            outputStream.write(jsonString.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            int settingsCount = backup.getJSONObject("settings").length();
            callback.onSuccess("Exported " + settingsCount + " settings");
        } catch (Exception e) {
            Log.e(TAG, "Error writing backup: " + e.getMessage(), e);
            callback.onError("Backup failed: " + e.getMessage());
        }
    }

    /**
     * Import settings from JSON
     */
    public void importSettings(JSONObject backup, BackupCallback callback) {
        try {
            // Validate backup format
            if (!backup.has("version") || !backup.has("settings")) {
                callback.onError("Invalid backup file format");
                return;
            }

            String version = backup.getString("version");
            Log.d(TAG, "Importing backup version: " + version);

            JSONObject settings = backup.getJSONObject("settings");
            int count = 0;

            // Import each setting
            Iterator<String> keys = settings.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = settings.getString(key);

                // Write to database
                databaseOpr.writeConfig(key, value, null);
                count++;
            }

            // Reload settings into GeneralVariables
            databaseOpr.getAllConfigParameter(null);

            callback.onSuccess("Imported " + count + " settings. Please restart app for changes to take effect.");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing backup: " + e.getMessage(), e);
            callback.onError("Invalid backup file: " + e.getMessage());
        }
    }

    /**
     * Read backup from input stream (for SAF)
     */
    public void readBackupFromStream(InputStream inputStream, BackupCallback callback) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();

            JSONObject backup = new JSONObject(builder.toString());
            importSettings(backup, callback);
        } catch (IOException e) {
            Log.e(TAG, "Error reading backup file: " + e.getMessage(), e);
            callback.onError("Could not read backup file: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing backup: " + e.getMessage(), e);
            callback.onError("Invalid backup file format: " + e.getMessage());
        }
    }
}
