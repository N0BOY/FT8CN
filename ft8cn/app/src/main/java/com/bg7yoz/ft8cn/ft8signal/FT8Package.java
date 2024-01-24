package com.bg7yoz.ft8cn.ft8signal;
/**
 * 按照FT8协议打包符号。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;

public class FT8Package {
    private static final String TAG = "FT8Package";
    public static final int NTOKENS = 2063592;
    public static final int MAX22 = 4194304;
    public static final int MAXGRID4 = 32400;


    private static final String A1 = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String A2 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String A3 = "0123456789";
    private static final String A4 = " ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String A5 = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ/";

    static {
        System.loadLibrary("ft8cn");
    }


    /**
     * 生成i3=4的非标准消息的77位数据包。
     * @param message 消息
     * @return 数据包
     */
    public static byte[] generatePack77_i4(Ft8Message message) {

        String toCall = message.callsignTo.replace("<", "").replace(">", "");
        String fromCall = message.callsignFrom.replace("<", "").replace(">", "");
        int hash12;
        if (message.checkIsCQ()) {//如果是CQ，就把自己的呼号的哈希加上
            hash12 = getHash12(fromCall);
        } else {
            hash12 = getHash12(toCall);
        }
        if (fromCall.length() > 11) {//非标准呼号的长度不等长于11位
            fromCall = fromCall.substring(0, 11);
        }

        byte[] data = new byte[10];
        long n58 = 0;
        for (int i = 0; i < fromCall.length(); i++) {
            n58 = n58 * 38 + A5.indexOf(fromCall.charAt(i));
        }
        //n58=3479529522318088L;

        data[0] = (byte) ((hash12 & 0x00000fff) >> 4);
        data[1] = (byte) ((hash12 & 0x0000000f) << 4);
        data[1] = (byte) (data[1] | ((n58 & 0x0fff_ffff_ffff_ffffL) >> 54));
        data[2] = (byte) (((n58 & 0x00ff_ffff_ffff_ffffL) >> 54 - 8));
        data[3] = (byte) (((n58 & 0x0000_ffff_ffff_ffffL) >> 54 - 8 - 8));
        data[4] = (byte) (((n58 & 0x0000_00ff_ffff_ffffL) >> 54 - 8 - 8 - 8));
        data[5] = (byte) (((n58 & 0x0000_0000_ffff_ffffL) >> 54 - 8 - 8 - 8 - 8));
        data[6] = (byte) (((n58 & 0x0000_0000_00ff_ffffL) >> 54 - 8 - 8 - 8 - 8 - 8));
        data[7] = (byte) (((n58 & 0x0000_0000_0000_ffffL) >> 54 - 48));
        data[8] = (byte) (((n58 & 0x0000_0000_0000_00ffL) << 2));
        //RRR=1,RR73=2,73=3,""=0
        if (message.checkIsCQ()) {
            data[9] = (byte) 0x60;
        } else {
            data[9] = (byte) 0x20;
            switch (message.extraInfo) {
                case "RRR": //r2=1
                    data[8] = (byte) (data[8] & 0xfe);
                    data[9] = (byte) (data[9] | 0x80);
                    break;
                case "RR73": //r2=2
                    data[8] = (byte) (data[8] | 0x01);
                    //data[9] = (byte) (data[9] | 0x00);//data[9]无需改变
                    break;
                case "73": //r2=3
                    data[8] = (byte) (data[8] | 0x01);
                    data[9] = (byte) (data[9] | 0x80);
                    break;
            }
        }

        return data;
    }

    /**
     * 从复合呼号中提取标准呼号，复合呼号是指带“/”的呼号。
     * 此应用场景：双方都是复合呼号，那么发送方（我方）要改为标准呼号。
     * 从复合呼号提取标准呼号的逻辑是：拆解“/”分隔的部分，取符合FT8标准呼号的正则表达式，如果没有则取最长字符串部分。
     * @param compoundCallsign 复合呼号
     * @return 标准呼号
     */
    public static String getStdCall(String compoundCallsign) {
        if (!compoundCallsign.contains("/")) return compoundCallsign;
        String[] callsigns = compoundCallsign.split("/");
        for (String callsign : callsigns) {//用正则表达式提取标准呼号
            //FT8的认定：标准业余呼号由一个或两个字符的前缀组成，其中至少一个必须是字母，后跟一个十进制数字和最多三个字母的后缀。
            if (callsign.matches("[A-Z0-9]?[A-Z0-9][0-9][A-Z][A-Z0-9]?[A-Z]?")) {
                return callsign;
            }
        }
        //当无法提取标准呼号时，取最长的字段
        int len = 0;
        int index = 0;
        for (int i = 0; i < callsigns.length; i++) {
            if (callsigns[i].length() > len) {
                len = callsigns[i].length();
                index = i;
            }
        }
        return callsigns[index];
    }

    /**
     * i1=1,i1=2，在FT8协议的定义当中，分别是标准消息，和欧盟甚高频（EU VHF），这两个消息的唯一区别是：
     * i1=1，消息可以带/R，i1=2，消息是可以带/P
     * 所以，这两个消息可以合并为一个类型。
     *
     * @param message 原始消息
     * @return packet77
     */
    public static byte[] generatePack77_i1(Ft8Message message) {
        String toCall = message.callsignTo.replace("<", "").replace(">", "");
        String fromCall = message.callsignFrom.replace("<", "").replace(">", "");

        if (message.checkIsCQ() && message.modifier != null) {//把修饰符加上
            if (message.modifier.length() > 0) {
                toCall = toCall + " " + message.modifier;
            }
        }

        //如果以/P 或/R结尾的呼号，要把这个/P /R去掉
        if (toCall.endsWith("/P") || toCall.endsWith("/R")) {
            toCall = toCall.substring(0, toCall.length() - 2);
        }

        if (fromCall.endsWith("/P") || fromCall.endsWith("/R")) {
            fromCall = fromCall.substring(0, fromCall.length() - 2);
//            fromCall = message.callsignFrom.substring(0, message.callsignFrom.length() - 2);
        }

        //当双方都是复合呼号或非标准呼号时（带/的呼号），我的呼号变成标准呼号
        if ((toCall.contains("/")) && fromCall.contains("/")) {
            fromCall = getStdCall(fromCall);//从复合呼号中提取标准呼号
            // fromCall = fromCall.substring(0, fromCall.indexOf("/"));
        }
        byte r1_p1=pack_r1_p1(message.callsignTo);

        byte r2_p2;
        //如果双方都有后缀，但不是相同的类型后缀，则取消r1或p1标志，以发送方后缀为准
        if ((message.callsignFrom.endsWith("/R")&&message.callsignTo.endsWith("/P"))
            ||(message.callsignFrom.endsWith("/P")&&message.callsignTo.endsWith("/R"))){
            r2_p2=0;
        }else {
            r2_p2 = pack_r1_p1(message.getCallsignFrom());
        }


        byte[] data = new byte[12];
        data[0] = (byte) ((pack_c28(toCall) & 0x0fffffff) >> 20);
        data[1] = (byte) ((pack_c28(toCall) & 0x00ffffff) >> 12);
        data[2] = (byte) ((pack_c28(toCall) & 0x0000ffff) >> 4);
        data[3] = (byte) ((pack_c28(toCall) & 0x0000000f) << 4);
        //data[3] = (byte) (data[3] | (pack_r1_p1(message.callsignTo) << 3));
        data[3] = (byte) (data[3] | (r1_p1 << 3));
        data[3] = (byte) (data[3] | (pack_c28(fromCall) & 0x00fffffff) >> 25);


        data[4] = (byte) ((pack_c28(fromCall) & 0x003ffffff) >> 25 - 8);
        data[5] = (byte) ((pack_c28(fromCall) & 0x00003ffff) >> 25 - 8 - 8);
        data[6] = (byte) ((pack_c28(fromCall) & 0x0000003ff) >> 25 - 8 - 8 - 8);
        data[7] = (byte) ((pack_c28(fromCall) & 0x0000000ff) << 7);


        data[7] = (byte) (data[7] | (r2_p2) << 6);
        //data[7] = (byte) (data[7] | (pack_r1_p1(message.getCallsignFrom())) << 6);
        data[7] = (byte) (data[7] | (pack_R1_g15(message.extraInfo) & 0x0ffff) >> 10);
        data[8] = (byte) ((pack_R1_g15(message.extraInfo) & 0x0003fff) >> 2);
        data[9] = (byte) ((pack_R1_g15(message.extraInfo) & 0x00000ff) << 6);
        data[9] = (byte) (data[9] | (message.i3 & 0x3) << 3);
        return data;
    }

    /**
     * 生成R1+g15数据（网格、或信号报告），实际是16位，包括前面R1。如R-17:R1=1,-17:R1=0
     *
     * @param grid4 网格或信号报告
     * @return 返回R1+g15数据
     */
    public static int pack_R1_g15(String grid4) {
        if (grid4 == null)// 只有两个呼号，没有信号报告和网格
        {
            return MAXGRID4 + 1;
        }
        if (grid4.length() == 0) {// 只有两个呼号，没有信号报告和网格
            return MAXGRID4 + 1;
        }

        // 特殊的报告，RRR,RR73,73
        if (grid4.equals("RRR"))
            return MAXGRID4 + 2;
        if (grid4.equals("RR73"))
            return MAXGRID4 + 3;
        if (grid4.equals("73"))
            return MAXGRID4 + 4;


        // 检查是不是标准的4字符网格
        if (grid4.matches("[A-Z][A-Z][0-9][0-9]")) {
            int igrid4 = grid4.charAt(0) - 'A';
            igrid4 = igrid4 * 18 + (grid4.charAt(1) - 'A');
            igrid4 = igrid4 * 10 + (grid4.charAt(2) - '0');
            igrid4 = igrid4 * 10 + (grid4.charAt(3) - '0');
            return igrid4;
        }


        // 检查是不是信号报告: +dd / -dd / R+dd / R-dd
        // 信号报告在-30到99dB之间
        // 信号报告的正则：[R]?[+-][0-9]{1,2}
        String s = grid4;
        if (grid4.charAt(0) == 'R') {
            s = grid4.substring(1);
            int irpt = 35 + Integer.parseInt(s);
            return (MAXGRID4 + irpt) | 0x8000; // R1 = 1
        } else {
            int irpt = 35 + Integer.parseInt(grid4);
            return (MAXGRID4 + irpt); // R1 = 0
        }

    }

    public static byte pack_r1_p1(String callsign) {
        String s = callsign.substring(callsign.length() - 2);
        if (s.equals("/R") || s.equals("/P")) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 根据呼号生成c28数据。呼号为标准呼号时,不带/R或/P。如果呼号不是标准呼号，用hash22+2063592;
     *
     * @param callsign 呼号
     * @return c28数据
     */
    public static int pack_c28(String callsign) {
        //byte[] data=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        switch (callsign) {
            case "DE":
                return 0;
            case "QRZ":
                return 1;
            case "CQ":
                return 2;
        }

        //判断是否有修饰符000-999,A-Z,AA-ZZ,AAA-ZZZ,AAAA-ZZZZ
        if (callsign.startsWith("CQ ") && callsign.length() > 3) {
            String temp = callsign.substring(3).trim().toUpperCase();
            if (temp.matches("[0-9]{3}")) {
                int i = Integer.parseInt(temp);
                return i + 3;
            }
            if (temp.matches("[A-Z]{1,4}")) {

                int a0 = 0;
                int a1 = 0;
                int a2 = 0;
                int a3 = 0;
                if (temp.length() == 1) {//A-Z
                    a0 = (int) temp.charAt(0) - 65;
                    return a0 + 1004;
                }
                if (temp.length() == 2) {//AA-ZZ
                    a0 = (int) temp.charAt(0) - 65;
                    a1 = (int) temp.charAt(1) - 65;
                    return a0 * 27 + a1 + 1031;
                }
                if (temp.length() == 3) {//AAA-ZZZ
                    a0 = (int) temp.charAt(0) - 65;
                    a1 = (int) temp.charAt(1) - 65;
                    a2 = (int) temp.charAt(2) - 65;
                    return a0 * 27 * 27 + a1 * 27 + a2 + 1760;
                }
                if (temp.length() == 4) {//AAAA-ZZZZ
                    a0 = (int) temp.charAt(0) - 65;
                    a1 = (int) temp.charAt(1) - 65;
                    a2 = (int) temp.charAt(2) - 65;
                    a3 = (int) temp.charAt(3) - 65;
                    return a0 * 27 * 27 * 27 + a1 * 27 * 27 + a2 * 27 + a3 + 21443;
                }
            }
        }


        //格式化成标准的呼号。6位、第3位带数字
        //c6也可以是非标准呼号。大于6位的都是非标准呼号
        String c6 = formatCallsign(callsign);
        //判断是不是标准呼号
        if (!GenerateFT8.checkIsStandardCallsign(callsign)) {//生成HASH22+2063592
            return NTOKENS + getHash22(callsign);
        }

        //6位呼号取值
        int i0, i1, i2, i3, i4, i5;
        i0 = A1.indexOf(c6.substring(0, 1));
        i1 = A2.indexOf(c6.substring(1, 2));
        i2 = A3.indexOf(c6.substring(2, 3));
        i3 = A4.indexOf(c6.substring(3, 4));
        i4 = A4.indexOf(c6.substring(4, 5));
        i5 = A4.indexOf(c6.substring(5, 6));

        int n28 = i0;
        n28 = n28 * 36 + i1;
        n28 = n28 * 10 + i2;
        n28 = n28 * 27 + i3;
        n28 = n28 * 27 + i4;
        n28 = n28 * 27 + i5;


        return NTOKENS + MAX22 + n28;

    }


    /**
     * 格式化标准的呼号
     * 标准呼号是6位，前缀是1~2个字母+1个数字，后缀对多3个字母
     * 格式化的内容：
     * 1.斯威士兰（Swaziland）的呼号前缀问题: 3DA0XYZ -> 3D0XYZ
     * 2.几内亚（Guinea）呼号前缀问题: 3XA0XYZ -> QA0XYZ
     * 3.第2位是数字的呼号，前面用空格补充。A0XYZ->" A0XYZ"
     * 4.后缀不足3位的，也要补足空格。BA2BI->"BA2BI "
     *
     * @param callsign 呼号
     * @return 返回C28值，以int的值来表示
     */
    private static String formatCallsign(String callsign) {
        String c6 = callsign;
        // 解决斯威士兰（Swaziland）的呼号前缀问题: 3DA0XYZ -> 3D0XYZ
        if (callsign.length() > 3 && callsign.substring(0, 4).equals("3DA0") && callsign.length() <= 7) {
            c6 = "3D0" + callsign.substring(4);
            // 解决几内亚（Guinea）呼号前缀问题: 3XA0XYZ -> QA0XYZ
        } else if (callsign.length() > 3 && callsign.substring(0, 3).matches("3X[A-Z]") && callsign.length() <= 7) {
            c6 = "Q" + callsign.substring(2);
        } else {
            // 第2位是数字,第3位是字母的，要在前面补充空格：A0XYZ -> " A0XYZ",A6开头的除外
            if (callsign.substring(0, 3).matches("[A-Z][0-9][A-Z]")) {
                c6 = " " + callsign;
            }
        }

        if (c6.length() < 6) {//如果长度不足6位，结尾补充空格
            for (int i = 0; i < 6 - c6.length() + 1; i++) {
                c6 = c6 + " ";
            }
        }

        return c6;
    }

    public static native int getHash12(String callsign);


    public static native int getHash10(String callsign);

    public static native int getHash22(String callsign);
}
