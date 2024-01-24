package com.bg7yoz.ft8cn.wave;

/**
 * 用于重采样的库。
 * @author bg7yoz
 * @date 2023-09-09
 */
public class FT8Resample {

    static {
        System.loadLibrary("ft8cn");
    }

    public static native short[] get16Resample16(short[] inputData, int inputRate
            , int outputRate,int channels);

    public static native float[] get32Resample16(short[] inputData, int inputRate
            , int outputRate,int channels);
    public static native short[] get16Resample32(float[] inputData, int inputRate
            , int outputRate,int channels);
    public static native float[] get32Resample32(float[] inputData, int inputRate
            , int outputRate,int channels);

    public static native byte[] get8Resample16(short[] inputData, int inputRate
            , int outputRate,int channels);

    public static native byte[] get8Resample32(float[] inputData, int inputRate
            , int outputRate,int channels);

}
