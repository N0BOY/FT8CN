package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 生成FT8音频信号的类。音频数据是32位的浮点数组。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ft8signal.FT8Package;
import com.bg7yoz.ft8cn.ui.ToastMessage;

public class GenerateFT8 {
    private static final String TAG = "GenerateFT8";
    private static final int FTX_LDPC_K = 91;
    public static final int FTX_LDPC_K_BYTES = (FTX_LDPC_K + 7) / 8;
    private static final int FT8_NN = 79;
    private static final float FT8_SYMBOL_PERIOD = 0.160f;
    private static final float FT8_SYMBOL_BT = 2.0f;
    private static final float FT8_SLOT_TIME = 15.0f;
    private static final int Ft8num_samples = 15 * 12000;
    private static final float M_PI = 3.14159265358979323846f;

    public static final int num_tones = FT8_NN;//符号数量：FT8是79个，FT4是105个。
    public static final float symbol_period = FT8_SYMBOL_PERIOD;//FT8_SYMBOL_PERIOD=0.160f
    private static final float symbol_bt = FT8_SYMBOL_BT;//FT8_SYMBOL_BT=2.0f
    private static final float slot_time = FT8_SLOT_TIME;//FT8_SLOT_TIME=15f
    //public static int sample_rate = 48000;//采样率
    //public static int sample_rate = 12000;//采样率


    static {
        System.loadLibrary("ft8cn");
    }


    public static int checkI3ByCallsign(String callsign) {
        String substring = callsign.substring(callsign.length() - 2);
        if (substring.equals("/P")) {
            if (callsign.length() <= 8) {
                return 2;//i3=2消息
            } else {
                return 4;//说明时非标准呼号
            }
        }
        if (substring.equals("/R")) {
            if (callsign.length() <= 8) {
                return 1;//i3=2消息
            } else {
                return 4;//说明时非标准呼号
            }
        }
        if (callsign.contains("/")) {//除了/P /R以外，其余的都是非标准呼号
            return 4;
        }
        if (callsign.length() > 6) {//呼号大于6位，也是非标准呼号
            return 4;
        }
        if (callsign.length() == 0) {//没有呼号，就是自由文本
            return 0;
        }
        return 1;
    }

    public static String byteToBinString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            string.append(String.format(",%8s", Integer.toBinaryString(data[i] & 0xff)).replace(" ", "0"));
        }
        return string.toString();
    }

    public static String byteToHexString(byte[] data) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            string.append(String.format(",%02X", data[i]));
        }
        return string.toString();
    }


    /**
     * 检查是不是标准呼号
     *
     * @param callsign 呼号
     * @return 是不是
     */
    public static boolean checkIsStandardCallsign(String callsign) {
        String temp;
        if (callsign.endsWith("/P") || callsign.endsWith("/R")){
            temp=callsign.substring(0,callsign.length()-2);
        }else {
            temp=callsign;
        }
       // Log.e(TAG, "checkIsStandardCallsign: 呼号："+temp.matches("[a-zA-Z0-9]?[a-zA-Z][0-9][a-zA-Z][a-zA-Z0-9]?[a-zA-Z]") );
        //return temp.matches("[A-Z0-9]?[A-Z][0-9][A-Z][A-Z0-9]?[A-Z]?");

        return temp.matches("[A-Z0-9]?[A-Z0-9][0-9][A-Z][A-Z0-9]?[A-Z]?");

        //FT8的认定：标准业余呼号由一个或两个字符的前缀组成，其中至少一个必须是字母，后跟一个十进制数字和最多三个字母的后缀。
    }

    /**
     * 检查是不是信号报告
     *
     * @param extraInfo 扩展消息
     * @return 是不是
     */
    private static boolean checkIsReport(String extraInfo) {
        if (extraInfo.equals("73") || extraInfo.equals("RRR")
                || extraInfo.equals("RR73")||extraInfo.equals("")) {
            return false;
        }
        return !extraInfo.trim().matches("[A-Z][A-Z][0-9][0-9]");
    }

    public static float[] generateFt8(Ft8Message msg, float frequency,int sample_rate){
        return generateFt8(msg,frequency,sample_rate,true);
    }

    /**
     * 生成FT8信号
     * @param msg 消息
     * @param frequency 频率
     * @param sample_rate 采样率
     * @param hasModifier 是否有修饰符
     * @return
     */
    public static float[] generateFt8(Ft8Message msg, float frequency,int sample_rate,boolean hasModifier) {
        if (msg.callsignFrom.length()<3){
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return null;
        }
        // 首先，将文本数据打包为二进制消息,共12个字节
        byte[] packed = new byte[FTX_LDPC_K_BYTES];
        //把"<>"去掉
        msg.callsignTo = msg.callsignTo.replace("<", "").replace(">", "");
        msg.callsignFrom = msg.callsignFrom.replace("<", "").replace(">", "");
        if (hasModifier) {
            msg.modifier = GeneralVariables.toModifier;//修饰符
        }else {
            msg.modifier="";
        }
        //msg.callsignTo="CQ AzCz";

        //判定用非标准呼号i3=4的条件：
        //1.FROMCALL为非标准呼号 ，且 符合2或3
        //2.扩展消息时 网格、RR73,RRR,73
        //3.CQ,QRZ,DE



        if (msg.i3 != 0) {//目前只支持i3=1,i3=2,i3=4,i3=0 && n3=0
            if (!checkIsStandardCallsign(msg.callsignFrom)
                    && (!checkIsReport(msg.extraInfo) || msg.checkIsCQ())) {
                msg.i3 = 4;
            } else if (msg.callsignFrom.endsWith("/P")||(msg.callsignTo.endsWith("/P"))) {
                msg.i3 = 2;
            } else {
                msg.i3 = 1;
            }
        }

        if (msg.i3 == 1 || msg.i3 == 2) {
            packed = FT8Package.generatePack77_i1(msg);
        } else if (msg.i3 == 4) {//说明是非标准呼号
            packed = FT8Package.generatePack77_i4(msg);
        } else {
            packFreeTextTo77(msg.getMessageText(), packed);
        }

        return generateFt8ByA91(packed,frequency,sample_rate);

        /*
        // 其次，将二进制消息编码为FSK音调序列,79个字节
        byte[] tones = new byte[num_tones]; // 79音调（符号）数组
        //此处是88个字节（91+7）/8，可以使用a91生成音频
        ft8_encode(packed, tones);

        // 第三，将FSK音调转换为音频信号b


        int num_samples = (int) (0.5f + num_tones * symbol_period * sample_rate); // 数据信号中的采样数0.5+79*0.16*12000

        //float[] signal = new float[Ft8num_samples];
        float[] signal = new float[num_samples];

        //Ft8num_sampleFT8声音的总采样数，不是字节数。15*12000
        //for (int i = 0; i < Ft8num_samples; i++)//把数据全部静音。
        for (int i = 0; i < num_samples; i++)//把数据全部静音。
        {
            signal[i] = 0;
        }

        // 用79个字节符号，生成FT8音频
        synth_gfsk(tones, num_tones, frequency, symbol_bt, symbol_period, sample_rate, signal, 0);
        for (int i = 0; i < num_samples; i++)//把数据全部静音。
        {
           if (signal[i]>1.0||signal[i]<-1.0){
               Log.e(TAG, "generateFt8: "+signal[i] );
           }
        }
        return signal;
        */
    }

    public static float[] generateFt8ByA91(byte[] a91, float frequency,int sample_rate){
        byte[] tones = new byte[num_tones]; // 79音调（符号）数组
        //此处是12个字节（91+7）/8，可以使用a91生成音频
        ft8_encode(a91, tones);

        // 第三，将FSK音调转换为音频信号b


        int num_samples = (int) (0.5f + num_tones * symbol_period * sample_rate); // 数据信号中的采样数0.5+79*0.16*12000



        //int num_silence = (int) ((slot_time * sample_rate - num_samples) / 2); // 两端填充静音到15秒（15*12000-num_samples）/2（1.18秒的样本数）
        //int num_total_samples = num_silence + num_samples + num_silence;         // 填充信号中的样本数2.36秒+12.64秒=15秒的样本数

        //float[] signal = new float[Ft8num_samples];
        float[] signal = new float[num_samples];

        //Ft8num_sampleFT8声音的总采样数，不是字节数。15*12000
        //for (int i = 0; i < Ft8num_samples; i++)//把数据全部静音。
        for (int i = 0; i < num_samples; i++)//把数据全部静音。
        {
            signal[i] = 0;
        }

        // 用79个字节符号，生成FT8音频
        synth_gfsk(tones, num_tones, frequency, symbol_bt, symbol_period, sample_rate, signal, 0);
//        for (int i = 0; i < num_samples; i++)//把数据全部静音。
//        {
//            if (signal[i]>1.0||signal[i]<-1.0){
//                Log.e(TAG, "generateFt8: "+signal[i] );
//            }
//        }
        return signal;
    }


    private static native int packFreeTextTo77(String msg, byte[] c77);

    private static native int pack77(String msg, byte[] c77);

    private static native void ft8_encode(byte[] payload, byte[] tones);

    private static native void synth_gfsk(byte[] symbols, int n_sym, float f0,
                                          float symbol_bt, float symbol_period,
                                          int signal_rate, float[] signal, int offset);
}
