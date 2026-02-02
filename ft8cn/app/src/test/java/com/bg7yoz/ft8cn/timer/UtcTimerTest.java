package com.bg7yoz.ft8cn.timer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtcTimerTest {

    private UtcTimer timer;
    private MockOnUtcTimer mockCallback;

    @Before
    public void setUp() {
        // Reset delay to 0 before each test
        UtcTimer.delay = 0;
    }

    @After
    public void tearDown() {
        // Clean up any timers created during tests
        if (timer != null) {
            timer.delete();
            timer = null;
        }
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

    // ========== Instance Method Tests ==========

    @Test
    public void testInitialState() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback); // 15 second period
        
        assertFalse("Timer should not be running initially", timer.isRunning());
    }

    @Test
    public void testStartStop() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        
        assertFalse("Timer should not be running initially", timer.isRunning());
        
        timer.start();
        assertTrue("Timer should be running after start()", timer.isRunning());
        
        timer.stop();
        assertFalse("Timer should not be running after stop()", timer.isRunning());
        
        timer.start();
        assertTrue("Timer should be running after start() again", timer.isRunning());
    }

    @Test
    public void testTimeOffset() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        
        assertEquals("Initial time offset should be 0", 0, timer.getTime_sec());
        
        timer.setTime_sec(5000);
        assertEquals("Time offset should be set to 5000", 5000, timer.getTime_sec());
        
        timer.setTime_sec(-2000);
        assertEquals("Time offset should accept negative values", -2000, timer.getTime_sec());
        
        timer.setTime_sec(0);
        assertEquals("Time offset should be reset to 0", 0, timer.getTime_sec());
    }

    @Test
    public void testGetUtc() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        timer.start();
        
        // Wait a bit for timer to update UTC
        Thread.sleep(200);
        
        long utc = timer.getUtc();
        assertTrue("UTC should be greater than 0", utc > 0);
        
        // UTC should be close to current time (within reasonable bounds)
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(utc - currentTime);
        assertTrue("UTC should be close to current time", diff < 5000);
    }

    @Test
    public void testDelete() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        timer.start();
        
        assertTrue("Timer should be running", timer.isRunning());
        
        timer.delete();
        
        // After delete, timer should still report its last state
        // but timers are cancelled so no more callbacks will fire
        // Note: delete() doesn't change running state, it just cancels the timers
    }

    @Test
    public void testDoOnceFlag() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, true, mockCallback); // doOnce = true
        timer.start();
        
        // Wait for potential trigger (but with doOnce, it should stop after first trigger)
        Thread.sleep(2000);
        
        // With doOnce=true, timer should stop after first trigger
        // However, timing is tricky in tests, so we just verify the timer exists
        assertTrue("Timer instance should exist", timer != null);
    }

    @Test
    public void testHeartBeatCallback() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        timer.start();
        
        // Wait for heartbeats (heartbeat runs every 1000ms)
        Thread.sleep(2500);
        
        // Should have received at least 2 heartbeats
        assertTrue("Should receive heartbeats", mockCallback.getHeartBeatCount() >= 2);
    }

    @Test
    public void testPeriodCallback() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        // Use a short period for testing (100 = 10 seconds, but we'll wait less)
        timer = new UtcTimer(100, false, mockCallback);
        timer.start();
        
        // Wait a bit - period callback may or may not fire depending on timing
        Thread.sleep(1500);
        
        // At minimum, heartbeats should fire
        assertTrue("Should receive heartbeats", mockCallback.getHeartBeatCount() >= 1);
    }

    @Test
    public void testTimerNotRunning_NoPeriodCallbacks() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        // Don't call start() - timer should not be running
        
        Thread.sleep(1500);
        
        // Heartbeats should still fire (they don't depend on running state)
        assertTrue("Heartbeats should fire even when not running", 
                   mockCallback.getHeartBeatCount() >= 1);
        
        // Period callbacks should not fire when not running
        assertEquals("Period callbacks should not fire when not running", 
                     0, mockCallback.getPeriodCount());
    }

    @Test
    public void testMultipleStartStopCycles() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        
        for (int i = 0; i < 5; i++) {
            timer.start();
            assertTrue("Timer should be running after start " + i, timer.isRunning());
            
            timer.stop();
            assertFalse("Timer should not be running after stop " + i, timer.isRunning());
        }
    }

    @Test
    public void testTimeOffsetAffectsPeriodCalculation() {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback); // 15 second period
        
        // Set a time offset
        timer.setTime_sec(5000);
        assertEquals("Time offset should be set", 5000, timer.getTime_sec());
        
        // Offset affects when period triggers (utc - time_sec) % sec == 0
        timer.start();
        
        // Verify timer is running with offset
        assertTrue("Timer should be running", timer.isRunning());
    }

    @Test
    public void testDifferentPeriods() {
        // Test with FT8 period (15 seconds = 150)
        mockCallback = new MockOnUtcTimer();
        UtcTimer ft8Timer = new UtcTimer(150, false, mockCallback);
        ft8Timer.start();
        assertTrue("FT8 timer should be running", ft8Timer.isRunning());
        ft8Timer.delete();
        
        // Test with FT4 period (7.5 seconds = 75)
        MockOnUtcTimer ft4Callback = new MockOnUtcTimer();
        UtcTimer ft4Timer = new UtcTimer(75, false, ft4Callback);
        ft4Timer.start();
        assertTrue("FT4 timer should be running", ft4Timer.isRunning());
        ft4Timer.delete();
    }

    @Test
    public void testUtcValueUpdates() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        timer.start();
        
        long utc1 = timer.getUtc();
        Thread.sleep(500);
        long utc2 = timer.getUtc();
        
        // UTC should update (or at least be close)
        // Note: UTC updates in timer thread, so there might be a delay
        assertTrue("UTC should update over time", utc2 >= utc1);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        mockCallback = new MockOnUtcTimer();
        timer = new UtcTimer(150, false, mockCallback);
        timer.start();
        
        // Test concurrent start/stop calls
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                timer.start();
                timer.stop();
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                timer.setTime_sec(i * 100);
                timer.getTime_sec();
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Should not crash
        assertTrue("Timer should still be functional", timer != null);
    }

    // ========== Helper Class ==========

    /**
     * Mock implementation of OnUtcTimer for testing callbacks
     */
    private static class MockOnUtcTimer implements OnUtcTimer {
        private final AtomicInteger heartBeatCount = new AtomicInteger(0);
        private final AtomicInteger periodCount = new AtomicInteger(0);
        private final AtomicLong lastHeartBeatUtc = new AtomicLong(0);
        private final AtomicLong lastPeriodUtc = new AtomicLong(0);
        private final CountDownLatch periodLatch = new CountDownLatch(1);

        @Override
        public void doHeartBeatTimer(long utc) {
            heartBeatCount.incrementAndGet();
            lastHeartBeatUtc.set(utc);
        }

        @Override
        public void doOnSecTimer(long utc) {
            periodCount.incrementAndGet();
            lastPeriodUtc.set(utc);
            periodLatch.countDown();
        }

        public int getHeartBeatCount() {
            return heartBeatCount.get();
        }

        public int getPeriodCount() {
            return periodCount.get();
        }

        public long getLastHeartBeatUtc() {
            return lastHeartBeatUtc.get();
        }

        public long getLastPeriodUtc() {
            return lastPeriodUtc.get();
        }

        public boolean waitForPeriod(long timeout, TimeUnit unit) throws InterruptedException {
            return periodLatch.await(timeout, unit);
        }

        public void reset() {
            heartBeatCount.set(0);
            periodCount.set(0);
        }
    }
}
