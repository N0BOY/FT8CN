package com.bg7yoz.ft8cn.ft8transmit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for TransmitCallsign class.
 * Tests SNR formatting, target callsign detection, and constructor behavior.
 */
public class TransmitCallsignTest {

    // ==================== SNR Formatting Tests ====================

    @Test
    public void testGetSnr_Positive_SingleDigit() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 5);

        assertEquals("+05", tc.getSnr());
    }

    @Test
    public void testGetSnr_Positive_DoubleDigit() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 15);

        assertEquals("+15", tc.getSnr());
    }

    @Test
    public void testGetSnr_Positive_Max() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 30);

        assertEquals("+30", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_SingleDigit() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -5);

        assertEquals("-05", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_DoubleDigit() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -15);

        assertEquals("-15", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_Max() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -30);

        assertEquals("-30", tc.getSnr());
    }

    @Test
    public void testGetSnr_Zero() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 0);

        assertEquals("+00", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_One() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -1);

        assertEquals("-01", tc.getSnr());
    }

    @Test
    public void testGetSnr_Positive_One() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 1);

        assertEquals("+01", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_Nine() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -9);

        assertEquals("-09", tc.getSnr());
    }

    @Test
    public void testGetSnr_Positive_Ten() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 10);

        assertEquals("+10", tc.getSnr());
    }

    @Test
    public void testGetSnr_Negative_Ten() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -10);

        assertEquals("-10", tc.getSnr());
    }

    // ==================== Target Callsign Detection Tests ====================

    @Test
    public void testHaveTargetCallsign_ValidCallsign() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 0);

        assertTrue(tc.haveTargetCallsign());
    }

    @Test
    public void testHaveTargetCallsign_CQ_ReturnsFalse() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "CQ", 0);

        assertFalse(tc.haveTargetCallsign());
    }

    @Test
    public void testHaveTargetCallsign_Null_ReturnsFalse() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, null, 0);

        assertFalse(tc.haveTargetCallsign());
    }

    @Test
    public void testHaveTargetCallsign_PortableCallsign() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC/P", 0);

        assertTrue(tc.haveTargetCallsign());
    }

    @Test
    public void testHaveTargetCallsign_RoverCallsign() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC/R", 0);

        assertTrue(tc.haveTargetCallsign());
    }

    @Test
    public void testHaveTargetCallsign_EmptyString() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "", 0);

        // Empty string is not "CQ" and not null, so returns true
        // This tests the actual behavior of the method
        assertTrue(tc.haveTargetCallsign());
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_Simple() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 0);

        assertEquals("K1ABC", tc.callsign);
        assertEquals(1, tc.i3);
        assertEquals(0, tc.n3);
        assertEquals(0, tc.sequential);
    }

    @Test
    public void testConstructor_Full() {
        TransmitCallsign tc = new TransmitCallsign(2, 0, "K1ABC/P", 1500.5f, 1, -10);

        assertEquals("K1ABC/P", tc.callsign);
        assertEquals(2, tc.i3);
        assertEquals(0, tc.n3);
        assertEquals(1500.5f, tc.frequency, 0.01f);
        assertEquals(1, tc.sequential);
        assertEquals(-10, tc.snr);
    }

    @Test
    public void testConstructor_NonStandardCallsign() {
        TransmitCallsign tc = new TransmitCallsign(4, 0, "VK9/K1ABC", 1500.0f, 0, 5);

        assertEquals("VK9/K1ABC", tc.callsign);
        assertEquals(4, tc.i3);
    }

    @Test
    public void testConstructor_EvenSequence() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 0);

        assertEquals(0, tc.sequential);
    }

    @Test
    public void testConstructor_OddSequence() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 1, 0);

        assertEquals(1, tc.sequential);
    }

    // ==================== Field Access Tests ====================

    @Test
    public void testDxccField() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 0);
        tc.dxcc = "United States";

        assertEquals("United States", tc.dxcc);
    }

    @Test
    public void testCqZoneField() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 0);
        tc.cqZone = 5;

        assertEquals(5, tc.cqZone);
    }

    @Test
    public void testItuField() {
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 0);
        tc.itu = 8;

        assertEquals(8, tc.itu);
    }

    // ==================== Boundary Tests ====================

    @Test
    public void testSnr_BoundaryValues() {
        // Test typical FT8 SNR range (-24 to +30 dB)
        TransmitCallsign tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, -24);
        assertEquals("-24", tc.getSnr());

        tc = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 30);
        assertEquals("+30", tc.getSnr());
    }

    @Test
    public void testFrequency_TypicalValues() {
        // Test typical FT8 audio frequencies (200-2800 Hz)
        TransmitCallsign tc1 = new TransmitCallsign(1, 0, "K1ABC", 200.0f, 0, 0);
        assertEquals(200.0f, tc1.frequency, 0.01f);

        TransmitCallsign tc2 = new TransmitCallsign(1, 0, "K1ABC", 2800.0f, 0, 0);
        assertEquals(2800.0f, tc2.frequency, 0.01f);

        TransmitCallsign tc3 = new TransmitCallsign(1, 0, "K1ABC", 1500.0f, 0, 0);
        assertEquals(1500.0f, tc3.frequency, 0.01f);
    }
}
