package com.bg7yoz.ft8cn.serialport.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for MonotonicClock utility class
 */
public class MonotonicClockTest {

    @Test
    public void testMillis_returnsPositiveValue() {
        long result = MonotonicClock.millis();
        assertTrue("millis() should return a positive value", result > 0);
    }

    @Test
    public void testMillis_returnsIncreasingValues() {
        long first = MonotonicClock.millis();
        // Small delay to ensure different values
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long second = MonotonicClock.millis();
        assertTrue("Second call should return a value >= first call", second >= first);
    }

    @Test
    public void testMillis_returnsReasonableValue() {
        long result = MonotonicClock.millis();
        // Should be a reasonable millisecond value (not negative, not extremely large)
        assertTrue("millis() should return a reasonable value", result > 0 && result < Long.MAX_VALUE);
    }
}
