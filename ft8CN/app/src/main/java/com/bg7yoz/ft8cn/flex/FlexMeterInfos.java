package com.bg7yoz.ft8cn.flex;

import java.util.HashMap;

/**
 * Meter的ID与定义映射表，启动Flex后，Meter的ID值并不是固定不变的，所以要保存一个Hash表
 * @author BGY70Z
 * @date 2023-03-20
 */
public class FlexMeterInfos extends HashMap<Integer, FlexMeterInfos.FlexMeterInfo> {
    private static final String TAG = "FlexMeters";
    public int sMeterId = -1;
    public int tempCId = -1;
    public int swrId = -1;
    public int pwrId = -1;
    public int alcId = -1;

    public FlexMeterInfos(String content) {
        setMeterInfos(content);
    }

    /**
     * 根据电台回复的消息，获取每个METER的ID与meter定义的映射表
     *
     * @param content 消息
     */
    public synchronized void setMeterInfos(String content) {
        String[] temp;
        if (content.length() == 0) return;
        temp = content.substring(content.indexOf("meter ") + "meter ".length()).split("#");
        for (int i = 0; i < temp.length; i++) {
            String[] val = temp[i].split("=");
            if (val.length == 2) {
                if (val[0].contains(".")) {
                    int index = Integer.parseInt(val[0].substring(0, val[0].indexOf(".")));
                    FlexMeterInfo meterInfo;

                    meterInfo = this.get(index);
                    if (meterInfo == null) {
                        meterInfo = new FlexMeterInfo();
                        this.put(index, meterInfo);
                    }


                    if (val[0].toLowerCase().contains(".src")) {

                        meterInfo.src = val[1];
                    }
                    if (val[0].toLowerCase().contains(".num")) {
                        meterInfo.num = val[1];
                    }
                    if (val[0].toLowerCase().contains(".nam")) {
                        meterInfo.nam = val[1];
                        //为了方便MeterList快速查询
                        if (val[1].toUpperCase().contains("LEVEL")) {
                            sMeterId = index;
                        } else if (val[1].toUpperCase().contains("PATEMP")) {
                            tempCId = index;
                        } else if (val[1].toUpperCase().contains("SWR")) {
                            swrId = index;
                        } else if (val[1].toUpperCase().contains("FWDPWR")) {
                            pwrId = index;
                        } else if (val[1].toUpperCase().contains("ALC")) {
                            alcId = index;
                        }

                    }
                    if (val[0].toLowerCase().contains(".low")) {
                        meterInfo.low = Float.parseFloat(val[1]);
                    }
                    if (val[0].toLowerCase().contains(".hi")) {
                        meterInfo.hi = Float.parseFloat(val[1]);
                    }
                    if (val[0].toLowerCase().contains(".desc")) {
                        meterInfo.desc = val[1];
                    }
                    if (val[0].toLowerCase().contains(".unit")) {
                        String s = val[1].toUpperCase();
                        if (s.contains("DB")) {
                            meterInfo.unit = FlexMeterType.dBm;
                        } else if (s.contains("SWR")) {
                            meterInfo.unit = FlexMeterType.swr;
                        } else if (s.contains("DEG")) {
                            meterInfo.unit = FlexMeterType.Temperature;
                        } else if (s.contains("VOLT")) {
                            meterInfo.unit = FlexMeterType.volt;
                        } else {
                            meterInfo.unit = FlexMeterType.other;
                        }

                    }
                    if (val[0].toLowerCase().contains(".fps")) {
                        meterInfo.fps = val[1];
                    }
                    if (val[0].toLowerCase().contains(".peak")) {
                        meterInfo.peak = val[1];
                    }
                }
            }
        }
//        sMeterId=getMeterId("LEVEL");
//        tempCId=getMeterId("PATEMP");
//        swrId=getMeterId("SWR");
//        pwrId=getMeterId("FWDPWR");
//        alcId=getMeterId("ALC");
    }

    /**
     * 查Meter得id,没有返回-1
     *
     * @return id
     */
    private int getMeterId(String s) {
        for (int key : this.keySet()) {
            if (get(key).nam.equalsIgnoreCase(s)) {
                return key;
            }
        }
        return -1;
    }


    public static class FlexMeterInfo {
        public String src;
        public String num;
        public String nam;
        public float low;
        public float hi;
        public String desc;
        public FlexMeterType unit = FlexMeterType.other;
        public String fps;
        public String peak;

        @Override
        public String toString() {
            return "{" +
                    "src='" + src + '\'' +
                    ", num='" + num + '\'' +
                    ", nam='" + nam + '\'' +
                    ", low='" + low + '\'' +
                    ", hi='" + hi + '\'' +
                    ", desc='" + desc + '\'' +
                    ", unit='" + unit + '\'' +
                    ", fps='" + fps + '\'' +
                    ", peak='" + peak + '\'' +
                    '}';
        }
    }
}
