package com.bg7yoz.ft8cn.flex;
/**
 * Meter的哈希表。Meter是2个32位的数，ID+VALUE。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;

import java.util.HashMap;

public class FlexMeterList extends HashMap<Integer, FlexMeterList.FlexMeter> {
    public float sMeterVal=-150;//-150~10
    public float tempCVal=0;//0~100
    public float alcVal=-150;//-150~20
    public float swrVal=1;//1~999
    public float pwrVal=0;//0~100W
    public synchronized void setMeters(byte[] data, FlexMeterInfos infos) {
        for (int i = 0; i < data.length / 4; i++) {
            int val = readShortData(data, i * 4);
            FlexMeter meter = get(val);
            if (meter == null) {
                if (infos.get(val) == null) continue;
                meter = new FlexMeter();
                meter.name = infos.get(val).nam;
                meter.desc = infos.get(val).desc;
                meter.type = infos.get(val).unit;
                meter.id = val;
            }
            switch (meter.type) {
                case dBm:
                case swr:
                    meter.value = readShortData(data, i * 4 + 2) / 128f;
                    if (meter.name.contains("PWR")){//把dBm转换成功率值
                        meter.value=(float) Math.pow(10,meter.value/10f)/1000f;
                    }
                    //节省资源，提前赋值
                    if (meter.id==infos.sMeterId) sMeterVal=meter.value;
                    if (meter.id==infos.swrId) swrVal=meter.value;
                    if (meter.id==infos.pwrId) pwrVal=meter.value;
                    if (meter.id==infos.alcId) alcVal=meter.value;
                    break;
                case volt:
                    meter.value = readShortData(data, i * 4 + 2) / 256f;
                    break;
                case Temperature:
                    meter.value = readShortData(data, i * 4 + 2) / 64f;
                    //节省资源，提前赋值
                    if (meter.id==infos.tempCId) tempCVal=meter.value;
                    break;
                case other:
                default:
                    meter.value = readShortData(data, i * 4 + 2);
            }

            put(val, meter);
        }

    }

    public synchronized String getMeters(){
        StringBuilder temp=new StringBuilder();
        int i=0;
        for (int key:this.keySet()) {
            i++;
            temp.append(String.format("%-35s",get(key).toString()));
            if (i%2==0){
                temp.append("\n");
            }
        }
        return temp.toString();
    }



    /**
     * 把字节转换成short，不做小端转换！！
     *
     * @param data 字节数据
     * @return short
     */
    public static short readShortData(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start + 1] & 0xff
                | ((short) data[start] & 0xff) << 8);
    }

    public static float readShortFloat(byte[] data, int start) {
        if (data.length - start < 2) return 0.0f;
        int accum = 0;
        accum = accum | (data[start] & 0xff) << 0;
        accum = accum | (data[start + 1] & 0xff) << 8;
        return Float.intBitsToFloat(accum);
    }


    public static class FlexMeter {
        public int id;
        public float value;
        public String name;
        public String desc;
        public FlexMeterType type=FlexMeterType.other;

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("%02d.%s : %.1f",id,name,value);

        }
    }

}
