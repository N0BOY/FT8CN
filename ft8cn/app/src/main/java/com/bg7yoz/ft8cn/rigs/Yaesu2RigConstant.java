package com.bg7yoz.ft8cn.rigs;

public class Yaesu2RigConstant {
    private static final String TAG = "Yaesu2RigConstant";
    //LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17
    public static final int LSB = 0x00;
    public static final int USB = 0x01;
    public static final int CW = 0x02;
    public static final int CW_R = 0x03;
    public static final int AM = 0x04;
    public static final int FM = 0x05;
    public static final int DIG = 0x0A;
    public static final int PKT = 0x0C;
    public static final int swr_817_alert_min=6;//相当于3.0,
    public static final int alc_817_alert_max=7;//取值时0-9，合适值是7
    //PTT状态

    //指令集
    private static final byte[] PTT_ON = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08};
    private static final byte[] PTT_OFF = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x88};
    private static final byte[] GET_METER = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD};
    private static final byte[] GET_CONNECT = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte[] GET_DISCONNECT = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80};
    //USB模式
    private static final byte[] USB_MODE = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07};
    //DIG模式
    private static final byte[] DIG_MODE = {(byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07};
    private static final byte[] READ_FREQ = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03};


    public static String getModeStr(int mode) {
        switch (mode) {
            case LSB:
                return "LSB";
            case USB:
                return "USB";
            case AM:
                return "AM";
            case CW:
                return "CW";
            case FM:
                return "FM";
            case CW_R:
                return "CW_R";
            case DIG:
                return "DIG";
            case PKT:
                return "PKT";
            default:
                return "UNKNOWN";
        }
    }


    public static byte[] setPTTState(boolean on) {
        if (on) {
            return PTT_ON;
        } else {
            return PTT_OFF;
        }

    }
    public static byte[] setOperationUSBMode() {
        return DIG_MODE;
    }
    public static byte[] setOperationUSB847Mode() {
        return USB_MODE;
    }
    public static byte[] readMeter() {
        return GET_METER;
    }

    public static byte[] sendConnectData() {
        return GET_CONNECT;
    }
    public static byte[] sendDisconnectData() {
        return GET_DISCONNECT;
    }
    public static byte[] setOperationFreq(long freq) {
        byte[] data = new byte[]{
                (byte) (((byte) (freq % 1000000000 / 100000000) << 4) + (byte) (freq % 100000000 / 10000000))
                , (byte) (((byte) (freq % 10000000 / 1000000) << 4) + (byte) (freq % 1000000 / 100000))
                , (byte) (((byte) (freq % 100000 / 10000) << 4) + (byte) (freq % 10000 / 1000))
                , (byte) (((byte) (freq % 1000 / 100) << 4) + (byte) (freq % 100))
                ,(byte) 0x01
        };
        return data;
    }
    public static byte[] setReadOperationFreq(){
        return READ_FREQ;
    }


}
