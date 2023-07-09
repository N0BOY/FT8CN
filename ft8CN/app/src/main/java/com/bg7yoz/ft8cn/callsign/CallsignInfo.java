package com.bg7yoz.ft8cn.callsign;
/**
 * 呼号信息类，用于归属地查询
 *
 * @author BG7YOZ
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

public class CallsignInfo {
    public static String TAG="CallsignInfo";
    public String CallSign;//呼号
    public String CountryNameEn;//国家
    public String CountryNameCN;//国家中文名
    public int CQZone;//CQ分区
    public int ITUZone;//ITU分区
    public String Continent;//大陆缩写
    public float Latitude;//以度为单位的纬度，+ 表示北
    public float Longitude;//以度为单位的经度，+ 表示西
    public float GMT_offset;//与 GMT 的本地时间偏移
    public String DXCC;//DXCC前缀

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        String country;
        if (GeneralVariables.isChina) {
            country=CountryNameCN;
        }else {
            country=CountryNameEn;
        }
        //return String.format("呼号:%s\n位置:%s\nCQ分区:%d\nITU分区:%d\n大陆:%s\n经纬度:%.2f,%.2f\n时区:%.0f\nDXCC前缀:%s"
        return String.format(GeneralVariables.getStringFromResource(R.string.callsign_info)
                , CallSign, country, CQZone, ITUZone, Continent, Longitude, Latitude, GMT_offset, DXCC);
    }


    public CallsignInfo(String callSign, String countryNameEn,
                        String countryNameCN, int CQZone, int ITUZone,
                        String continent, float latitude, float longitude,
                        float GMT_offset, String DXCC) {
        CallSign = callSign;
        CountryNameEn = countryNameEn;
        CountryNameCN = countryNameCN;
        this.CQZone = CQZone;
        this.ITUZone = ITUZone;
        Continent = continent;
        Latitude = latitude;
        Longitude = longitude;
        this.GMT_offset = GMT_offset;
        this.DXCC = DXCC;
    }

    public CallsignInfo(String s) {
        String[] info = s.split(":");
        if (info.length<9){
            Log.e(TAG,"呼号数据格式错误！"+s);
            return;
        }
        CountryNameEn = info[0].replace("\n", "").trim();
        CQZone = Integer.parseInt(info[1].replace("\n", "").replace(" ", ""));
        ITUZone = Integer.parseInt(info[2].replace("\n", "").replace(" ", ""));
        Continent = info[3].replace("\n", "").replace(" ", "");
        Latitude = Float.parseFloat(info[4].replace("\n", "").replace(" ", ""));
        Longitude = Float.parseFloat(info[5].replace("\n", "").replace(" ", ""));
        GMT_offset = Float.parseFloat(info[6].replace("\n", "").replace(" ", ""));
        DXCC = info[7].replace("\n", "").replace(" ", "");
        CallSign= info[8].replace("\n", "").replace(" ", "");
    }
}

