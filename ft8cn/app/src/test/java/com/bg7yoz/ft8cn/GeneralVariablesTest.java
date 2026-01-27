package com.bg7yoz.ft8cn;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for GeneralVariables utility functions.
 * Tests message function order detection, callsign checking, and exclusion logic.
 */
public class GeneralVariablesTest {

    @Before
    public void setUp() {
        // Reset state
        GeneralVariables.myCallsign = "W9XYZ";
        GeneralVariables.addExcludedCallsigns("");
    }

    // ========== Function Order Tests ==========

    @Test
    public void testCheckFun1_GridReport() {
        // Valid grid squares (4 characters, A-Z, 0-9)
        assertTrue(GeneralVariables.checkFun1("OL50"));
        assertTrue(GeneralVariables.checkFun1("FN20"));
        assertTrue(GeneralVariables.checkFun1("DM33"));
        assertTrue(GeneralVariables.checkFun1("AA00"));
        assertTrue(GeneralVariables.checkFun1("ZZ99"));
        
        // Empty string is valid for function 1
        assertTrue(GeneralVariables.checkFun1(""));
        assertTrue(GeneralVariables.checkFun1("   ")); // Whitespace trimmed
        
        // Invalid: RR73 is not a grid (it's function 4)
        assertFalse(GeneralVariables.checkFun1("RR73"));
        
        // Invalid: wrong length
        assertFalse(GeneralVariables.checkFun1("OL5"));
        assertFalse(GeneralVariables.checkFun1("OL500"));
        
        // Invalid: wrong format
        assertFalse(GeneralVariables.checkFun1("1234"));
        assertFalse(GeneralVariables.checkFun1("ABCD"));
    }

    @Test
    public void testCheckFun2_SignalReport() {
        // Valid signal reports (numeric, not 73, at least 2 characters)
        assertTrue(GeneralVariables.checkFun2("-10"));
        assertTrue(GeneralVariables.checkFun2("10"));
        assertTrue(GeneralVariables.checkFun2("-20"));
        assertTrue(GeneralVariables.checkFun2("+5"));
        assertTrue(GeneralVariables.checkFun2("00")); // Two digits
        
        // Invalid: 73 is function 5, not function 2
        assertFalse(GeneralVariables.checkFun2("73"));
        
        // Invalid: too short (must be at least 2 characters)
        assertFalse(GeneralVariables.checkFun2("0")); // Only 1 character
        assertFalse(GeneralVariables.checkFun2("1"));
        assertFalse(GeneralVariables.checkFun2(""));
        
        // Invalid: not numeric
        assertFalse(GeneralVariables.checkFun2("ABC"));
        assertFalse(GeneralVariables.checkFun2("R-10")); // That's function 3
    }

    @Test
    public void testCheckFun3_ReportWithR() {
        // Valid R-prefixed reports (at least 3 characters)
        assertTrue(GeneralVariables.checkFun3("R-10"));
        assertTrue(GeneralVariables.checkFun3("R10"));
        assertTrue(GeneralVariables.checkFun3("R00")); // At least 3 chars
        assertTrue(GeneralVariables.checkFun3("R+5"));
        
        // Invalid: too short (must be at least 3 characters)
        assertFalse(GeneralVariables.checkFun3("R"));
        assertFalse(GeneralVariables.checkFun3("R0")); // Only 2 characters
        assertFalse(GeneralVariables.checkFun3("R1")); // Only 2 characters
        
        // Invalid: doesn't start with R
        assertFalse(GeneralVariables.checkFun3("-10"));
        assertFalse(GeneralVariables.checkFun3("10"));
        
        // Invalid: second character is R (like "RR73")
        assertFalse(GeneralVariables.checkFun3("RR73"));
        assertFalse(GeneralVariables.checkFun3("RRR"));
    }

    @Test
    public void testCheckFun4_RR73OrRRR() {
        // Valid function 4 messages
        assertTrue(GeneralVariables.checkFun4("RR73"));
        assertTrue(GeneralVariables.checkFun4("RRR"));
        assertTrue(GeneralVariables.checkFun4("  RR73  ")); // Whitespace trimmed
        
        // Invalid
        assertFalse(GeneralVariables.checkFun4("RR"));
        assertFalse(GeneralVariables.checkFun4("R73"));
        assertFalse(GeneralVariables.checkFun4("73"));
        assertFalse(GeneralVariables.checkFun4(""));
    }

    @Test
    public void testCheckFun5_73() {
        // Valid function 5
        assertTrue(GeneralVariables.checkFun5("73"));
        assertTrue(GeneralVariables.checkFun5("  73  ")); // Whitespace trimmed
        
        // Invalid
        assertFalse(GeneralVariables.checkFun5("RR73"));
        assertFalse(GeneralVariables.checkFun5("R73"));
        assertFalse(GeneralVariables.checkFun5(""));
        assertFalse(GeneralVariables.checkFun5("7"));
        assertFalse(GeneralVariables.checkFun5("3"));
    }

    @Test
    public void testCheckFunOrderByExtraInfo() {
        // Test order detection priority (5 > 4 > 3 > 2 > 1)
        assertEquals(5, GeneralVariables.checkFunOrderByExtraInfo("73"));
        assertEquals(4, GeneralVariables.checkFunOrderByExtraInfo("RR73"));
        assertEquals(4, GeneralVariables.checkFunOrderByExtraInfo("RRR"));
        assertEquals(3, GeneralVariables.checkFunOrderByExtraInfo("R-10"));
        assertEquals(2, GeneralVariables.checkFunOrderByExtraInfo("-10"));
        assertEquals(1, GeneralVariables.checkFunOrderByExtraInfo("OL50"));
        assertEquals(1, GeneralVariables.checkFunOrderByExtraInfo(""));
        
        // Invalid returns -1
        assertEquals(-1, GeneralVariables.checkFunOrderByExtraInfo("INVALID"));
        assertEquals(-1, GeneralVariables.checkFunOrderByExtraInfo("ABC"));
    }

    @Test
    public void testCheckFunOrder_WithCQ() {
        Ft8Message cqMessage = new Ft8Message(0);
        cqMessage.i3 = 0;
        cqMessage.n3 = 0;
        cqMessage.extraInfo = "CQ";
        cqMessage.callsignFrom = "CQ";
        cqMessage.callsignTo = "CQ"; // Must be set for checkIsCQ() to work
        
        // CQ messages should return order 6
        assertEquals(6, GeneralVariables.checkFunOrder(cqMessage));
    }

    @Test
    public void testCheckFunOrder_WithExtraInfo() {
        Ft8Message msg = new Ft8Message(0);
        msg.i3 = 0;
        msg.n3 = 0;
        msg.extraInfo = "R-10";
        msg.callsignFrom = "K1ABC";
        msg.callsignTo = "W9XYZ";
        
        assertEquals(3, GeneralVariables.checkFunOrder(msg));
    }

    @Test
    public void testCheckFun2_3_ExtractReport() {
        // Extract report from function 2 or 3
        assertEquals(-10, GeneralVariables.checkFun2_3("-10"));
        assertEquals(10, GeneralVariables.checkFun2_3("10"));
        assertEquals(0, GeneralVariables.checkFun2_3("0"));
        assertEquals(-20, GeneralVariables.checkFun2_3("-20"));
        assertEquals(5, GeneralVariables.checkFun2_3("+5"));
        
        // R-prefixed reports
        assertEquals(-10, GeneralVariables.checkFun2_3("R-10"));
        assertEquals(10, GeneralVariables.checkFun2_3("R10"));
        assertEquals(0, GeneralVariables.checkFun2_3("R0"));
        
        // 73 returns -100 (invalid)
        assertEquals(-100, GeneralVariables.checkFun2_3("73"));
        
        // Invalid formats return -100
        assertEquals(-100, GeneralVariables.checkFun2_3("ABC"));
        assertEquals(-100, GeneralVariables.checkFun2_3(""));
        assertEquals(-100, GeneralVariables.checkFun2_3("RR73"));
    }

    @Test
    public void testCheckFun1_6_GridSquare() {
        // Valid grid squares
        assertTrue(GeneralVariables.checkFun1_6("OL50"));
        assertTrue(GeneralVariables.checkFun1_6("FN20"));
        assertTrue(GeneralVariables.checkFun1_6("DM33"));
        
        // Invalid: RR73 is not a grid
        assertFalse(GeneralVariables.checkFun1_6("RR73"));
        
        // Invalid: wrong format
        assertFalse(GeneralVariables.checkFun1_6("OL5"));
        assertFalse(GeneralVariables.checkFun1_6("1234"));
        assertFalse(GeneralVariables.checkFun1_6(""));
    }

    @Test
    public void testCheckFun4_5_EndOfQSO() {
        // Valid end-of-QSO messages
        assertTrue(GeneralVariables.checkFun4_5("RR73"));
        assertTrue(GeneralVariables.checkFun4_5("RRR"));
        assertTrue(GeneralVariables.checkFun4_5("73"));
        
        // Invalid
        assertFalse(GeneralVariables.checkFun4_5("R-10"));
        assertFalse(GeneralVariables.checkFun4_5("-10"));
        assertFalse(GeneralVariables.checkFun4_5("OL50"));
        assertFalse(GeneralVariables.checkFun4_5(""));
    }

    // ========== Callsign Tests ==========

    @Test
    public void testCheckIsMyCallsign() {
        GeneralVariables.myCallsign = "W9XYZ";
        
        // Exact match
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ"));
        
        // Contains my callsign
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ/P"));
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ/MM"));
        
        // Different callsign
        assertFalse(GeneralVariables.checkIsMyCallsign("K1ABC"));
        assertFalse(GeneralVariables.checkIsMyCallsign("W9ABC"));
        
        // Empty my callsign
        GeneralVariables.myCallsign = "";
        assertFalse(GeneralVariables.checkIsMyCallsign("W9XYZ"));
    }

    @Test
    public void testCheckIsMyCallsign_CompoundCallsigns() {
        GeneralVariables.myCallsign = "W9XYZ/P";
        
        // Should match base callsign
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ"));
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ/P"));
        assertTrue(GeneralVariables.checkIsMyCallsign("W9XYZ/MM"));
        
        // Should not match different base
        assertFalse(GeneralVariables.checkIsMyCallsign("K1ABC"));
    }

    @Test
    public void testGetShortCallsign() {
        // Simple callsign
        assertEquals("W9XYZ", GeneralVariables.getShortCallsign("W9XYZ"));
        
        // Compound callsign - should return longest part
        assertEquals("W9XYZ", GeneralVariables.getShortCallsign("W9XYZ/P"));
        assertEquals("W9XYZ", GeneralVariables.getShortCallsign("P/W9XYZ"));
        assertEquals("W9XYZ", GeneralVariables.getShortCallsign("W9XYZ/MM"));
        
        // Multiple slashes
        assertEquals("W9XYZ", GeneralVariables.getShortCallsign("W9XYZ/P/MM"));
    }

    // ========== Exclusion Tests ==========

    @Test
    public void testCheckIsExcludeCallsign() {
        // Add excluded callsigns
        GeneralVariables.addExcludedCallsigns("K1, W1, VE");
        
        // Should match prefixes
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        assertTrue(GeneralVariables.checkIsExcludeCallsign("W1XYZ"));
        assertTrue(GeneralVariables.checkIsExcludeCallsign("VE3ABC"));
        
        // Should not match non-excluded
        assertFalse(GeneralVariables.checkIsExcludeCallsign("W9XYZ"));
        assertFalse(GeneralVariables.checkIsExcludeCallsign("K2ABC"));
        
        // Case insensitive
        assertTrue(GeneralVariables.checkIsExcludeCallsign("k1abc"));
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
    }

    @Test
    public void testAddExcludedCallsigns_VariousFormats() {
        // Comma-separated
        GeneralVariables.addExcludedCallsigns("K1, W1, VE");
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        
        // Space-separated
        GeneralVariables.addExcludedCallsigns("K1 W1 VE");
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        
        // Pipe-separated
        GeneralVariables.addExcludedCallsigns("K1|W1|VE");
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        
        // Mixed separators
        GeneralVariables.addExcludedCallsigns("K1, W1|VE");
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
    }

    @Test
    public void testAddExcludedCallsigns_ClearsPrevious() {
        GeneralVariables.addExcludedCallsigns("K1, W1");
        assertTrue(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        assertTrue(GeneralVariables.checkIsExcludeCallsign("W1XYZ"));
        
        // Add new list - should clear previous
        GeneralVariables.addExcludedCallsigns("VE");
        assertFalse(GeneralVariables.checkIsExcludeCallsign("K1ABC"));
        assertFalse(GeneralVariables.checkIsExcludeCallsign("W1XYZ"));
        assertTrue(GeneralVariables.checkIsExcludeCallsign("VE3ABC"));
    }

    @Test
    public void testGetExcludeCallsigns() {
        GeneralVariables.addExcludedCallsigns("K1, W1, VE");
        String excluded = GeneralVariables.getExcludeCallsigns();
        
        // Should contain all excluded callsigns
        assertTrue(excluded.contains("K1"));
        assertTrue(excluded.contains("W1"));
        assertTrue(excluded.contains("VE"));
    }

    @Test
    public void testGetExcludeCallsigns_Empty() {
        GeneralVariables.addExcludedCallsigns("");
        String excluded = GeneralVariables.getExcludeCallsigns();
        
        assertEquals("", excluded);
    }
}
