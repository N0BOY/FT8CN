package com.bg7yoz.ft8cn;
/**
 * Ft8Message类是用于展现FT8信号的解析结果。
 * 包括UTC时间、信噪比、时间偏移、频率、得分、消息的文本、消息的哈希值
 * ----2022.5.6-----
 * time_sec可能是时间偏移，目前还不能完全确定，待后续解决。
 * 1.为方便在列表中显示，各要素通过Get方法，返回String类型的结果。
 * -----2022.5.13---
 * 2.增加i3,n3消息类型内容
 * @author BG7YOZ
 * @date 2022.5.6
 */

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.ft8signal.FT8Package;
import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;
import com.bg7yoz.ft8cn.ft8transmit.TransmitCallsign;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Ft8Message {
    private static String TAG = "Ft8Message";
    public int i3 = 0;
    public int n3 = 0;
    public int signalFormat = FT8Common.FT8_MODE;//是不是FT8格式的消息
    public long utcTime;//UTC时间
    public boolean isValid;//是否是有效信息
    public int snr = 0;//信噪比
    public float time_sec = 0;//时间偏移(秒)
    public float freq_hz = 0;//频率
    public int score = 0;//得分
    public int messageHash;//消息的哈希

    public String callsignFrom = null;//发起呼叫的呼号
    public String callsignTo = null;//接收呼叫的呼号

    public String modifier = null;//目标呼号的修饰符 如CQ POTA BG7YOZ OL50中的POTA

    public String extraInfo = null;
    public String maidenGrid = null;
    public int report = -100;//当-100时，意味着没有信号报告
    public long callFromHash10 = 0;//12位长度的哈希码
    public long callFromHash12 = 0;//12位长度的哈希码
    public long callFromHash22 = 0;//12位长度的哈希码
    public long callToHash10 = 0;//12位长度的哈希码
    public long callToHash12 = 0;//12位长度的哈希码
    public long callToHash22 = 0;//12位长度的哈希码
    //private boolean isCallMe = false;//是不是CALL我的消息
    public long band;//载波频率

    public String fromWhere = null;//用于显示地址
    public String toWhere = null;//用于显示地址

    public boolean isQSL_Callsign = false;//是不是通联过的呼号

    public static MessageHashMap hashList = new MessageHashMap();


    public boolean fromDxcc = false;
    public boolean fromItu = false;
    public boolean fromCq = false;
    public boolean toDxcc = false;
    public boolean toItu = false;
    public boolean toCq = false;

    public LatLng fromLatLng = null;
    public LatLng toLatLng = null;

    public boolean isWeakSignal=false;




    @NonNull
    @SuppressLint({"SimpleDateFormat", "DefaultLocale"})
    @Override
    public String toString() {
        return String.format("%s %d %+4.2f %4.0f  ~  %s Hash : %#06X",
                new SimpleDateFormat("HHmmss").format(utcTime),
                snr, time_sec, freq_hz, getMessageText(), messageHash);
    }

    /**
     * 创建一个解码消息对象，要确定信号的格式。
     *
     * @param signalFormat
     */
    public Ft8Message(int signalFormat) {
        this.signalFormat = signalFormat;
    }

    public Ft8Message(String callTo, String callFrom, String extraInfo) {
        //如果是自由文本，callTo=CQ,callFrom=MyCall,extraInfo=freeText
        this.callsignTo = callTo.toUpperCase();
        this.callsignFrom = callFrom.toUpperCase();
        this.extraInfo = extraInfo.toUpperCase();
    }

    public Ft8Message(int i3, int n3, String callTo, String callFrom, String extraInfo) {
        this.callsignTo = callTo;
        this.callsignFrom = callFrom;
        this.extraInfo = extraInfo;
        this.i3 = i3;
        this.n3 = n3;
        this.utcTime = UtcTimer.getSystemTime();//用于显示TX
    }

    /**
     * 创建一个解码消息对象
     *
     * @param message 如果message不为null，则创建一个与message内容一样的解码消息对象
     */
    public Ft8Message(Ft8Message message) {
        if (message != null) {

            signalFormat = message.signalFormat;
            utcTime = message.utcTime;
            isValid = message.isValid;
            snr = message.snr;
            time_sec = message.time_sec;
            freq_hz = message.freq_hz;
            score = message.score;
            band = message.band;

            messageHash = message.messageHash;

            if (message.callsignFrom.equals("<...>")) {//到哈希列表中查一下
                callsignFrom = hashList.getCallsign(new long[]{message.callFromHash10, message.callFromHash12, message.callFromHash22});
            } else {
                callsignFrom = message.callsignFrom;
            }

            if (message.callsignTo.equals("<...>")) {//到哈希列表中查一下
                callsignTo = hashList.getCallsign(new long[]{message.callToHash10, message.callToHash12, message.callToHash22});
            } else {
                callsignTo = message.callsignTo;
            }
            if (message.i3 == 4) {
                hashList.addHash(FT8Package.getHash22(message.callsignFrom), message.callsignFrom);
                hashList.addHash(FT8Package.getHash12(message.callsignFrom), message.callsignFrom);
                hashList.addHash(FT8Package.getHash10(message.callsignFrom), message.callsignFrom);
            }

            extraInfo = message.extraInfo;
            maidenGrid = message.maidenGrid;
            report = message.report;
            callToHash10 = message.callToHash10;
            callToHash12 = message.callToHash12;
            callToHash22 = message.callToHash22;
            callFromHash10 = message.callFromHash10;
            callFromHash12 = message.callFromHash12;
            callFromHash22 = message.callFromHash22;


            i3 = message.i3;
            n3 = message.n3;

            //把哈希和呼号对应关系保存到列表里
            hashList.addHash(callToHash10, callsignTo);
            hashList.addHash(callToHash12, callsignTo);
            hashList.addHash(callToHash22, callsignTo);
            hashList.addHash(callFromHash10, callsignFrom);
            hashList.addHash(callFromHash12, callsignFrom);
            hashList.addHash(callFromHash22, callsignFrom);


            //Log.d(TAG, String.format("i3:%d,n3:%d,From:%s,To:%s", i3, n3, getCallsignFrom(), getCallsignTo()));
        }
    }

    /**
     * 返回解码消息的所使用的频率
     *
     * @return String 为方便显示，返回值是字符串
     */
    @SuppressLint("DefaultLocale")
    public String getFreq_hz() {
        return String.format("%04.0f", freq_hz);
    }

    public String getMessageText(boolean showWeekSignal){
        if (isWeakSignal && showWeekSignal){
            return "*"+getMessageText();
        }else {
            return getMessageText();
        }
    }

    /**
     * 返回解码消息的文本内容
     *
     * @return String
     */
    public String getMessageText() {

        if (i3 == 0 && n3 == 0) {//说明是自由文本
            if (extraInfo.length() < 13) {
                return String.format("%-13s", extraInfo.toUpperCase());
            } else {
                return extraInfo.toUpperCase().substring(0, 13);
            }
        }
        if (modifier != null && checkIsCQ()) {//修饰符
            if (modifier.matches("[0-9]{3}|[A-Z]{1,4}")) {
                return String.format("%s %s %s %s", callsignTo, modifier, callsignFrom, extraInfo).trim();
            }
        }
        return String.format("%s %s %s", callsignTo, callsignFrom, extraInfo).trim();
    }

    /**
     * 返回解码消息带信噪比的内容
     *
     * @return 内容
     */
    @SuppressLint("DefaultLocale")
    public String getMessageTextWithDb() {
        return String.format("%d %s %s %s", snr, callsignTo, callsignFrom, extraInfo).trim();
    }

    /**
     * 返回消息的延迟时间。可能不一定对，待研究清楚解码算法后在确定
     *
     * @return String 为方便显示，返回值是字符串。
     */
    @SuppressLint("DefaultLocale")
    public String getDt() {
        return String.format("%.1f", time_sec);
    }

    /**
     * 返回解码消息的信噪比dB值，该计算方法还为搞定，暂时用000代替
     *
     * @return String 为方便显示，返回值是字符串
     */
    public String getdB() {
        return String.valueOf(snr);
    }

    /**
     * 检查消息处于奇数还是偶数序列。
     *
     * @return boolean 处于偶数序列true，第0,30秒为true
     */
    public boolean isEvenSequence() {
        if (signalFormat == FT8Common.FT8_MODE) {
            return (utcTime / 1000) % 15 == 0;
        } else {
            return (utcTime / 100) % 75 == 0;
        }
    }

    /**
     * 显示当前消息处于哪一个时间序列的。
     *
     * @return String 以时间周期取模为结果。
     */
    @SuppressLint("DefaultLocale")
    public int getSequence() {
        if (signalFormat == FT8Common.FT8_MODE) {
            return (int) ((((utcTime + 750) / 1000) / 15) % 2);
        } else {
            return (int) (((utcTime + 370) / 100) / 75) % 2;
        }
    }

    @SuppressLint("DefaultLocale")
    public int getSequence4() {
        if (signalFormat == FT8Common.FT8_MODE) {
            return (int) ((((utcTime + 750) / 1000) / 15) % 4);
        } else {
            return (int) (((utcTime + 370) / 100) / 75) % 4;
        }
    }

    /**
     * 消息中含有mycall呼号的
     *
     * @return boolean
     */
    public boolean inMyCall() {
        if (GeneralVariables.myCallsign.length() == 0) return false;
        return this.callsignFrom.contains(GeneralVariables.myCallsign)
                || this.callsignTo.contains(GeneralVariables.myCallsign);
        //return (this.callsignFrom.contains(mycall) || this.callsignTo.contains(mycall)) && (!mycall.equals(""));
    }
/*
i3.n3类型	基本目的	消息范例	位字段标签
0.0	自由文本（Free Text）	TNX BOB 73 GL	f71
0.1	远征（DXpedition）	K1ABC RR73; W9XYZ <KH1/KH7Z> -08	c28 c28 h10 r5
0.3	野外日（Field Day）	K1ABC W9XYZ 6A WI	c28 c28 R1 n4 k3 S7
0.4	野外日（Field Day）	W9XYZ K1ABC R 17B EMA	c28 c28 R1 n4 k3 S7
0.5	遥测（Telemetry）	123456789ABCDEF012	t71
1.	标准消息（Std Msg）	K1ABC/R W9XYZ/R R EN37	c28 r1 c28 r1 R1 g15
2.	欧盟甚高频（EU VHF）	G4ABC/P PA9XYZ JO22	c28 p1 c28 p1 R1 g15
3.	电传（RTTY RU）	K1ABC W9XYZ 579 WI	t1 c28 c28 R1 r3 s13
4.	非标准呼叫（NonStd Call）	<W9XYZ> PJ4/K1ABC RRR	h12 c58 h1 r2 c1
5.	欧盟甚高频（EU VHF）	<G4ABC> <PA9XYZ> R 570007 JO22DB	h12 h22 R1 r3 s11 g25
*/
/*
标签	传达的信息
c1	第一个呼号是CQ；h12被忽略
c28	标准呼号、CQ、DE、QRZ或22位哈希
c58	非标准呼号，最多11个字符
f71	自由文本，最多13个字符
g15	4字符网格、报告、RRR、RR73、73或空白
g25	6字符网格
h1	哈希呼号是第二个呼号
h10	哈希呼号，10位
h12	哈希呼号，12位
h22	哈希呼号，22位
k3	野外日级别（Class）：A、B、…F
n4	发射器数量：1-16、17-32
p1	呼号后缀 /P
r1	呼号后缀/R
r2	RRR、RR73、73、或空白
r3	报告：2-9，显示为529-599或52-59
R1	R
r5	报告：-30到+30，仅偶数
s11	序列号（0-2047）
s13	序列号（0-7999）或州/省
S7	ARRL/RAC部分
t1	TU;
t71	遥感数据，最多18位十六进制数字

*/


    /**
     * 获取发送者的呼号，fromTo的最终解决办法要在decode.c中解决---TO DO----
     * 可获取发送者呼号的消息类型为i1，i2,i3,i4,i5,i0.1,i0.3,i0.4
     *
     * @return String 返回呼号
     */
    public String getCallsignFrom() {
        if (callsignFrom == null) {
            return "";
        }
        return callsignFrom.replace("<", "").replace(">", "");
    }

    /**
     * 获取通联信息中的接收呼号
     *
     * @return
     */
    public String getCallsignTo() {
        if (callsignTo == null) {
            return "";
        }
        if (callsignTo.length() < 2) {
            return "";
        }
        if (callsignTo.substring(0, 2).equals("CQ") || callsignTo.substring(0, 2).equals("DE")
                || callsignTo.substring(0, 3).equals("QRZ")) {
            return "";
        }
        return callsignTo.replace("<", "").replace(">", "");
    }

    /**
     * 从消息中获取梅登海德网格信息
     *
     * @return String，梅登海德网格，如果没有返回""。
     */
    public String getMaidenheadGrid(DatabaseOpr db) {
        if (i3 != 1 && i3 != 2) {//一般只有i3=1或i3=2，标准消息，甚高频消息才有网格
            return GeneralVariables.getGridByCallsign(callsignFrom, db);//到对应表中找一下网格
        } else {
            String[] msg = getMessageText().split(" ");
            if (msg.length < 1) {
                return GeneralVariables.getGridByCallsign(callsignFrom, db);//到对应表中找一下网格
            }
            String s = msg[msg.length - 1];
            if (MaidenheadGrid.checkMaidenhead(s)) {
                return s;
            } else {//不是网格信息，就可能是信号报告
                return GeneralVariables.getGridByCallsign(callsignFrom, db);//到对应表中找一下网格
            }
        }
    }

    public String getToMaidenheadGrid(DatabaseOpr db) {
        if (checkIsCQ()) return "";
        return GeneralVariables.getGridByCallsign(callsignTo, db);
    }

    /**
     * 查看消息是不是CQ
     *
     * @return boolean 是CQ返回true
     */
    public boolean checkIsCQ() {
        String s = callsignTo.trim().split(" ")[0];
        if (s == null) {
            return false;
        } else {
            return (s.equals("CQ") || s.equals("DE") || s.equals("QRZ"));
        }
    }

    /**
     * 查消息的类型。i3.n3。
     *
     * @return 消息类型
     */

    public String getCommandInfo() {
        return getCommandInfoByI3N3(i3, n3);
    }

    /**
     * 查消息的类型。i3.n3。
     *
     * @param i i3
     * @param n n3
     * @return 消息类型
     */
    @SuppressLint("DefaultLocale")
    public static String getCommandInfoByI3N3(int i, int n) {
        String format = "%d.%d:%s";
        switch (i) {
            case 1:
            case 2:
                return String.format(format, i, 0, GeneralVariables.getStringFromResource(R.string.std_msg));
            case 5:
            case 3:
            case 4:
                return String.format(format, i, 0, GeneralVariables.getStringFromResource(R.string.none_std_msg));
            case 0:
                switch (n) {
                    case 0:
                        return String.format(format, i, n, GeneralVariables.getStringFromResource(R.string.free_text));
                    case 1:
                        return String.format(format, i, n, GeneralVariables.getStringFromResource(R.string.dXpedition));
                    case 3:
                    case 4:
                        return String.format(format, i, n, GeneralVariables.getStringFromResource(R.string.field_day));
                    case 5:
                        return String.format(format, i, n, GeneralVariables.getStringFromResource(R.string.telemetry));
                }
        }
        return "";
    }

    //获取发送者的传输对象
    public TransmitCallsign getFromCallTransmitCallsign() {
        return new TransmitCallsign(this.i3, this.n3, this.callsignFrom, freq_hz
                , this.getSequence()
                , snr);
    }

    //获取发送者的传输对象，注意！！！与发送者的时序是相反的！！！
    public TransmitCallsign getToCallTransmitCallsign() {
        if (report == -100) {//如果消息中没有信号报告，就用发送方的SNR代替
            return new TransmitCallsign(this.i3, this.n3, this.callsignTo, freq_hz, (this.getSequence() + 1) % 2, snr);
        } else {
            return new TransmitCallsign(this.i3, this.n3, this.callsignTo, freq_hz, (this.getSequence() + 1) % 2, report);
        }
    }

    @SuppressLint("DefaultLocale")
    public String toHtml() {
        StringBuilder result = new StringBuilder();

        result.append("<td class=\"default\" >");
        result.append(UtcTimer.getDatetimeStr(utcTime));
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        result.append(getdB());
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        result.append(String.format("%.1f", time_sec));
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        result.append(String.format("%.0f", freq_hz));
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        result.append(getMessageText());
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        result.append(BaseRigOperation.getFrequencyStr(band));
        result.append("</td>\n");

        return result.toString();
    }
}
