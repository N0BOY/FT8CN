package com.bg7yoz.ft8cn.rigs;

import android.annotation.SuppressLint;

public class Flex6000RigConstant {
    private static final String TAG = "Flex6000RigConstant";
    //LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17
    public static final int LSB = 0x00;
    public static final int USB = 0x01;
    public static final int CW_L = 0x03;
    public static final int CW_U = 0x04;
    public static final int FM = 0x05;
    public static final int AM = 0x06;
    public static final int DIGI_U= 0x07;
    public static final int DIGI_L= 0x09;

    //PTT状态

    //指令集
    private static final String PTT_ON = "ZZTX1;";
    private static final String PTT_OFF = "ZZTX0;";
    private static final String USB_DIGI = "ZZMD07;";
    private static final String READ_FREQ = "ZZFA;";
    private static final String SET_VFO = "ZZFR";




    public static String getModeStr(int mode) {
        switch (mode) {
            case LSB:
                return "LSB";
            case USB:
                return "USB";
            case CW_L:
                return "CW_L";
            case CW_U:
                return "CW_U";
            case FM:
                return "FM";
            case AM:
                return "AM";
            case DIGI_U:
                return "DIGI_U";
            case DIGI_L:
                return "DIGI_L";
            default:
                return "UNKNOWN";
        }
    }


    public static byte[] setPTTState(boolean on) {
        if (on) {
            return PTT_ON.getBytes();
        } else {
            return PTT_OFF.getBytes();
        }

    }

    public static byte[] setFlexTTState(boolean on) {
        if (on) {
            return PTT_ON.getBytes();
        } else {
            return PTT_OFF.getBytes();
        }

    }



    //设置成VFO模式
    public static byte[] setVFOMode(){
        return SET_VFO.getBytes();
    }


    public static byte[] setOperationUSB_DIGI_Mode() {
        return USB_DIGI.getBytes();
    }


    @SuppressLint("DefaultLocale")
    public static byte[] setOperationFreq(long freq) {
        return String.format("ZZFA%011d\r",freq).getBytes();
    }

    public static byte[] setReadOperationFreq(){
        return READ_FREQ.getBytes();
    }


}
