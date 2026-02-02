package com.bg7yoz.ft8cn.log;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages application log files for toast notifications and external service calls.
 * @author BGY70Z
 */
public class ApplicationLogManager {
    private static final String TAG = "ApplicationLogManager";
    private static final int MAX_LOG_SIZE = 100000; // 100KB max per log file
    private static final int MAX_LINES = 1000; // Keep last 1000 lines when trimming
    
    public enum LogType {
        TOAST_NOTIFICATIONS("toast_log.txt"),
        EXTERNAL_CALLS("external_calls_log.txt");
        
        private final String filename;
        
        LogType(String filename) {
            this.filename = filename;
        }
        
        public String getFilename() {
            return filename;
        }
    }
    
    private final Context context;
    private final SimpleDateFormat dateFormat;
    
    public ApplicationLogManager(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }
    
    /**
     * Get the log file for a specific log type
     */
    private File getLogFile(LogType logType) {
        File logDir = new File(context.getFilesDir(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        return new File(logDir, logType.getFilename());
    }
    
    /**
     * Write a log entry to the specified log file
     */
    public synchronized void writeLog(LogType logType, String message) {
        try {
            File logFile = getLogFile(logType);
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("[%s] %s\n", timestamp, message);
            
            // Append to file
            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                writer.append(logEntry);
                writer.flush();
            }
            
            // Trim file if it gets too large
            trimLogFile(logFile);
            
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file: " + logType.getFilename(), e);
        }
    }
    
    /**
     * Read log entries from the specified log file
     */
    public synchronized List<String> readLog(LogType logType, int maxLines) {
        List<String> lines = new ArrayList<>();
        File logFile = getLogFile(logType);
        
        if (!logFile.exists()) {
            return lines;
        }
        
        try (FileInputStream fis = new FileInputStream(logFile);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader reader = new BufferedReader(isr)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            // Return last maxLines if there are more
            if (lines.size() > maxLines) {
                return lines.subList(lines.size() - maxLines, lines.size());
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading log file: " + logType.getFilename(), e);
        }
        
        return lines;
    }
    
    /**
     * Get all log entries from the specified log file
     */
    public synchronized String getAllLogText(LogType logType) {
        List<String> lines = readLog(logType, Integer.MAX_VALUE);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Clear the log file
     */
    public synchronized void clearLog(LogType logType) {
        File logFile = getLogFile(logType);
        if (logFile.exists()) {
            logFile.delete();
        }
    }
    
    /**
     * Trim log file if it exceeds MAX_LOG_SIZE by keeping only the last MAX_LINES
     */
    private void trimLogFile(File logFile) {
        try {
            if (logFile.length() > MAX_LOG_SIZE) {
                List<String> lines = new ArrayList<>();
                
                // Read all lines
                try (FileInputStream fis = new FileInputStream(logFile);
                     InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                     BufferedReader reader = new BufferedReader(isr)) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                
                // Keep only last MAX_LINES
                if (lines.size() > MAX_LINES) {
                    List<String> trimmedLines = lines.subList(lines.size() - MAX_LINES, lines.size());
                    
                    // Write back
                    try (FileOutputStream fos = new FileOutputStream(logFile, false);
                         OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                        for (String line : trimmedLines) {
                            writer.append(line).append("\n");
                        }
                        writer.flush();
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error trimming log file: " + logFile.getName(), e);
        }
    }
    
    /**
     * Get log file size in bytes
     */
    public long getLogFileSize(LogType logType) {
        File logFile = getLogFile(logType);
        return logFile.exists() ? logFile.length() : 0;
    }
}
