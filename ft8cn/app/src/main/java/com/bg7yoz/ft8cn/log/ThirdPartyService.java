package com.bg7yoz.ft8cn.log;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

enum ServiceType{
    Cloudlog,
    QRZ
}

public class ThirdPartyService {
    public static String TAG = "ThirdPartyService";
    
    // Track recently uploaded QSOs to prevent duplicate uploads
    // Key format: "callsign|freq" (e.g., "W1ABC|14074000")
    // Tracks upload timestamp to allow re-upload after 1 hour
    private static final HashMap<String, Long> uploadedQSOs = new HashMap<>(); // Key -> upload timestamp (milliseconds)
    private static final long DUPLICATE_UPLOAD_INTERVAL_MS = 3600000; // 1 hour - minimum time between uploads of same callsign+freq

    private static String QSLRecordToADIF(QSLRecord qslRecord, ServiceType serv){
        StringBuilder logStr = new StringBuilder();
        logStr.append(String.format("<call:%d>%s "
                , qslRecord.getToCallsign().length()
                , qslRecord.getToCallsign()));

        if (qslRecord.getToMaidenGrid() != null) {
            logStr.append(String.format("<gridsquare:%d>%s "
                    , qslRecord.getToMaidenGrid().length()
                    , qslRecord.getToMaidenGrid()));
        }

        if (qslRecord.getMode() != null) {
            logStr.append(String.format("<mode:%d>%s "
                    , qslRecord.getMode().length()
                    , qslRecord.getMode()));
        }

        if (String.valueOf(qslRecord.getSendReport()) != null) {
            logStr.append(String.format("<rst_sent:%d>%s "
                    , String.valueOf(qslRecord.getSendReport()).length()
                    , String.valueOf(qslRecord.getSendReport())));
        }

        if (String.valueOf(qslRecord.getReceivedReport()) != null) {
            logStr.append(String.format("<rst_rcvd:%d>%s "
                    , String.valueOf(qslRecord.getReceivedReport()).length()
                    , String.valueOf(qslRecord.getReceivedReport())));
        }

        if (qslRecord.getQso_date() != null) {
            logStr.append(String.format("<qso_date:%d>%s "
                    , qslRecord.getQso_date().length()
                    , qslRecord.getQso_date()));
        }

        if (qslRecord.getTime_on() != null) {
            logStr.append(String.format("<time_on:%d>%s "
                    , qslRecord.getTime_on().length()
                    , qslRecord.getTime_on()));
        }
        if (qslRecord.getBandLength() != null) {
            logStr.append(String.format("<band:%d>%s "
                    , qslRecord.getBandLength().length()
                    , qslRecord.getBandLength()));
        }

        if (qslRecord.getQso_date_off() != null) {
            logStr.append(String.format("<qso_date_off:%d>%s "
                    , qslRecord.getQso_date_off().length()
                    , qslRecord.getQso_date_off()));
        }

        if (qslRecord.getTime_off() != null) {
            logStr.append(String.format("<time_off:%d>%s "
                    , qslRecord.getTime_off().length()
                    , qslRecord.getTime_off()));
        }

        if (String.valueOf(qslRecord.getBandFreq()) != null) {
            String freq = "";
            Log.d(TAG,String.valueOf(qslRecord.getBandFreq()));
            if (serv == ServiceType.Cloudlog || serv == ServiceType.QRZ){
                double i = (double)qslRecord.getBandFreq() / 1000000;
                freq = String.valueOf(i);
            }

            logStr.append(String.format("<freq:%d>%s "
                    , freq.length()
                    , freq));
        }

        if (qslRecord.getMyCallsign() != null) {
            logStr.append(String.format("<station_callsign:%d>%s "
                    , qslRecord.getMyCallsign().length()
                    , qslRecord.getMyCallsign()));
        }

        if (qslRecord.getMyMaidenGrid() != null) {
            logStr.append(String.format("<my_gridsquare:%d>%s "
                    , qslRecord.getMyMaidenGrid().length()
                    , qslRecord.getMyMaidenGrid()));
        }

        String comment = qslRecord.getComment();

        //<comment:15>Distance: 99 mi <eor>
        //在写库的时候，一定要加" mi"
        logStr.append(String.format("<comment:%d>%s <eor>\n"
                , comment.length()
                , comment));
        return logStr.toString();
    }
    public static void UploadToCloudLog(QSLRecord qslRecord){
        // 转换为adif格式
        String logStr = QSLRecordToADIF(qslRecord,ServiceType.Cloudlog);
        Log.d(TAG,logStr);
        String address = GeneralVariables.getCloudlogServerAddress();
        if (!address.endsWith("/")){
            address+="/";
        }
        HashMap<String,String> json = new HashMap<>();
        json.put("key", GeneralVariables.getCloudlogServerApiKey());
        json.put("station_profile_id", GeneralVariables.getCloudlogStationID());
        json.put("type","adif");
        json.put("string", logStr);

        JSONStringer js = new JSONStringer();
        try {
            String result = js.object().key("key").value(GeneralVariables.getCloudlogServerApiKey()).key("station_profile_id").value(GeneralVariables.getCloudlogStationID())
                    .key("type").value("adif").key("string").value(logStr).endObject().toString();
            String clRes = sendPostRequest(address+"api/qso/",result);
            Log.d(TAG,"Updated to Cloudlog successfully. result:"+clRes);
        }catch (Exception k){
            Log.d(TAG, k.toString());
        }
    }
    public static boolean CheckCloudlogConnection(){
        String address = GeneralVariables.getCloudlogServerAddress();
        String apiKey = GeneralVariables.getCloudlogServerApiKey();
        // 检查地址末尾是否含有 /
        if (!address.endsWith("/")){
            address+="/";
        }
        try{
            String url = address + "api/auth/"+ apiKey;
            Log.d(TAG, "URL: "+url);
            String result = sendGetRequest(url);
            Log.d(TAG, result);
            if (!result.equals("<auth><status>Valid</status><rights>rw</rights></auth>")){
                return false;
            }
            return true;
        }catch (Exception e){
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean CheckQRZConnection(){
        String apiKey = GeneralVariables.getQrzApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "QRZ API key is not set");
            return false;
        }
        try{
            String url = "https://logbook.qrz.com/api?KEY="+apiKey+"&ACTION=STATUS";
            String result = sendGetRequestWithUserAgent(url);
            if (result == null) {
                Log.e(TAG, "QRZ connection check failed: No response");
                return false;
            }
            HashMap<String,String> status = new HashMap<>();
            for (String s : result.split("&")) {
                String[] split = s.split("=");
                if (split.length>1){
                    status.put(split[0],split[1]);
                }
            }
            Log.d(TAG, "QRZ status: " + status.toString());
            if (status.get("RESULT") == null || !status.get("RESULT").equals("OK")){
                Log.e(TAG, "QRZ connection check failed: RESULT=" + status.get("RESULT"));
                return false;
            }
            return true;
        }catch (Exception e){
            Log.e(TAG, "QRZ connection check exception: " + e.toString(), e);
            return false;
        }
    }

    /**
     * Generate a unique key for a QSO to track duplicate uploads
     * Format: "callsign|freq"
     * Uses only callsign and frequency to identify duplicates
     */
    private static String generateQSOKey(QSLRecord qslRecord) {
        String callsign = qslRecord.getToCallsign() != null ? qslRecord.getToCallsign() : "";
        String freq = String.valueOf(qslRecord.getBandFreq());
        return String.format("%s|%s", callsign, freq);
    }
    
    /**
     * Check if a QSO should be uploaded
     * Allows re-upload only if more than 1 hour has passed since last upload
     * @param qsoKey The unique key for the QSO (callsign|freq)
     * @return true if should skip upload (duplicate within 1 hour), false if should upload
     */
    private static synchronized boolean shouldSkipUpload(String qsoKey) {
        long currentTime = System.currentTimeMillis();
        
        Long lastUploadTime = uploadedQSOs.get(qsoKey);
        if (lastUploadTime == null) {
            // Not uploaded before, allow upload
            return false;
        }
        
        // Check if more than 1 hour has passed since last upload
        long timeSinceLastUpload = currentTime - lastUploadTime;
        if (timeSinceLastUpload > DUPLICATE_UPLOAD_INTERVAL_MS) {
            // More than 1 hour has passed, allow re-upload
            Log.d(TAG, String.format("QSO upload allowed after %d ms (>1 hour): %s", timeSinceLastUpload, qsoKey));
            return false;
        }
        
        // Less than 1 hour since last upload, skip (duplicate)
        Log.d(TAG, String.format("QSO already uploaded %d ms ago (<1 hour), skipping: %s", timeSinceLastUpload, qsoKey));
        return true;
    }
    
    /**
     * Mark a QSO as uploaded with current timestamp
     * @param qsoKey The unique key for the QSO (callsign|freq)
     */
    private static synchronized void markAsUploaded(String qsoKey) {
        uploadedQSOs.put(qsoKey, System.currentTimeMillis());
    }
    
    /**
     * Upload QSO to QRZ with retry logic and exponential backoff
     * Retries up to 3 times with backoff delays: 3s, 6s, 12s
     * Prevents duplicate uploads of the same QSO
     */
    public static void UploadToQRZ(QSLRecord qslRecord){
        String apikey = GeneralVariables.getQrzApiKey();
        
        if (apikey == null || apikey.isEmpty()) {
            Log.e(TAG, "QRZ API key is not set");
            return;
        }

        // Check for duplicate upload
        // Key is callsign + frequency - same callsign on same frequency is a duplicate
        // Allow re-upload only if more than 1 hour has passed
        String qsoKey = generateQSOKey(qslRecord);
        if (shouldSkipUpload(qsoKey)) {
            return;
        }

        // 转换为adif格式
        String logStr = QSLRecordToADIF(qslRecord, ServiceType.QRZ);
        Log.d(TAG,"ADIF data: " + logStr);
        
        // Retry logic: up to 3 attempts with exponential backoff (3s, 6s, 12s)
        final int MAX_RETRIES = 3;
        final int INITIAL_BACKOFF_MS = 3000; // 3 seconds
        boolean success = false;
        String lastError = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Log.d(TAG, String.format("QRZ upload attempt %d/%d", attempt, MAX_RETRIES));
                
                UploadResult result = attemptQRZUpload(qslRecord, logStr, apikey);
                
                if (result.success) {
                    success = true;
                    // Mark as uploaded with current timestamp to prevent duplicate uploads
                    markAsUploaded(qsoKey);
                    // Mark QSO as uploaded to QRZ in database
                    com.bg7yoz.ft8cn.database.DatabaseOpr.getInstance(
                            GeneralVariables.getMainContext(), "data.db")
                            .setQSLTableIsQRZUploadedByRecord(qslRecord, true);
                    // Show success message with callsign
                    String callsign = qslRecord.getToCallsign();
                    if (callsign != null && !callsign.isEmpty()) {
                        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.qrz_upload_success), callsign));
                    } else {
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.qrz_upload_success).replace(": %s", ""));
                    }
                    Log.d(TAG, String.format("QRZ upload succeeded on attempt %d", attempt));
                    return; // Success - exit retry loop
                } else {
                    lastError = result.errorMessage;
                    Log.w(TAG, String.format("QRZ upload attempt %d failed: %s", attempt, lastError));
                    
                    // If this was the last attempt, don't wait
                    if (attempt < MAX_RETRIES) {
                        // Calculate exponential backoff: 3s, 6s, 12s
                        long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2^(attempt-1) * 3000
                        Log.d(TAG, String.format("Waiting %d ms before retry...", backoffMs));
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Retry backoff interrupted", e);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                if (lastError == null || lastError.isEmpty()) {
                    lastError = e.toString();
                }
                Log.e(TAG, String.format("QRZ upload attempt %d exception: %s", attempt, lastError), e);
                
                // If this was the last attempt, don't wait
                if (attempt < MAX_RETRIES) {
                    // Calculate exponential backoff: 3s, 6s, 12s
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2^(attempt-1) * 3000
                    Log.d(TAG, String.format("Waiting %d ms before retry after exception...", backoffMs));
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "Retry backoff interrupted", ie);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted
        if (!success) {
            String finalError = lastError != null ? lastError : "Unknown error after " + MAX_RETRIES + " attempts";
            Log.e(TAG, "QRZ upload failed after " + MAX_RETRIES + " attempts: " + finalError);
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.qrz_upload_failed), finalError));
        }
    }
    
    /**
     * Helper class to hold upload result
     */
    private static class UploadResult {
        boolean success;
        String errorMessage;
        
        UploadResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Upload multiple QSOs to QRZ in a single API call
     * @param qslRecords List of QSO records to upload
     * @return BulkUploadResult indicating success/failure and count of uploaded records
     */
    public static BulkUploadResult UploadMultipleToQRZ(ArrayList<QSLRecord> qslRecords) {
        String apikey = GeneralVariables.getQrzApiKey();
        
        if (apikey == null || apikey.isEmpty()) {
            Log.e(TAG, "QRZ API key is not set");
            return new BulkUploadResult(false, 0, "QRZ API key is not set");
        }

        if (qslRecords == null || qslRecords.isEmpty()) {
            return new BulkUploadResult(false, 0, "No QSO records to upload");
        }

        // Build combined ADIF string with multiple records
        StringBuilder combinedAdif = new StringBuilder();
        for (QSLRecord qslRecord : qslRecords) {
            // Check for duplicate upload
            String qsoKey = generateQSOKey(qslRecord);
            if (shouldSkipUpload(qsoKey)) {
                continue; // Skip this QSO if recently uploaded
            }
            String logStr = QSLRecordToADIF(qslRecord, ServiceType.QRZ);
            combinedAdif.append(logStr);
        }

        if (combinedAdif.length() == 0) {
            return new BulkUploadResult(false, 0, "All QSOs were skipped (recently uploaded)");
        }

        // Retry logic: up to 3 attempts with exponential backoff (3s, 6s, 12s)
        final int MAX_RETRIES = 3;
        final int INITIAL_BACKOFF_MS = 3000; // 3 seconds
        String lastError = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Log.d(TAG, String.format("QRZ bulk upload attempt %d/%d (%d QSOs)", attempt, MAX_RETRIES, qslRecords.size()));
                
                BulkUploadResult result = attemptBulkQRZUpload(combinedAdif.toString(), apikey);
                
                if (result.success) {
                    // Mark all QSOs as uploaded
                    for (QSLRecord qslRecord : qslRecords) {
                        String qsoKey = generateQSOKey(qslRecord);
                        markAsUploaded(qsoKey);
                        // Mark QSO as uploaded to QRZ in database
                        com.bg7yoz.ft8cn.database.DatabaseOpr.getInstance(
                                GeneralVariables.getMainContext(), "data.db")
                                .setQSLTableIsQRZUploadedByRecord(qslRecord, true);
                    }
                    Log.d(TAG, String.format("QRZ bulk upload succeeded on attempt %d: %d QSOs uploaded", attempt, result.count));
                    return result;
                } else {
                    lastError = result.errorMessage;
                    Log.w(TAG, String.format("QRZ bulk upload attempt %d failed: %s", attempt, lastError));
                    
                    // If this was the last attempt, don't wait
                    if (attempt < MAX_RETRIES) {
                        // Calculate exponential backoff: 3s, 6s, 12s
                        long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2^(attempt-1) * 3000
                        Log.d(TAG, String.format("Waiting %d ms before retry...", backoffMs));
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Retry backoff interrupted", e);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                if (lastError == null || lastError.isEmpty()) {
                    lastError = e.toString();
                }
                Log.e(TAG, "QRZ bulk upload exception: " + lastError, e);
                
                // If this was the last attempt, don't wait
                if (attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries failed
        return new BulkUploadResult(false, 0, lastError != null ? lastError : "Unknown error after " + MAX_RETRIES + " attempts");
    }

    /**
     * Attempt a bulk QRZ upload with multiple ADIF records
     * @param combinedAdif The combined ADIF formatted log string with multiple records
     * @param apikey The QRZ API key
     * @return BulkUploadResult indicating success or failure with error message and count
     */
    private static BulkUploadResult attemptBulkQRZUpload(String combinedAdif, String apikey) {
        try {
            // URL encode the ADIF data to handle special characters
            String encodedAdif = URLEncoder.encode(combinedAdif, StandardCharsets.UTF_8.toString());
            // Use INSERT action - it can handle multiple ADIF records
            String url = String.format("https://logbook.qrz.com/api?KEY=%s&ACTION=INSERT&ADIF=%s", apikey, encodedAdif);
            Log.d(TAG, "QRZ bulk upload URL: " + url.replace(apikey, "***")); // Log URL with masked API key
            
            String result = sendGetRequestWithUserAgent(url);
            if (result != null) {
                Log.d(TAG, "QRZ API response: " + result);
                
                // Parse response to check if upload was successful
                HashMap<String,String> response = new HashMap<>();
                for (String s : result.split("&")) {
                    String[] split = s.split("=");
                    if (split.length > 1) {
                        // URL decode the value
                        try {
                            String key = split[0];
                            String value = java.net.URLDecoder.decode(split[1], StandardCharsets.UTF_8.toString());
                            response.put(key, value);
                        } catch (Exception e) {
                            // If decoding fails, use raw value
                            response.put(split[0], split[1]);
                        }
                    }
                }
                
                // Check if upload was successful: RESULT=OK (new), RESULT=REPLACE (duplicate overwritten),
                // or status=fail with reason containing "duplicate" (duplicates detected)
                String resultValue = response.get("RESULT");
                // Check both lowercase and uppercase field names (QRZ may return either)
                String status = response.get("status");
                if (status == null) status = response.get("STATUS");
                String reason = response.get("reason");
                if (reason == null) reason = response.get("REASON");
                
                // Check for duplicate case: status=fail with reason containing "duplicate" (anywhere in the string)
                boolean isDuplicateFailure = status != null && status.equalsIgnoreCase("fail") 
                        && reason != null && reason.toLowerCase().contains("duplicate");
                
                boolean success = false;
                if (isDuplicateFailure) {
                    // Treat duplicate failures as success - QSOs were already uploaded
                    success = true;
                } else if (resultValue != null && (resultValue.equals("OK") || resultValue.equals("REPLACE"))) {
                    // Standard success cases
                    success = true;
                }
                
                if (success) {
                    // Extract COUNT from response (show count for OK, REPLACE, and duplicate failures)
                    int count = 0;
                    try {
                        String countStr = response.get("COUNT");
                        if (countStr != null && !countStr.isEmpty()) {
                            count = Integer.parseInt(countStr);
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Could not parse COUNT from response: " + response.get("COUNT"));
                    }
                    
                    if (isDuplicateFailure) {
                        Log.d(TAG, "QRZ bulk upload: status=fail, reason contains duplicate, COUNT=" + count + " - marking QSOs as uploaded");
                    } else if (resultValue != null && resultValue.equals("REPLACE")) {
                        Log.d(TAG, "QRZ bulk upload: RESULT=REPLACE (duplicates), COUNT=" + count + " - marking QSOs as uploaded");
                    }
                    return new BulkUploadResult(true, count, null);
                } else {
                    // Upload failed - extract error message and include full response for debugging
                    String errorMsg = response.get("ERROR");
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = response.get("REASON");
                    }
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = response.get("RESULT");
                    }
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Unknown error";
                    }
                    // Include full response for debugging
                    String fullErrorMsg = errorMsg + " (Full response: " + result + ")";
                    return new BulkUploadResult(false, 0, fullErrorMsg);
                }
            } else {
                // No response - network error or HTTP error
                return new BulkUploadResult(false, 0, "No response from server");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.toString();
            }
            return new BulkUploadResult(false, 0, errorMsg);
        }
    }

    /**
     * Result class for bulk upload operations
     */
    public static class BulkUploadResult {
        public final boolean success;
        public final int count;
        public final String errorMessage;

        public BulkUploadResult(boolean success, int count, String errorMessage) {
            this.success = success;
            this.count = count;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Attempt a single QRZ upload
     * @param qslRecord The QSO record to upload
     * @param logStr The ADIF formatted log string
     * @param apikey The QRZ API key
     * @return UploadResult indicating success or failure with error message
     */
    private static UploadResult attemptQRZUpload(QSLRecord qslRecord, String logStr, String apikey) {
        try {
            // URL encode the ADIF data to handle special characters
            String encodedAdif = URLEncoder.encode(logStr, StandardCharsets.UTF_8.toString());
            // Fix URL format: use ? for query parameters, not / after /api
            String url = String.format("https://logbook.qrz.com/api?KEY=%s&ACTION=INSERT&ADIF=%s", apikey, encodedAdif);
            Log.d(TAG, "QRZ upload URL: " + url.replace(apikey, "***")); // Log URL with masked API key
            
            String result = sendGetRequestWithUserAgent(url);
            if (result != null) {
                Log.d(TAG, "QRZ API response: " + result);
                
                // Parse response to check if upload was successful
                HashMap<String,String> response = new HashMap<>();
                for (String s : result.split("&")) {
                    String[] split = s.split("=");
                    if (split.length > 1) {
                        // URL decode the value
                        try {
                            String key = split[0];
                            String value = java.net.URLDecoder.decode(split[1], StandardCharsets.UTF_8.toString());
                            response.put(key, value);
                        } catch (Exception e) {
                            // If decoding fails, use raw value
                            response.put(split[0], split[1]);
                        }
                    }
                }
                
                // Check if upload was successful (RESULT=OK)
                if (response.get("RESULT") != null && response.get("RESULT").equals("OK")) {
                    return new UploadResult(true, null);
                } else {
                    // Upload failed - extract error message
                    String errorMsg = response.get("ERROR");
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = response.get("RESULT");
                    }
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Unknown error";
                    }
                    return new UploadResult(false, errorMsg);
                }
            } else {
                // No response - network error or HTTP error
                return new UploadResult(false, "No response from server");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.toString();
            }
            return new UploadResult(false, errorMsg);
        }
    }

    public static String sendPostRequest(String url, String json) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json");

            // 获取OutputStream，将请求的数据写入流中
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            // cloudlog使用HTTP_CREATED作为创建记录成功的响应
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode==HttpURLConnection.HTTP_CREATED) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (reader != null) {
                reader.close();
            }
        }

        return null;
    }
    public static String sendGetRequest(String url) throws IOException {
        return sendGetRequestWithUserAgent(url, null);
    }

    private static String sendGetRequestWithUserAgent(String url) throws IOException {
        // Build User-Agent: callsign + application name + version
        String userAgent = "FT8CN/" + GeneralVariables.VERSION;
        if (GeneralVariables.myCallsign != null && !GeneralVariables.myCallsign.isEmpty()) {
            userAgent = GeneralVariables.myCallsign + " " + userAgent;
        }
        return sendGetRequestWithUserAgent(url, userAgent);
    }

    private static String sendGetRequestWithUserAgent(String url, String userAgent) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法为GET
            conn.setRequestMethod("GET");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json");
            
            // Set User-Agent header (required by QRZ API)
            if (userAgent != null && !userAgent.isEmpty()) {
                conn.setRequestProperty("User-Agent", userAgent);
            }

            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "HTTP response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                // Read error response body for better error messages
                try {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e(TAG, "QRZ API error response (" + responseCode + "): " + errorResponse.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read error response: " + e.toString());
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }
}
