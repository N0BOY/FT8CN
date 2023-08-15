package com.bg7yoz.ft8cn.log;

/**
 * 通联过的呼号的日志。
 * @author BGY70Z
 * @date 2023-03-20
 */
public class QSLCallsignRecord {
    private String callsign;
    private String mode;
    private String grid;
    private String band;
    private String lastTime;
    public String where=null;
    public String dxccStr="";
    public boolean isQSL=false;//是否手工确认
    public boolean isLotW_QSL = false;//是否是lotw确认的

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        if (callsign!=null) {
            this.callsign = callsign;
        }else {
            this.callsign="";
        }
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        if (lastTime!=null) {
            this.lastTime = lastTime;
        }else {
            this.lastTime="";
        }
    }



    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if (mode!=null) {
            this.mode = mode;
        }else {
            this.mode="";
        }
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        if (grid!=null) {
            this.grid = grid;
        }else {
            this.grid="";
        }
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        if (band!=null) {
            this.band = band;
        }else {
            this.band="";
        }
    }
}
