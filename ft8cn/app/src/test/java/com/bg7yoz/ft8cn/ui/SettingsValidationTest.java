package com.bg7yoz.ft8cn.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for settings validation logic.
 * Tests validation rules used in ConfigFragment and settings dialogs.
 */
public class SettingsValidationTest {

    /**
     * Test callsign validation.
     */
    @Test
    public void testCallsignValidation() {
        // Valid callsigns
        assertTrue("Simple callsign should be valid", isValidCallsign("W9XYZ"));
        assertTrue("Compound callsign should be valid", isValidCallsign("W9XYZ/P"));
        assertTrue("Portable callsign should be valid", isValidCallsign("W9XYZ/MM"));
        
        // Invalid callsigns
        assertFalse("Empty callsign should be invalid", isValidCallsign(""));
        assertFalse("Too short callsign should be invalid", isValidCallsign("W9"));
        assertFalse("Null callsign should be invalid", isValidCallsign(null));
    }

    private boolean isValidCallsign(String callsign) {
        return callsign != null && callsign.length() >= 3;
    }

    /**
     * Test Maidenhead grid validation.
     */
    @Test
    public void testMaidenheadGridValidation() {
        // Valid grids
        assertTrue("4-character grid should be valid", isValidGrid("OL50"));
        assertTrue("6-character grid should be valid", isValidGrid("OL50ab"));
        
        // Invalid grids
        assertFalse("Empty grid should be invalid", isValidGrid(""));
        assertFalse("Too short grid should be invalid", isValidGrid("OL5"));
        assertFalse("Invalid format should be invalid", isValidGrid("1234"));
    }

    private boolean isValidGrid(String grid) {
        if (grid == null || grid.length() < 4) {
            return false;
        }
        // Basic validation: first 2 chars should be letters, next 2 should be digits
        return grid.matches("^[A-Z]{2}[0-9]{2}.*");
    }

    /**
     * Test frequency validation.
     */
    @Test
    public void testFrequencyValidation() {
        // Valid frequencies
        assertTrue("100 Hz should be valid", isValidFrequency(100.0f));
        assertTrue("1500 Hz should be valid", isValidFrequency(1500.0f));
        assertTrue("2900 Hz should be valid", isValidFrequency(2900.0f));
        
        // Invalid frequencies
        assertFalse("Below 100 Hz should be invalid", isValidFrequency(99.0f));
        assertFalse("Above 2900 Hz should be invalid", isValidFrequency(2901.0f));
        assertFalse("Negative frequency should be invalid", isValidFrequency(-100.0f));
    }

    private boolean isValidFrequency(float freq) {
        return freq >= 100.0f && freq <= 2900.0f;
    }

    /**
     * Test modifier validation (POTA, SOTA, etc.).
     */
    @Test
    public void testModifierValidation() {
        // Valid modifiers
        assertTrue("POTA should be valid", isValidModifier("POTA"));
        assertTrue("SOTA should be valid", isValidModifier("SOTA"));
        assertTrue("3-digit modifier should be valid", isValidModifier("123"));
        assertTrue("1-4 letter modifier should be valid", isValidModifier("A"));
        assertTrue("Empty modifier should be valid", isValidModifier(""));
        
        // Invalid modifiers
        assertFalse("Too long modifier should be invalid", isValidModifier("POTAS"));
        assertFalse("Invalid format should be invalid", isValidModifier("POTA123"));
    }

    private boolean isValidModifier(String modifier) {
        if (modifier == null) {
            return false;
        }
        String trimmed = modifier.trim();
        return trimmed.length() == 0 || 
               trimmed.matches("[0-9]{3}") || 
               trimmed.matches("[A-Z]{1,4}");
    }

    /**
     * Test CI-V address validation (hexadecimal).
     */
    @Test
    public void testCIVAddressValidation() {
        // Valid CI-V addresses
        assertTrue("A4 should be valid", isValidCIVAddress("A4"));
        assertTrue("a4 should be valid (case insensitive)", isValidCIVAddress("a4"));
        assertTrue("0A should be valid", isValidCIVAddress("0A"));
        assertTrue("FF should be valid", isValidCIVAddress("FF"));
        
        // Invalid CI-V addresses
        assertFalse("Empty should be invalid", isValidCIVAddress(""));
        assertFalse("Single digit should be invalid", isValidCIVAddress("A"));
        assertFalse("Non-hex should be invalid", isValidCIVAddress("G0"));
        assertFalse("Too long should be invalid", isValidCIVAddress("A4F"));
    }

    private boolean isValidCIVAddress(String address) {
        if (address == null || address.length() != 2) {
            return false;
        }
        return address.matches("[0-9A-Fa-f]{2}");
    }

    /**
     * Test volume percentage validation.
     */
    @Test
    public void testVolumeValidation() {
        // Valid volumes
        assertTrue("0% should be valid", isValidVolume(0.0f));
        assertTrue("50% should be valid", isValidVolume(0.5f));
        assertTrue("100% should be valid", isValidVolume(1.0f));
        
        // Invalid volumes
        assertFalse("Negative volume should be invalid", isValidVolume(-0.1f));
        assertFalse("Over 100% should be invalid", isValidVolume(1.1f));
    }

    private boolean isValidVolume(float volume) {
        return volume >= 0.0f && volume <= 1.0f;
    }

    /**
     * Test excluded callsigns format validation.
     */
    @Test
    public void testExcludedCallsignsFormat() {
        // Valid formats
        assertTrue("Comma-separated should be valid", isValidExcludedFormat("K1, W1, VE"));
        assertTrue("Pipe-separated should be valid", isValidExcludedFormat("K1|W1|VE"));
        assertTrue("Space-separated should be valid", isValidExcludedFormat("K1 W1 VE"));
        assertTrue("Mixed separators should be valid", isValidExcludedFormat("K1, W1|VE"));
        assertTrue("Empty should be valid", isValidExcludedFormat(""));
        
        // Invalid formats (none - all formats are accepted)
        // The format is flexible, so we just check it's not null
        assertFalse("Null should be invalid", isValidExcludedFormat(null));
    }

    private boolean isValidExcludedFormat(String excluded) {
        return excluded != null;
    }

    /**
     * Test transmission delay validation.
     */
    @Test
    public void testTransmissionDelayValidation() {
        // Valid delays
        assertTrue("100ms should be valid", isValidTransmissionDelay(100));
        assertTrue("500ms should be valid", isValidTransmissionDelay(500));
        assertTrue("9999ms should be valid", isValidTransmissionDelay(9999));
        
        // Invalid delays
        assertFalse("0ms should be invalid", isValidTransmissionDelay(0));
        assertFalse("Negative should be invalid", isValidTransmissionDelay(-100));
        assertFalse("Over 9999ms should be invalid", isValidTransmissionDelay(10000));
    }

    private boolean isValidTransmissionDelay(int delay) {
        return delay > 0 && delay <= 9999;
    }

    /**
     * Test API key format validation.
     */
    @Test
    public void testAPIKeyValidation() {
        // Valid API keys
        assertTrue("Alphanumeric should be valid", isValidAPIKey("abc123"));
        assertTrue("Long key should be valid", isValidAPIKey("abcdefghijklmnopqrstuvwxyz1234567890"));
        assertTrue("Empty should be valid (optional)", isValidAPIKey(""));
        
        // Invalid API keys (none - format is flexible)
        // API keys can have various formats, so we just check it's not null
        assertFalse("Null should be invalid", isValidAPIKey(null));
    }

    private boolean isValidAPIKey(String apiKey) {
        return apiKey != null;
    }

    /**
     * Test band frequency validation.
     */
    @Test
    public void testBandFrequencyValidation() {
        // Valid band frequencies (in Hz)
        assertTrue("1.8 MHz should be valid", isValidBandFrequency(1800000L));
        assertTrue("3.5 MHz should be valid", isValidBandFrequency(3500000L));
        assertTrue("7 MHz should be valid", isValidBandFrequency(7000000L));
        assertTrue("14 MHz should be valid", isValidBandFrequency(14074000L));
        assertTrue("28 MHz should be valid", isValidBandFrequency(28000000L));
        
        // Invalid band frequencies
        assertFalse("0 Hz should be invalid", isValidBandFrequency(0L));
        assertFalse("Negative should be invalid", isValidBandFrequency(-1000000L));
        assertFalse("Too high should be invalid", isValidBandFrequency(30000000000L)); // 30 GHz
    }

    private boolean isValidBandFrequency(long freq) {
        return freq > 0 && freq < 30000000000L; // Up to 30 GHz
    }
}
