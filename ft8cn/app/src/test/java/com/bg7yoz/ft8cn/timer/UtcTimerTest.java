package com.bg7yoz.ft8cn.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtcTimerTest {

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
}
