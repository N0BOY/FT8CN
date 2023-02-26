package com.bg7yoz.ft8cn.rigs;

public class Yaesu2Command {
    private static final String TAG="Yaesu 2 Command";

    public static long getFrequency(byte[] rawData){
        if (rawData.length==5){
            return  ((int) (rawData[0] >> 4) & 0xf) * 100000000
                    +(int) (rawData[0] & 0x0f) * 10000000
                    +((int) (rawData[1] >> 4) & 0xf) * 1000000
                    +(int) (rawData[1] & 0x0f) * 100000
                    +((int) (rawData[2] >> 4) & 0xf) * 10000
                    +(int) (rawData[2] & 0x0f) * 1000
                    +((int) (rawData[3] >> 4) & 0xf) * 10000
                    +(int) (rawData[3] & 0x0f) * 1000;
        }else {
            return -1;
        }
    }


}
