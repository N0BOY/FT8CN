package com.bg7yoz.ft8cn.flex;

/**
 * VITA协议处理工具
 * @author BG7YOZ
 */



import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

enum VitaPacketType {
    IF_DATA,//IF Data packet without Stream Identifier
    IF_DATA_WITH_STREAM,//IF Data packet with Stream Identifier
    EXT_DATA,//Extension Data packet without Stream Identifier
    EXT_DATA_WITH_STREAM,//Extension Data packet with Stream Identifier
    IF_CONTEXT,//IF Context packet(see Section 7)
    EXT_CONTEXT//Extension Context packet(see Section 7);
};

//时间戳的类型
//时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位， 主要传递UTC时间或者 GPS 时间，
//小数部分主要有三种，一种是sample-count ，以采样周期为最小分辨率，一种是real-time以ps为最小单位，
// 第三种是以任意选择的时间进行累加得出的，前面两种时间戳可以直接与整数部分叠加，
// 第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
//小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
//所有的时间带来都是在以一个采样数据为该reference-point 时间
enum VitaTSI {
    TSI_NONE,//No Integer-seconds Timestamp field included
    TSI_UTC,//Coordinated Universal Time(UTC)
    TSI_GPS,//GPS time
    TSI_OTHER//Other
};
//时间戳小数部分类型
//小数部分主要有三种：
// 一种是sample-count ，以采样周期为最小分辨率，
// 一种是real-time以ps为最小单位，
// 第三种是以任意选择的时间进行累加得出的，
// 前面两种时间戳可以直接与整数部分叠加，
// 第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
// 小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
//  所有的时间带来都是在以一个采样数据为该参考点（reference-point）的时间。
enum VitaTSF {
    TSF_NONE,//No Fractional-seconds Timestamp field included. 不包括分数秒时间戳字段
    TSF_SAMPLE_COUNT,//Sample Count Timestamp. 样本计数时间戳
    TSF_REALTIME,//Real Time(Picoseconds) Timestamp. 实时（皮秒）时间戳
    TSF_FREERUN,//Free Running Count Timestamp. 自由运行计数时间戳
};

public class VITA {
    private static final String TAG="VITA";

    // 最小有效的VITA包长度
    private static final int VITAmin = 28;

    public static final int FRS_OUI = 0x12cd;
    //public static final int VITA_PORT = 4991;

    public static final int FLEX_CLASS_ID = 0x534C;
    public static final int FLEX_DAX_AUDIO_CLASS_ID = 0x534C03E3;
    public static final int FLEX_DAX_IQ_CLASS_ID = 0x534C00E3;
    public static final int FLEX_FFT_CLASS_ID = 0x534C8003;
    public static final int FLEX_METER_CLASS_ID = 0x534C8002;
    public static final int FLEX_Discovery_stream_ID = 0x800;


    public static final int VS_Meter = 0x8002;
    public static final int VS_PAN_FFT = 0x8003;
    public static final int VS_Waterfall = 0x8004;
    public static final int VS_Opus = 0x8005;
    public static final int DAX_IQ_24Khz = 0x00e3;
    public static final int DAX_IQ_48Khz = 0x00e4;
    public static final int DAX_IQ_96Khz = 0x00e5;
    public static final int DAX_IQ_192KHz = 0x00e6;
    public static final int VS_DAX_Audio = 0x03e3;
    public static final int VS_Discovery = 0xffff;


    private byte[] buffer;
    public VitaPacketType packetType;
    public boolean classIdPresent;//指示数据包中是否包含类标识符（类ID）字段
    public boolean trailerPresent;//指示数据包是否包含尾部。
    public VitaTSI tsi;//时间戳的类型。
    public VitaTSF tsf;//时间戳小数部分类型
    public int packetCount;//包计数器，可以对连续的IF data packet进行计数，这些packet具有相同的Stream  Identifier 和packet type。
    public int packetSize;//表示有多少32bit数在IF Data packet 里面

    //时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位，小数部分64位。
    public long integerTimestamp;//u_int32，long是64位的
    public long fracTimeStamp;
    public long oui;
    public int informationClassCode;//无用了，用classId代替
    public int packetClassCode;//无用了，用classId代替
    public int classId;//FLEX应该是0x534CFFF，是informationClassCode与packetClassCode合并的
    public byte[] payload = null;
    public long trailer;
    public boolean isAvailable=false;//电台对象是否有效。

    public boolean streamIdPresent;//是否有流字符

    //用来区分不同的 packet stream 。
    //stream ID 不是必须的，如果仅有一个数据包在单一数据链路传递的话就可以不用要，
    //如果 packet stream想用同一 stream ID 的话那每一个packet都得有，
    //在系统内部，不同的packet stream 之间的 Stream ID是不同的。
    //如果要用到 data-context 配对，那么IF data packet需要 Stream ID
    public long streamId;//流ID，32位，FLEX应当是0x0800

    /*
    VITA前28个字节是VITA头
     */


    public VITA(byte[] data) {
        this.buffer = data;
        //如果包的长度太小，或包为空，就退出计算
        if (data == null) return;
        if (data.length<VITAmin)return;

        isAvailable=true;//数据长度达到28个字节，说明是有效的。
        packetType=VitaPacketType.values()[(data[0]>>4)&0x0f];
        classIdPresent=(data[0]&0x8)==0x8;//指示数据包中是否包含类标识符（类ID）字段
        trailerPresent=(data[0]&0x4)==0x4;//指示数据包是否包含尾部。
        tsi=VitaTSI.values()[(data[1]>>6)&0x3];//如果有时间戳的话指示时间戳的整数部分是啥类型的
        tsf=VitaTSF.values()[(data[1]>>4)&0x3];
        packetCount=data[1]&0x0f;
        packetSize=((((int)data[2])&0x00ff)<<8)|((int)data[3])&0x00ff;

        int offset=4;//定位
        //检查是否有流字符
        streamIdPresent=packetType==VitaPacketType.IF_DATA_WITH_STREAM
                ||packetType==VitaPacketType.EXT_DATA_WITH_STREAM;
        if (streamIdPresent){//是否有流ID,获取流ID，32位
            streamId=((((long)data[offset])&0x00ff)<<24)|((((int)data[offset+1])&0x00ff)<<16)
                    |((((int)data[offset+2])&0x00ff)<<8)|((int)data[offset+3])&0x00ff;
            offset+=4;
        }

        if (classIdPresent){
            //只取24位，前8位保留
            oui=((((int)data[offset+1])&0x00ff)<<16)
                    |((((int)data[offset+2])&0x00ff)<<8)|((int)data[offset+3])&0x00ff;

            informationClassCode=((((int)data[offset+4])&0x00ff)<<8)|((int)data[offset+5])&0x00ff;
            packetClassCode=((((int)data[offset+6])&0x00ff)<<8)|((int)data[offset+7])&0x00ff;

            classId=((((int)data[offset+4])&0x00ff)<<24)|((((int)data[offset+5])&0x00ff)<<16)
                    |((((int)data[offset+6])&0x00ff)<<8)|((int)data[offset+7])&0x00ff;
            offset+=8;
        }

        //获取时间戳,以秒为单位的时间戳，32位。
        //时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位， 主要传递UTC时间或者 GPS 时间，
        //小数部分主要有三种，一种是sample-count ，以采样周期为最小分辨率，一种是real-time以ps为最小单位，
        // 第三种是以任意选择的时间进行累加得出的，前面两种时间戳可以直接与整数部分叠加，
        // 第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
        //小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
        //所有的时间带来都是在以一个采样数据为该reference-point 时间
        if (tsi!=VitaTSI.TSI_NONE){//32位,
            integerTimestamp=((((long)data[offset])&0x00ff)<<24)|((((int)data[offset+1])&0x00ff)<<16)
                    |((((int)data[offset+2])&0x00ff)<<8)|((int)data[offset+3])&0x00ff;
            offset+=4;
        }

        //获取时间戳的小数部分，64位。
        if (tsf!=VitaTSF.TSF_NONE){
            fracTimeStamp=((((long)data[offset])&0x00ff)<<56)|((((long)data[offset+1])&0x00ff)<<48)
                    |((((long)data[offset+2])&0x00ff)<<36)|((int)data[offset+3])&0x00ff
                    |((((long)data[offset+4])&0x00ff)<<24)|((((int)data[offset+5])&0x00ff)<<16)
                    |((((int)data[offset+6])&0x00ff)<<8)|((int)data[offset+7])&0x00ff;
            offset+=8;
        }
        //Log.e(TAG, String.format("VITA: data length:%d,offset:%d",data.length,offset) );
        if (offset<data.length){
            payload=new byte[data.length-offset-(trailerPresent?2:0)];//如果有尾部，就减去一个word的位置
            System.arraycopy(data,offset,payload,0,payload.length);
        }
        if (trailerPresent){
            trailer=((((int)data[data.length-2])&0x00ff)<<8)|((int)data[data.length-1])&0x00ff;
        }
    }

    /**
     * 获取payload的长度，如果没有数据，payload长度为0；
     * @return payload长度
     */
    public int getPayloadLength(){
        if (buffer==null){
            return 0;
        }else {
            return buffer.length;
        }
    }

    /**
     * 显示扩展数据
     * @return string
     */
    public String showPayload(){
        if (payload!=null) {
            return new String(payload).replace(" ","\n");
        }else {
            return "";
        }
    }

    public String showPayloadHex(){
        if (payload!=null){
            return byteToStr(payload);
        }else {
            return "";
        }
    }

    /**
     * 显示VITA 49的包头信息
     * @return string
     */
    @SuppressLint("DefaultLocale")
    public String showHeadStr(){
        return String.format("包类型(packetType): %s\n" +
                        "包数量(packetCount): %d\n" +
                        "包大小(packetSize): %d\n" +
                        "是否有流ID(streamIdPresent): %s\n" +
                        "流ID(streamId): 0x%X\n" +
                        "是否有类ID(classIdPresent): %s\n" +
                        "类ID(classId): 0x%X\n" +
                        "类高位(informationClassCode): 0x%X\n"+
                        "类低位(packetClassCode): 0x%X\n"+
                        "公司标识码(oui): 0x%X\n" +
                        "时间戳类型(tsi): %s\n" +
                        "时间戳整数部分(integerTimestamp):%s\n" +
                        "时间戳小数部分类型(tsf): %s\n" +
                        "时间戳小数部分值(fracTimeStamp): %d\n" +
                        "负载长度(payloadLength): %d\n" +
                        "是否有尾部(trailerPresent): %s\n"

                ,packetType.toString()
                ,packetCount
                ,packetSize
                ,streamIdPresent?"是":"否"
                ,streamId
                ,classIdPresent?"是":"否"
                ,classId
                ,informationClassCode
                ,packetClassCode
                ,oui
                ,tsi.toString()
                ,timestampToDateStr(integerTimestamp*1000)
                ,tsf.toString()
                ,fracTimeStamp
                ,(payload==null?0:payload.length)
                ,trailerPresent?"是":"否"
        );
    }

    /**
     * 显示VITA 49 包数据
     * @return string
     */
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%s负载(payload):\n%s\n"
                ,showHeadStr()
                ,(payload==null?"":new String(payload))
        );




    }

    public static String timestampToDateStr(Long timestamp){
        //final String DATETIME_CONVENTIONAL_CN = "yyyy-MM-dd HH:mm:ss";
        //SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_CONVENTIONAL_CN);
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        String sd = sdf.format(new Date(timestamp)); // 时间戳转换日期
        //System.out.println(sd);
        return sd;
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
}
