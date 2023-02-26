package com.bg7yoz.ft8cn.rigs;

/**
 * crc16-ccitt-false加密工具
 *
 * @author eguid
 *
 */
public class CRC16 {

    /**
     * crc16-ccitt-false加/解密（四字节）
     *
     * @param bytes
     * @return
     */
    public static int crc16(byte[] bytes) {
        return crc16(bytes, bytes.length);
    }

    /**
     * crc16-ccitt-false加/解密（四字节）
     *
     * @param bytes -字节数组
     * @return
     */
    public static int crc16(byte[] bytes, int len) {
        int crc = 0xFFFF;
        for (int j = 0; j < len; j++) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (bytes[j] & 0xff);// byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }

    /**
     * crc16-ccitt-false加/解密（四字节）
     *
     * @param bytes
     * @return
     */
    public static int crc16(byte[] bytes, int start, int len) {
        int crc = 0xFFFF;
        for (; start < len; start++) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (bytes[start] & 0xff);// byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }

    /**
     * crc16-ccitt-false加/解密
     *
     * @param bytes
     *            -字节数组
     * @return
     */
    public static short crc16_short(byte[] bytes) {
        return crc16_short(bytes, 0, bytes.length);
    }

    /**
     * crc16-ccitt-false加/解密（计算从0位置开始的len长度）
     *
     * @param bytes
     *            -字节数组
     * @param len
     *            -长度
     * @return
     */
    public static short crc16_short(byte[] bytes, int len) {
        return (short) crc16(bytes, len);
    }

    /**
     * crc16-ccitt-false加/解密（两字节）
     *
     * @param bytes
     * @return
     */
    public static short crc16_short(byte[] bytes, int start, int len) {
        return (short) crc16(bytes, start, len);
    }
}