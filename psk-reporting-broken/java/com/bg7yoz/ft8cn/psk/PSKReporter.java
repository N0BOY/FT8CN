package com.bg7yoz.ft8cn.psk;

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.log.ApplicationLogManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Reports decoded spots to PSK Reporter (report.pskreporter.info) using the IPFIX protocol.
 * Based on the protocol at https://pskreporter.info/pskdev.html and WSJT-X implementation.
 * Spots are batched and sent via UDP at most once per 2 minutes (with randomization).
 * Each callsign is reported at most once per 5 minutes (cache) to reduce server load.
 */
public class PSKReporter {

    private static final String TAG = "PSKReporter";
    private static final String HOST = "report.pskreporter.info";
    private static final int PORT = 4739;
    private static final int DEBUG_PORT = 14739;
    private static final int MIN_SEND_INTERVAL_SEC = 120;
    private static final int CACHE_TIMEOUT_SEC = 300;  // 5 minutes - don't repeat same call
    private static final int MIN_PAYLOAD_LENGTH = 508;
    private static final int MAX_PAYLOAD_LENGTH = 10000;
    private static final int FLUSH_INTERVAL = 20;  // send descriptors every ~20 packets (~hourly at 2 min/send)
    private static final int INFORMATION_SOURCE_AUTO = 1;

    // IPFIX template set IDs (cookie cutter from pskdev.html)
    private static final int RECEIVER_TEMPLATE_ID = 0x6392;
    private static final int SENDER_TEMPLATE_ID = 0x6393;

    // Record format descriptor bytes (receiver: 4 fields; sender: 8 fields with sNR, iMD, senderLocator)
    private static final byte[] RECEIVER_DESCRIPTOR = new byte[]{
        0x00, 0x03, 0x00, 0x2C, (byte) 0x99, (byte) 0x92, 0x00, 0x04, 0x00, 0x01,
        (byte) 0x80, 0x02, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x04, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x08, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x09, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        0x00, 0x00
    };
    private static final byte[] SENDER_DESCRIPTOR = new byte[]{
        0x00, 0x02, 0x00, 0x44, (byte) 0x99, (byte) 0x93, 0x00, 0x08,
        (byte) 0x80, 0x01, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x05, 0x00, 0x04, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x06, 0x00, 0x01, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x07, 0x00, 0x01, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x0A, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x0B, 0x00, 0x01, 0x00, 0x00, 0x76, (byte) 0x8F,
        (byte) 0x80, 0x03, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x76, (byte) 0x8F,
        0x00, (byte) 0x96, 0x00, 0x04
    };

    private final String programInfo;
    private final Random random = new Random();
    private final Map<String, Long> spotCache = new ConcurrentHashMap<>();
    private final Queue<Spot> spotQueue = new LinkedList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private long sequenceNumber = 0;
    private long observationId;
    private int flushCounter = 0;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private ApplicationLogManager logManager;

    public static class Spot {
        final String callsign;
        final long freqHz;
        final int snr;
        final String senderLocator;
        final long flowStartSeconds;

        public Spot(String callsign, long freqHz, int snr, String senderLocator, long flowStartSeconds) {
            this.callsign = callsign;
            this.freqHz = freqHz;
            this.snr = snr;
            this.senderLocator = senderLocator != null ? senderLocator : "";
            this.flowStartSeconds = flowStartSeconds;
        }
    }

    public PSKReporter() {
        this.programInfo = "FT8CN " + GeneralVariables.VERSION;
        this.observationId = random.nextInt() & 0x7FFFFFFF;
        if (observationId == 0) observationId = 1;
        // Initialize log manager if context is available
        try {
            android.content.Context context = GeneralVariables.getMainContext();
            if (context != null) {
                this.logManager = new ApplicationLogManager(context);
            }
        } catch (Exception e) {
            // Ignore - log manager will be null and we'll just use Log.d
        }
    }
    
    /**
     * Log debug message to both Log.d and PSK Reporter log file
     */
    private void logDebug(String message) {
        Log.d(TAG, message);
        writeToFile(message);
    }
    
    /**
     * Log warning message to both Log.w and PSK Reporter log file
     */
    private void logWarn(String message) {
        Log.w(TAG, message);
        writeToFile("WARN: " + message);
    }
    
    /**
     * Log error message to both Log.e and PSK Reporter log file
     */
    private void logError(String message) {
        Log.e(TAG, message);
        writeToFile("ERROR: " + message);
    }
    
    /**
     * Write message to PSK Reporter log file
     */
    private void writeToFile(String message) {
        if (logManager != null) {
            try {
                logManager.writeLog(ApplicationLogManager.LogType.PSK_REPORTER, message);
            } catch (Exception e) {
                // Ignore logging errors
            }
        }
    }

    /**
     * Start the reporter (timer for periodic sends). Call when decoding starts and PSK Reporter is enabled.
     */
    public void start() {
        if (running) {
            logDebug("start: Already running, skipping");
            return;
        }
        running = true;
        flushCounter = 0;
        logDebug("start: PSK Reporter started, scheduling first send");
        scheduleSend();
    }

    /**
     * Stop the reporter (no more sends scheduled). Does not shut down the scheduler
     * so that start() can be called again if the user re-enables the setting.
     */
    public void stop() {
        logDebug("stop: PSK Reporter stopped (was running=" + running + ")");
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception ignored) {}
            socket = null;
        }
    }

    /**
     * Add a spot for the given remote station. Deduplicated by callsign+freq; same call on same band reported at most once per 5 minutes.
     */
    public void addSpot(String callsign, long freqHz, int snr, String senderLocator, long flowStartSeconds) {
        if (callsign == null || callsign.isEmpty()) {
            logDebug("addSpot: Skipping null or empty callsign");
            return;
        }
        
        // Auto-start if not already running AND PSK Reporter is enabled (in case start() wasn't called)
        // This respects the user's setting to disable PSK Reporter
        if (!running && GeneralVariables.enablePskReporter) {
            logDebug("addSpot: Auto-starting PSK Reporter (was not running, but enabled)");
            start();
        } else if (!running && !GeneralVariables.enablePskReporter) {
            logDebug("addSpot: PSK Reporter is disabled, not auto-starting");
            return;  // Don't add spots if disabled
        }
        
        String key = callsign.toUpperCase() + ":" + freqHz;
        long now = System.currentTimeMillis() / 1000;
        Long last = spotCache.get(key);
        if (last != null && (now - last) < CACHE_TIMEOUT_SEC) {
            logDebug("addSpot: Skipping duplicate " + callsign + " on " + freqHz + " Hz (cached " + (now - last) + "s ago)");
            return;  // skip duplicate
        }
        spotCache.put(key, now);
        synchronized (spotQueue) {
            spotQueue.add(new Spot(callsign.toUpperCase(), freqHz, snr, senderLocator, flowStartSeconds));
            logDebug("addSpot: Added spot for " + callsign + " on " + freqHz + " Hz (queue size: " + spotQueue.size() + ")");
        }
        // prune cache of old entries (older than 10 min)
        Iterator<Map.Entry<String, Long>> it = spotCache.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > 600) it.remove();
        }
    }

    private int nextScheduledDelay = 0;  // Track the next scheduled delay for logging
    
    private void scheduleSend() {
        if (!running) {
            logDebug("scheduleSend: Not running, skipping schedule");
            return;
        }
        int delaySec = MIN_SEND_INTERVAL_SEC + random.nextInt(61);  // 120–181 seconds
        nextScheduledDelay = delaySec;  // Store for logging
        logDebug("scheduleSend: Scheduled send in " + delaySec + " seconds (running=" + running + ")");
        scheduler.schedule(this::sendReport, delaySec, TimeUnit.SECONDS);
    }

    private void sendReport() {
        logDebug("sendReport: Called (running=" + running + ", queue size=" + spotQueue.size() + ")");
        if (!running) {
            logDebug("sendReport: Not running, exiting");
            return;
        }
        
        // Determine port based on debug mode
        int port = GeneralVariables.pskReporterDebugMode ? DEBUG_PORT : PORT;
        logDebug("sendReport: Debug mode = " + GeneralVariables.pskReporterDebugMode + ", using " + HOST + ":" + port);
        
        try {
            ensureSocket();
            if (socket == null) {
                // Log socket creation failure
                scheduleSend();  // Schedule next attempt first to get the delay
                logExternalCall("PSK Reporter", HOST + ":" + port, "SKIPPED", 
                    String.format("Socket unavailable, next send in %d seconds", nextScheduledDelay));
                return;
            }

            String rxCall = GeneralVariables.myCallsign != null ? GeneralVariables.myCallsign : "";
            String rxGrid = GeneralVariables.getMyMaidenhead4Grid() != null ? GeneralVariables.getMyMaidenhead4Grid() : "";
            String antenna = GeneralVariables.pskReporterAntenna != null ? GeneralVariables.pskReporterAntenna : "";

            if (rxCall.isEmpty()) {
                scheduleSend();  // Schedule next attempt first to get the delay
                logExternalCall("PSK Reporter", HOST + ":" + port, "SKIPPED", 
                    String.format("Callsign not set, next send in %d seconds", nextScheduledDelay));
                return;
            }

            ArrayList<Spot> toSend = new ArrayList<>();
            synchronized (spotQueue) {
                while (!spotQueue.isEmpty() && toSend.size() < 80) {  // ~80 spots per packet typical
                    toSend.add(spotQueue.poll());
                }
            }

            // Log if no spots to send
            if (toSend.isEmpty()) {
                logDebug("sendReport: Queue is empty, skipping send");
                scheduleSend();  // Schedule next attempt first to get the delay
                logExternalCall("PSK Reporter", HOST + ":" + port, "SKIPPED", 
                    String.format("No spots to report, next send in %d seconds", nextScheduledDelay));
                return;
            }
            
            logDebug("sendReport: Preparing to send " + toSend.size() + " spots");
            
            // Log receiver information
            logDebug("sendReport: Receiver info - Call: " + rxCall + ", Grid: " + rxGrid + ", Antenna: " + antenna + ", Program: " + programInfo);
            
            // Log each spot being sent
            StringBuilder spotDetails = new StringBuilder("sendReport: Spots being sent:\n");
            for (int i = 0; i < toSend.size() && i < 10; i++) {  // Log first 10 spots
                Spot s = toSend.get(i);
                spotDetails.append(String.format("  [%d] %s @ %d Hz, SNR: %d, Grid: %s, Time: %d\n", 
                    i + 1, s.callsign, s.freqHz, s.snr, s.senderLocator.isEmpty() ? "(none)" : s.senderLocator, s.flowStartSeconds));
            }
            if (toSend.size() > 10) {
                spotDetails.append(String.format("  ... and %d more spots\n", toSend.size() - 10));
            }
            logDebug(spotDetails.toString());

            boolean includeDescriptors = (flushCounter < 3) || (flushCounter % FLUSH_INTERVAL == 0);
            flushCounter++;
            logDebug("sendReport: Packet includes descriptors: " + includeDescriptors + " (flushCounter: " + flushCounter + ")");
            logDebug("sendReport: Packet header - Sequence: " + sequenceNumber + ", Observation ID: " + observationId + ", Export time: " + (System.currentTimeMillis() / 1000));

            byte[] packet = buildPacket(rxCall, rxGrid, antenna, toSend, includeDescriptors);
            if (packet != null && packet.length > 0) {
                String debugModeStr = GeneralVariables.pskReporterDebugMode ? " (DEBUG MODE)" : "";
                logDebug("sendReport: Built packet (" + packet.length + " bytes), sending to " + HOST + ":" + port + debugModeStr);
                
                // Log hex dump of packet for debugging (full packet)
                StringBuilder hexDump = new StringBuilder("sendReport: Packet hex dump (full packet, " + packet.length + " bytes):\n");
                for (int i = 0; i < packet.length; i += 16) {
                    hexDump.append(String.format("%04X: ", i));
                    for (int j = 0; j < 16 && (i + j) < packet.length; j++) {
                        hexDump.append(String.format("%02X ", packet[i + j] & 0xFF));
                    }
                    // Add ASCII representation
                    hexDump.append("  ");
                    for (int j = 0; j < 16 && (i + j) < packet.length; j++) {
                        byte b = packet[i + j];
                        hexDump.append((b >= 32 && b < 127) ? (char)b : '.');
                    }
                    hexDump.append("\n");
                }
                logDebug(hexDump.toString());
                
                // Log packet structure breakdown
                logDebug("sendReport: Packet structure breakdown:");
                logDebug("  - IPFIX Header: 16 bytes");
                if (includeDescriptors) {
                    logDebug("  - Receiver Template Set: 44 bytes (Set ID 3)");
                    logDebug("  - Sender Template Set: 68 bytes (Set ID 2)");
                }
                logDebug("  - Receiver Data Set: Set ID 0x" + Integer.toHexString(RECEIVER_TEMPLATE_ID));
                if (!toSend.isEmpty()) {
                    logDebug("  - Sender Data Set: Set ID 0x" + Integer.toHexString(SENDER_TEMPLATE_ID) + 
                        ", " + toSend.size() + " records");
                }
                
                InetAddress addr = InetAddress.getByName(HOST);
                DatagramPacket dp = new DatagramPacket(packet, packet.length, addr, port);
                socket.send(dp);  // UDP send - no acknowledgment from server
                sequenceNumber++;  // IPFIX sequence number increments by 1 per message, not per record
                logDebug("sendReport: UDP packet queued for send (" + toSend.size() + " spots, sequence=" + sequenceNumber + ") - Note: PSK Reporter uses UDP and does not send acknowledgments");
                
                // Schedule next send first to get the delay
                scheduleSend();
                
                // Log successful PSK Reporter call with next send time
                // Note: "SUCCESS" means packet was queued for sending, not that server received it
                // PSK Reporter uses UDP (fire-and-forget) and doesn't send responses
                logExternalCall("PSK Reporter", HOST + ":" + port, "SUCCESS", 
                    String.format("Sent %d spots (UDP, no ack%s), next send in %d seconds", 
                        toSend.size(), GeneralVariables.pskReporterDebugMode ? ", DEBUG MODE" : "", nextScheduledDelay));
            } else {
                // Packet build failed, but schedule next attempt
                scheduleSend();
            }
        } catch (Exception e) {
            logWarn("PSK Reporter send failed: " + e.getMessage());
            
            // Schedule next attempt first to get the delay
            scheduleSend();
            
            // Log failed PSK Reporter call with next send time
            logExternalCall("PSK Reporter", HOST + ":" + port, "FAILED", 
                String.format("%s, next send in %d seconds", 
                    e.getMessage() != null ? e.getMessage() : "Unknown error", nextScheduledDelay));
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
                socket = null;
            }
        }
        // Note: scheduleSend() is now called earlier in each branch to get nextScheduledDelay for logging
    }
    
    /**
     * Log external service call
     */
    private void logExternalCall(String service, String endpoint, String status, String details) {
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

    private void ensureSocket() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(5000);
                } catch (Exception e) {
                    logWarn("Could not create UDP socket: " + e.getMessage());
            }
        }
    }

    private byte[] buildPacket(String rxCall, String rxGrid, String antenna, ArrayList<Spot> spots, boolean includeDescriptors) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_PAYLOAD_LENGTH);
            DataOutputStream out = new DataOutputStream(baos);

            // Header: version 10, length (placeholder), export time (placeholder), sequence number, observation ID
            out.writeShort(10);
            int lengthPos = baos.size();
            out.writeShort(0);  // length filled later
            out.writeInt((int) (System.currentTimeMillis() / 1000));  // export time
            out.writeInt((int) sequenceNumber);
            out.writeInt((int) observationId);

            if (includeDescriptors) {
                out.write(RECEIVER_DESCRIPTOR);
                out.write(SENDER_DESCRIPTOR);
            }

            // Receiver information record (required in every packet)
            ByteArrayOutputStream receiverData = new ByteArrayOutputStream();
            writeUtfString(receiverData, rxCall);
            writeUtfString(receiverData, rxGrid);
            writeUtfString(receiverData, programInfo);
            writeUtfString(receiverData, antenna);
            byte[] receiverBytes = padToFour(receiverData.toByteArray());
            out.writeShort((short) RECEIVER_TEMPLATE_ID);
            out.writeShort((short) (4 + receiverBytes.length));
            out.write(receiverBytes);

            // Sender information records
            byte[] senderBytes = null;
            if (!spots.isEmpty()) {
                ByteArrayOutputStream senderData = new ByteArrayOutputStream();
                for (Spot s : spots) {
                    writeUtfString(senderData, s.callsign);
                    writeIntBigEndian(senderData, (int) s.freqHz);
                    senderData.write(s.snr);  // 1 byte signed
                    senderData.write(0);      // iMD
                    writeUtfString(senderData, "FT8");
                    senderData.write(INFORMATION_SOURCE_AUTO);
                    writeUtfString(senderData, s.senderLocator);
                    writeIntBigEndian(senderData, (int) s.flowStartSeconds);
                }
                senderBytes = padToFour(senderData.toByteArray());
                out.writeShort((short) SENDER_TEMPLATE_ID);
                out.writeShort((short) (4 + senderBytes.length));
                out.write(senderBytes);
            }

            byte[] packet = baos.toByteArray();
            int len = packet.length;
            packet[lengthPos] = (byte) (len >> 8);
            packet[lengthPos + 1] = (byte) len;

            return packet;
            } catch (IOException e) {
                logError("Build packet failed: " + e.getMessage());
            return null;
        }
    }

    private static void writeUtfString(ByteArrayOutputStream out, String s) throws IOException {
        byte[] utf = (s != null ? s : "").getBytes(StandardCharsets.UTF_8);
        if (utf.length > 254) utf = java.util.Arrays.copyOf(utf, 254);
        out.write(utf.length);
        out.write(utf);
    }

    private static void writeUtfString(DataOutputStream out, String s) throws IOException {
        byte[] utf = (s != null ? s : "").getBytes(StandardCharsets.UTF_8);
        if (utf.length > 254) utf = java.util.Arrays.copyOf(utf, 254);
        out.writeByte(utf.length);
        out.write(utf);
    }

    private static void writeIntBigEndian(ByteArrayOutputStream out, int v) {
        out.write((v >> 24) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private static byte[] padToFour(byte[] b) {
        int pad = (4 - b.length % 4) % 4;
        if (pad == 0) return b;
        byte[] r = new byte[b.length + pad];
        System.arraycopy(b, 0, r, 0, b.length);
        // IPFIX padding bytes must be 0x00
        for (int i = b.length; i < r.length; i++) {
            r[i] = 0;
        }
        return r;
    }
}
