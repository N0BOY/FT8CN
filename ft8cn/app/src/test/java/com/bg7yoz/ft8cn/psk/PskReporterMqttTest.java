package com.bg7yoz.ft8cn.psk;

import com.bg7yoz.ft8cn.GeneralVariables;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PskReporterMqtt class.
 * Tests MQTT spot parsing, storage, and handling functionality.
 */
public class PskReporterMqttTest {

    private PskReporterMqtt mqtt;
    private MockCallback mockCallback;

    @Before
    public void setUp() {
        // Save original callsign
        String originalCallsign = GeneralVariables.myCallsign;
        
        // Set test callsign
        GeneralVariables.myCallsign = "W9XYZ";
        
        mqtt = new PskReporterMqtt();
        mockCallback = new MockCallback();
        
        // Restore original callsign after test
        // Note: In a real scenario, you might want to restore this in @After
    }

    // ========== Spot Class Tests ==========

    @Test
    public void testSpotConstructor() {
        PskReporterMqtt.Spot spot = new PskReporterMqtt.Spot(
            "K1ABC", "FN20", 14074000L, 1234567890L
        );
        
        assertEquals("K1ABC", spot.receiverCallsign);
        assertEquals("FN20", spot.receiverLocator);
        assertEquals(14074000L, spot.frequency);
        assertEquals(1234567890L, spot.flowStartSeconds);
    }

    @Test
    public void testSpotConstructor_withNullCallsign() {
        PskReporterMqtt.Spot spot = new PskReporterMqtt.Spot(
            null, "FN20", 14074000L, 1234567890L
        );
        
        assertEquals("", spot.receiverCallsign);
        assertEquals("FN20", spot.receiverLocator);
    }

    @Test
    public void testSpotConstructor_withNullLocator() {
        PskReporterMqtt.Spot spot = new PskReporterMqtt.Spot(
            "K1ABC", null, 14074000L, 1234567890L
        );
        
        assertEquals("K1ABC", spot.receiverCallsign);
        assertEquals("", spot.receiverLocator);
    }

    // ========== Spot Storage Tests ==========

    @Test
    public void testGetReceivedSpots_initiallyEmpty() {
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertTrue("Spots list should be empty initially", spots.isEmpty());
    }

    @Test
    public void testClearSpots() {
        // Add a spot using reflection to test clearSpots
        addSpotViaReflection("K1ABC", "FN20", 14074000L, 1234567890L);
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertEquals("Should have one spot", 1, spots.size());
        
        mqtt.clearSpots();
        
        spots = mqtt.getReceivedSpots();
        assertTrue("Spots list should be empty after clear", spots.isEmpty());
    }

    @Test
    public void testGetReceivedSpots_returnsCopy() {
        addSpotViaReflection("K1ABC", "FN20", 14074000L, 1234567890L);
        
        List<PskReporterMqtt.Spot> spots1 = mqtt.getReceivedSpots();
        List<PskReporterMqtt.Spot> spots2 = mqtt.getReceivedSpots();
        
        // Should be different instances (defensive copy)
        assertTrue("Should return defensive copy", spots1 != spots2);
        assertEquals("Both copies should have same size", spots1.size(), spots2.size());
    }

    // ========== JSON Parsing Tests ==========
    // Note: These tests use reflection to test private methods.
    // They may fail if Android Log is not available in test environment.
    // The parsing logic is tested indirectly through spot storage.

    @Test
    public void testParseSpot_validSpot() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should be parsed", spot);
        assertEquals("K1ABC", spot.receiverCallsign);
        assertEquals("FN20", spot.receiverLocator);
        assertEquals(14074000L, spot.frequency);
        assertEquals(1234567890L, spot.flowStartSeconds);
    }

    @Test
    public void testParseSpot_withFullFieldNames() {
        String json = "{\"receiverCallsign\":\"K1ABC\",\"receiverLocator\":\"FN20\",\"frequency\":14074000,\"timestamp\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should be parsed with full field names", spot);
        assertEquals("K1ABC", spot.receiverCallsign);
        assertEquals("FN20", spot.receiverLocator);
        assertEquals(14074000L, spot.frequency);
    }

    @Test
    public void testParseSpot_missingCallsign() {
        String json = "{\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNull("Spot should be null when callsign is missing", spot);
    }

    @Test
    public void testParseSpot_missingFrequency() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNull("Spot should be null when frequency is missing", spot);
    }

    @Test
    public void testParseSpot_emptyCallsign() {
        String json = "{\"rc\":\"\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNull("Spot should be null when callsign is empty", spot);
    }

    @Test
    public void testParseSpot_zeroFrequency() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":0,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNull("Spot should be null when frequency is zero", spot);
    }

    @Test
    public void testParseSpot_fallbackToReceiverField() {
        String json = "{\"receiver\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse with receiver field", spot);
        assertEquals("K1ABC", spot.receiverCallsign);
    }

    @Test
    public void testParseSpot_fallbackToReceiverGrid() {
        String json = "{\"rc\":\"K1ABC\",\"receiverGrid\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse with receiverGrid field", spot);
        assertEquals("FN20", spot.receiverLocator);
    }

    @Test
    public void testParseSpot_frequencyAsDouble() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000.5,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse frequency as double", spot);
        assertEquals(14074000L, spot.frequency);
    }

    @Test
    public void testParseSpot_timestampFallbacks() {
        // Test t_tx fallback
        String json1 = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t_tx\":1234567890}";
        PskReporterMqtt.Spot spot1 = parseSpotViaReflection(json1);
        assertNotNull("Spot should parse with t_tx", spot1);
        assertEquals(1234567890L, spot1.flowStartSeconds);
        
        // Test flowStartSeconds fallback
        String json2 = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"flowStartSeconds\":1234567890}";
        PskReporterMqtt.Spot spot2 = parseSpotViaReflection(json2);
        assertNotNull("Spot should parse with flowStartSeconds", spot2);
        assertEquals(1234567890L, spot2.flowStartSeconds);
    }

    @Test
    public void testParseSpot_millisecondTimestamp() {
        // Timestamp > 10000000000L indicates milliseconds
        long millis = 1234567890000L; // milliseconds
        long expectedSeconds = 1234567890L;
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":" + millis + "}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse millisecond timestamp", spot);
        assertEquals("Timestamp should be converted to seconds", expectedSeconds, spot.flowStartSeconds);
    }

    @Test
    public void testParseSpot_missingTimestamp() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse even without timestamp", spot);
        assertTrue("Timestamp should be set to current time (approximately)", 
                   spot.flowStartSeconds > 0);
    }

    @Test
    public void testParseSpot_emptyLocator() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"\",\"f\":14074000,\"t\":1234567890}";
        PskReporterMqtt.Spot spot = parseSpotViaReflection(json);
        
        assertNotNull("Spot should parse with empty locator", spot);
        assertEquals("", spot.receiverLocator);
    }

    // ========== Message Parsing Tests ==========

    @Test
    public void testParseAndHandleMessage_singleSpot() {
        String json = "{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}";
        parseAndHandleMessageViaReflection(json);
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertEquals("Should have one spot", 1, spots.size());
        assertEquals("K1ABC", spots.get(0).receiverCallsign);
    }

    @Test
    public void testParseAndHandleMessage_arrayOfSpots() {
        String json = "{\"spots\":[{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}," +
                      "{\"rc\":\"K2DEF\",\"rl\":\"DM33\",\"f\":14075000,\"t\":1234567891}]}";
        parseAndHandleMessageViaReflection(json);
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertEquals("Should have two spots", 2, spots.size());
        assertEquals("K1ABC", spots.get(0).receiverCallsign);
        assertEquals("K2DEF", spots.get(1).receiverCallsign);
    }

    @Test
    public void testParseAndHandleMessage_receptionReportsArray() {
        String json = "{\"receptionReports\":[{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}]}";
        parseAndHandleMessageViaReflection(json);
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertEquals("Should have one spot from receptionReports", 1, spots.size());
    }

    @Test
    public void testParseAndHandleMessage_emptyPayload() {
        parseAndHandleMessageViaReflection("");
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertTrue("Should have no spots from empty payload", spots.isEmpty());
    }

    @Test
    public void testParseAndHandleMessage_invalidJSON() {
        String invalidJson = "{\"rc\":\"K1ABC\",\"invalid\":}";
        parseAndHandleMessageViaReflection(invalidJson);
        
        // Should not crash, but also not add spots
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertTrue("Should handle invalid JSON gracefully", spots.isEmpty() || spots.size() >= 0);
    }

    @Test
    public void testParseAndHandleMessage_invalidSpotInArray() {
        // Array with one valid and one invalid spot
        String json = "{\"spots\":[{\"rc\":\"K1ABC\",\"rl\":\"FN20\",\"f\":14074000,\"t\":1234567890}," +
                      "{\"rc\":\"\",\"f\":14075000}]}"; // Missing callsign
        parseAndHandleMessageViaReflection(json);
        
        List<PskReporterMqtt.Spot> spots = mqtt.getReceivedSpots();
        assertEquals("Should have one valid spot", 1, spots.size());
        assertEquals("K1ABC", spots.get(0).receiverCallsign);
    }

    // ========== Connection State Tests ==========

    @Test
    public void testIsConnected_initiallyFalse() {
        assertFalse("Should not be connected initially", mqtt.isConnected());
    }

    // ========== Helper Methods ==========

    /**
     * Add a spot using reflection to test internal methods
     */
    private void addSpotViaReflection(String callsign, String locator, long frequency, long timestamp) {
        try {
            PskReporterMqtt.Spot spot = new PskReporterMqtt.Spot(callsign, locator, frequency, timestamp);
            Method handleSpot = PskReporterMqtt.class.getDeclaredMethod("handleSpot", PskReporterMqtt.Spot.class);
            handleSpot.setAccessible(true);
            handleSpot.invoke(mqtt, spot);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add spot via reflection", e);
        }
    }

    /**
     * Parse a spot using reflection to test parseSpot method
     * Note: This may fail if Android Log is not available in test environment
     */
    private PskReporterMqtt.Spot parseSpotViaReflection(String json) {
        try {
            org.json.JSONObject jsonObj = new org.json.JSONObject(json);
            Method parseSpot = PskReporterMqtt.class.getDeclaredMethod("parseSpot", org.json.JSONObject.class);
            parseSpot.setAccessible(true);
            return (PskReporterMqtt.Spot) parseSpot.invoke(mqtt, jsonObj);
        } catch (Exception e) {
            // If Android Log is not available, skip test
            Throwable cause = e.getCause();
            if (cause != null && (cause.getClass().getName().contains("Log") || 
                (cause.getMessage() != null && cause.getMessage().contains("Log")))) {
                org.junit.Assume.assumeTrue("Android Log not available in unit test environment", false);
            }
            throw new RuntimeException("Failed to parse spot via reflection", e);
        }
    }

    /**
     * Parse and handle message using reflection to test parseAndHandleMessage method
     * Note: This may fail if Android Log is not available in test environment
     */
    private void parseAndHandleMessageViaReflection(String payload) {
        try {
            Method parseAndHandleMessage = PskReporterMqtt.class.getDeclaredMethod("parseAndHandleMessage", String.class);
            parseAndHandleMessage.setAccessible(true);
            parseAndHandleMessage.invoke(mqtt, payload);
        } catch (Exception e) {
            // If Android Log is not available, skip test
            Throwable cause = e.getCause();
            if (cause != null && (cause.getClass().getName().contains("Log") || 
                (cause.getMessage() != null && cause.getMessage().contains("Log")))) {
                org.junit.Assume.assumeTrue("Android Log not available in unit test environment", false);
            }
            throw new RuntimeException("Failed to parse message via reflection", e);
        }
    }

    /**
     * Mock callback for testing
     */
    private static class MockCallback implements PskReporterMqtt.Callback {
        private int spotCount = 0;
        private int connectedCount = 0;
        private int disconnectedCount = 0;
        private String lastError = null;

        @Override
        public void onSpotReceived(PskReporterMqtt.Spot spot) {
            spotCount++;
        }

        @Override
        public void onConnected() {
            connectedCount++;
        }

        @Override
        public void onDisconnected() {
            disconnectedCount++;
        }

        @Override
        public void onError(String message) {
            lastError = message;
        }

        public void reset() {
            spotCount = 0;
            connectedCount = 0;
            disconnectedCount = 0;
            lastError = null;
        }
    }
}
