package com.bg7yoz.ft8cn.rigs;

public class GuoHeRigConstant {
    private static final String TAG = "Yaesu2RigConstant";
    //LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17
    public static final int USB = 0x00;
    public static final int LSB = 0x01;
    public static final int CW_R = 0x02;
    public static final int CW_L = 0x03;
    public static final int AM = 0x04;
    public static final int WFM = 0x05;
    public static final int NFM = 0x06;
    public static final int DIGI = 0x07;
    public static final int PKT = 0x08;

    //PTT状态

    //指令集
//    private static final byte[] PTT_ON = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08};
//    private static final byte[] PTT_OFF = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x88};
    private static final byte[] PTT_ON = {(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x04, (byte) 0x07, (byte) 0x00, (byte) 0x89, (byte) 0xCB};
    private static final byte[] PTT_OFF = {(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x04, (byte) 0x07, (byte) 0x01, (byte) 0x99, (byte) 0xEA};
    private static final byte[] USB_MODE = {(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x05, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x44};
    private static final byte[] FT8_MODE = {(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x05, (byte) 0x0A, (byte) 0x08, (byte) 0x08, (byte) 0xF7, (byte) 0xE5};
    private static final byte[] READ_FREQ = {(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x03, (byte) 0x0B, (byte) 0xF9, (byte) 0x37};
//    private static final byte[] READ_FREQ =   {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03};


    public static String getModeStr(int mode) {
        switch (mode) {
            case LSB:
                return "LSB";
            case USB:
                return "USB";
            case CW_R:
                return "CW_R";
            case CW_L:
                return "CW_L";
            case AM:
                return "AM";
            case WFM:
                return "WFM";
            case NFM:
                return "NFM";
            case DIGI:
                return "DIGI";
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
        return USB_MODE;
    }
    public static byte[] setOperationFT8Mode() {
        return FT8_MODE;
    }
    public static byte[] setOperationFreq(long freq) {

        byte[] data = new byte[]{(byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0x0b, (byte) 0x09
                , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00//频率VFOA
                , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00//频率VFOB
                , (byte) 0x3B, (byte) 0x6B};

        data[6] = (byte)  ((0x0000000000ff000000 & freq) >> 24);
        data[7] = (byte)  ((0x000000000000ff0000 & freq) >> 16);
        data[8] = (byte)  ((0x00000000000000ff00 & freq) >> 8);
        data[9] = (byte)  (0x0000000000000000ff & freq);
        data[10] = (byte) ((0x0000000000ff000000 & freq) >> 24);
        data[11] = (byte) ((0x000000000000ff0000 & freq) >> 16);
        data[12] = (byte) ((0x00000000000000ff00 & freq) >> 8);
        data[13] = (byte) (0x0000000000000000ff & freq);

        byte[] crcData=new byte[]{(byte)0x0b,(byte) 0x09
                ,(byte)  ((0x0000000000ff000000 & freq) >> 24)
                ,(byte)  ((0x000000000000ff0000 & freq) >> 16)
                ,(byte)  ((0x00000000000000ff00 & freq) >> 8)
                ,(byte)  (0x0000000000000000ff & freq)
                ,(byte) ((0x0000000000ff000000 & freq) >> 24)
                ,(byte) ((0x000000000000ff0000 & freq) >> 16)
                ,(byte) ((0x00000000000000ff00 & freq) >> 8)
                ,(byte) (0x0000000000000000ff & freq)};

        int crc=CRC16.crc16(crcData);
        data[14]=(byte) ((crc&0x00ff00)>>8);
        data[15]=(byte) (crc&0x0000ff);



        return data;

    }

    public static byte[] setReadOperationFreq() {
        return READ_FREQ;
    }



}
