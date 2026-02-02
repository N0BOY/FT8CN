package com.bg7yoz.ft8cn.psk;

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumes PSK Reporter spots via MQTT, similar to GridTracker2 implementation.
 * Connects to mqtt.pskreporter.info:1883 and subscribes to pskr/filter/v2/+/+/${callsign}/#
 * Receives spots in real-time via MQTT instead of polling via HTTP.
 */
public class PskReporterMqtt {

    private static final String TAG = "PskReporterMqtt";
    private static final String MQTT_BROKER = "tcp://mqtt.pskreporter.info:1883";
    private static final String TOPIC_PREFIX = "pskr/filter/v2/+/+/";
    private static final String TOPIC_SUFFIX = "/#";
    private static final int QOS = 0;  // At most once delivery
    private static final int CONNECTION_TIMEOUT = 30;
    private static final int KEEP_ALIVE_INTERVAL = 60;

    private MqttClient mqttClient;
    private String clientId;
    private String topic;
    private Callback callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean connected = false;
    private final List<Spot> receivedSpots = new ArrayList<>();
    private final Object spotsLock = new Object();

    public static class Spot {
        public final String receiverCallsign;  // The station that heard us (rc field)
        public final String receiverLocator;  // Grid of the station that heard us (rl field)
        public final long frequency;  // Frequency in Hz (f field)
        public final long flowStartSeconds;  // Unix timestamp in seconds (t field)

        public Spot(String receiverCallsign, String receiverLocator, long frequency, long flowStartSeconds) {
            this.receiverCallsign = receiverCallsign != null ? receiverCallsign : "";
            this.receiverLocator = receiverLocator != null ? receiverLocator : "";
            this.frequency = frequency;
            this.flowStartSeconds = flowStartSeconds;
        }
    }

    public interface Callback {
        void onSpotReceived(Spot spot);
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }

    /**
     * Start MQTT connection and subscribe to spots for the given callsign.
     * @param callback Callback for receiving spots and connection events
     */
    public void start(Callback callback) {
        this.callback = callback;
        String call = GeneralVariables.myCallsign != null ? GeneralVariables.myCallsign.trim().toUpperCase() : "";
        if (call.isEmpty()) {
            if (callback != null) callback.onError("Callsign not set");
            return;
        }

        // Build topic: pskr/filter/v2/+/+/${callsign}/#
        // This subscribes to spots where the user's callsign is the SENDER (sc field)
        // The topic structure is: pskr/filter/v2/{band}/{mode}/{sendercall}/{receivercall}/...
        // So we want: pskr/filter/v2/+/+/${callsign}/# to get all spots where we are the sender
        this.topic = TOPIC_PREFIX + call + TOPIC_SUFFIX;
        
        // Generate unique client ID
        this.clientId = "FT8CN_" + call + "_" + System.currentTimeMillis();

        executor.execute(() -> {
            try {
                // Log connection attempt
                logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "CONNECTING", 
                    "Attempting to connect and subscribe to " + topic);
                connect();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start MQTT: " + e.getMessage());
                logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "FAILED", 
                    "Start failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                if (callback != null) callback.onError("Connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Stop MQTT connection and unsubscribe.
     */
    public void stop() {
        executor.execute(() -> {
            disconnect();
        });
    }

    /**
     * Get all received spots (thread-safe).
     */
    public List<Spot> getReceivedSpots() {
        synchronized (spotsLock) {
            return new ArrayList<>(receivedSpots);
        }
    }

    /**
     * Clear all received spots.
     */
    public void clearSpots() {
        synchronized (spotsLock) {
            receivedSpots.clear();
        }
    }

    private void connect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "Already connected");
                logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "ALREADY_CONNECTED", 
                    "Connection already established");
                return;
            }

            Log.d(TAG, "Connecting to MQTT broker: " + MQTT_BROKER);
            Log.d(TAG, "Subscribing to topic: " + topic);
            Log.d(TAG, "Client ID: " + clientId);

            mqttClient = new MqttClient(MQTT_BROKER, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(CONNECTION_TIMEOUT);
            options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
            options.setAutomaticReconnect(true);

            // Set callback BEFORE connecting to ensure it's available when messages arrive
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    connected = false;
                    Log.w(TAG, "MQTT connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
                    if (callback != null) {
                        // Call on background thread - MainViewModel will handle thread safety
                        callback.onDisconnected();
                    }
                    
                    // Log external call
                    logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "DISCONNECTED", 
                        cause != null ? cause.getMessage() : "Connection lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // This callback runs on the MQTT client's thread
                    // We need to process messages immediately to avoid blocking
                    try {
                        String payload = new String(message.getPayload(), "UTF-8");
                        Log.d(TAG, "Received MQTT message on topic: " + topic);
                        Log.d(TAG, "Message payload length: " + payload.length() + " bytes");
                        Log.d(TAG, "Message: " + (payload.length() > 500 ? payload.substring(0, 500) + "..." : payload));
                        
                        // Parse and handle on this thread - callback will handle thread safety
                        parseAndHandleMessage(payload);
                    } catch (Exception e) {
                        Log.w(TAG, "Error processing MQTT message: " + e.getMessage());
                        Log.w(TAG, "Exception stack trace: ", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriptions
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(topic, QOS);
            
            connected = true;
            Log.d(TAG, "MQTT connected and subscribed");
            
            if (callback != null) callback.onConnected();
            
            // Log successful connection
            logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "CONNECTED", 
                "Subscribed to " + topic);
                
        } catch (MqttException e) {
            connected = false;
            Log.e(TAG, "MQTT connection failed: " + e.getMessage());
            if (callback != null) callback.onError("Connection failed: " + e.getMessage());
            
            // Log failed connection
            logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "FAILED", 
                e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(topic);
                mqttClient.disconnect();
                Log.d(TAG, "MQTT disconnected");
                
                // Log disconnection
                logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "DISCONNECTED", "Unsubscribed and disconnected");
            }
            connected = false;
        } catch (MqttException e) {
            Log.w(TAG, "Error disconnecting MQTT: " + e.getMessage());
            logExternalCall("PSK Reporter MQTT", MQTT_BROKER, "FAILED", 
                "Disconnect error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Parse MQTT message payload (JSON format) and extract spot information.
     * PSK Reporter MQTT messages contain JSON with spot data.
     * Each message is typically a single spot object with abbreviated field names.
     */
    private void parseAndHandleMessage(String payload) {
        try {
            if (payload == null || payload.trim().isEmpty()) {
                Log.w(TAG, "Empty payload received");
                return;
            }
            
            // PSK Reporter MQTT messages are typically single spot objects, not arrays
            // Try parsing as JSON object first
            JSONObject json = new JSONObject(payload);
            
            // Check if it's an array of spots (less common but possible)
            if (json.has("spots") || json.has("receptionReports")) {
                JSONArray spotsArray = json.optJSONArray("spots");
                if (spotsArray == null) {
                    spotsArray = json.optJSONArray("receptionReports");
                }
                
                if (spotsArray != null) {
                    Log.d(TAG, "Parsing array of " + spotsArray.length() + " spots");
                    for (int i = 0; i < spotsArray.length(); i++) {
                        JSONObject spotJson = spotsArray.getJSONObject(i);
                        Spot spot = parseSpot(spotJson);
                        if (spot != null) {
                            handleSpot(spot);
                        } else {
                            Log.w(TAG, "Failed to parse spot at index " + i);
                        }
                    }
                    return;
                }
            }
            
            // Try to parse as single spot object (most common format)
            Spot spot = parseSpot(json);
            if (spot != null) {
                Log.d(TAG, "Successfully parsed spot: " + spot.receiverCallsign + " @ " + spot.frequency + " Hz");
                handleSpot(spot);
            } else {
                Log.w(TAG, "Failed to parse spot from JSON. Available keys: " + getJsonKeys(json));
            }
            
        } catch (org.json.JSONException e) {
            Log.w(TAG, "JSON parsing error: " + e.getMessage());
            String safePayloadPreview = (payload == null) ? "null"
                    : (payload.length() > 500 ? payload.substring(0, 500) + "..." : payload);
            Log.d(TAG, "Payload (first 500 chars): " + safePayloadPreview);
        } catch (Exception e) {
            Log.w(TAG, "Error parsing MQTT message: " + e.getMessage());
            String safePayloadPreview = (payload == null) ? "null"
                    : (payload.length() > 500 ? payload.substring(0, 500) + "..." : payload);
            Log.d(TAG, "Payload (first 500 chars): " + safePayloadPreview);
        }
    }
    
    /**
     * Get all keys from a JSON object for debugging
     */
    private String getJsonKeys(JSONObject json) {
        try {
            java.util.Iterator<String> keys = json.keys();
            StringBuilder sb = new StringBuilder();
            while (keys.hasNext()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(keys.next());
            }
            return sb.toString();
        } catch (Exception e) {
            return "error getting keys";
        }
    }

    /**
     * Parse a single spot from JSON object.
     * According to http://mqtt.pskreporter.info/, PSK Reporter MQTT uses abbreviated field names:
     * - rc: receiver call (the station that heard us)
     * - rl: receiver locator (grid of the station that heard us)
     * - f: frequency in Hz
     * - t: timestamp (seconds since 1970-01-01)
     * - sc: sender call (us, the station that was heard)
     * - sl: sender locator (our grid)
     * - rp: report (SNR)
     * - md: mode
     * - b: band
     * - sq: sequence number
     * - t_tx: transmission start time
     * - sa/ra: sender/receiver ADIF country codes
     * 
     * Since we subscribe to spots where our callsign is the sender (sc), we want to extract
     * the receiver information (rc, rl) to show who heard us.
     */
    private Spot parseSpot(JSONObject json) {
        try {
            // According to official docs: http://mqtt.pskreporter.info/
            // We subscribe to pskr/filter/v2/+/+/${callsign}/# where ${callsign} is the sender (sc)
            // So we want to extract receiver info (rc, rl) to show who heard us
            
            // Receiver callsign (rc) - the station that heard us
            String receiverCallsign = json.optString("rc", "");
            if (receiverCallsign.isEmpty()) {
                // Fallback to full names for compatibility
                receiverCallsign = json.optString("receiverCallsign", "");
            }
            if (receiverCallsign.isEmpty()) {
                receiverCallsign = json.optString("receiver", "");
            }
            
            // Receiver locator/grid (rl) - grid of the station that heard us
            String receiverLocator = json.optString("rl", "");
            if (receiverLocator.isEmpty()) {
                // Fallback to full names for compatibility
                receiverLocator = json.optString("receiverLocator", "");
            }
            if (receiverLocator.isEmpty()) {
                receiverLocator = json.optString("receiverGrid", "");
            }
            
            // Frequency (f) - in Hz
            long frequency = json.optLong("f", 0);
            if (frequency == 0) {
                // Try as double/float (in case it's stored as decimal)
                double freqDouble = json.optDouble("f", 0);
                if (freqDouble > 0) {
                    frequency = (long) freqDouble;
                }
            }
            if (frequency == 0) {
                // Fallback to full names
                frequency = json.optLong("frequency", 0);
            }
            
            // Timestamp (t) - seconds since 1970-01-01
            // According to docs, t is the timestamp from PSK Reporter (either transmission start or decode time)
            // t_tx is the transmission start time if available
            long flowStartSeconds = json.optLong("t", 0);
            if (flowStartSeconds == 0) {
                // Try t_tx (transmission start time) as fallback
                flowStartSeconds = json.optLong("t_tx", 0);
            }
            if (flowStartSeconds == 0) {
                // Fallback to full names
                flowStartSeconds = json.optLong("flowStartSeconds", 0);
            }
            if (flowStartSeconds == 0) {
                flowStartSeconds = json.optLong("timestamp", 0);
            }
            
            // Handle millisecond timestamps (convert to seconds)
            // Timestamps should be in seconds, but check just in case
            if (flowStartSeconds > 10000000000L) {
                // Timestamp is in milliseconds, convert to seconds
                flowStartSeconds = flowStartSeconds / 1000;
            }
            
            // If still no timestamp, use current time
            if (flowStartSeconds == 0) {
                flowStartSeconds = System.currentTimeMillis() / 1000;
            }
            
            // Validate required fields
            if (receiverCallsign.isEmpty() || frequency == 0) {
                Log.d(TAG, "Invalid spot - missing callsign or frequency. rc='" + receiverCallsign + 
                    "', f=" + frequency + ", JSON keys: " + getJsonKeys(json));
                return null;  // Invalid spot - must have callsign and frequency
            }
            
            return new Spot(receiverCallsign, receiverLocator, frequency, flowStartSeconds);
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing spot JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handle a received spot - add to list and notify callback.
     */
    private void handleSpot(Spot spot) {
        synchronized (spotsLock) {
            // Add to list (keep most recent spots, limit to reasonable size)
            receivedSpots.add(spot);
            if (receivedSpots.size() > 1000) {
                receivedSpots.remove(0);  // Remove oldest
            }
        }
        
        if (callback != null) {
            callback.onSpotReceived(spot);
        }
    }

    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
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
}
