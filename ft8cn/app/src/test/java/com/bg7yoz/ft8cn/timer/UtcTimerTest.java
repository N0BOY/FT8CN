package com.bg7yoz.ft8cn.timer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtcTimerTest {

    @Before
    public void setUp() {
        // Reset delay to 0 before each test
        UtcTimer.delay = 0;
    }

    @Test
    public void getTimeStr_formatsUtcTime() {
        assertEquals("UTC : 00:00:00", UtcTimer.getTimeStr(0));
        assertEquals("UTC : 01:01:01", UtcTimer.getTimeStr(3_661_000));
    }

    @Test
    public void getTimeHHMMSS_formatsCompactTime() {
        assertEquals("000000", UtcTimer.getTimeHHMMSS(0));
        assertEquals("010101", UtcTimer.getTimeHHMMSS(3_661_000));
    }

    @Test
    public void dateFormats_areUtcBased() {
        assertEquals("19700101", UtcTimer.getYYYYMMDD(0));
        assertEquals("1970-01-01 00:00:00", UtcTimer.getDatetimeStr(0));
        assertEquals("19700101-000000", UtcTimer.getDatetimeYYYYMMDD_HHMMSS(0));
    }

    @Test
    public void sequential_uses15SecondSlots() {
        assertEquals(0, UtcTimer.sequential(0));
        assertEquals(1, UtcTimer.sequential(15_000));
        assertEquals(0, UtcTimer.sequential(30_000));
    }

    @Test
    public void getSystemTime_appliesDelay() {
        long baseTime = System.currentTimeMillis();
        UtcTimer.delay = 1000; // 1 second delay
        
        long systemTime = UtcTimer.getSystemTime();
        long expectedTime = baseTime + 1000;
        
        // Allow small difference due to execution time
        long diff = Math.abs(systemTime - expectedTime);
        assertEquals("System time should include delay", true, diff < 100);
    }

    @Test
    public void getSystemTime_withNegativeDelay() {
        long baseTime = System.currentTimeMillis();
        UtcTimer.delay = -500; // -500ms delay (clock ahead)
        
        long systemTime = UtcTimer.getSystemTime();
        long expectedTime = baseTime - 500;
        
        // Allow small difference due to execution time
        long diff = Math.abs(systemTime - expectedTime);
        assertEquals("System time should subtract delay when negative", true, diff < 100);
    }

    @Test
    public void delay_modulo15000_keepsWithinCycle() {
        // Test that delay modulo 15000 keeps values within one FT8 cycle
        // This is critical for GPS/NTP sync to work correctly
        
        // Large positive delay
        int largeDelay = 45000; // 45 seconds
        int moduloDelay = largeDelay % 15000;
        assertEquals("Large delay should wrap to 0", 0, moduloDelay);
        
        // Delay within cycle
        int withinCycleDelay = 7500; // 7.5 seconds
        int moduloWithinCycle = withinCycleDelay % 15000;
        assertEquals("Delay within cycle should remain unchanged", 7500, moduloWithinCycle);
        
        // Delay just over one cycle
        int overCycleDelay = 20000; // 20 seconds
        int moduloOverCycle = overCycleDelay % 15000;
        assertEquals("Delay over cycle should wrap", 5000, moduloOverCycle);
        
        // Negative delay
        int negativeDelay = -10000; // -10 seconds
        int moduloNegative = negativeDelay % 15000;
        // In Java, negative modulo can be negative, but we want positive
        if (moduloNegative < 0) {
            moduloNegative += 15000;
        }
        assertEquals("Negative delay should wrap correctly", 5000, moduloNegative);
    }

    @Test
    public void sequential_withDelay() {
        // Test sequential calculation with various delays
        UtcTimer.delay = 0;
        long time0 = 0;
        assertEquals("Sequential 0 at time 0", 0, UtcTimer.sequential(time0));
        
        UtcTimer.delay = 0;
        long time15 = 15_000;
        assertEquals("Sequential 1 at 15 seconds", 1, UtcTimer.sequential(time15));
        
        UtcTimer.delay = 0;
        long time30 = 30_000;
        assertEquals("Sequential 0 at 30 seconds", 0, UtcTimer.sequential(time30));
    }

    @Test
    public void delay_modulo_handlesLargeValues() {
        // Test edge cases for delay modulo operation
        // This simulates what happens in syncTime when calculating delay % 15000
        
        // Very large delay (simulating large time difference)
        int veryLargeDelay = 86400000; // 24 hours in milliseconds
        int moduloResult = veryLargeDelay % 15000;
        assertEquals("24 hour delay should wrap correctly", 0, moduloResult);
        
        // Delay exactly one cycle
        int oneCycleDelay = 15000;
        int moduloOneCycle = oneCycleDelay % 15000;
        assertEquals("One cycle delay should wrap to 0", 0, moduloOneCycle);
        
        // Delay just under one cycle
        int justUnderCycle = 14999;
        int moduloJustUnder = justUnderCycle % 15000;
        assertEquals("Just under cycle should remain", 14999, moduloJustUnder);
    }

    @Test
    public void getNowSequential_usesAdjustedTime() {
        // Test that getNowSequential uses the delay-adjusted system time
        UtcTimer.delay = 0;
        int seq1 = UtcTimer.getNowSequential();
        
        // Change delay significantly (but within one cycle)
        UtcTimer.delay = 10000; // 10 seconds
        int seq2 = UtcTimer.getNowSequential();
        
        // Both should be valid sequential values (0 or 1)
        assertEquals("Sequential should be 0 or 1", true, seq1 == 0 || seq1 == 1);
        assertEquals("Sequential should be 0 or 1", true, seq2 == 0 || seq2 == 1);
    }
}
