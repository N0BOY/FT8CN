package com.bg7yoz.ft8cn.ft8listener;

import com.bg7yoz.ft8cn.Ft8Message;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for DecodeDuplicateFilter.
 * Tests multi-factor duplicate detection including message text, callsigns, frequency, and time.
 */
public class DecodeDuplicateFilterTest {

    private Ft8Message createMessage(String text, String from, String to, float freq, float time, int snr) {
        Ft8Message msg = new Ft8Message(0);
        msg.i3 = 0;
        msg.n3 = 0;
        msg.extraInfo = text;
        msg.callsignFrom = from;
        msg.callsignTo = to;
        msg.freq_hz = freq;
        msg.time_sec = time;
        msg.snr = snr;
        return msg;
    }

    @Test
    public void testExactDuplicate() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        Ft8Message duplicate = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, duplicate));
    }

    @Test
    public void testDifferentCallsignFrom() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Same text, same frequency, same time, but different callsign
        Ft8Message different = createMessage("CQ TEST", "K2XYZ", "CQ", 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, different));
    }

    @Test
    public void testDifferentCallsignTo() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "W9XYZ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Same text, same frequency, same time, but different "to" callsign
        Ft8Message different = createMessage("CQ TEST", "K1ABC", "W8ABC", 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, different));
    }

    @Test
    public void testFrequencyTolerance() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Within 3 Hz tolerance
        Ft8Message withinTolerance = createMessage("CQ TEST", "K1ABC", "CQ", 1502.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, withinTolerance));

        // Exactly 3 Hz difference
        Ft8Message exactly3Hz = createMessage("CQ TEST", "K1ABC", "CQ", 1503.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, exactly3Hz));

        // Just over 3 Hz tolerance
        Ft8Message overTolerance = createMessage("CQ TEST", "K1ABC", "CQ", 1504.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, overTolerance));
    }

    @Test
    public void testTimeTolerance() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Within 0.2 second tolerance
        Ft8Message withinTolerance = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.3f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, withinTolerance));

        // Exactly 0.2 second difference
        Ft8Message exactly0_2 = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.4f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, exactly0_2));

        // Just over 0.2 second tolerance
        Ft8Message overTolerance = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.41f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, overTolerance));
    }

    @Test
    public void testSNRUpdate() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Duplicate with higher SNR should update base message SNR
        Ft8Message higherSNR = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -5);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, higherSNR));
        assertEquals(-5, base.snr, 0.01f); // Base SNR should be updated
    }

    @Test
    public void testSNRNotUpdatedWhenLower() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -5);
        messages.add(base);

        // Duplicate with lower SNR should not update base message SNR
        Ft8Message lowerSNR = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, lowerSNR));
        assertEquals(-5, base.snr, 0.01f); // Base SNR should remain unchanged
    }

    @Test
    public void testNullCallsigns() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", null, null, 1500.0f, 0.2f, -10);
        messages.add(base);

        // Both null - should match
        Ft8Message bothNull = createMessage("CQ TEST", null, null, 1500.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, bothNull));

        // One null, one not - should not match
        Ft8Message oneNull = createMessage("CQ TEST", "K1ABC", null, 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, oneNull));
    }

    @Test
    public void testDifferentMessageText() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Same callsigns, frequency, time, but different text
        Ft8Message differentText = createMessage("CQ OTHER", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, differentText));
    }

    @Test
    public void testMultipleMessagesInList() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10));
        messages.add(createMessage("CQ OTHER", "K2XYZ", "CQ", 1600.0f, 0.3f, -15));
        messages.add(createMessage("CQ THIRD", "K3DEF", "CQ", 1700.0f, 0.4f, -20));

        // Should match first message
        Ft8Message duplicate = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, duplicate));

        // Should not match any
        Ft8Message notDuplicate = createMessage("CQ NEW", "K4GHI", "CQ", 1800.0f, 0.5f, -25);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, notDuplicate));
    }

    @Test
    public void testEdgeCaseFrequencyBoundaries() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Exactly at 3 Hz boundary
        Ft8Message atBoundary = createMessage("CQ TEST", "K1ABC", "CQ", 1503.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, atBoundary));

        // Just over boundary
        Ft8Message overBoundary = createMessage("CQ TEST", "K1ABC", "CQ", 1503.1f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, overBoundary));
    }

    @Test
    public void testEdgeCaseTimeBoundaries() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(base);

        // Exactly at 0.2 second boundary
        Ft8Message atBoundary = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.4f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, atBoundary));

        // Just over boundary
        Ft8Message overBoundary = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.41f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, overBoundary));
    }

    @Test
    public void testNegativeFrequency() {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createMessage("CQ TEST", "K1ABC", "CQ", -500.0f, 0.2f, -10);
        messages.add(base);

        // Should handle negative frequencies correctly
        Ft8Message duplicate = createMessage("CQ TEST", "K1ABC", "CQ", -500.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, duplicate));

        Ft8Message different = createMessage("CQ TEST", "K1ABC", "CQ", -497.0f, 0.2f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, different)); // Within 3 Hz

        Ft8Message tooFar = createMessage("CQ TEST", "K1ABC", "CQ", -496.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, tooFar)); // Over 3 Hz
    }

    @Test
    public void testEmptyMessageList() {
        ArrayList<Ft8Message> messages = new ArrayList<>();

        Ft8Message candidate = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, candidate));
    }

    @Test
    public void testRealWorldScenario_SameStationDifferentDecode() {
        // Simulate same signal decoded twice (initial + deep decode)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message initial = createMessage("K1ABC W9XYZ -10", "K1ABC", "W9XYZ", 1500.5f, 0.25f, -12);
        messages.add(initial);

        // Same message decoded again with slightly different frequency/time (within tolerance)
        Ft8Message deepDecode = createMessage("K1ABC W9XYZ -10", "K1ABC", "W9XYZ", 1500.3f, 0.26f, -10);
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, deepDecode));
        assertEquals(-10, initial.snr, 0.01f); // SNR should be updated to higher value
    }

    @Test
    public void testRealWorldScenario_DifferentStationsSameText() {
        // Two different stations with same message text (should NOT be duplicates)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message station1 = createMessage("CQ TEST", "K1ABC", "CQ", 1500.0f, 0.2f, -10);
        messages.add(station1);

        // Different station, same text, same frequency/time
        Ft8Message station2 = createMessage("CQ TEST", "K2XYZ", "CQ", 1500.0f, 0.2f, -10);
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, station2));
    }
}
