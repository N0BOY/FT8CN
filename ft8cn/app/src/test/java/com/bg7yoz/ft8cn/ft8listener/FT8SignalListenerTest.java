package com.bg7yoz.ft8cn.ft8listener;

import com.bg7yoz.ft8cn.Ft8Message;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FT8SignalListenerTest {

    @Test
    public void checkMessageSame_requiresMatchingCallsignAndOffsets() throws Exception {
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message base = createFreeTextMessage("CQ TEST", "K1ABC", "CQ");
        base.freq_hz = 1500.0f;
        base.time_sec = 0.2f;
        base.snr = -10;
        messages.add(base);

        // Same text, different callsign -> should NOT be considered duplicate
        Ft8Message differentCall = createFreeTextMessage("CQ TEST", "K2XYZ", "CQ");
        differentCall.freq_hz = 1500.0f;
        differentCall.time_sec = 0.2f;
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, differentCall));

        // Same text and callsign, but outside frequency tolerance -> not duplicate
        Ft8Message differentFreq = createFreeTextMessage("CQ TEST", "K1ABC", "CQ");
        differentFreq.freq_hz = 1510.0f;
        differentFreq.time_sec = 0.2f;
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, differentFreq));

        // Same text and callsign, but outside time tolerance -> not duplicate
        Ft8Message differentTime = createFreeTextMessage("CQ TEST", "K1ABC", "CQ");
        differentTime.freq_hz = 1500.0f;
        differentTime.time_sec = 0.6f;
        assertFalse(DecodeDuplicateFilter.isDuplicate(messages, differentTime));

        // Same text, callsign, and within tolerances -> duplicate
        Ft8Message duplicate = createFreeTextMessage("CQ TEST", "K1ABC", "CQ");
        duplicate.freq_hz = 1502.0f;
        duplicate.time_sec = 0.3f;
        assertTrue(DecodeDuplicateFilter.isDuplicate(messages, duplicate));
    }

    private Ft8Message createFreeTextMessage(String text, String from, String to) {
        Ft8Message message = new Ft8Message(0);
        message.i3 = 0;
        message.n3 = 0;
        message.extraInfo = text;
        message.callsignFrom = from;
        message.callsignTo = to;
        return message;
    }
}
