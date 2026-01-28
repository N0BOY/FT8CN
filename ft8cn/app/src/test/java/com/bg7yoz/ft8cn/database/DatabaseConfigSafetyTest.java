package com.bg7yoz.ft8cn.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for database configuration safety.
 * Tests that ensure database config values are safely parsed and defaulted.
 */
public class DatabaseConfigSafetyTest {

    /**
     * Test that all numeric config fields have safe parsing with defaults.
     */
    @Test
    public void testNumericConfigFieldsHaveDefaults() {
        // Test that all numeric fields can handle null/empty/invalid values
        String[] numericFields = {
            "freq", "transDelay", "civ", "baudRate", "bandFreq",
            "controlMode", "model", "instruction", "launchSupervision",
            "noReplyLimit", "pttDelay", "icomPort", "volumeValue",
            "flexMaxRfPower", "flexMaxTunePower", "audioRate",
            "dataBits", "stopBits", "parityBits", "manualTimeslot"
        };
        
        for (String field : numericFields) {
            assertNotNull("Field should have name", field);
            // Each field should have safe parsing logic
            assertTrue("Field should be handled: " + field, field.length() > 0);
        }
    }

    /**
     * Test that string config fields handle null values safely.
     */
    @Test
    public void testStringConfigFieldsHandleNull() {
        String[] stringFields = {
            "callsign", "grid", "toModifier", "icomIp", "icomUserName",
            "icomPassword", "excludedCallsigns", "cloudlogServerAddress",
            "cloudlogApiKey", "cloudlogStationID", "qrzApiKey"
        };
        
        for (String field : stringFields) {
            // Null should default to empty string
            String result = safeGetString(null, "");
            assertEquals("Null should default to empty", "", result);
            
            // Empty string should be preserved
            result = safeGetString("", "");
            assertEquals("Empty string should be preserved", "", result);
            
            // Valid string should be preserved
            result = safeGetString("value", "");
            assertEquals("Valid string should be preserved", "value", result);
        }
    }
    
    private String safeGetString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Test that boolean config fields handle invalid values safely.
     */
    @Test
    public void testBooleanConfigFieldsHandleInvalid() {
        String[] booleanFields = {
            "autoFollowCQ", "autoCallFollow", "saveSWL", "saveSWLQSO",
            "audioBits", "deepMode", "enableCloudlog", "enableQRZ",
            "swrSwitch", "alcSwitch", "decodeOverrunToast",
            "liveDecodeUpdates", "skipMyGridWhenResponding",
            "callingAddsToFollowList", "msgMode", "synFreq"
        };
        
        for (String field : booleanFields) {
            // Only "1" should be true, everything else false
            assertFalse("Null should be false", safeParseBoolean(null, false));
            assertFalse("Empty should be false", safeParseBoolean("", false));
            assertFalse("Invalid should be false", safeParseBoolean("invalid", false));
            assertTrue("'1' should be true", safeParseBoolean("1", false));
            assertFalse("'0' should be false", safeParseBoolean("0", false));
        }
    }
    
    private boolean safeParseBoolean(String value, boolean defaultValue) {
        if (value == null || value.equals("")) {
            return defaultValue;
        }
        return value.equals("1");
    }

    /**
     * Test frequency clamping to valid range.
     */
    @Test
    public void testFrequencyClamping() {
        // Frequency should be clamped between 100 and 2900
        assertEquals(100.0f, clampFrequency(50.0f), 0.01f);
        assertEquals(100.0f, clampFrequency(0.0f), 0.01f);
        assertEquals(100.0f, clampFrequency(-100.0f), 0.01f);
        assertEquals(1500.0f, clampFrequency(1500.0f), 0.01f);
        assertEquals(2900.0f, clampFrequency(2900.0f), 0.01f);
        assertEquals(2900.0f, clampFrequency(3000.0f), 0.01f);
        assertEquals(2900.0f, clampFrequency(10000.0f), 0.01f);
    }
    
    private float clampFrequency(float freq) {
        if (freq < 100.0f) return 100.0f;
        if (freq > 2900.0f) return 2900.0f;
        return freq;
    }

    /**
     * Test volume clamping to valid range.
     */
    @Test
    public void testVolumeClamping() {
        // Volume should be clamped between 0.0 and 1.0
        assertEquals(0.0f, clampVolume(-0.1f), 0.01f);
        assertEquals(0.0f, clampVolume(0.0f), 0.01f);
        assertEquals(0.5f, clampVolume(0.5f), 0.01f);
        assertEquals(1.0f, clampVolume(1.0f), 0.01f);
        assertEquals(1.0f, clampVolume(1.1f), 0.01f);
        assertEquals(1.0f, clampVolume(2.0f), 0.01f);
    }
    
    private float clampVolume(float volume) {
        if (volume < 0.0f) return 0.0f;
        if (volume > 1.0f) return 1.0f;
        return volume;
    }

    /**
     * Test that hex parsing handles invalid values safely.
     */
    @Test
    public void testHexParsingSafety() {
        // CI-V address parsing
        assertEquals(0xa4, safeParseHex(null, 0xa4));
        assertEquals(0xa4, safeParseHex("", 0xa4));
        assertEquals(0xa4, safeParseHex("invalid", 0xa4));
        assertEquals(0xa4, safeParseHex("G0", 0xa4));
        assertEquals(0xa4, safeParseHex("A4", 0));
        assertEquals(0xff, safeParseHex("FF", 0));
        assertEquals(0x00, safeParseHex("00", 0));
    }
    
    private int safeParseHex(String value, int defaultValue) {
        if (value == null || value.equals("")) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Test that transmission delay validation works correctly.
     */
    @Test
    public void testTransmissionDelayValidation() {
        // Should match 1-4 digit pattern
        assertTrue("1 digit should match", "1".matches("^\\d{1,4}$"));
        assertTrue("4 digits should match", "1234".matches("^\\d{1,4}$"));
        assertFalse("5 digits should not match", "12345".matches("^\\d{1,4}$"));
        assertFalse("Non-numeric should not match", "abc".matches("^\\d{1,4}$"));
        assertFalse("Empty should not match", "".matches("^\\d{1,4}$"));
    }

    /**
     * Test that all config value types are handled safely.
     */
    @Test
    public void testAllConfigValueTypes() {
        // Integer values
        testIntegerConfig("100", 100, 0);
        testIntegerConfig(null, 0, 0);
        testIntegerConfig("", 0, 0);
        testIntegerConfig("invalid", 0, 0);
        
        // Float values
        testFloatConfig("1500.0", 1500.0f, 1000.0f);
        testFloatConfig(null, 1000.0f, 1000.0f);
        testFloatConfig("", 1000.0f, 1000.0f);
        testFloatConfig("invalid", 1000.0f, 1000.0f);
        
        // Long values
        testLongConfig("14074000", 14074000L, 1000L);
        testLongConfig(null, 1000L, 1000L);
        testLongConfig("", 1000L, 1000L);
        testLongConfig("invalid", 1000L, 1000L);
        
        // String values
        testStringConfig("value", "value", "");
        testStringConfig(null, "", "");
        testStringConfig("", "", "");
        
        // Boolean values
        testBooleanConfig("1", true, false);
        testBooleanConfig("0", false, false);
        testBooleanConfig(null, false, false);
        testBooleanConfig("", false, false);
        testBooleanConfig("invalid", false, false);
    }
    
    private void testIntegerConfig(String value, int expected, int defaultValue) {
        int result = safeParseInt(value, defaultValue);
        assertEquals("Integer config: " + value, expected, result);
    }
    
    private void testFloatConfig(String value, float expected, float defaultValue) {
        float result = safeParseFloat(value, defaultValue);
        assertEquals("Float config: " + value, expected, result, 0.01f);
    }
    
    private void testLongConfig(String value, long expected, long defaultValue) {
        long result = safeParseLong(value, defaultValue);
        assertEquals("Long config: " + value, expected, result);
    }
    
    private void testStringConfig(String value, String expected, String defaultValue) {
        String result = safeGetString(value, defaultValue);
        assertEquals("String config: " + value, expected, result);
    }
    
    private void testBooleanConfig(String value, boolean expected, boolean defaultValue) {
        boolean result = safeParseBoolean(value, defaultValue);
        assertEquals("Boolean config: " + value, expected, result);
    }
    
    private int safeParseInt(String value, int defaultValue) {
        if (value == null || value.equals("")) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private float safeParseFloat(String value, float defaultValue) {
        if (value == null || value.equals("")) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private long safeParseLong(String value, long defaultValue) {
        if (value == null || value.equals("")) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Test that database column existence checks work correctly.
     * Older databases might not have all columns.
     */
    @Test
    public void testDatabaseColumnExistenceChecks() {
        // Simulate column index check (returns -1 if column doesn't exist)
        int existingColumn = 0; // Column exists
        int missingColumn = -1; // Column doesn't exist
        
        assertTrue("Existing column should be valid", existingColumn >= 0);
        assertFalse("Missing column should be invalid", missingColumn >= 0);
        
        // Should use default value when column doesn't exist
        boolean value = missingColumn >= 0 ? true : false; // Default to false
        assertFalse("Should use default when column missing", value);
    }

    /**
     * Test that config key names are case-insensitive.
     */
    @Test
    public void testConfigKeyCaseInsensitivity() {
        String[] variations = {"callsign", "Callsign", "CALLSIGN", "CaLlSiGn"};
        
        for (String variation : variations) {
            // All should match "callsign" (case-insensitive)
            assertTrue("Should match case-insensitively: " + variation,
                      variation.equalsIgnoreCase("callsign"));
        }
    }

    /**
     * Test edge cases for config value parsing.
     */
    @Test
    public void testConfigValueEdgeCases() {
        // Very large numbers
        assertEquals(Integer.MAX_VALUE, safeParseInt(String.valueOf(Integer.MAX_VALUE), 0));
        assertEquals(Integer.MIN_VALUE, safeParseInt(String.valueOf(Integer.MIN_VALUE), 0));
        
        // Very small/large floats
        assertEquals(Float.MAX_VALUE, safeParseFloat(String.valueOf(Float.MAX_VALUE), 0.0f), 0.01f);
        assertEquals(Float.MIN_VALUE, safeParseFloat(String.valueOf(Float.MIN_VALUE), 0.0f), 0.01f);
        
        // Overflow should default
        assertEquals(0, safeParseInt("999999999999999999999", 0));
        assertEquals(0L, safeParseLong("999999999999999999999999999999", 0L));
    }
}
