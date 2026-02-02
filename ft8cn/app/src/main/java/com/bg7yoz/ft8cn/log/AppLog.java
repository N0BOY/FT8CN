package com.bg7yoz.ft8cn.log;

import android.content.Context;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

/**
 * Enhanced logging utility that writes to both logcat (Log.e) and persistent files.
 * This allows developers to use logcat while also providing user-accessible logs.
 * 
 * Usage:
 *   AppLog.e(TAG, "QRZ upload failed", e);  // Writes to both logcat and file
 *   AppLog.externalCall("QRZ", "qrz.com", "FAILED", "Connection timeout");
 * 
 * @author BGY70Z
 */
public class AppLog {
    private static final String TAG = "AppLog";
    private static ApplicationLogManager logManager = null;
    
    /**
     * Initialize the log manager (call once at app startup)
     */
    public static void init(Context context) {
        if (logManager == null && context != null) {
            logManager = new ApplicationLogManager(context);
        }
    }
    
    /**
     * Log an error - writes to both logcat and external calls log if it's an external service error
     */
    public static int e(String tag, String message) {
        int result = Log.e(tag, message);
        
        // Also write to file if it's an external service call
        if (isExternalServiceCall(message)) {
            writeToExternalCallsLog(tag, message);
        }
        
        return result;
    }
    
    /**
     * Log an error with exception - writes to both logcat and external calls log if relevant
     */
    public static int e(String tag, String message, Throwable tr) {
        int result = Log.e(tag, message, tr);
        
        // Also write to file if it's an external service call
        if (isExternalServiceCall(message)) {
            String fullMessage = message + (tr != null ? ": " + tr.getMessage() : "");
            writeToExternalCallsLog(tag, fullMessage);
        }
        
        return result;
    }
    
    /**
     * Log external service call explicitly
     */
    public static void externalCall(String service, String endpoint, String status, String details) {
        String message = String.format("%s call to %s: %s - %s", service, endpoint, status, details);
        
        // Write to logcat
        if ("FAILED".equals(status)) {
            Log.e("ExternalCall", message);
        } else {
            Log.d("ExternalCall", message);
        }
        
        // Write to file
        writeToExternalCallsLog("ExternalCall", message);
    }
    
    /**
     * Check if a log message is about an external service call
     */
    private static boolean isExternalServiceCall(String message) {
        if (message == null) return false;
        String msg = message.toUpperCase();
        return msg.contains("QRZ") || 
               msg.contains("PSK REPORTER") || 
               msg.contains("NTP") || 
               msg.contains("GPS") ||
               msg.contains("CLOUDLOG") ||
               msg.contains("EXTERNAL") ||
               msg.contains("API CALL") ||
               msg.contains("HTTP") ||
               msg.contains("NETWORK");
    }
    
    /**
     * Write to external calls log file
     */
    private static void writeToExternalCallsLog(String tag, String message) {
        if (logManager == null) {
            // Try to initialize if not already done
            Context context = GeneralVariables.getMainContext();
            if (context != null) {
                init(context);
            }
        }
        
        if (logManager != null) {
            try {
                logManager.writeLog(ApplicationLogManager.LogType.EXTERNAL_CALLS, 
                    String.format("[%s] %s", tag, message));
            } catch (Exception e) {
                // Don't log logging errors to avoid recursion
            }
        }
    }
    
    // Delegate other Log methods for convenience
    public static int d(String tag, String message) {
        return Log.d(tag, message);
    }
    
    public static int w(String tag, String message) {
        return Log.w(tag, message);
    }
    
    public static int i(String tag, String message) {
        return Log.i(tag, message);
    }
    
    public static int v(String tag, String message) {
        return Log.v(tag, message);
    }
}
