package com.bg7yoz.ft8cn.ft8transmit;

/**
 * 记录QSO的类，用于保存数据库。
 * @author BGY70Z
 * @date 2023-03-20
 */
public class QSLRecord {
    private long startTime;//起始时间
    private long endTime;//结束时间

    private String myCallsign;//我的呼号
    private String myMaidenGrid;//我的网格
    private String toCallsign;//对方的呼号
    private String toMaidenGrid;//对方的网格
    private int sendReport;//对方收到我的报告（也就是我发送的信号强度）
    private int receivedReport;//我收到对方的报告（也就是SNR）
    private String mode="FT8";

    private long bandFreq;//发射的波段
    private int  frequency;//发射的频率


    public QSLRecord(long startTime, long endTime, String myCallsign, String myMaidenGrid
            , String toCallsign, String toMaidenGrid, int sendReport, int receivedReport
            , String mode, long bandFreq, int frequency) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.myCallsign = myCallsign;
        this.myMaidenGrid = myMaidenGrid;
        this.toCallsign = toCallsign;
        this.toMaidenGrid = toMaidenGrid;
        this.sendReport = sendReport;
        this.receivedReport = receivedReport;
        this.mode = mode;
        this.bandFreq = bandFreq;
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "QSLRecord{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", myCallsign='" + myCallsign + '\'' +
                ", myMaidenGrid='" + myMaidenGrid + '\'' +
                ", toCallsign='" + toCallsign + '\'' +
                ", toMaidenGrid='" + toMaidenGrid + '\'' +
                ", sendReport=" + sendReport +
                ", receivedReport=" + receivedReport +
                ", mode='" + mode + '\'' +
                ", bandFreq=" + bandFreq +
                ", frequency=" + frequency +
                '}';
    }

    public long getEndTime() {
        return endTime;
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

    public int getFrequency() {
        return frequency;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getMyCallsign() {
        return myCallsign;
    }

    public String getMyMaidenGrid() {
        return myMaidenGrid;
    }

    public int getSendReport() {
        return sendReport;
    }

    public int getReceivedReport() {
        return receivedReport;
    }
}
