package com.bg7yoz.ft8cn.rigs;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CRC16Test {

    @Test
    public void crc16_matchesKnownVector() {
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        int crc = CRC16.crc16(data);
        assertEquals("CRC16-CCITT-FALSE for 123456789 should match", 0x29B1, crc);
    }

    @Test
    public void crc16_overloadMatchesFullLength() {
        byte[] data = "HELLO".getBytes(StandardCharsets.US_ASCII);
        assertEquals(CRC16.crc16(data), CRC16.crc16(data, data.length));
    }

    @Test
    public void crc16_startLenUsesEndIndex() {
        byte[] data = "ABCDE".getBytes(StandardCharsets.US_ASCII);
        byte[] slice = Arrays.copyOfRange(data, 1, 4); // "BCD"
        assertEquals(CRC16.crc16(slice), CRC16.crc16(data, 1, 4));
    }

    @Test
    public void crc16ShortMatchesLower16Bits() {
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        short shortCrc = CRC16.crc16_short(data);
        assertEquals((short) CRC16.crc16(data), shortCrc);
    }
}
