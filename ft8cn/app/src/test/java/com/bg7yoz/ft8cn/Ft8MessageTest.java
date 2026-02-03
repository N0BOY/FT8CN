package com.bg7yoz.ft8cn;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Ft8Message class.
 * Tests message text formatting, sequence detection, CQ checks, and callsign extraction.
 *
 * Note: Tests focus on pure logic that doesn't require Android framework.
 * Methods that use GeneralVariables, DatabaseOpr, or R.string are not tested here.
 */
public class Ft8MessageTest {

    private Ft8Message message;

    @Before
    public void setUp() {
        message = new Ft8Message(FT8Common.FT8_MODE);
    }

    // ==================== Message Text Formatting Tests ====================

    @Test
    public void testGetMessageText_FreeText_ShortMessage() {
        message.i3 = 0;
        message.n3 = 0;
        message.extraInfo = "TNX 73";

        String result = message.getMessageText();

        // Free text is padded to 13 characters
        assertEquals("TNX 73       ", result);
        assertEquals(13, result.length());
    }

    @Test
    public void testGetMessageText_FreeText_ExactLength() {
        message.i3 = 0;
        message.n3 = 0;
        message.extraInfo = "TNX BOB 73 GL";

        String result = message.getMessageText();

        assertEquals("TNX BOB 73 GL", result);
        assertEquals(13, result.length());
    }

    @Test
    public void testGetMessageText_FreeText_TooLong() {
        message.i3 = 0;
        message.n3 = 0;
        message.extraInfo = "THIS IS TOO LONG FOR FT8";

        String result = message.getMessageText();

        // Should be truncated to 13 characters
        assertEquals("THIS IS TOO L", result);
        assertEquals(13, result.length());
    }

    @Test
    public void testGetMessageText_FreeText_LowercaseConvertedToUppercase() {
        message.i3 = 0;
        message.n3 = 0;
        message.extraInfo = "hello world";

        String result = message.getMessageText();

        assertEquals("HELLO WORLD  ", result);
    }

    @Test
    public void testGetMessageText_StandardMessage() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";

        String result = message.getMessageText();

        assertEquals("W9XYZ K1ABC EN50", result);
    }

    @Test
    public void testGetMessageText_StandardMessage_WithReport() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "-15";

        String result = message.getMessageText();

        assertEquals("W9XYZ K1ABC -15", result);
    }

    @Test
    public void testGetMessageText_StandardMessage_RR73() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "RR73";

        String result = message.getMessageText();

        assertEquals("W9XYZ K1ABC RR73", result);
    }

    @Test
    public void testGetMessageText_CQ_WithModifier() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "CQ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";
        message.modifier = "POTA";

        String result = message.getMessageText();

        assertEquals("CQ POTA K1ABC EN50", result);
    }

    @Test
    public void testGetMessageText_CQ_WithNumericModifier() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "CQ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";
        message.modifier = "599";

        String result = message.getMessageText();

        assertEquals("CQ 599 K1ABC EN50", result);
    }

    @Test
    public void testGetMessageText_CQ_InvalidModifier_Ignored() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "CQ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";
        message.modifier = "TOOLONG"; // More than 4 chars, doesn't match pattern

        String result = message.getMessageText();

        // Invalid modifier should be ignored
        assertEquals("CQ K1ABC EN50", result);
    }

    @Test
    public void testGetMessageText_FieldDay() {
        message.i3 = 0;
        message.n3 = 3;
        message.callsignTo = "K1ABC";
        message.callsignFrom = "W9XYZ";
        message.r_flag = 0;
        message.eu_serial = 6;
        message.arrl_class = "A";
        message.arrl_rac = "WI";

        String result = message.getMessageText();

        assertEquals("K1ABC W9XYZ 6A WI", result);
    }

    @Test
    public void testGetMessageText_FieldDay_WithRFlag() {
        message.i3 = 0;
        message.n3 = 4;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.r_flag = 1;
        message.eu_serial = 17;
        message.arrl_class = "B";
        message.arrl_rac = "EMA";

        String result = message.getMessageText();

        assertEquals("W9XYZ K1ABC R 17B EMA", result);
    }

    @Test
    public void testGetMessageText_RTTY_RU() {
        message.i3 = 3;
        message.n3 = 0;
        message.callsignTo = "K1ABC";
        message.callsignFrom = "W9XYZ";
        message.rtty_tu = 0;
        message.r_flag = 0;
        message.report = 579;
        message.rtty_state = "WI";

        String result = message.getMessageText();

        assertEquals("K1ABC W9XYZ 579 WI", result);
    }

    @Test
    public void testGetMessageText_RTTY_RU_WithTU() {
        message.i3 = 3;
        message.n3 = 0;
        message.callsignTo = "K1ABC";
        message.callsignFrom = "W9XYZ";
        message.rtty_tu = 1;
        message.r_flag = 1;
        message.report = 599;
        message.rtty_state = "CA";

        String result = message.getMessageText();

        assertEquals("TU; K1ABC W9XYZ R 599 CA", result);
    }

    @Test
    public void testGetMessageText_EU_VHF() {
        message.i3 = 5;
        message.n3 = 0;
        message.callsignTo = "<G4ABC>";
        message.callsignFrom = "<PA9XYZ>";
        message.r_flag = 1;
        message.report = 57;
        message.eu_serial = 7;
        message.maidenGrid = "JO22DB";

        String result = message.getMessageText();

        assertEquals("<G4ABC> <PA9XYZ> R 570007 JO22DB", result);
    }

    @Test
    public void testGetMessageText_WithWeakSignalMarker() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";
        message.isWeakSignal = true;

        String result = message.getMessageText(true);

        assertEquals("*W9XYZ K1ABC EN50", result);
    }

    @Test
    public void testGetMessageText_WeakSignal_NotShown() {
        message.i3 = 1;
        message.n3 = 0;
        message.callsignTo = "W9XYZ";
        message.callsignFrom = "K1ABC";
        message.extraInfo = "EN50";
        message.isWeakSignal = true;

        String result = message.getMessageText(false);

        assertEquals("W9XYZ K1ABC EN50", result);
    }

    // ==================== CQ Detection Tests ====================

    @Test
    public void testCheckIsCQ_CQ() {
        message.callsignTo = "CQ";

        assertTrue(message.checkIsCQ());
    }

    @Test
    public void testCheckIsCQ_CQ_WithModifier() {
        message.callsignTo = "CQ POTA";

        assertTrue(message.checkIsCQ());
    }

    @Test
    public void testCheckIsCQ_DE() {
        message.callsignTo = "DE";

        assertTrue(message.checkIsCQ());
    }

    @Test
    public void testCheckIsCQ_QRZ() {
        message.callsignTo = "QRZ";

        assertTrue(message.checkIsCQ());
    }

    @Test
    public void testCheckIsCQ_RegularCallsign() {
        message.callsignTo = "W9XYZ";

        assertFalse(message.checkIsCQ());
    }

    @Test
    public void testCheckIsCQ_CallsignStartingWithCQ() {
        // A callsign that starts with CQ but isn't CQ
        message.callsignTo = "CQ5A"; // Portuguese special callsign

        assertFalse(message.checkIsCQ());
    }

    // ==================== Callsign Extraction Tests ====================

    @Test
    public void testGetCallsignFrom_Normal() {
        message.callsignFrom = "K1ABC";

        assertEquals("K1ABC", message.getCallsignFrom());
    }

    @Test
    public void testGetCallsignFrom_WithBrackets() {
        message.callsignFrom = "<K1ABC>";

        assertEquals("K1ABC", message.getCallsignFrom());
    }

    @Test
    public void testGetCallsignFrom_Null() {
        message.callsignFrom = null;

        assertEquals("", message.getCallsignFrom());
    }

    @Test
    public void testGetCallsignTo_Normal() {
        message.callsignTo = "W9XYZ";

        assertEquals("W9XYZ", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_WithBrackets() {
        message.callsignTo = "<W9XYZ>";

        assertEquals("W9XYZ", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_CQ_ReturnsEmpty() {
        message.callsignTo = "CQ";

        assertEquals("", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_DE_ReturnsEmpty() {
        message.callsignTo = "DE";

        assertEquals("", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_QRZ_ReturnsEmpty() {
        message.callsignTo = "QRZ";

        assertEquals("", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_Null() {
        message.callsignTo = null;

        assertEquals("", message.getCallsignTo());
    }

    @Test
    public void testGetCallsignTo_TooShort() {
        message.callsignTo = "A";

        assertEquals("", message.getCallsignTo());
    }

    // ==================== Sequence Detection Tests ====================

    @Test
    public void testGetSequence_FT8_Even() {
        message.signalFormat = FT8Common.FT8_MODE;
        // Time at start of even slot (0 seconds into minute)
        // (0 + 750) / 1000 / 15 % 2 = 0
        message.utcTime = 0;

        assertEquals(0, message.getSequence());
    }

    @Test
    public void testGetSequence_FT8_Odd() {
        message.signalFormat = FT8Common.FT8_MODE;
        // Time at 15 seconds into minute
        // (15000 + 750) / 1000 / 15 % 2 = 1
        message.utcTime = 15000;

        assertEquals(1, message.getSequence());
    }

    @Test
    public void testGetSequence_FT8_30Seconds() {
        message.signalFormat = FT8Common.FT8_MODE;
        // Time at 30 seconds
        // (30000 + 750) / 1000 / 15 % 2 = 0
        message.utcTime = 30000;

        assertEquals(0, message.getSequence());
    }

    @Test
    public void testGetSequence_FT8_45Seconds() {
        message.signalFormat = FT8Common.FT8_MODE;
        // Time at 45 seconds
        // (45000 + 750) / 1000 / 15 % 2 = 1
        message.utcTime = 45000;

        assertEquals(1, message.getSequence());
    }

    @Test
    public void testGetSequence4_FT8() {
        message.signalFormat = FT8Common.FT8_MODE;

        // Test all 4 sequences
        message.utcTime = 0;
        assertEquals(0, message.getSequence4());

        message.utcTime = 15000;
        assertEquals(1, message.getSequence4());

        message.utcTime = 30000;
        assertEquals(2, message.getSequence4());

        message.utcTime = 45000;
        assertEquals(3, message.getSequence4());

        // Back to 0
        message.utcTime = 60000;
        assertEquals(0, message.getSequence4());
    }

    @Test
    public void testIsEvenSequence_FT8_AtZero() {
        message.signalFormat = FT8Common.FT8_MODE;
        message.utcTime = 0;

        assertTrue(message.isEvenSequence());
    }

    @Test
    public void testIsEvenSequence_FT8_AtMidSlot() {
        message.signalFormat = FT8Common.FT8_MODE;
        // 7 seconds into first slot - not at slot boundary
        message.utcTime = 7000;

        assertFalse(message.isEvenSequence());
    }

    @Test
    public void testIsEvenSequence_FT8_At15Seconds() {
        message.signalFormat = FT8Common.FT8_MODE;
        message.utcTime = 15000;

        assertTrue(message.isEvenSequence());
    }

    @Test
    public void testIsEvenSequence_FT8_At30Seconds() {
        message.signalFormat = FT8Common.FT8_MODE;
        message.utcTime = 30000;

        assertTrue(message.isEvenSequence());
    }

    // ==================== Formatting Tests ====================

    @Test
    public void testGetFreq_hz_Formatting() {
        message.freq_hz = 1500.0f;

        assertEquals("1500", message.getFreq_hz());
    }

    @Test
    public void testGetFreq_hz_LowFrequency() {
        message.freq_hz = 300.0f;

        assertEquals("0300", message.getFreq_hz());
    }

    @Test
    public void testGetFreq_hz_HighFrequency() {
        message.freq_hz = 2800.5f;

        assertEquals("2801", message.getFreq_hz());
    }

    @Test
    public void testGetDt_Positive() {
        message.time_sec = 0.5f;

        assertEquals("0.5", message.getDt());
    }

    @Test
    public void testGetDt_Negative() {
        message.time_sec = -0.3f;

        assertEquals("-0.3", message.getDt());
    }

    @Test
    public void testGetDt_Zero() {
        message.time_sec = 0.0f;

        assertEquals("0.0", message.getDt());
    }

    @Test
    public void testGetdB() {
        message.snr = -15;

        assertEquals("-15", message.getdB());
    }

    @Test
    public void testGetdB_Positive() {
        message.snr = 5;

        assertEquals("5", message.getdB());
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_WithCallsigns() {
        Ft8Message msg = new Ft8Message("CQ", "K1ABC", "EN50");

        assertEquals("CQ", msg.callsignTo);
        assertEquals("K1ABC", msg.callsignFrom);
        assertEquals("EN50", msg.extraInfo);
    }

    @Test
    public void testConstructor_LowercaseConvertedToUppercase() {
        Ft8Message msg = new Ft8Message("cq", "k1abc", "en50");

        assertEquals("CQ", msg.callsignTo);
        assertEquals("K1ABC", msg.callsignFrom);
        assertEquals("EN50", msg.extraInfo);
    }

    @Test
    public void testConstructor_WithI3N3() {
        Ft8Message msg = new Ft8Message(1, 0, "W9XYZ", "K1ABC", "-15");

        assertEquals(1, msg.i3);
        assertEquals(0, msg.n3);
        assertEquals("W9XYZ", msg.callsignTo);
        assertEquals("K1ABC", msg.callsignFrom);
        assertEquals("-15", msg.extraInfo);
    }
}
