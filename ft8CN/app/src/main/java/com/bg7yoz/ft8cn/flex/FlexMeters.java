package com.bg7yoz.ft8cn.flex;
/**
 * flexRadio仪表处理
 * @author BG7YOZ
 */

import android.util.Log;

import java.util.HashMap;

public class FlexMeters extends HashMap<Integer, FlexMeters.FlexMeter> {
    private static final String TAG="FlexMeters";
    public FlexMeters(String content) {
        String[] temp =content.substring(content.indexOf("meter ")+"meter ".length()).split("#");
        for (int i = 0; i < temp.length; i++) {
            String[] val = temp[i].split("=");
            if (val.length == 2) {
                if (val[0].contains(".")) {
                    int index = Integer.parseInt(val[0].substring(0, val[0].indexOf(".")));
                    FlexMeter meter;
                    if (this.containsKey(index)){
                        meter=this.get(index);
                    }else  {
                        meter=new FlexMeter();
                        this.put(index,meter);
                    }

                    if (val[0].toLowerCase().contains(".src")) {
                        meter.src = val[1];
                    }
                    if (val[0].toLowerCase().contains(".num")) {
                        meter.num = val[1];
                    }
                    if (val[0].toLowerCase().contains(".nam")) {
                        meter.nam = val[1];
                    }
                    if (val[0].toLowerCase().contains(".low")) {
                        meter.low = val[1];
                    }
                    if (val[0].toLowerCase().contains(".hi")) {
                        meter.hi = val[1];
                    }
                    if (val[0].toLowerCase().contains(".desc")) {
                        meter.desc = val[1];
                    }
                    if (val[0].toLowerCase().contains(".unit")) {
                        meter.unit = val[1];
                    }
                    if (val[0].toLowerCase().contains(".fps")) {
                        meter.fps = val[1];
                    }
                    if (val[0].toLowerCase().contains(".peak")) {
                        meter.peak = val[1];
                    }
                }
            }
        }
    }

    public void getAllMeters(){
        for (int key:this.keySet()) {
            //s.append(String.format("%d->%s\n",key,get(key)));
            Log.e(TAG, "getAllMeters: "+String.format("ID:%d FIELDS:%s\n",key,get(key)) );
        }
    }


    public static class FlexMeter {
        public String src;
        public String num;
        public String nam;
        public String low;
        public String hi;
        public String desc;
        public String unit;
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
