package com.bg7yoz.ft8cn.psk;

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Fetches reception reports (spots) for the user's callsign from retrieve.pskreporter.info.
 * Uses senderCallsign, band (frange), and mode=FT8. Runs HTTP GET on a background thread.
 */
public class PskReporterRetrieve {

    private static final String TAG = "PskReporterRetrieve";
    private static final String BASE_URL = "https://retrieve.pskreporter.info/query";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int FLOW_START_SECONDS = -21600;  // last 6 hours (API max 24h)
    private static final int BAND_RANGE_HZ = 500000;  // ±500 kHz around current band

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class Spot {
        public final String receiverCallsign;
        public final String receiverLocator;
        public final long frequency;
        public final long flowStartSeconds;  // Unix time (seconds)

        public Spot(String receiverCallsign, String receiverLocator, long frequency, long flowStartSeconds) {
            this.receiverCallsign = receiverCallsign != null ? receiverCallsign : "";
            this.receiverLocator = receiverLocator != null ? receiverLocator : "";
            this.frequency = frequency;
            this.flowStartSeconds = flowStartSeconds;
        }
    }

    public interface Callback {
        void onResult(List<Spot> spots);
        void onError(String message);
    }

    /**
     * Fetch spots for the current callsign, band, and mode (FT8). Calls callback on the same thread
     * that invoked fetch (caller should post to main thread if updating UI).
     */
    public void fetch(Callback callback) {
        String call = GeneralVariables.myCallsign != null ? GeneralVariables.myCallsign.trim() : "";
        if (call.isEmpty()) {
            if (callback != null) callback.onError("Callsign not set");
            return;
        }

        long band = GeneralVariables.band;
        long lowFreq = Math.max(0, band - BAND_RANGE_HZ);
        long highFreq = band + BAND_RANGE_HZ;
        String frange = lowFreq + "-" + highFreq;

        String query = BASE_URL
                + "?senderCallsign=" + urlEncode(call)
                + "&flowStartSeconds=" + FLOW_START_SECONDS
                + "&mode=FT8"
                + "&frange=" + urlEncode(frange)
                + "&rptlimit=100";

        executor.execute(() -> {
            try {
                URL url = new URL(query);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("Accept", "application/xml, text/xml, */*");

                int code = conn.getResponseCode();
                if (code != HttpsURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_OK) {
                    String errorMsg = "HTTP " + code;
                    logExternalCall("PSK Reporter Retrieve", BASE_URL, "FAILED", errorMsg);
                    if (callback != null) callback.onError(errorMsg);
                    return;
                }

                InputStream in = conn.getInputStream();
                List<Spot> spots = parseXml(in);
                
                // Log successful PSK Reporter retrieve call
                logExternalCall("PSK Reporter Retrieve", BASE_URL, "SUCCESS", String.format("Retrieved %d spots", spots.size()));
                
                if (callback != null) callback.onResult(spots);
            } catch (Exception e) {
                Log.w(TAG, "Fetch failed: " + e.getMessage());
                
                // Log failed PSK Reporter retrieve call
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Network error";
                logExternalCall("PSK Reporter Retrieve", BASE_URL, "FAILED", errorMsg);
                
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
    
    /**
     * Log external service call
     */
    private static void logExternalCall(String service, String endpoint, String status, String details) {
        try {
            android.content.Context context = GeneralVariables.getMainContext();
            if (context != null) {
                com.bg7yoz.ft8cn.log.ApplicationLogManager logManager = 
                    new com.bg7yoz.ft8cn.log.ApplicationLogManager(context);
                String message = String.format("%s call to %s: %s - %s", service, endpoint, status, details);
                logManager.writeLog(com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType.EXTERNAL_CALLS, message);
            }
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    /**
     * Parse PSK Reporter query response XML. Expects receptionReport elements with attributes
     * receiverCallsign, receiverLocator, senderCallsign, frequency, flowStartSeconds.
     */
    private static List<Spot> parseXml(InputStream in) {
        List<Spot> spots = new ArrayList<>();
        try {
            org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
            parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, "UTF-8");

            int eventType = parser.getEventType();
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("receptionReport".equalsIgnoreCase(name)) {
                        String recvCall = parser.getAttributeValue(null, "receiverCallsign");
                        String recvLoc = parser.getAttributeValue(null, "receiverLocator");
                        String freqStr = parser.getAttributeValue(null, "frequency");
                        String flowStr = parser.getAttributeValue(null, "flowStartSeconds");
                        long freq = 0;
                        long flow = 0;
                        try {
                            if (freqStr != null) freq = Long.parseLong(freqStr.trim());
                            if (flowStr != null) flow = Long.parseLong(flowStr.trim());
                        } catch (NumberFormatException ignored) {}
                        spots.add(new Spot(recvCall, recvLoc, freq, flow));
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.w(TAG, "Parse error: " + e.getMessage());
        }
        return spots;
    }
}
