package com.bg7yoz.ft8cn.flex;
/**
 * VITA49协议的简单解包和封包操作。此库停止维护，将使用VITA49.java替换此库。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */
/*
public static intVH_PKT_TYPE(x)      ((x & 0xF0000000) >> 28)
public static intVH_C(x)             ((x & 0x08000000) >> 26)
public static intVH_T(x)             ((x & 0x04000000) >> 25)
public static intVH_TSI(x)           ((x & 0x00c00000) >> 21)
public static intVH_TSF(x)           ((x & 0x00300000) >> 19)
public static intVH_PKT_CNT(x)       ((x & 0x000f0000) >> 16)
public static intVH_PKT_SIZE(x)      (x & 0x0000ffff)
 */

// Enumerates for field values

import android.annotation.SuppressLint;

import java.nio.ByteBuffer;



public class VITA {
    private static final String TAG = "VITA";

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

    //与x6100有关的id信息
    public  static final long XIEGU_Discovery_Class_Id = 0x005849454755FFFFL;
    public  static final int XIEGU_Discovery_Stream_Id = 0x00000800;
    public static final long XIEGU_AUDIO_CLASS_ID = 0x00584945475500A1L;
    public static final int XIEGU_PING_Stream_Id= 0x00000600;
    public static final long XIEGU_PING_CLASS_ID = 0x58494547550006L;
    public static final long XIEGU_METER_CLASS_ID = 0x58494547550007L;
    public static final int XIEGU_METER_Stream_Id= 0x00000700;



    private byte[] buffer;
    public boolean streamIdPresent;//是否有流字符
    public VitaPacketType packetType;
    public boolean classIdPresent;//指示数据包中是否包含类标识符（类ID）字段
    public boolean trailerPresent;//指示数据包是否包含尾部。
    public VitaTSI tsi;//时间戳的类型。
    public VitaTSF tsf;//时间戳小数部分类型
    public int packetCount;//包计数器，可以对连续的IF data packet进行计数，这些packet具有相同的Stream  Identifier 和packet type。
    public int packetSize;//表示有多少32bit数在IF Data packet 里面
    public int streamId;//流ID，32位
    //时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位，小数部分64位。
    public int integerTimestamp;//u_int32，long是64位的
    public long fracTimeStamp;
    public long oui;
    public int informationClassCode;//无用了，用classId代替
    public int packetClassCode;//无用了，用classId代替
    public int classId;//FLEX应该是0x534CFFF，是informationClassCode与packetClassCode合并的
    public long classId64;

    public byte[] payload = null;
    public long trailer;
    public boolean isAvailable = false;//电台对象是否有效。

    /**
     * 设置vita数据包的头部
     * @param packet 数据包
     * @return 数据包
     */
    public byte[] setVitaPacketHeader(byte[] packet){
        if (packet.length < 28) return packet;
        packet[0] = (byte) (packetType.ordinal() << 4);//packetType
        if (classIdPresent) packet[0] |=0x08;
        if (trailerPresent) packet[0] |= 0x04;


        packet[1] = (byte) (packetCount & 0xf);//packet count
        packet[1] |= (byte) (tsi.ordinal() << 6);//TSI
        packet[1] |= (byte) (tsf.ordinal() << 4);//TSF

        packet[2] = (byte) ((packetSize & 0xff00 ) >> 8 & 0xff);//packetSize 1（高8位）
        packet[3] = (byte) (packetSize & 0xff);//packetSize 2（低8位）

        //-----Stream Identifier--No.2 word----
        packet[4] = (byte) (((streamId & 0x00ff000000) >> 24) & 0xff);
        packet[5] = (byte) (((streamId & 0x0000ff0000) >> 16) & 0xff);
        packet[6] = (byte) (((streamId & 0x000000ff00) >> 8) & 0xff);
        packet[7] = (byte) (streamId &   0x00000000ff);
        //----Class ID--No.3 words----

        packet[8] = 0x00;
        packet[9] = (byte)(((classId64 & 0x00ff000000000000L) >> 48)& 0xff);
        packet[10] =(byte)(((classId64 & 0x0000ff0000000000L) >> 40)& 0xff);
        packet[11] =(byte)(((classId64 & 0x000000ff00000000L) >> 32)& 0xff);

        //---Class ID--No.4 word----
        packet[12] =(byte)(((classId64 & 0x00000000ff000000L) >> 24)& 0xff);
        packet[13] =(byte)(((classId64 & 0x0000000000ff0000L) >> 16)& 0xff);
        packet[14] =(byte)(((classId64 & 0x000000000000ff00L) >> 8)& 0xff);
        packet[15] =(byte)((classId64 & 0x00000000000000ffL ));

        //---Timestamp Int--No.5 word----
        packet[16] =(byte)(((integerTimestamp & 0xff000000) >> 24)& 0xff);
        packet[17] =(byte)(((integerTimestamp & 0x00ff0000) >> 16)& 0xff);
        packet[18] =(byte)(((integerTimestamp & 0x0000ff00) >> 8)& 0xff);
        packet[19] =(byte)(integerTimestamp & 0x000000ff);

        //---Timestamp franc Int--No.6 No.7 word----
        packet[20] = (byte)(((fracTimeStamp & 0xff00000000000000L) >> 56)& 0xff);
        packet[21] = (byte)(((fracTimeStamp & 0x00ff000000000000L) >> 48)& 0xff);
        packet[22] = (byte)(((fracTimeStamp & 0x0000ff0000000000L) >> 40)& 0xff);
        packet[23] = (byte)(((fracTimeStamp & 0x000000ff00000000L) >> 32)& 0xff);
        //---Class ID--No.4 word----

        packet[24] =(byte)(((fracTimeStamp & 0x00000000ff000000L) >> 24)& 0xff);
        packet[25] =(byte)(((fracTimeStamp & 0x0000000000ff0000L) >> 16)& 0xff);
        packet[26] =(byte)(((fracTimeStamp & 0x000000000000ff00L) >> 8)& 0xff);
        packet[27] =(byte)(fracTimeStamp & 0x00000000000000ffL ) ;
        return packet;
    }

    public byte[] pingDataToVita(){
        byte[] result=new byte[28];
        return setVitaPacketHeader(result);
    }

    /**
     * 生成音频流的VITA数据包
     *
     * @param count 包计数器
     * @param payload 音频流数据
     * @return vita数据包
     */
    public byte[] audioShortDataToVita(int count, short[] payload){
        packetType = VitaPacketType.EXT_DATA_WITH_STREAM;
        classIdPresent = true;
        trailerPresent = false;//没有尾巴
        tsi = VitaTSI.TSI_OTHER;//
        tsf = VitaTSF.TSF_SAMPLE_COUNT;
        this.packetCount = count;
        this.packetSize =  (payload.length / 2) + 7;

        byte[] result=new byte[payload.length*2 + 28];
        setVitaPacketHeader(result);

        //----HEADER--No.1 word------

//        result[0] = (byte) (packetType.ordinal() << 4);//packetType
//        if (classIdPresent) result[0] |=0x08;
//        if (trailerPresent) result[0] |= 0x04;
//
//
//        result[1] = (byte) (packetCount & 0xf);//packet count
//        result[1] |= (byte) (tsi.ordinal() << 6);//TSI
//        result[1] |= (byte) (tsf.ordinal() << 4);//TSF
//
//        result[2] = (byte) ((packetSize & 0xff00 ) >> 8 & 0xff);//packetSize 1（高8位）
//        result[3] = (byte) (packetSize & 0xff);//packetSize 2（低8位）
//
//        //-----Stream Identifier--No.2 word----
//        result[4] = (byte) (((streamId & 0x00ff000000) >> 24) & 0xff);
//        result[5] = (byte) (((streamId & 0x0000ff0000) >> 16) & 0xff);
//        result[6] = (byte) (((streamId & 0x000000ff00) >> 8) & 0xff);
//        result[7] = (byte) (streamId &   0x00000000ff);
//        //----Class ID--No.3 words----
//
//        result[8] = 0x00;
//        result[9] = (byte)(((classId64 & 0x00ff000000000000L) >> 48)& 0xff);
//        result[10] =(byte)(((classId64 & 0x0000ff0000000000L) >> 40)& 0xff);
//        result[11] =(byte)(((classId64 & 0x000000ff00000000L) >> 32)& 0xff);
//
//        //---Class ID--No.4 word----
//        result[12] =(byte)(((classId64 & 0x00000000ff000000L) >> 24)& 0xff);
//        result[13] =(byte)(((classId64 & 0x0000000000ff0000L) >> 16)& 0xff);
//        result[14] =(byte)(((classId64 & 0x000000000000ff00L) >> 8)& 0xff);
//        result[15] =(byte)((classId64 & 0x00000000000000ffL ));
//
//        //---Timestamp Int--No.5 word----
//        result[16] =(byte)(((integerTimestamp & 0xff000000) >> 24)& 0xff);
//        result[17] =(byte)(((integerTimestamp & 0x00ff0000) >> 16)& 0xff);
//        result[18] =(byte)(((integerTimestamp & 0x0000ff00) >> 8)& 0xff);
//        result[19] =(byte)(integerTimestamp & 0x000000ff);
//
//        //---Timestamp franc Int--No.6 No.7 word----
//        result[20] = (byte)(((fracTimeStamp & 0xff00000000000000L) >> 56)& 0xff);
//        result[21] = (byte)(((fracTimeStamp & 0x00ff000000000000L) >> 48)& 0xff);
//        result[22] = (byte)(((fracTimeStamp & 0x0000ff0000000000L) >> 40)& 0xff);
//        result[23] = (byte)(((fracTimeStamp & 0x000000ff00000000L) >> 32)& 0xff);
//        //---Class ID--No.4 word----
//
//        result[24] =(byte)(((fracTimeStamp & 0x00000000ff000000L) >> 24)& 0xff);
//        result[25] =(byte)(((fracTimeStamp & 0x0000000000ff0000L) >> 16)& 0xff);
//        result[26] =(byte)(((fracTimeStamp & 0x000000000000ff00L) >> 8)& 0xff);
//        result[27] =(byte)(fracTimeStamp & 0x00000000000000ffL ) ;

        for (int i = 0; i < payload.length ; i++) {
            result[28+i*2]=(byte)(payload[i]&0xff) ;
            result[28+i*2+1]= (byte)((payload[i]>>8)&0xff) ;
        }
        return result;
    }


    /**
     * 生成音频流的VITA数据包，id应当是电台create stream是赋给的
     * @param data 音频流数据
     * @return vita数据包
     */
    public byte[] audioFloatDataToVita(int count, float[] data) {
        byte[] result = new byte[data.length * 4 + 28];//一个float占用4个字节，28字节是包头的长度7个word
        packetType = VitaPacketType.EXT_DATA_WITH_STREAM;
        classIdPresent = true;
        trailerPresent = false;//没有尾巴
        tsi = VitaTSI.TSI_OTHER;//
        tsf = VitaTSF.TSF_SAMPLE_COUNT;
        this.packetCount = count;
        this.packetSize = (data.length / 2) + 7;


        //----HEADER--No.1 word------

        result[0] = (byte) (packetType.ordinal() << 4);//packetType
        if (classIdPresent) result[0] |=0x08;
        if (trailerPresent) result[0] |= 0x04;


        result[1] = (byte) (packetCount & 0xf);//packet count
        result[1] |= (byte) (tsi.ordinal() << 6);//TSI
        result[1] |= (byte) (tsf.ordinal() << 4);//TSF

        result[2] = (byte) ((packetSize & 0xff00 ) >> 8 & 0xff);//packetSize 1（高8位）
        result[3] = (byte) (packetSize & 0xff);//packetSize 2（低8位）

        //-----Stream Identifier--No.2 word----
        result[4] = (byte) (((streamId & 0x00ff000000) >> 24) & 0xff);
        result[5] = (byte) (((streamId & 0x0000ff0000) >> 16) & 0xff);
        result[6] = (byte) (((streamId & 0x000000ff00) >> 8) & 0xff);
        result[7] = (byte) (streamId &   0x00000000ff);
        //----Class ID--No.3 words----

        result[8] = 0x00;
        result[9] = (byte)(((classId64 & 0x00ff000000000000L) >> 48)& 0xff);
        result[10] =(byte)(((classId64 & 0x0000ff0000000000L) >> 40)& 0xff);
        result[11] =(byte)(((classId64 & 0x000000ff00000000L) >> 32)& 0xff);

        //---Class ID--No.4 word----
        result[12] =(byte)(((classId64 & 0x00000000ff000000L) >> 24)& 0xff);
        result[13] =(byte)(((classId64 & 0x0000000000ff0000L) >> 16)& 0xff);
        result[14] =(byte)(((classId64 & 0x000000000000ff00L) >> 8)& 0xff);
        result[15] =(byte)((classId64 & 0x00000000000000ffL ));

        //---Timestamp Int--No.5 word----
        result[16] =(byte)(((integerTimestamp & 0xff000000) >> 24)& 0xff);
        result[17] =(byte)(((integerTimestamp & 0x00ff0000) >> 16)& 0xff);
        result[18] =(byte)(((integerTimestamp & 0x0000ff00) >> 8)& 0xff);
        result[19] =(byte)(integerTimestamp & 0x000000ff);

        //---Timestamp franc Int--No.6 No.7 word----
        result[20] = (byte)(((fracTimeStamp & 0xff00000000000000L) >> 56)& 0xff);
        result[21] = (byte)(((fracTimeStamp & 0x00ff000000000000L) >> 48)& 0xff);
        result[22] = (byte)(((fracTimeStamp & 0x0000ff0000000000L) >> 40)& 0xff);
        result[23] = (byte)(((fracTimeStamp & 0x000000ff00000000L) >> 32)& 0xff);
        //---Class ID--No.4 word----

        result[24] =(byte)(((fracTimeStamp & 0x00000000ff000000L) >> 24)& 0xff);
        result[25] =(byte)(((fracTimeStamp & 0x0000000000ff0000L) >> 16)& 0xff);
        result[26] =(byte)(((fracTimeStamp & 0x000000000000ff00L) >> 8)& 0xff);
        result[27] =(byte)(fracTimeStamp & 0x00000000000000ffL ) ;


        for (int i = 0; i < data.length; i++) {
            byte[] bytes = ByteBuffer.allocate(4).putFloat(data[i]).array();//float转byte[]
            result[i * 4 + 28] = bytes[0];
            result[i * 4 + 29] = bytes[1];
            result[i * 4 + 30] = bytes[2];
            result[i * 4 + 31] = bytes[3];
        }




        return result;
    }


    public VITA() {
    }

    public VITA( VitaPacketType packetType
            , VitaTSI tsi, VitaTSF tsf
            , int packetCount
            , int streamId, long classId64) {
        this.streamIdPresent = true;
        this.packetType = packetType;
        this.classIdPresent = true;
        this.trailerPresent = false;
        this.tsi = tsi;
        this.tsf = tsf;
        this.packetCount = packetCount;
        this.streamId = streamId;
        this.classId64 = classId64;
        this.isAvailable = true;
    }

    public VITA(byte[] data) {
        this.buffer = data;
        //如果包的长度太小，或包为空，就退出计算
        if (data == null) return;
        if (data.length < VITAmin) return;

        isAvailable = true;//数据长度达到28个字节，说明是有效的。
        packetType = VitaPacketType.values()[(data[0] >> 4) & 0x0f];
        classIdPresent = (data[0] & 0x8) == 0x8;//指示数据包中是否包含类标识符（类ID）字段
        trailerPresent = (data[0] & 0x4) == 0x4;//指示数据包是否包含尾部。
        tsi = VitaTSI.values()[(data[1] >> 6) & 0x3];//如果有时间戳的话指示时间戳的整数部分是啥类型的
        tsf = VitaTSF.values()[(data[1] >> 4) & 0x3];
        packetCount = data[1] & 0x0f;
        packetSize = ((((int) data[2]) & 0x00ff) << 8) | ((int) data[3]) & 0x00ff;

        int offset = 4;//定位
        //检查是否有流字符
        streamIdPresent = packetType == VitaPacketType.IF_DATA_WITH_STREAM
                || packetType == VitaPacketType.EXT_DATA_WITH_STREAM;

        if (streamIdPresent) {//是否有流ID,获取流ID，32位
            streamId = ((((int) data[offset]) & 0x00ff) << 24) | ((((int) data[offset + 1]) & 0x00ff) << 16)
                    | ((((int) data[offset + 2]) & 0x00ff) << 8) | ((int) data[offset + 3]) & 0x00ff;
            offset += 4;
        }

        if (classIdPresent) {
            //只取24位，前8位保留
            oui = ((((int) data[offset + 1]) & 0x00ff) << 16)
                    | ((((int) data[offset + 2]) & 0x00ff) << 8) | ((int) data[offset + 3]) & 0x00ff;

            informationClassCode = ((((int) data[offset + 4]) & 0x00ff) << 8) | ((int) data[offset + 5]) & 0x00ff;
            packetClassCode = ((((int) data[offset + 6]) & 0x00ff) << 8) | ((int) data[offset + 7]) & 0x00ff;

            classId = ((((int) data[offset + 4]) & 0x00ff) << 24) | ((((int) data[offset + 5]) & 0x00ff) << 16)
                    | ((((int) data[offset + 6]) & 0x00ff) << 8) | ((int) data[offset + 7]) & 0x00ff;

            classId64 = ((((long) data[offset + 1]) & 0x00ff) << 48)
                    | ((((long) data[offset + 2]) & 0x00ff) << 40)
                    | ((((long) data[offset + 3]) & 0x00ff)<<32)
                    | ((((long) data[offset + 4]) & 0x00ff) << 24)
                    | ((((long) data[offset + 5]) & 0x00ff) << 16)
                    | ((((long) data[offset + 6]) & 0x00ff) << 8)
                    | ((long) data[offset + 7]) & 0x00ff;
            //Log.d(TAG,String.format("classId64:%X",classId64));
            offset += 8;
        }
        //Log.e(TAG, "VITA: "+String.format("id: 0x%x, classIdPresent:0x%x,packetSize:%d",streamId,classId,packetSize) );

        //获取时间戳,以秒为单位的时间戳，32位。
        //时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位， 主要传递UTC时间或者 GPS 时间，
        //小数部分主要有三种，一种是sample-count ，以采样周期为最小分辨率，一种是real-time以ps为最小单位，第三种是以任意选择的时间进行累加得出的，前面两种时间戳可以直接与整数部分叠加，第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
        //小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
        //所有的时间带来都是在以一个采样数据为该reference-point 时间
        if (tsi != VitaTSI.TSI_NONE) {//32位,
            integerTimestamp = ((((int) data[offset]) & 0x00ff) << 24) | ((((int) data[offset + 1]) & 0x00ff) << 16)
                    | ((((int) data[offset + 2]) & 0x00ff) << 8) | ((int) data[offset + 3]) & 0x00ff;
            offset += 4;
        }
        //获取时间戳的小数部分，64位。
        if (tsf != VitaTSF.TSF_NONE) {
            fracTimeStamp = ((((long) data[offset]) & 0x00ff) << 56) | ((((long) data[offset + 1]) & 0x00ff) << 48)
                    | ((((long) data[offset + 2]) & 0x00ff) << 40) | ((((long) data[offset + 3]) & 0x00ff) << 32)
                    | ((((long) data[offset + 4]) & 0x00ff) << 24) | ((((int) data[offset + 5]) & 0x00ff) << 16)
                    | ((((int) data[offset + 6]) & 0x00ff) << 8) | ((int) data[offset + 7]) & 0x00ff;
            offset += 8;
        }


        //Log.e(TAG, String.format("VITA: data length:%d,offset:%d",data.length,offset) );
        if (offset < data.length) {
            payload = new byte[data.length - offset - (trailerPresent ? 2 : 0)];//如果有尾部，就减去一个word的位置
            System.arraycopy(data, offset, payload, 0, payload.length);
        }
        if (trailerPresent) {
            trailer = ((((int) data[data.length - 2]) & 0x00ff) << 8) | ((int) data[data.length - 1]) & 0x00ff;
        }
    }

    /**
     * 获取payload的长度，如果没有数据，payload长度为0；
     *
     * @return payload长度
     */
    public int getPayloadLength() {
        if (buffer == null) {
            return 0;
        } else {
            return buffer.length;
        }
    }

    /**
     * 显示扩展数据
     *
     * @return string
     */
    public String showPayload() {
        if (payload != null) {
            return new String(payload);//.replace(" ", "\n");
        } else {
            return "";
        }
    }

    public String showPayloadHex() {
        if (payload != null) {
            return byteToStr(payload);
        } else {
            return "";
        }
    }

    /**
     * 显示VITA 49的包头信息
     *
     * @return string
     */
    @SuppressLint("DefaultLocale")
    public String showHeadStr() {
        return String.format("包类型(packetType): %s\n" +
                        "包数量(packetCount): %d\n" +
                        "包大小(packetSize): %d\n" +
                        "是否有流ID(streamIdPresent): %s\n" +
                        "流ID(streamId): 0x%X\n" +
                        "是否有类ID(classIdPresent): %s\n" +
                        "类ID(classId): 0x%X\n" +
                        "类高位(informationClassCode): 0x%X\n" +
                        "类低位(packetClassCode): 0x%X\n" +
                        "公司标识码(oui): 0x%X\n" +
                        "时间戳类型(tsi): %s\n" +
                        "时间戳整数部分(integerTimestamp):0x%X\n" +
                        "时间戳小数部分类型(tsf): %s\n" +
                        "时间戳小数部分值(fracTimeStamp): 0x%X\n" +
                        "负载长度(payloadLength): %d\n" +
                        "是否有尾部(trailerPresent): %s\n"

                , packetType.toString()
                , packetCount
                , packetSize
                , streamIdPresent ? "是" : "否"
                , streamId
                , classIdPresent ? "是" : "否"
                , classId
                , informationClassCode
                , packetClassCode
                , oui
                , tsi.toString()
                , integerTimestamp
                , tsf.toString()
                , fracTimeStamp
                , (payload == null ? 0 : payload.length)
                , trailerPresent ? "是" : "否"
        );
    }

    /**
     * 显示VITA 49 包数据
     *
     * @return string
     */
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%s负载(payload):\n%s\n"
                , showHeadStr()
                , (payload == null ? "" : new String(payload))
        );


    }



    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }


    /**
     * 把字节转换成short，做小端模式转换
     *
     * @param data 字节数据
     * @param  start 起始位置
     * @return short
     */
    public static short readShortDataBigEnd(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start] & 0xff
                | ((short) data[start +1] & 0xff) << 8);
    }

    /**
     * 把字节转换成short，不做小端转换！！
     *
     * @param data 字节数据
     * @param  start 起始位置
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

}
