package com.bg7yoz.ft8cn.ft8transmit;

import android.annotation.SuppressLint;

public class TransmitCallsign {
    private static final String TAG="TransmitCallsign";
    public String callsign;
    public float frequency;
    public int sequential;
    public int snr;
    public int i3;
    public int n3;
    public String dxcc;
    public int cqZone;
    public int itu;

    public TransmitCallsign(int i3,int n3,String callsign, int sequential) {
        this.callsign = callsign;
        this.sequential = sequential;
        this.i3=i3;
        this.n3=n3;
    }

    public TransmitCallsign(int i3,int n3,String callsign, float frequency
            , int sequential, int snr) {
        this.callsign = callsign;
        this.frequency = frequency;
        this.sequential = sequential;
        this.snr = snr;
        this.i3=i3;
        this.n3=n3;

    }

    /**
     * 当目标呼号为空，或CQ，说明没有目标呼号
     * @return 是否有目标呼号
     */
    public boolean haveTargetCallsign(){
        if (callsign==null){
            return false;
        }
        return !callsign.equals("CQ");
    }

    @SuppressLint("DefaultLocale")
    public String getSnr(){
        if (snr>0){
            return String.format("+%d",snr);
        }else {
            return String.format("%d",snr);
        }
    }
}
