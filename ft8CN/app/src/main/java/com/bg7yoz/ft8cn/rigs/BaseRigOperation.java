package com.bg7yoz.ft8cn.rigs;

import android.annotation.SuppressLint;

public class BaseRigOperation {
    @SuppressLint("DefaultLocale")
    public static String getFrequencyStr(long freq) {
        return String.format("%d.%03dMhz", freq / 1000000, (freq % 1000000) / 1000);
    }
    /**
     * 检查是不是在WSPR2的频段内
     *
     * @param freq 频率
     * @return 是否
     */
    public static boolean checkIsWSPR2(long freq) {
        //freq=电台频率+声音频率
        return (freq >= 137400 && freq <= 137600)       //2190m
                || (freq >= 475400 && freq <= 475600)   //630m
                || (freq >= 1838000 && freq <= 1838200)  //160m
                || (freq >= 3594000 && freq <= 3594200)  //80m
                || (freq >= 5288600 && freq <= 5288800)  //60m
                || (freq >= 7040000 && freq <= 7040200)  //40m
                || (freq >= 10140100 && freq <= 10140300)   //30m
                || (freq >= 14097000 && freq <= 14097200)   //20m
                || (freq >= 18106000 && freq <= 18106200)   //17m
                || (freq >= 21096000 && freq <= 21096200)   //15m
                || (freq >= 24926000 && freq <= 24926200)   //12m
                || (freq >= 28126000 && freq <= 28126200)   //10m
                || (freq >= 50294400 && freq <= 50294600)   //6m
                || (freq >= 70092400 && freq <= 70092600)   //4m
                || (freq >= 144489900 && freq <= 144490100) //2m
                || (freq >= 432301600 && freq <= 432301800) //70cm
                || (freq >= 1296501400 && freq <= 1296501600);//23cm
    }

    /**
     * 通过频率获取波长
     *
     * @param freq 频率
     * @return 波长
     */
    @SuppressLint("DefaultLocale")
    public static String getMeterFromFreq(long freq) {
        if (freq >= 135700 && freq <= 137800) {
            return "2200m";
        }  // 2200m
        else if (freq >= 472000 && freq <= 479000) {
            return "630m";
        }  // 160m
        else if (freq >= 1800000 && freq <= 2000000) {
            return "160m";
        }  // 160m
        else if (freq >= 3500000 && freq <= 4000000) {
            return "80m";
        }  //  80m
        else if (freq >= 5351500 && freq <= 5366500) {
            return "60m";
        }  //  80m
        else if (freq >= 7000000 && freq <= 7300000) {
            return "40m";
        }  //  40m
        else if (freq >= 10100000 && freq <= 10150000) {
            return "30m";
        }  //  30m
        else if (freq >= 14000000 && freq <= 14350000) {
            return "20m";
        }  //  20m
        else if (freq >= 18068000 && freq <= 18168000) {
            return "17m";
        }  //  17m
        else if (freq >= 21000000 && freq <= 21450000) {
            return "15m";
        }  //  15m
        else if (freq >= 24890000 && freq <= 24990000) {
            return "12m";
        }  //  12m
        else if (freq >= 28000000 && freq <= 29700000) {
            return "10m";
        }  //  10m
        else if (freq >= 50000000 && freq <= 54000000) {
            return "6m";
        }  //   6m
        else if (freq >= 144000000 && freq <= 148000000) {
            return "2m";
        }  //   2m
        else if (freq >= 220000000 && freq <= 225000000) {
            return "1.25m";
        }  //1.25m
        else if (freq >= 420000000 && freq <= 450000000) {
            return "70cm";
        }  //0.7m
        else if (freq >= 902000000 && freq <= 928000000) {
            return "33cm";
        }  //0.33m
        else if (freq >= 1240000000 && freq <= 1300000000) {
            return "23cm";
        }  //0.23m
        else {
            return calculationMeterFromFreq(freq);
        }//不在范围内，就计算一下
    }

    @SuppressLint("DefaultLocale")
    private static String calculationMeterFromFreq(Long freq) {
        if (freq == 0) return "";
        float meter = 300000000f / (float) freq;
        if (meter < 1) {//以厘米为单位
            return String.format("%dcm", Math.round(meter * 10) * 10);
        } else if (meter < 20) {//小于20米，以米为单位
            return String.format("%dm", Math.round(meter));
        } else {//大于20M,以10米为单位
            return String.format("%dm", Math.round(meter / 10) * 10);
        }
    }

    public static String getFrequencyAllInfo(long freq) {
        return String.format("%s (%s)", getFrequencyStr(freq), getMeterFromFreq(freq));
    }

    @SuppressLint("DefaultLocale")
    public static String getFrequencyFloat(long freq) {
        return String.format("%d.%06d", freq / 1000000, (freq % 1000000));
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("0x%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
}
