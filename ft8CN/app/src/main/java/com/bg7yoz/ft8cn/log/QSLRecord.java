package com.bg7yoz.ft8cn.log;

import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.HashMap;
import java.util.Objects;

/**
 * 用于记录通联成功信息的类。通联成功是指FT8完成6条消息的通联。并不是互认。
 * isLotW_import是指是否是外部的数据导入，因为用户可能使用了JTDX等软件通联，这样可以把通联的结果导入到FT8CN
 * isLotW_QSL是指是否被平台确认。
 * isQSL是指是否被手工确认
 *
 * @author BGY70Z
 * @date 2023-03-20
 */
public class QSLRecord {
    private static final String TAG = "QSLRecord";
    public long id = -1;
    //private long startTime;//起始时间
    private String qso_date;
    private String time_on;
    private String qso_date_off;
    private String time_off;
    //private long endTime;//结束时间

    private final String myCallsign;//我的呼号
    private String myMaidenGrid;//我的网格
    private String toCallsign;//对方的呼号
    private String toMaidenGrid;//对方的网格
    private int sendReport;//对方收到我的报告（也就是我发送的信号强度）
    private int receivedReport;//我收到对方的报告（也就是SNR）
    private String mode = "FT8";
    private String bandLength = "";
    private long bandFreq;//发射的波段
    private int wavFrequency;//发射的频率
    private String comment;
    public boolean isQSL = false;//手工确认
    public boolean isLotW_import = false;//是否是从外部数据导入的，此项需要在数据库中比对才能设定
    public boolean isLotW_QSL = false;//是否是lotw确认的

    public boolean saved = false;//是否被保存到数据库中

    /**
     * 用于SWL QSO记录，记录SWL QSO的条件是收听到双方的信号报告
     *
     * @param msg FT8消息
     */
    public QSLRecord(Ft8Message msg) {
        this.qso_date_off = UtcTimer.getYYYYMMDD(msg.utcTime);
        this.time_off = UtcTimer.getTimeHHMMSS(msg.utcTime);
        this.myCallsign = msg.callsignFrom;
        this.toCallsign = msg.callsignTo;
        wavFrequency = Math.round(msg.freq_hz);
        sendReport = -100;
        receivedReport = -100;
        bandLength = BaseRigOperation.getMeterFromFreq(GeneralVariables.band);//获取波长
        bandFreq = GeneralVariables.band;
        comment = "SWL By FT8CN";
    }

    /**
     * 构建通联成功的对象
     *
     * @param startTime      起始时间
     * @param endTime        结束时间
     * @param myCallsign     我的呼号
     * @param myMaidenGrid   我的网格
     * @param toCallsign     对方呼号
     * @param toMaidenGrid   对方网格
     * @param sendReport     发送的报告
     * @param receivedReport 接收的报告
     * @param mode           模式 默认FT8
     * @param bandFreq       载波频率
     * @param wavFrequency   声音频率
     */
    public QSLRecord(long startTime, long endTime, String myCallsign, String myMaidenGrid
            , String toCallsign, String toMaidenGrid, int sendReport, int receivedReport
            , String mode, long bandFreq, int wavFrequency) {
        //this.startTime = startTime;
        this.qso_date = UtcTimer.getYYYYMMDD(startTime);
        this.time_on = UtcTimer.getTimeHHMMSS(startTime);
        this.qso_date_off = UtcTimer.getYYYYMMDD(endTime);
        this.time_off = UtcTimer.getTimeHHMMSS(endTime);
        this.myCallsign = myCallsign;
        this.myMaidenGrid = myMaidenGrid;
        this.toCallsign = toCallsign;
        this.toMaidenGrid = toMaidenGrid;
        this.sendReport = sendReport;
        this.receivedReport = receivedReport;
        this.mode = mode;
        this.bandLength = BaseRigOperation.getMeterFromFreq(bandFreq);//获取波长
        this.bandFreq = bandFreq;
        this.wavFrequency = wavFrequency;
        String distance = "";
        if (!myMaidenGrid.equals("") && !toMaidenGrid.equals("")) {
            distance = MaidenheadGrid.getDistStrEN(myMaidenGrid, toMaidenGrid);
        }
        this.comment =
                distance.equals("") ? "QSO by FT8CN"
                        : String.format("Distance: %s, QSO by FT8CN", distance);
    }

    public void update(QSLRecord record) {
        this.qso_date_off = record.qso_date_off;
        this.time_off = record.time_off;
        this.toMaidenGrid = record.toMaidenGrid;
        this.sendReport = record.sendReport;
        this.receivedReport = record.receivedReport;
    }

    public QSLRecord(HashMap<String, String> map) {
        isLotW_import = true;//说明是外部导入的数据
        if (map.containsKey("CALL")) {//对方呼号
            toCallsign = map.get("CALL");
        }
        if (map.containsKey("STATION_CALLSIGN")) {//我的呼号
            myCallsign = map.get("STATION_CALLSIGN");
        } else {
            myCallsign = "";
        }
        if (map.containsKey("BAND")) {//载波波长
            bandLength = map.get("BAND");
        } else {
            bandLength = "";
        }

        if (map.containsKey("FREQ")) {//载波频率
            try {//要把float转成Long
                float freq = Float.parseFloat(Objects.requireNonNull(map.get("FREQ")));
                bandFreq = Math.round(freq * 1000000);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "QSLRecord: freq" + e.getMessage());
            }
        }
        if (map.containsKey("MODE")) {//模式
            mode = map.get("MODE");
        } else {
            mode = "";
        }
        if (map.containsKey("QSO_DATE")) {//通联日期
            qso_date = map.get("QSO_DATE");
        } else {
            qso_date = "";
        }
        if (map.containsKey("TIME_ON")) {//通联起始时间
            time_on = map.get("TIME_ON");
        } else {
            time_on = "";
        }
        if (map.containsKey("QSO_DATE_OFF")) {//通联结束日期，此字段只在JTDX中有。
            qso_date_off = map.get("QSO_DATE_OFF");
        } else {
            qso_date_off = qso_date;
        }
        if (map.containsKey("TIME_OFF")) {//通联结束时间，n1mm、Log32、JTDX有，Lotw没有
            time_off = map.get("TIME_OFF");
        } else {
            time_off = "";
        }
        if (map.containsKey("QSL_RCVD")) {//通联互认，lotw中有。
            isLotW_QSL = Objects.requireNonNull(map.get("QSL_RCVD")).equalsIgnoreCase("Y");
        }
        if (map.containsKey("LOTW_QSL_RCVD")) {//通联互认，log32中有。
            isLotW_QSL = Objects.requireNonNull(map.get("LOTW_QSL_RCVD")).equalsIgnoreCase("Y");
        }
        if (map.containsKey("QSL_MANUAL")) {//通联互认，lotw中有。
            isQSL = Objects.requireNonNull(map.get("QSL_MANUAL")).equalsIgnoreCase("Y");
        }

        if (map.containsKey("MY_GRIDSQUARE")) {//我的网格（lotw,log32有，lotw根据设置不同，也可能没有）N1MM没有网格
            myMaidenGrid = map.get("MY_GRIDSQUARE");
        } else {
            myMaidenGrid = "";
        }

        if (map.containsKey("GRIDSQUARE")) {//对方的网格（lotw,log32有，lotw根据设置不同，也可能没有）N1MM没有网格
            toMaidenGrid = map.get("GRIDSQUARE");
        } else {
            toMaidenGrid = "";
        }


        if (map.containsKey("RST_RCVD")) {//接收到的报告。信号报告n1mm,log32,jtdx有，Lotw没有
            try {//要把float转成Long
                receivedReport = Integer.parseInt(Objects.requireNonNull(map.get("RST_RCVD").trim()));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "QSLRecord: RST_RCVD:" + e.getMessage());
            }
        } else {
            receivedReport = -120;
        }

        if (map.containsKey("RST_SENT")) {//接收到的报告。信号报告n1mm,log32,jtdx有，Lotw没有
            try {//要把float转成Long
                sendReport = Integer.parseInt(Objects.requireNonNull(map.get("RST_SENT").trim()));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "QSLRecord: RST_SENT:" + e.getMessage());
            }
        } else {
            sendReport = -120;
        }
        if (map.containsKey("COMMENT")) {//注释，JTDX中有
            comment = map.get("COMMENT");
        } else {
            comment = String.format(GeneralVariables.getStringFromResource(R.string.qsl_record_import_time)
                    , UtcTimer.getDatetimeStr(UtcTimer.getSystemTime()));
        }


    }

    /**
     * SWL QSO的提示
     *
     * @return 提示
     */
    public String swlQSOInfo() {
        return String.format("QSO of SWL:%s<--%s", toCallsign, myCallsign);
    }

    @Override
    public String toString() {
        return "QSLRecord{" +
                "id=" + id +
                ", qso_date='" + qso_date + '\'' +
                ", time_on='" + time_on + '\'' +
                ", qso_date_off='" + qso_date_off + '\'' +
                ", time_off='" + time_off + '\'' +
                ", myCallsign='" + myCallsign + '\'' +
                ", myMaidenGrid='" + myMaidenGrid + '\'' +
                ", toCallsign='" + toCallsign + '\'' +
                ", toMaidenGrid='" + toMaidenGrid + '\'' +
                ", sendReport=" + sendReport +
                ", receivedReport=" + receivedReport +
                ", mode='" + mode + '\'' +
                ", bandLength='" + bandLength + '\'' +
                ", bandFreq=" + bandFreq +
                ", wavFrequency=" + wavFrequency +
                ", isQSL=" + isQSL +
                ", isLotW_import=" + isLotW_import +
                ", isLotW_QSL=" + isLotW_QSL +
                ", saved=" + saved +
                ", comment='" + comment + '\'' +
                '}';
    }

    public String toHtmlString() {
        String ss = saved ? "<font color=red>, saved=true</font>" : ", saved=false";
        return "QSLRecord{" +
                "id=" + id +
                ", qso_date='" + qso_date + '\'' +
                ", time_on='" + time_on + '\'' +
                ", qso_date_off='" + qso_date_off + '\'' +
                ", time_off='" + time_off + '\'' +
                ", myCallsign='" + myCallsign + '\'' +
                ", myMaidenGrid='" + myMaidenGrid + '\'' +
                ", toCallsign='" + toCallsign + '\'' +
                ", toMaidenGrid='" + toMaidenGrid + '\'' +
                ", sendReport=" + sendReport +
                ", receivedReport=" + receivedReport +
                ", mode='" + mode + '\'' +
                ", bandLength='" + bandLength + '\'' +
                ", bandFreq=" + bandFreq +
                ", wavFrequency=" + wavFrequency +
                ", isQSL=" + isQSL +
                ", isLotW_import=" + isLotW_import +
                ", isLotW_QSL=" + isLotW_QSL +
                ss +
                ", comment='" + comment + '\'' +
                '}';
    }

    public String getBandLength() {
        return bandLength;
    }

    public String getToCallsign() {
        return toCallsign;
    }

    public String getToMaidenGrid() {
        return toMaidenGrid;
    }

    public String getMode() {
        return mode;
    }

    public long getBandFreq() {
        return bandFreq;
    }

    public int getWavFrequency() {
        return wavFrequency;
    }


    public String getMyCallsign() {
        return myCallsign;
    }

    public String getMyMaidenGrid() {
        return myMaidenGrid;
    }

    public void setMyMaidenGrid(String myMaidenGrid) {
        this.myMaidenGrid = myMaidenGrid;
    }

    public int getSendReport() {
        return sendReport;
    }

    public int getReceivedReport() {
        return receivedReport;
    }

    public String getQso_date() {
        return qso_date;
    }

    public String getTime_on() {
        return time_on;
    }

    public String getQso_date_off() {
        return qso_date_off;
    }

    public String getTime_off() {
        return time_off;
    }

    public String getStartTime() {
        return qso_date + "-" + time_on;
    }

    public String getEndTime() {
        return qso_date_off + "-" + time_off;
    }

    public String getComment() {
        return comment;
    }


    public void setToMaidenGrid(String toMaidenGrid) {
        this.toMaidenGrid = toMaidenGrid;
    }

    public void setSendReport(int sendReport) {
        this.sendReport = sendReport;
    }

    public void setReceivedReport(int receivedReport) {
        this.receivedReport = receivedReport;
    }

    public void setQso_date(String qso_date) {
        this.qso_date = qso_date;
    }

    public void setTime_on(String time_on) {
        this.time_on = time_on;
    }
}
