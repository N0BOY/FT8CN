package com.bg7yoz.ft8cn.serialport.util;

public final class MonotonicClock {

    private static final long NS_PER_MS = 1_000_000;

    private MonotonicClock() {
    }

    public static long millis() {
        return System.nanoTime() / NS_PER_MS;
    }

}
