package com.bg7yoz.ft8cn.icom;

/**
 * iCom电台各种数据包类型工具
 * @author  BG7YOZ
 */
public class IComPacketTypes {
    private static final String TAG = "IComPacketTypes";

    public static final int TX_BUFFER_SIZE = 0xf0;
    /**
     * 各类型包的长度
     */
    public static final int CONTROL_SIZE = 0x10;
    public static final int WATCHDOG_SIZE = 0x14;
    public static final int PING_SIZE = 0x15;
    public static final int OPENCLOSE_SIZE = 0x16;
    public static final int RETRANSMIT_RANGE_SIZE = 0x18;//重新传输，范围
    public static final int TOKEN_SIZE = 0x40;
    public static final int STATUS_SIZE = 0x50;
    public static final int LOGIN_RESPONSE_SIZE = 0x60;
    public static final int LOGIN_SIZE = 0x80;
    public static final int CONNINFO_SIZE = 0x90;
    public static final int CAPABILITIES_SIZE = 0x42;//功能包
    public static final int RADIO_CAP_SIZE = 0x66;
    public static final int CAP_CAPABILITIES_SIZE = 0xA8;//0x42+0x66
    public static final int AUDIO_HEAD_SIZE=0x18;//音频数据包的头是0x10+0x08，后面再跟音频数据


    public static final short CMD_NULL = 0x00;//空指令
    public static final short CMD_RETRANSMIT = 0x01;//请求重新发送，seq是重新发送的序号
    public static final short CMD_ARE_YOU_THERE = 0x03;//询问是否在？seq值必须为0
    public static final short CMD_I_AM_HERE = 0x04;//回答是否在？
    public static final short CMD_DISCONNECT = 0x05;//断开
    public static final short CMD_ARE_YOU_READY = 0x06;//询问电台是否准备好了，seq=1
    public static final short CMD_I_AM_READY = 0x06;//电台回复已经准备好
    public static final short CMD_PING = 0x07;//ping ,seq有自己的序列

    public static final byte TOKEN_TYPE_DELETE = 0x01;//令牌删除包
    public static final byte TOKEN_TYPE_CONFIRM = 0x02;//令牌确认包
    public static final byte TOKEN_TYPE_DISCONNECT = 0x04;//断开CI-V和音频流
    public static final byte TOKEN_TYPE_RENEWAL = 0x05;//令牌续订


    public static final long PING_PERIOD_MS = 500;//ping时钟的周期
    public static final long ARE_YOU_THERE_PERIOD_MS = 500;//查找电台的时钟周期
    public static final long IDLE_PERIOD_MS = 100;//空包时钟的周期
    public static final long TOKEN_RENEWAL_PERIOD_MS = 60000;//令牌旭东的时钟周期
    public static final long PURGE_MILLISECONDS = 10000;//数据缓冲区的时间最长是10秒钟
    public static final long OPEN_CLOSE_PERIOD_MS = 500;//civ指令定时发送open指令，确保端口打开
    public static final long WATCH_DOG_PERIOD_MS = 500;//监视数据接收状况的看门狗
    public static final long WATCH_DOG_ALERT_MS = 2000;//触发数据接收状况报警的阈值
    public static final long METER_TIMER_PERIOD_MS = 500;//检查meter的时钟周期

    public static final int AUDIO_SAMPLE_RATE = 12000;//音频的采样率

    public static final short CODEC_ALL_SUPPORTED = 0x018b;
    public static final short CODEC_ONLY_24K = 0x0100;
    public static final short CODEC_ONLY_12K = 0x0080;
    public static final short CODEC_ONLY_441K = 0x0040;//44.1k
    public static final short CODEC_ONLY_2205K = 0x0020;//22.05k
    public static final short CODEC_ONLY_11025K = 0x0010;//11.025k
    public static final short CODEC_ONLY_48K = 0x0008;
    public static final short CODEC_ONLY_32K = 0x0004;
    public static final short CODEC_ONLY_16K = 0x0002;
    public static final short CODEC_ONLY_8K = 0x0001;

    public static class IcomCodecType {
        public static final byte ULAW_1CH_8BIT = 0x01;
        public static final byte LPCM_1CH_8BIT = 0x02;
        public static final byte LPCM_1CH_16BIT = 0x04;//FT8CN推荐值
        public static final byte PCM_2CH_8BIT = 0x08;
        public static final byte LPCM_2CH_16BIT = 0x10;
        public static final byte ULAW_2CH_8BIT = 0x20;
        public static final byte OPUS_CH1 = 0x40;
        public static final byte OPUS_CH2 = (byte) 0x80;
    }


    /**
     * 控制指令数据包。用于简单通信和重传请求的不带内容的数据包,0x10
     */
    public static class ControlPacket {
        /**
         * 把控制数据包转换成数据流
         *
         * @return 数据流
         */
        public static byte[] toBytes(short type, short seq, int sentId, int rcvdId) {
            byte[] packet = new byte[CONTROL_SIZE];
            System.arraycopy(intToBigEndian(CONTROL_SIZE), 0, packet, 0, 4);
            System.arraycopy(shortToBigEndian(type), 0, packet, 4, 2);
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);
            System.arraycopy(intToBigEndian(sentId), 0, packet, 8, 4);
            System.arraycopy(intToBigEndian(rcvdId), 0, packet, 12, 4);
            return packet;
        }

        public static byte[] idlePacketData(short seq, int sendid, int rcvdId) {
            return toBytes(CMD_NULL, seq, sendid, rcvdId);
        }

        public static boolean isControlPacket(byte[] data) {//0x10
            return data.length == CONTROL_SIZE && readIntBigEndianData(data, 0x00) == CONTROL_SIZE;
        }

        public static short getType(byte[] data) {
            if (data.length < CONTROL_SIZE) return 0;
            return readShortBigEndianData(data, 0x04);
        }

        public static short getSeq(byte[] data) {
            if (data.length < CONTROL_SIZE) return 0;
            return readShortBigEndianData(data, 0x06);
        }

        public static int getSentId(byte[] data) {
            if (data.length < CONTROL_SIZE) return 0;
            return readIntBigEndianData(data, 0x08);
        }

        public static int getRcvdId(byte[] data) {
            if (data.length < CONTROL_SIZE) return 0;
            return readIntBigEndianData(data, 0x0c);
        }
        public static void setRcvdId(byte[] data,int rcvdId){
            System.arraycopy(intToBigEndian(rcvdId),0x00,data,0x0c,4);
        }
    }

    public static class AudioPacket{
        /**
         *  quint32 len;        // 0x00
         *         quint16 type;       // 0x04
         *         quint16 seq;        // 0x06
         *         quint32 sentid;     // 0x08
         *         quint32 rcvdid;     // 0x0c
         *
         *
         *         //接收的时候，ident=0x8116 ，8106，8006
         *         quint16 ident;      // 0x10 发射的时候： 当datalen=0xa0时,ident=0x9781,否则ident=0x0080;
         *         quint16 sendseq;    // 0x12
         *         quint16 unused;     // 0x14
         *         quint16 datalen;    // 0x16
         */
        public static boolean isAudioPacket(byte[] data){
            if (data.length<AUDIO_HEAD_SIZE) return false;
            return data.length-AUDIO_HEAD_SIZE==readShortData(data,0x16);
        }
        public static short getDataLen(byte[] data){
            return readShortData(data,0x16);
        }
        public static byte[] getAudioData(byte[] data){
            byte[] audio=new byte[data.length-AUDIO_HEAD_SIZE];
            System.arraycopy(data,0x18,audio,0,audio.length);
            return audio;
        }

        public static byte[] getTxAudioPacket(byte[] audio,short seq,int sentid,int rcvdid,short sendSeq){
            byte[] packet=new byte[audio.length+AUDIO_HEAD_SIZE];
            System.arraycopy(intToBigEndian(packet.length),0,packet,0,4);
            System.arraycopy(shortToBigEndian(seq),0,packet,0x06,2);
            System.arraycopy(intToBigEndian(sentid),0,packet,0x08,4);
            System.arraycopy(intToBigEndian(rcvdid),0,packet,0x0c,4);
            if (audio.length==0xa0){
                System.arraycopy(shortToByte((short)0x8197),0,packet,0x10,2);
            }else {//这个是常用的数值
                System.arraycopy(shortToByte((short)0x8000),0,packet,0x10,2);
            }
            System.arraycopy(shortToByte(sendSeq),0,packet,0x12,2);

            System.arraycopy(shortToByte((short)audio.length),0,packet,0x16,2);
            System.arraycopy(audio,0,packet,0x18,audio.length);
            return packet;
        }
    }

    public static class CivPacket{
           /*
         quint32 len;        // 0x00
        quint16 type;       // 0x04
        quint16 seq;        // 0x06
        quint32 sentid;     // 0x08
        quint32 rcvdid;     // 0x0c
        char reply;       // 0x10,civ是c1
        quint16 civ_len;        // 0x11 此字段是小端模式，0x0001,在数组中的顺序是0x0100
        quint16 sendseq;    //0x13
        byte[] civ_data;//0x15
         */
        public static boolean checkIsCiv(byte[] data){
            if (data.length<=0x15) return false;
            //是civ指令的条件：长度不能小于0x15，dataLen字段与实际相符，reply=0xc1，type!=0x01
            return (data.length-0x15==readShortBigEndianData(data,0x11))
                    &&(data[0x10]==(byte)0xc1)
                    &&(ControlPacket.getType(data)!=CMD_RETRANSMIT);
        }
        public static byte[] getCivData(byte[] data){
            byte[] civ=new byte[data.length-0x15];
            System.arraycopy(data,0x15,civ,0,data.length-0x15);
            return civ;
        }
        public static byte[] setCivData(short seq,int sentid,int rcvdid,short civSeq, byte[] data){
            byte[] packet=new byte[data.length+0x15];
            System.arraycopy(intToBigEndian(packet.length),0,packet,0,4);
            System.arraycopy(shortToBigEndian(seq),0,packet,0x06,2);
            System.arraycopy(intToBigEndian(sentid),0,packet,0x08,4);
            System.arraycopy(intToBigEndian(rcvdid),0,packet,0x0c,4);
            packet[0x10]=(byte) 0xc1;
            System.arraycopy(shortToBigEndian((short)data.length),0,packet,0x11,2);
            System.arraycopy(shortToByte(civSeq),0,packet,0x13,2);
            System.arraycopy(data,0,packet,0x15,data.length);
            return packet;
        }
    }

    public static class OpenClosePacket{
        /*
         quint32 len;        // 0x00
        quint16 type;       // 0x04
        quint16 seq;        // 0x06
        quint32 sentid;     // 0x08
        quint32 rcvdid;     // 0x0c
        char reply;       // 0x10,openClose是c0,civ是c1
        quint16 civ_len;        // 0x11 此字段是小端模式，0x0001,在数组中的顺序是0x0100
        quint16 sendseq;    //0x13
        char magic;         // 0x15
         */
        public static byte[] toBytes(short seq, int sentId, int rcvdId,short civSeq,byte magic) {
            byte[] packet = new byte[OPENCLOSE_SIZE];
            System.arraycopy(intToBigEndian(OPENCLOSE_SIZE), 0, packet, 0, 4);
            //System.arraycopy(shortToBigEndian(type), 0, packet, 4, 2);
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);
            System.arraycopy(intToBigEndian(sentId), 0, packet, 8, 4);
            System.arraycopy(intToBigEndian(rcvdId), 0, packet, 12, 4);

            packet[0x10]=(byte)0xc0;
            System.arraycopy(shortToBigEndian((short)0x0001),0,packet,0x11,2);
            System.arraycopy(shortToByte(civSeq),0,packet,0x13,2);
            packet[0x15]=magic;
            return packet;
        }
    }

    /**
     * ping 数据包，0x15。
     * 如果reply==0，说明是对方ping过来的包，必须要回复。如果reply=1,说明是对方回Ping的，本地的pingSeq++
     */
    public static class PingPacket {
        /*
        quint32 len;        // 0x00
        quint16 type;       // 0x04
        quint16 seq;        // 0x06
        quint32 sentid;     // 0x08
        quint32 rcvdid;     // 0x0c
        char  reply;        // 0x10 如果接收的数据中reply=0x00，我就要用sendReplayPingData()回复Ping。
        union { // 此部分发送与接收定义是不同的
            struct { // Ping包
                quint32 time;      // 0x11
            };
            struct { // 发送
                quint16 datalen;    // 0x11
                quint16 sendseq;    //0x13
            };
        }
         */
        public static boolean isPingPacket(byte[] data) {
            return readShortBigEndianData(data, 0x04) == CMD_PING;
        }

        public static byte getReply(byte[] data) {
            return data[0x10];
        }


        /**
         * 根据接收的Ping包，生成回复的Ping包。
         *
         * @param data     接收到的Ping包
         * @param localID  主机的ID
         * @param remoteID 电台的ID
         * @return 回复的PIng包
         */
        public static byte[] sendReplayPingData(byte[] data, int localID, int remoteID) {
            byte[] packet = new byte[PING_SIZE];
            System.arraycopy(intToBigEndian(PING_SIZE), 0, packet, 0, 4);      //len  int32 0x00
            System.arraycopy(shortToBigEndian(CMD_PING), 0, packet, 4, 2);//type int16 0x04
            packet[0x6] = data[0x6];                                                    //seq  int16 0x06
            packet[0x7] = data[0x7];//把原来的seq值取过来
            System.arraycopy(intToBigEndian(localID), 0, packet, 8, 4);  //localID  int32  0x08
            System.arraycopy(intToBigEndian(remoteID), 0, packet, 12, 4);//remoteID int32  0x0c
            packet[0x10] = (byte) (0x01); //reply byte 0x10, reply=01是回Ping,reply=00是Ping
            //Time
            packet[0x11] = data[0x11];
            packet[0x12] = data[0x12];
            packet[0x13] = data[0x13];
            packet[0x14] = data[0x14];
            return packet;
        }

        /**
         * 生成用于Ping电台的数据包，seq应当是pingSeq,与指令数据包不在一个序列，序列号要等接收到回Ping的包再自增。
         *
         * @param localID  主机ID
         * @param remoteID 电台ID
         * @param seq      序列号
         * @return 数据包
         */
        public static byte[] sendPingData(int localID, int remoteID, short seq) {
            byte[] packet = new byte[PING_SIZE];
            System.arraycopy(intToBigEndian(PING_SIZE), 0, packet, 0, 4);//    len  int32 0x00发送时，len=0
            System.arraycopy(shortToBigEndian(CMD_PING), 0, packet, 4, 2);//type int16 0x04
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);//seq  int16 0x06
            System.arraycopy(intToBigEndian(localID), 0, packet, 8, 4);  //localID  int32  0x08
            System.arraycopy(intToBigEndian(remoteID), 0, packet, 12, 4);//remoteID int32  0x0c
            packet[0x10] = (byte) (0x00);//ping对方，=0x00，如果回复=0x01
            //Time
            System.arraycopy(intToBigEndian((int) System.currentTimeMillis())
                    , 0, packet, 11, 4);

            return packet;
        }
    }

    /**
     * Status（0x50）包，
     */
    public static class StatusPacket {
        /*
         quint32 len;                // 0x00
        quint16 type;               // 0x04
        quint16 seq;                // 0x06
        quint32 sentid;             // 0x08
        quint32 rcvdid;             // 0x0c
        char unuseda[2];          // 0x10
        quint16 payloadsize;      // 0x12
        quint8 requestreply;      // 0x13
        quint8 requesttype;       // 0x14
        quint16 innerseq;         // 0x16
        char unusedb[2];          // 0x18
        quint16 tokrequest;         // 0x1a
        quint32 token;              // 0x1c
        union {
            struct {
                quint16 authstartid;    // 0x20
                char unusedd[5];        // 0x22
                quint16 commoncap;      // 0x27
                char unusede;           // 0x29
                quint8 macaddress[6];     // 0x2a
            };
            quint8 guid[GUIDLEN];                  // 0x20
        };
        quint32 error;             // 0x30
        char unusedg[12];         // 0x34
        char disc;                // 0x40
        char unusedh;             // 0x41
        quint16 civport;          // 0x42 // Sent bigendian
        quint16 unusedi;          // 0x44 // Sent bigendian
        quint16 audioport;        // 0x46 // Sent bigendian
        char unusedj[7];          // 0x49
         */
        public static boolean isStatusPacket(byte[] data) {
            return (data.length == STATUS_SIZE && readIntBigEndianData(data, 0) == STATUS_SIZE);
        }

        public static boolean getAuthOK(byte[] data) {
            return readIntBigEndianData(data, 0x30) == 0;//0x30 error=0,failed error=0xffffffff
        }

        public static boolean getIsConnected(byte[] data) {
            return data[0x40] == 0x00;
        }

        public static int getRigCivPort(byte[] data) {
            return readShortData(data, 0x42) & 0xffff;
        }

        public static int getRigAudioPort(byte[] data) {
            return readShortData(data, 0x46) & 0xffff;
        }

    }


    /**
     * 0x90包。电台回复，或APP回复连接参数包.
     */
    public static class ConnInfoPacket {
        /*
        quint32 len;              // 0x00
        quint16 type;             // 0x04
        quint16 seq;              // 0x06
        quint32 sentid;           // 0x08
        quint32 rcvdid;           // 0x0c
        char unuseda[2];          // 0x10
        quint16 payloadsize;      // 0x12
        quint8 requestreply;      // 0x13
        quint8 requesttype;       // 0x14
        quint16 innerseq;         // 0x16
        char unusedb[2];          // 0x18
        quint16 tokrequest;       // 0x1a 主机的TOKEN
        quint32 token;            // 0x1c 电台的TOKEN
        union {
            struct {
                quint16 authstartid;    // 0x20
                char unusedg[5];        // 0x22
                quint16 commoncap;      // 0x27 当commonCap==0x1080时，用mac地址标识电台，否则就是16字节的GUID
                char unusedh;           // 0x29
                quint8 macaddress[6];     // 0x2a
            };
            quint8 guid[GUIDLEN];                  // 0x20
        };
        char unusedab[16];        // 0x30
        char name[32];                  // 0x40
        union { // 此部分有两种类型：发送和接收
            struct { // 接收到数据的结构
                quint32 busy;            // 0x60
                char computer[16];        // 0x64
                char unusedi[16];         // 0x74
                quint32 ipaddress;        // 0x84
                char unusedj[8];          // 0x78
            };
            struct { // 发送的数据结构，用于告知电台以下参数
                char username[16];    // 0x60 用户名的密文
                char rxenable;        // 0x70 可以接收=0x01
                char txenable;        // 0x71 可以发射=0x01
                char rxcodec;         // 0x72 接收的编码方式，IcomCodecType,0x04,LPcm 16Bit 1ch,
                char txcodec;         // 0x73 发射的编码方式，IcomCodecType,0x04,LPcm 16Bit 1ch,
                quint32 rxsample;     // 0x74 接收采样率
                quint32 txsample;     // 0x78 发射采样率
                quint32 civport;      // 0x7c 主机端CI-V端口
                quint32 audioport;    // 0x80 主机端音频端口
                quint32 txbuffer;     // 0x84 发送缓冲区，不清楚时什么单位wfView的值时0x96
                quint8 convert;      // 0x88
                char unusedl[7];      // 0x89

         */

        public static boolean getBusy(byte[] data) {
            return data[0x60] == 0x00;
        }

        public static byte[] getMacAddress(byte[] data) {
            byte[] mac = new byte[6];
            System.arraycopy(data, 0x2a, mac, 0, 6);//macAddress
            return mac;
        }

        public static String getRigName(byte[] data) {
            byte[] name = new byte[32];
            System.arraycopy(data, 0x40, name, 0, 32);//rig Name
            return new String(name).trim();
        }

        public static byte[] connectRequestPacket(short seq, int localSID, int remoteSID
                , byte requestReply, byte requestType
                , short authInnerSendSeq, short tokRequest, int token
                , byte[] macAddress
                , String rigName, String userName, int sampleRate
                , int civPort, int audioPort, int txBufferSize) {
            byte[] packet = new byte[CONNINFO_SIZE];
            System.arraycopy(intToBigEndian(CONNINFO_SIZE), 0, packet, 0, 4);//len
            System.arraycopy(shortToBigEndian((short) 0), 0, packet, 4, 2);  //type=0
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);        //seq
            System.arraycopy(intToBigEndian(localSID), 0, packet, 8, 4);     //localID
            System.arraycopy(intToBigEndian(remoteSID), 0, packet, 12, 4);   //remoteID
            //System.arraycopy({0x00,0x00}, 0, packet, 16, 2);                  //unuseda byte[2]
            System.arraycopy(shortToByte((short) (CONNINFO_SIZE - 0x10))
                    , 0, packet, 18, 2);//payloadsize
            packet[20] = requestReply;//0x01;//requestReply;
            packet[21] = requestType;//requestType;应当是0x03,回复电台我需要的参数。
            System.arraycopy(shortToByte(authInnerSendSeq), 0, packet, 22, 2);//innerSeq
            //System.arraycopy(unusedB, 0, packet, 24, 2);//unusedb
            System.arraycopy(shortToByte(tokRequest), 0, packet, 26, 2);//tokRequest主机的TOKEN
            System.arraycopy(intToByte(token), 0, packet, 28, 4);//电台的TOKEN
            packet[0x26] = 0x10;
            packet[0x27] = (byte) 0x80;//这两个字节是commCap字段，默认设成0x1080
            System.arraycopy(macAddress, 0, packet, 0x28, 6);//macAddress

            System.arraycopy(stringToByte(rigName, 32), 0, packet, 64, 32);//电台名称
            System.arraycopy(passCode(userName), 0, packet, 96, 16);//用户名密文
            packet[0x70] = 0x01;//rxEnable，支持接收
            packet[0x71] = 0x01;//txEnable，支持发射
            packet[0x72] = IcomCodecType.LPCM_1CH_16BIT;//rxCodec,0x04:LPcm 16Bit 1ch,0x02:LPcm 8Bit 1ch
            packet[0x73] = IcomCodecType.LPCM_1CH_16BIT;//txCodec,0x04:LPcm 16Bit 1ch,0x02:LPcm 8Bit 1ch
            System.arraycopy(intToByte(sampleRate), 0, packet, 0x74, 4);//rxSampleRate，采样率
            System.arraycopy(intToByte(sampleRate), 0, packet, 0x78, 4);//txSampleRate，采样率
            System.arraycopy(intToByte(civPort), 0, packet, 0x7c, 4);//civPort，本地CI-V端口
            System.arraycopy(intToByte(audioPort), 0, packet, 0x80, 4);//audioPort，本地音频端口
            System.arraycopy(intToByte(txBufferSize), 0, packet, 0x84, 4);//txBuffer，发送缓冲区
            packet[0x88] = 0x01;//convert

            return packet;
        }

        public static byte[] connInfoPacketData(byte[] rigData,
                                                short seq, int localSID, int remoteSID
                , byte requestReply, byte requestType
                , short authInnerSendSeq, short tokRequest, int token
                , String rigName, String userName, int sampleRate
                , int civPort, int audioPort, int txBufferSize) {
            byte[] packet = new byte[CONNINFO_SIZE];
            System.arraycopy(intToBigEndian(CONNINFO_SIZE), 0, packet, 0, 4);//len
            System.arraycopy(shortToBigEndian((short) 0), 0, packet, 4, 2);  //type=0
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);        //seq
            System.arraycopy(intToBigEndian(localSID), 0, packet, 8, 4);     //localID
            System.arraycopy(intToBigEndian(remoteSID), 0, packet, 12, 4);   //remoteID
            //System.arraycopy({0x00,0x00}, 0, packet, 16, 2);                  //unuseda byte[2]
            System.arraycopy(shortToByte((short) (CONNINFO_SIZE - 0x10))
                    , 0, packet, 18, 2);//payloadsize
            packet[20] = requestReply;//0x01;//requestReply;
            packet[21] = requestType;//requestType;应当是0x03,回复电台我需要的参数。
            System.arraycopy(shortToByte(authInnerSendSeq), 0, packet, 22, 2);//innerSeq
            //System.arraycopy(unusedB, 0, packet, 24, 2);//unusedb
            System.arraycopy(shortToByte(tokRequest), 0, packet, 26, 2);//tokRequest主机的TOKEN
            System.arraycopy(intToByte(token), 0, packet, 28, 4);//电台的TOKEN
            //把电台发送的数据复制回去：
            //00 00 00 00 00 00 00 10 80 00 00 90 c7 0f b6 ed
            //autID|unusedG       |comcp|uH|macAddress       |
            //00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            //|unusedB 共16字节                               |
            System.arraycopy(rigData, 32, packet, 32, 32);

            System.arraycopy(stringToByte(rigName, 32), 0, packet, 64, 32);//电台名称
            System.arraycopy(passCode(userName), 0, packet, 96, 16);//用户名密文
            packet[0x70] = 0x01;//rxEnable，支持接收
            packet[0x71] = 0x01;//txEnable，支持发射
            packet[0x72] = IcomCodecType.LPCM_1CH_16BIT;//rxCodec,0x04:LPcm 16Bit 1ch,0x02:LPcm 8Bit 1ch
            packet[0x73] = IcomCodecType.LPCM_1CH_16BIT;//txCodec,0x04:LPcm 16Bit 1ch,0x02:LPcm 8Bit 1ch
            System.arraycopy(intToByte(sampleRate), 0, packet, 0x74, 4);//rxSampleRate，采样率
            System.arraycopy(intToByte(sampleRate), 0, packet, 0x78, 4);//txSampleRate，采样率
            System.arraycopy(intToByte(civPort), 0, packet, 0x7c, 4);//civPort，本地CI-V端口
            System.arraycopy(intToByte(audioPort), 0, packet, 0x80, 4);//audioPort，本地音频端口
            System.arraycopy(intToByte(txBufferSize), 0, packet, 0x84, 4);//txBuffer，发送缓冲区
            packet[0x88] = 0x01;//convert

            return packet;
        }
    }


    /**
     * 0xA8包，电台回复电台的参数信息，0x42+0x66，0x42是capabilites头，0x66是radioCap
     */
    public static class CapCapabilitiesPacket {
        /**
         * 查radioCap的数量
         *
         * @param data 数据包
         * @return 数量
         */
        public static int getRadioPacketCount(byte[] data) {
            int count;
            count = (data.length - CAPABILITIES_SIZE) / RADIO_CAP_SIZE;
            if (count > 0) {
                return count;
            } else {
                return count;
            }
        }

        /**
         * 获取radioCap数据包，也许radioCap的数量不止一个，所以要用index来指定哪一个数据包
         *
         * @param data  数据包
         * @param index 数据包的索引，以0为起点
         * @return radioCap数据包
         */
        public static byte[] getRadioCapPacket(byte[] data, int index) {
            if (data.length < (CAPABILITIES_SIZE + RADIO_CAP_SIZE * (index + 1)))
                return null;//如果小于最小长度，就返回空
            byte[] packet = new byte[0x66];
            System.arraycopy(data, CAPABILITIES_SIZE + RADIO_CAP_SIZE * index, packet, 0, RADIO_CAP_SIZE);
            return packet;
        }
    }

    /**
     * 电台参数数据包（0x66长度），它是在Capabilities（0x42长度）包的后面，如果是一个，总数据包的长度是0xA8，
     */
    public static class RadioCapPacket {
        /*
         union {
            struct {
                quint8 unusede[7];          // 0x00
                quint16 commoncap;          // 0x07
                quint8 unused;              // 0x09
                quint8 macaddress[6];       // 0x0a
            };
            quint8 guid[GUIDLEN];           // 0x0
        };
        char name[32];            // 0x10
        char audio[32];           // 0x30
        quint16 conntype;         // 0x50
        char civ;                 // 0x52
        quint16 rxsample;         // 0x53
        quint16 txsample;         // 0x55
        quint8 enablea;           // 0x57
        quint8 enableb;           // 0x58
        quint8 enablec;           // 0x59
        quint32 baudrate;         // 0x5a
        quint16 capf;             // 0x5e
        char unusedi;             // 0x60
        quint16 capg;             // 0x61
        char unusedj[3];          // 0x63
         */

        /**
         * 获取电台名称
         *
         * @param data 0x66数据包
         * @return 名称
         */
        public static String getRigName(byte[] data) {
            byte[] rigName = new byte[32];
            System.arraycopy(data, 0x10, rigName, 0, 32);
            return new String(rigName).trim();
        }

        public static String getAudioName(byte[] data) {
            byte[] audioName = new byte[32];
            System.arraycopy(data, 0x30, audioName, 0, 32);
            return new String(audioName).trim();
        }

        public static byte getCivAddress(byte[] data) {
            return data[0x52];
        }

        public static short getRxSupportSample(byte[] data) {
            return readShortData(data, 0x53);
        }

        public static short getTxSupportSample(byte[] data) {
            return readShortData(data, 0x55);
        }

        public static boolean getSupportTX(byte[] data) {
            return data[0x57] == 0x01;
        }

    }

    /**
     * TOKEN(0x40)数据包。
     * 发送时，requestType=0x02是令牌确认，发送0x01是删除令牌。
     * 接收时，requestType=0x05，且requestReply=0x02&&type=0x01说明是电台令牌续订操作，这时要判断response的值。
     * response=0x00 00 00 00说明时续订成功，response=0xff ff ff ff说明时拒绝续订，此时应记录下RemoteId、
     * Token,tokRequest的值，关闭各端口，重新开始登录操作
     */
    public static class TokenPacket {
        /*
        public int len = TOKEN_SIZE;    // 0x00 (int32)
        public short type;              // 0x04(int16)
        public short seq;               // 0x06(int16)
        public int sentId;              // 0x08(int32) MyID,与本地IP地址、端口有关
        public int rcvdId;              // 0x0c(int32)
        public byte[] unusedA = new byte[2];       // 0x10 char[2]//可能是用于指令的序号
        public short payloadSize = TOKEN_SIZE - 0x10;// 0x12(int16) 负载长度，是包长度-包头16字节
        public byte requestReply;        // 0x14(int8)
        public byte requestType;         // 0x15(int8)
        public short innerSeq;           // 0x16(int16)
        public byte[] unusedB = new byte[2];// 0x18(char[2]
        public short tokRequest;         // 0x1a(int16)
        public int token;                // 0x1c(int32)
        public short tokRequest;        // 0x20
        public byte[] unusedG = new byte[5];  // 0x22
        public short commonCap;//0x27
        public byte unusedH;//0x29
        public byte[] macAddress=new byte[6];//0x2a
        public byte[] guid=new byte[16];//0x20
        public int response;//0x30
        public byte[] unusedE=new byte[12];//0x34
         */

        /**
         * 生成Token数据包
         *
         * @param seq              序列号
         * @param localSID         主机的ID
         * @param remoteSID        电台的ID
         * @param requestType      发送0x02是令牌确认，发送0x01是删除令牌。如果接收0x05，且requestReply=0x02&&type=0x01
         *                         说明是电台令牌续订成功
         * @param authInnerSendSeq 内部序列号
         * @param tokRequest       主机的TOKEN
         * @param token            电台的TOKEN
         * @return 数组
         */
        public static byte[] getTokenPacketData(
                short seq, int localSID, int remoteSID, byte requestType
                , short authInnerSendSeq, short tokRequest, int token) {
            byte[] packet = new byte[TOKEN_SIZE];
            System.arraycopy(intToBigEndian(TOKEN_SIZE), 0, packet, 0, 4); //len  int32 0x00
            //System.arraycopy(shortToBigEndian((short) 0), 0, packet, 4, 2);//type int16 0x04
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);      //seq  int16 0x06
            System.arraycopy(intToBigEndian(localSID), 0, packet, 8, 4);   //localId  int32 0x08
            System.arraycopy(intToBigEndian(remoteSID), 0, packet, 12, 4); //remoteId int32 0x0c
            //System.arraycopy({0x00,0x00}, 0, packet, 16, 2);               //unusedA byte[2] 0x10
            System.arraycopy(shortToByte((short) (TOKEN_SIZE - 0x10))
                    , 0, packet, 18, 2);                         //payloadSize int16  0x12
            packet[20] = 0x01;        //requestReply byte 0x14
            packet[21] = requestType; //requestType  byte 0x15
            System.arraycopy(shortToByte(authInnerSendSeq), 0, packet, 22, 2);//innerSeq int16 0x16
            //System.arraycopy(unusedB, 0, packet, 24, 2);                            //unusedB byte[2] 0x18
            System.arraycopy(shortToByte(tokRequest), 0, packet, 26, 2);//tokRequest int16 0x1a
            System.arraycopy(intToByte(token), 0, packet, 28, 4);      //token int32 0x1c
            //后面其余的部分为用0填充
            return packet;
        }

        public static byte[] getRenewalPacketData(
                short seq, int localSID, int remoteSID
                , short authInnerSendSeq, short tokRequest, int token
        ) {
            return getTokenPacketData(seq, localSID, remoteSID, TOKEN_TYPE_RENEWAL, authInnerSendSeq, tokRequest, token);
        }

        /**
         * 检查是不是令牌续订成功，条件：requestType=0x05，且requestReply=0x02&&type=0x01&&response=0x00
         *
         * @param data 数据包
         * @return 是否续订成功
         */
        public static boolean TokenRenewalOK(byte[] data) {
            byte requestType = data[0x15];
            byte requestReply = data[0x14];
            short type = readShortBigEndianData(data, 0x04);
            int response = readIntData(data, 0x30);
            return requestType == 0x05 && requestReply == 0x02 && type == 0x01 && response == 0x00;
        }

        public static byte getRequestType(byte[] data) {
            return data[21];//0x15
        }

        public static byte getRequestReply(byte[] data) {
            return data[20];//0x14
        }

        public static int getResponse(byte[] data) {
            return readIntData(data, 0x30);
        }

        public static short getTokRequest(byte[] data) {
            return readShortData(data, 26);//0x1a
        }

        public static int getToken(byte[] data) {
            return readIntData(data, 28);//0x1c
        }
    }


    /**
     * 登录回复包，0x60包，长度96；
     */
    public static class LoginResponsePacket {
        /*
        public int len;// 0x00 (int32)
        public short type;              // 0x04(int16)
        public short seq;               // 0x06(int16)
        public int sentId;              // 0x08(int32) MyID,与本地IP地址、端口有关
        public int rcvdId;              // 0x0c(int32)
        public byte[] unusedA = new byte[2];       // 0x10 char[2]//可能是用于指令的序号
        public short payloadSize;// 0x12(int16) 负载长度，是包长度-包头16字节
        public byte requestReply;        // 0x14(int8)
        public byte requestType;         // 0x15(int8)
        public short innerSeq;           // 0x16(int16)
        public byte[] unusedB = new byte[2];// 0x18(char[2]
        public short tokRequest;         // 0x1a(int16)
        public int token;                // 0x1c(int32)
        public short authStartId;        // 0x20
        public byte[] unusedD = new byte[14];  // 0x22
        public int error;              // 0x30
        public byte[] unusedE = new byte[12];     // 0x34
        public byte[] connection = new byte[16];  // 0x40
        public byte[] unusedF = new byte[16];     // 0x50
         */

        public static boolean authIsOK(byte[] data) {
            return readIntData(data, 0x30) == 0x00;
        }

        public static int errorNum(byte[] data) {
            return readIntData(data, 0x30);
        }

        public static int getToken(byte[] data) {
            return readIntData(data, 0x1c);
        }

        public static String getConnection(byte[] data) {
            byte[] connection = new byte[16];
            System.arraycopy(data, 0x40, connection, 0, 16);
            return new String(connection).trim();
        }

    }


    /**
     * 登录用数据包,0x80包，长度128
     */
    public static class LoginPacket {
        /*
        public int len = LOGIN_SIZE;    // 0x00 (int32)
        public short type;              // 0x04(int16)
        public short seq;               // 0x06(int16)
        public int sentId;              // 0x08(int32) MyID,与本地IP地址、端口有关
        public int rcvdId;              // 0x0c(int32)
        public byte[] unusedA = new byte[2];       // 0x10 char[2]//可能是用于指令的序号
        public short payloadSize = LOGIN_SIZE - 0x10;// 0x12(int16) 负载长度，是包长度-包头16字节
        public byte requestReply;        // 0x14(int8)
        public byte requestType;         // 0x15(int8)
        public short innerSeq;           // 0x16(int16)
        public byte[] unusedB = new byte[2];// 0x18(char[2]
        public short tokRequest;         // 0x1a(int16)
        public int token;                // 0x1c(int32)
        public byte[] unusedC = new byte[32];    // 0x20(char[32])
        private byte[] userName = new byte[16];  // 0x40(char[16])
        private byte[] password = new byte[16];  // 0x50(char[16])
        private final byte[] name = new byte[16];// 0x60(char[16])
        public byte[] unusedF = new byte[16];    // 0x70(char[16])
         */


        /**
         * 生成Login（0x80）数据包
         *
         * @param seq              序号
         * @param localSID         主机ID
         * @param remoteSID        电台ID
         * @param authInnerSendSeq 内部序号
         * @param tokRequest       我的TOKEN
         * @param userName         用户名（明文）
         * @param password         密码（明文）
         * @param name             终端的名称
         * @return 数据包
         */
        public static byte[] loginPacketData(short seq, int localSID, int remoteSID, short authInnerSendSeq
                , short tokRequest, int token, String userName, String password, String name) {
            byte[] packet = new byte[LOGIN_SIZE];
            System.arraycopy(intToBigEndian(LOGIN_SIZE), 0, packet, 0, 4); //len int32     0x00
            System.arraycopy(shortToBigEndian((short) 0), 0, packet, 4, 2);//type int16    0x04
            System.arraycopy(shortToBigEndian(seq), 0, packet, 6, 2);      //seq int 16    0x06
            System.arraycopy(intToBigEndian(localSID), 0, packet, 8, 4);   //localId int32 0x08
            System.arraycopy(intToBigEndian(remoteSID), 0, packet, 12, 4); //remoteId int32  0x0c
            //System.arraycopy({0x00,0x00}, 0, packet, 16, 2);                      //unusedA byte[2] 0x10
            System.arraycopy(shortToByte((short) (LOGIN_SIZE - 0x10))
                    , 0, packet, 18, 2);//payloadSize,int16,大端模式，是包长度-包头16字节  0x12
            packet[20] = 0x01;//requestReply byte 0x14
            packet[21] = 0x00;//requestType  byte 0x15
            System.arraycopy(shortToByte(authInnerSendSeq), 0, packet, 22, 2);//innerSeq int16 x016
            //System.arraycopy(unusedB, 0, packet, 24, 2);  //unusedB byte[2] 0x18
            System.arraycopy(shortToByte(tokRequest), 0, packet, 26, 2);//tokRequest int16 0x1a,我的token
            System.arraycopy(intToByte(token), 0, packet, 28, 4);//token int32 0x1c,电台的token
            //System.arraycopy(unusedC, 0, packet, 32, 32);               //unusedC byte[32] 0x0x20
            System.arraycopy(passCode(userName), 0, packet, 64, 16);//userName byte[16] 0x40,用户名密文
            System.arraycopy(passCode(password), 0, packet, 80, 16);//password byte[16] 0x50,密码密文
            System.arraycopy(stringToByte(name, 16), 0, packet, 96, 16);//name byte[16] 0x60,主机的名称
            //System.arraycopy(unusedF, 0, packet, 112, 16);                         //unusedF byte[16] 0x70
            return packet;
        }

    }


    /**
     * 用户名、密码加密算法，最长只支持到16位。
     *
     * @param pass 用户名或密码
     * @return 密文，最长16位
     */
    public static byte[] passCode(String pass) {
        byte[] sequence =//字典
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0x47, 0x5d, 0x4c, 0x42, 0x66, 0x20, 0x23, 0x46, 0x4e, 0x57, 0x45, 0x3d, 0x67, 0x76, 0x60, 0x41, 0x62, 0x39, 0x59, 0x2d, 0x68, 0x7e,
                        0x7c, 0x65, 0x7d, 0x49, 0x29, 0x72, 0x73, 0x78, 0x21, 0x6e, 0x5a, 0x5e, 0x4a, 0x3e, 0x71, 0x2c, 0x2a, 0x54, 0x3c, 0x3a, 0x63, 0x4f,
                        0x43, 0x75, 0x27, 0x79, 0x5b, 0x35, 0x70, 0x48, 0x6b, 0x56, 0x6f, 0x34, 0x32, 0x6c, 0x30, 0x61, 0x6d, 0x7b, 0x2f, 0x4b, 0x64, 0x38,
                        0x2b, 0x2e, 0x50, 0x40, 0x3f, 0x55, 0x33, 0x37, 0x25, 0x77, 0x24, 0x26, 0x74, 0x6a, 0x28, 0x53, 0x4d, 0x69, 0x22, 0x5c, 0x44, 0x31,
                        0x36, 0x58, 0x3b, 0x7a, 0x51, 0x5f, 0x52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0

                };

        byte[] passBytes = pass.getBytes();
        byte[] enCodePass = new byte[16];//密文不能超过16字节

        for (int i = 0; i < passBytes.length && i < 16; i++) {
            int p = (passBytes[i] + i) & 0xff;//防止出现负数
            if (p > 126) {
                p = 32 + p % 127;
            }
            enCodePass[i] = sequence[p];
        }
        return enCodePass;
    }

    /**
     * 把字符串转指定长度的byte数组
     *
     * @param s   字符串
     * @param len 长度
     * @return 数组
     */
    public static byte[] stringToByte(String s, int len) {
        byte[] buf = new byte[len];
        byte[] temp = s.getBytes();
        for (int i = 0; i < temp.length; i++) {
            if (i > len) break;
            buf[i] = temp[i];
        }
        return buf;
    }

    /**
     * 把int转为小端模式,(高位在结尾)
     *
     * @param n int
     * @return 数组
     */
    public static byte[] intToBigEndian(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * int32转换成4字节数组
     *
     * @param n int32
     * @return 数组
     */
    public static byte[] intToByte(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * 从流数据中读取小端模式的Int32
     *
     * @param data  流数据
     * @param start 起始点
     * @return Int32
     */
    public static int readIntBigEndianData(byte[] data, int start) {
        if (data.length - start < 4) return 0;
        return (int) data[start] & 0xff
                | ((int) data[start + 1] & 0xff) << 8
                | ((int) data[start + 2] & 0xff) << 16
                | ((int) data[start + 3] & 0xff) << 24;
    }

    public static int readIntData(byte[] data, int start) {
        if (data.length - start < 4) return 0;
        return (int) data[start + 3] & 0xff
                | ((int) data[start + 2] & 0xff) << 8
                | ((int) data[start + 1] & 0xff) << 16
                | ((int) data[start] & 0xff) << 24;
    }

    /**
     * 从流数据中读取小端模式的Short
     *
     * @param data  流数据
     * @param start 起始点
     * @return Int16
     */
    public static short readShortBigEndianData(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start] & 0xff
                | ((short) data[start + 1] & 0xff) << 8);
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

    /**
     * 把short转为小端模式,(高位在结尾)
     *
     * @param n short
     * @return 数组
     */
    public static byte[] shortToBigEndian(short n) {
        byte[] b = new byte[2];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        return b;
    }

    /**
     * 把short转成byte数组
     *
     * @param n short
     * @return 数组
     */
    public static byte[] shortToByte(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) (n >> 8 & 0xff);
        return b;
    }

    /**
     * 显示16进制内容
     *
     * @param data 数组
     * @return 内容
     */
    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
}
