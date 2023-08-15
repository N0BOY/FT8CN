package com.bg7yoz.ft8cn.flex;
/**
 * Flex的操作，命令使用TCP，数据流使用UDP。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;


public class FlexRadio {

    public enum FlexMode {LSB, USB, AM, CW, DIGL, DIGU, SAM, FM, NFM, DFM, RTTY, RAW, ARQ, UNKNOW}

    public enum AntMode {ANT1, ANT2, RX_A, XVTA, UNKNOW}


    private static final String TAG = "FlexRadio";
    public static int streamPort = 7051;
    private int flexStreamPort = 4993;
    public boolean isPttOn = false;
    public long streamTxId = 0x084000000;

    public static int getStreamPort() {//获取用于流传输的UDP端口，防止重复，采用自增方式
        return ++streamPort;
    }

    //private int streamPort;//当前用于流传输的UDP端口，这个是本实例的端口


    /*********************
     * 电台的基本信息，从discovery协议中获取
     *************************/
    private String discovery_protocol_version;//=3.0.0.2
    private String model;//=FLEX-6400
    private String serial;//=1418-6579-6400-0461
    private String version;//=3.3.32.8203
    private String nickname;//=FlexRADIO
    private String callsign;//=FlexRADIO
    private String ip = "";//=192.168.3.86
    private int port = 4992;//=4992//用于控制电台的TCP端口
    private String status;//=Available
    private String inUse_ip;//=192.168.3.5
    private String inUse_host;//=DESKTOP-RR564NK.local
    private String max_licensed_version;//=v3
    private String radio_license_id;//=00-1C-2D-05-04-70
    private String requires_additional_license;//=0
    private String fpc_mac;//=
    private int wan_connected;//=1
    private int licensed_clients;//=2
    private int available_clients;//=1
    private int max_panadapters;//=2
    private int available_panadapters;//=1
    private int max_slices;//=2
    private int available_slices;//=1
    private String gui_client_ips;//=192.168.3.5
    private String gui_client_hosts;//=DESKTOP-RR564NK.local
    private String gui_client_programs;//=SmartSDR-Win
    private String gui_client_stations;//=DESKTOP-RR564NK
    private String gui_client_handles;//=0x19EAFA02

    private long lastSeen;//最后一次消息的时间
    private boolean isAvailable = true;//电台是不是有效


    private int commandSeq = 1;//指令的序列
    private FlexCommand flexCommand;
    private int handle = 0;
    private String commandStr;


    private final StringBuilder buffer = new StringBuilder();//指令的缓存
    private final RadioTcpClient tcpClient = new RadioTcpClient();
    private RadioUdpClient streamClient;

    private boolean allFlexRadioStatusEvent = false;
    private String clientID = "";
    private long daxAudioStreamId = 0;
    private long daxTxAudioStreamId = 0;
    private long panadapterStreamId = 0;
    private final HashSet<Long> streamIdSet = new HashSet<>();

    //************************事件处理接口*******************************
    private OnReceiveDataListener onReceiveDataListener;//当前接收到的数据事件
    private OnTcpConnectStatus onTcpConnectStatus;//当TCP连接状态变化的事件
    private OnReceiveStreamData onReceiveStreamData;//当接收到流数据后的处理事件
    private OnCommandListener onCommandListener;//触发命令事件
    private OnMessageListener onMessageListener;//触发消息事件
    private OnStatusListener onStatusListener;//触发状态事件
    //*****************************************************************
    private AudioTrack audioTrack = null;

    public FlexRadio() {
        updateLastSeen();
    }

    public FlexRadio(String discoverStr) {
        update(discoverStr);
        updateLastSeen();
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * 到参数列表中找指定的字符类型参数
     *
     * @param parameters 参数列表
     * @param prefix     参数名前缀
     * @return 参数
     */
    private String getParameterStr(String[] parameters, String prefix) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].toLowerCase().startsWith(prefix.toLowerCase() + "=")) {
                return parameters[i].substring(prefix.length() + 1);
            }
        }
        //如果没找到，返回空字符串
        return "";

    }

    /**
     * 到参数列表中找指定的int类型参数
     *
     * @param parameters 参数列表
     * @param prefix     参数名前缀
     * @return 参数
     */
    private int getParameterInt(String[] parameters, String prefix) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].toLowerCase().startsWith(prefix.toLowerCase() + "=")) {
                try {
                    return Integer.parseInt(parameters[i].substring(prefix.length() + 1));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Log.e(TAG, "getParameterInt exception: " + e.getMessage());
                    return 0;
                }
            }
        }
        //如果没找到，返回0
        return 0;

    }

    /**
     * 从discovery协议中更新参数
     *
     * @param discoverStr 参数
     */
    public void update(String discoverStr) {
        String[] paras = discoverStr.split(" ");
        discovery_protocol_version = getParameterStr(paras, "discovery_protocol_version");
        model = getParameterStr(paras, "model");
        serial = getParameterStr(paras, "serial");
        version = getParameterStr(paras, "version");
        nickname = getParameterStr(paras, "nickname");
        callsign = getParameterStr(paras, "callsign");
        ip = getParameterStr(paras, "ip");
        port = getParameterInt(paras, "port");
        status = getParameterStr(paras, "status");
        inUse_ip = getParameterStr(paras, "inUse_ip");
        inUse_host = getParameterStr(paras, "inUse_host");
        max_licensed_version = getParameterStr(paras, "max_licensed_version");
        radio_license_id = getParameterStr(paras, "radio_license_id");
        requires_additional_license = getParameterStr(paras, "requires_additional_license");
        fpc_mac = getParameterStr(paras, "fpc_mac");
        wan_connected = getParameterInt(paras, "wan_connected");
        licensed_clients = getParameterInt(paras, "licensed_clients");
        available_clients = getParameterInt(paras, "available_clients");
        max_panadapters = getParameterInt(paras, "max_panadapters");
        available_panadapters = getParameterInt(paras, "available_panadapters");
        max_slices = getParameterInt(paras, "max_slices");
        available_slices = getParameterInt(paras, "available_slices");
        gui_client_ips = getParameterStr(paras, "gui_client_ips");
        gui_client_hosts = getParameterStr(paras, "gui_client_hosts");
        gui_client_programs = getParameterStr(paras, "gui_client_programs");
        gui_client_stations = getParameterStr(paras, "gui_client_stations");
        gui_client_handles = getParameterStr(paras, "gui_client_handles");
    }

    /**
     * 检查这个实例是否是同一个电台
     *
     * @param serialNum 电台序列号
     * @return 是/否
     */
    public boolean isEqual(String serialNum) {
        return this.serial.equalsIgnoreCase(serialNum);
    }


    /**
     * 连接到控制电台
     */
    public void connect() {
        this.connect(this.ip, this.port);
    }

    /**
     * 连接控制到电台，TCP
     *
     * @param ip   地址
     * @param port 端口
     */
    public void connect(String ip, int port) {
        if (tcpClient.isConnect()) {
            tcpClient.disconnect();
        }
        //Tcp连接触发的事件
        tcpClient.setOnDataReceiveListener(new RadioTcpClient.OnDataReceiveListener() {
            @Override
            public void onConnectSuccess() {
                if (onTcpConnectStatus != null) {
                    onTcpConnectStatus.onConnectSuccess(tcpClient);
                }
            }

            @Override
            public void onConnectFail() {
                if (onTcpConnectStatus != null) {
                    onTcpConnectStatus.onConnectFail(tcpClient);
                }
            }

            @Override
            public void onDataReceive(byte[] buffer) {
                if (onReceiveDataListener != null) {
                    onReceiveDataListener.onDataReceive(buffer);
                }
                onReceiveData(buffer);
            }
        });
        clearBufferData();//清除一下缓存的指令数据
        tcpClient.connect(ip, port);//连接TCP

        //openStreamPort();//打开接收数据流的端口
    }

    /**
     * 当接收到音频数据时的处理
     *
     * @param data 音频数据
     */
    private void doReceiveAudio(byte[] data) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveAudio(data);
        }
        if (audioTrack != null) {//如果音频播放已经打开，就写音频流数据
            float[] sound = getFloatFromBytes(data);
            audioTrack.write(sound, 0, sound.length, AudioTrack.WRITE_NON_BLOCKING);
        }
    }

    /**
     * 当接收到IQ数据时的处理
     *
     * @param data 数据
     */
    private void doReceiveIQ(byte[] data) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveIQ(data);
        }
    }

    /**
     * 当接收到FFT数据时的处理
     *
     * @param vita 数据
     */
    private void doReceiveFFT(VITA vita) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveFFT(vita);
        }
    }

    /**
     * 当接收到仪表数据时的处理
     *
     * @param vita 数据
     */
    private void doReceiveMeter(VITA vita) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveMeter(vita);
        }
    }

    /**
     * 当接收到未知数据时的处理
     *
     * @param data 数据
     */
    private void doReceiveUnKnow(byte[] data) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveUnKnow(data);
        }
    }

    /**
     * 打开音频，流方式。当收到音频流的时候，播放数据
     */
    public void openAudio() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat myFormat = new AudioFormat.Builder().setSampleRate(24000)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                , 24000 * 4, AudioTrack.MODE_STREAM
                , mySession);
        audioTrack.play();
    }

    /**
     * 关闭音频
     */
    public void closeAudio() {
        if (audioTrack != null) {
            audioTrack.stop();
            //audioTrack.release();
            audioTrack = null;
        }
    }

    private synchronized void addStreamIdToSet(long streamId) {
        streamIdSet.add(streamId);
    }

    /**
     * 打开接收数据流的端口
     */
    public void openStreamPort() {
        if (streamClient != null) {
            if (streamClient.isActivated()) {
                try {
                    streamClient.setActivated(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }


        RadioUdpClient.OnUdpEvents onUdpEvents = new RadioUdpClient.OnUdpEvents() {
            @Override
            public void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data) {
                if (flexStreamPort != packet.getPort()) flexStreamPort = packet.getPort();

                VITA vita = new VITA(data);
                addStreamIdToSet(vita.streamId);

                //Log.e(TAG, String.format("OnReceiveData: stream id:0x%x,class id:0x%x",vita.streamId,vita.classId) );
                switch (vita.classId) {
                    case VITA.FLEX_DAX_AUDIO_CLASS_ID://音频数据
                        //Log.e(TAG, String.format("FLEX_DAX_AUDIO_CLASS_ID stream id:0x%x",vita.streamId ));
                        doReceiveAudio(vita.payload);
                        break;
                    case VITA.FLEX_DAX_IQ_CLASS_ID://IQ数据
                        doReceiveIQ(vita.payload);
                        break;
                    case VITA.FLEX_FFT_CLASS_ID://频谱数据
                        doReceiveFFT(vita);
                        //Log.e(TAG, String.format("OnReceiveData: FFT:%d,STREAM ID:0x%x",vita.payload.length,vita.streamId));
                        break;
                    case VITA.FLEX_METER_CLASS_ID://仪表数据
                        //Log.e(TAG, String.format("FLEX_METER_CLASS_ID: stream id:0x%x",vita.streamId ));
                        doReceiveMeter(vita);
                        //Log.e(TAG, String.format("OnReceiveData: METER class id:0x%x,stream id:0x%x,length:%d\n%s"
                        //        ,vita.classId,vita.streamId,vita.payload.length,vita.showPayload() ));
                        break;
                    default://未知类型的数据
                        doReceiveUnKnow(data);
                        break;
                }
            }
        };

        //此处要确定stream的udp端口
        streamPort = getStreamPort();
        streamClient = new RadioUdpClient(streamPort);
        streamClient.setOnUdpEvents(onUdpEvents);
        try {
            streamClient.setActivated(true);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: " + e.getMessage());
        }


    }

    /**
     * 关闭接收数据流的端口
     */
    public synchronized void closeStreamPort() {
        if (streamClient != null) {
            if (streamClient.isActivated()) {
                try {
                    streamClient.setActivated(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        streamClient = null;
    }

    /**
     * 断开与电台的连接
     */
    public synchronized void disConnect() {
        if (tcpClient.isConnect()) {
            tcpClient.disconnect();
        }
    }

    /**
     * flexRadio要把12000采样率改为24000采样率，还要把单声道改为立体声
     * @param data 音频
     */
    public void sendWaveData(float[] data) {
        float[] temp = new float[data.length * 2];
        for (int i = 0; i < data.length; i++) {//转成立体声,24000采样率
            temp[i * 2] = data[i];
            temp[i * 2 + 1] = data[i];
        }
        //port=4991;
        //streamTxId=0x084000001;
        //每5毫秒一个包？立体声，共256个float
        Log.e(TAG, String.format("sendWaveData: streamid:0x%x,ip:%s,port:%d",streamTxId,ip, port) );
        new Thread(new Runnable() {
            @Override
            public void run() {

                VITA vita = new VITA();

                int count = 0;
                int packetCount=0;
                while (count<temp.length){
                    long now = System.currentTimeMillis() - 1;//获取当前时间



                    float[] voice=new float[256];//因为是立体声，240*2


                    //for (int j = 0; j <3 ; j++) {
                        for (int i = 0; i < voice.length; i++) {
                            voice[i] = temp[count];
                            count++;
                            if (count > temp.length) break;
                        }

                        byte[] send = vita.audioDataToVita(packetCount, streamTxId, voice);
                        packetCount++;
                        try {
                            streamClient.sendData(send, ip, port);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        if (count>temp.length) break;
                    //}
                    while (isPttOn) {
                        if (System.currentTimeMillis() - now >= 5) {//5毫秒一个周期,每个周期256个float。
                            break;
                        }
                    }
                    if (!isPttOn){
                       // Log.e(TAG, String.format("count：%d,temp.length:%d",count,temp.length ));
                    }

                }


//                for (int i = 0; i < (temp.length / (24 * 2 * 40)); i++) {//40毫秒的数据量
//                    if (!isPttOn) return;
//                    long now = System.currentTimeMillis() - 1;//获取当前时间
//
//                    float[] voice = new float[24 * 2 * 10];
//                    for (int j = 0; j < 24 * 2 *10; j++) {
//                        voice[j] = temp[i * 24 * 2 * 10 + j];
//                    }
//                    //Log.e(TAG, "sendWaveData: "+floatToStr(voice) );
//                    //streamTxId=0x84000001;
//                    byte[] send = vita.audioDataToVita(count, streamTxId, voice);
//                    count++;
//
//                    try {
//                        streamClient.sendData(send, ip, port);
//                    } catch (UnknownHostException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    while (isPttOn) {
//                        if (System.currentTimeMillis() - now >= 41) {//40毫秒一个周期,每个周期3个包，每个包64个float。
//                            break;
//                        }
//                    }
//                }
            }
        }).start();


        //设置发送音频包
        //streamClient.sendData();
    }
    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
    @SuppressLint("DefaultLocale")
    public static String floatToStr(float[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%f ", data[i]));
        }
        return s.toString();
    }
    /**
     * 电台是否连接
     *
     * @return 是否
     */
    public boolean isConnect() {
        return tcpClient.isConnect();
    }

    public synchronized void sendData(byte[] data) {
        tcpClient.sendByte(data);
    }

    /**
     * 制作命令，命令序号规则：后3位是命令的种类，序号除1000，是命令的真正序号
     *
     * @param command    命令的种类
     * @param cmdContent 命令的具体内容
     */
    @SuppressLint("DefaultLocale")
    public void sendCommand(FlexCommand command, String cmdContent) {
        if (tcpClient.isConnect()) {
            commandSeq++;
            flexCommand = command;
            commandStr = String.format("C%d%03d|%s\n", commandSeq, command.ordinal()
                    , cmdContent);
            tcpClient.sendByte(commandStr.getBytes());
            Log.e(TAG, "sendCommand: " + commandStr);
        }
    }

    /**
     * 清空缓存数据
     */
    private void clearBufferData() {
        buffer.setLength(0);
    }

    /**
     * 当接收到数据时触发的事件，此处是TCP连接得到的数据
     *
     * @param data 数据
     */
    private void onReceiveData(byte[] data) {
        String s = new String(data);
        if (!s.contains("\n")) {//不包含换行符，说明命令行没有接受完。
            buffer.append(s);
        } else {//说明已经有命令行了。可能不止一个哦。在此部分要触发OnReceiveLine
            String[] commands = s.split("\n");
            if (commands.length > 0) {//把收到数据的第一行，追加到之前接收的命令数据上
                buffer.append(commands[0]);
            }

            //先把缓存中的数据触发出来
            doReceiveLineEvent(buffer.toString());
            clearBufferData();
            //从第二行开始触发，最后一行不触发，最后一行要看是不是换行结尾
            for (int i = 1; i < commands.length - 1; i++) {
                doReceiveLineEvent(commands[i]);
            }

            if (commands.length > 1) {//当数据是多行的时候，最后一行的处理
                if (s.endsWith("\n")) {//如果是以换行结尾,或者缓冲区没满（接收完全了），就触发事件
                    doReceiveLineEvent(commands[commands.length - 1]);
                } else {//如果不是以换行结尾，说明指令没有接收完全
                    buffer.append(commands[commands.length - 1]);
                }
            }
        }
    }

    /**
     * 当接收到数据行时，触发的事件。可以触发两种事件：
     * 1.行数据事件onReceiveLineListener；
     * 2.命令事件onCommandListener。
     *
     * @param line 数据行
     */
    private void doReceiveLineEvent(String line) {

        FlexResponse response = new FlexResponse(line);
        //更新一下句柄
        switch (response.responseStyle) {
            case VERSION:
                this.version = response.version;
                break;
            case HANDLE:
                this.handle = response.handle;
                break;
            case RESPONSE:
                if (response.daxStreamId != 0) {
                    this.daxAudioStreamId = response.daxStreamId;
                }
                if (response.panadapterStreamId != 0) {
                    this.panadapterStreamId = response.panadapterStreamId;
                }
                if (response.daxTxStreamId != 0) {
                    this.daxTxAudioStreamId = response.daxTxStreamId;
                    Log.e(TAG, String.format("doReceiveLineEvent: txStreamID:0x%x", daxTxAudioStreamId));
                }

                break;
        }

        if (response.responseStyle == FlexResponseStyle.RESPONSE) {
            if (getCommandStyleFromResponse(response) == FlexCommand.CLIENT_GUI) {
                setClientIDFromResponse(response);//设置CLIENT ID
            }
        }

        //是不是显示其它终端的状态信息
        if (response.responseStyle == FlexResponseStyle.STATUS) {
            if (!allFlexRadioStatusEvent && (!(handle == response.handle || response.handle == 0))) {
                return;
            }

        }

        switch (response.responseStyle) {
            case RESPONSE://当接收到的是指令的返回消息
                doCommandResponse(response);//对一些指令返回的消息要处理一下。
                break;
            case STATUS://当接收到的是状态消息
                if (onStatusListener != null) {
                    onStatusListener.onStatus(response);
                }
                break;
            case MESSAGE://当接收到的是消息
                if (onMessageListener != null) {
                    onMessageListener.onMessage(response);
                    break;
                }
        }
    }

    /**
     * 处理命令返回的消息，同时触发命令返回消息事件
     *
     * @param response 返回消息
     */
    private void doCommandResponse(FlexResponse response) {
        if (onCommandListener != null) {
            onCommandListener.onResponse(response);
        }
    }


    private void setClientIDFromResponse(FlexResponse response) {
        if (response.responseStyle != FlexResponseStyle.RESPONSE) return;
        if (getCommandStyleFromResponse(response) != FlexCommand.CLIENT_GUI) return;
        if (response.content.equals("0")) {//R3001|0|0BF06C76-EB9E-47E0-B570-EAFB7D556055
            String[] temp = response.rawData.split("\\|");
            if (temp.length < 3) return;
            clientID = temp[2];
        }
    }

    public FlexCommand getCommandStyleFromResponse(FlexResponse response) {
        if (response.responseStyle != FlexResponseStyle.RESPONSE) {
            return FlexCommand.UNKNOW;
        }
        //Log.e(TAG, "getCommandStyleFromResponse: "+response.rawData );

        try {
            return FlexCommand.values()[Integer.parseInt(response.head.substring(response.head.length() - 3))];
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "getCommandStyleFromResponse exception: " + e.getMessage());
        }
        return FlexCommand.UNKNOW;
    }

    /**
     * 检查是不是 刚刚 离线，离线条件：5秒内没有收到电台的广播数据包
     *
     * @return 是否
     */
    public boolean isInvalidNow() {
        if (isAvailable) {//如果标记在线，而大于5秒的时间没有收到数据包，就视为刚刚离线。
            isAvailable = System.currentTimeMillis() - lastSeen < 1000 * 5;//小于5秒，就视为在线
            return !isAvailable;
        } else {//如果已经标记不在线了，就不是刚刚离弦的。
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("FlexRadio{version='%s', handle=%X}", version, handle);
    }

    //**************封装FlexRadio各种指令*开始***********************
    public synchronized void commandClientDisconnect() {
        sendCommand(FlexCommand.CLIENT_DISCONNECT, "client disconnect");
    }

    public synchronized void commandClientGui() {
        sendCommand(FlexCommand.CLIENT_GUI, "client gui");
    }

    public synchronized void commandClientSetEnforceNetWorkGui() {
        sendCommand(FlexCommand.CLIENT_SET_ENFORCE_NETWORK
                , "client set enforce_network_mtu=1 network_mtu=1450");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceRemove(int sliceOder) {
        sendCommand(FlexCommand.SLICE_REMOVE, String.format("slice r %d", sliceOder));
    }

    public synchronized void commandSliceList() {
        sendCommand(FlexCommand.SLICE_LIST, "slice list");
    }

    public synchronized void commandSliceCreate() {
        sendCommand(FlexCommand.SLICE_CREATE_FREQ, "slice create");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceTune(int sliceOder, String freq) {
        sendCommand(FlexCommand.SLICE_TUNE, String.format("slice t %d %s", sliceOder, freq));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetRxAnt(int sliceOder, AntMode antMode) {
        sendCommand(FlexCommand.SLICE_SET_RX_ANT, String.format("slice s %d rxant=%s", sliceOder, antMode.toString()));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetTxAnt(int sliceOder, AntMode antMode) {
        sendCommand(FlexCommand.SLICE_SET_TX_ANT, String.format("slice s %d txant=%s", sliceOder, antMode.toString()));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetMode(int sliceOder, FlexMode mode) {
        sendCommand(FlexCommand.SLICE_SET_TX_ANT, String.format("slice s %d mode=%s", sliceOder, mode.toString()));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetNR(int sliceOder, boolean on) {
        sendCommand(FlexCommand.SLICE_SET_NR, String.format("slice s %d nr=%s", sliceOder, on ? "on" : "off"));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceGetError(int sliceOder) {
        sendCommand(FlexCommand.SLICE_GET_ERROR, String.format("slice get_error %d", sliceOder));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetNB(int sliceOder, boolean on) {
        sendCommand(FlexCommand.SLICE_SET_NB, String.format("slice s %d nb=%s", sliceOder, on ? "on" : "off"));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetDaxAudio(int channel, int sliceOder, boolean txEnable) {
        sendCommand(FlexCommand.DAX_AUDIO, String.format("dax audio set %d slice=%d tx=%s", channel, sliceOder, txEnable ? "1" : "0"));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetDaxIQ(int channel, int panadapter, int rate) {
        sendCommand(FlexCommand.DAX_IQ, String.format("dax iq set %d pan=%d rat=%d", channel, panadapter, rate));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandUdpPort() {
        sendCommand(FlexCommand.CLIENT_UDPPORT, String.format("client udpport %d", streamPort));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandStreamCreateDaxRx(int channel) {
        sendCommand(FlexCommand.STREAM_CREATE_DAX_RX, String.format("stream create type=dax_rx dax_channel=%d", channel));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandStreamCreateDaxTx(int channel) {
        //sendCommand(FlexCommand.STREAM_CREATE_DAX_TX, String.format("stream create type=dax_tx dax_channel=%d", channel));
//        sendCommand(FlexCommand.STREAM_CREATE_DAX_TX, String.format("stream create type=dax_tx compression=none"));
        sendCommand(FlexCommand.STREAM_CREATE_DAX_TX, String.format("stream create type=remote_audio_tx"));
    }

    public synchronized void commandRemoveDaxStream() {
        sendCommand(FlexCommand.STREAM_REMOVE, String.format("stream remove 0x%x", getDaxAudioStreamId()));
    }

    public synchronized void commandRemoveAllStream() {
        for (Long id : streamIdSet) {
            sendCommand(FlexCommand.STREAM_REMOVE, String.format("stream remove 0x%x", id));
        }
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetFilter(int sliceOrder, int filt_low, int filt_high) {
        sendCommand(FlexCommand.FILT_SET, String.format("filt %d %d %d", sliceOrder, filt_low, filt_high));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandStartATU() {
        sendCommand(FlexCommand.FILT_SET, "atu start");
    }

    public synchronized void commandGetInfo() {
        sendCommand(FlexCommand.INFO, "info");
    }

    public synchronized void commandPanadapterCreate() {
        sendCommand(FlexCommand.PANADAPTER_CREATE, "display pan c freq=9.5 ant=ANT1 x=800 y=400");
    }

    public synchronized void commandPanadapterRemove() {
        sendCommand(FlexCommand.PANADAPTER_REMOVE, String.format("display pan r 0x%x", panadapterStreamId));
        //sendCommand(FlexCommand.PANADAPTER_REMOVE,"display pan r 0x40000001");
        //sendCommand(FlexCommand.PANADAPTER_REMOVE,"display pan r 0x40000000");
    }

    public synchronized void commandMeterCreateAmp() {
        sendCommand(FlexCommand.METER_CREATE_AMP, "meter create name=AFRAMP type=AMP min=-150.0 max=20.0 units=AMPS");
    }

    public synchronized void commandMeterList() {
        sendCommand(FlexCommand.METER_LIST, "meter list");
    }

    public synchronized void commandSubClientAll() {
        sendCommand(FlexCommand.SUB_CLIENT_ALL, "sub client all");
    }

    public synchronized void commandSubTxAll() {
        sendCommand(FlexCommand.SUB_TX_ALL, "sub client all");
    }

    public synchronized void commandSubAtuAll() {
        sendCommand(FlexCommand.SUB_ATU_ALL, "sub atu all");
    }

    public synchronized void commandSubAmplifierAtuAll() {
        sendCommand(FlexCommand.SUB_amplifier_ALL, "sub amplifier all");
    }

    public synchronized void commandSubMeterAll() {
        sendCommand(FlexCommand.SUB_METER_ALL, "sub meter all");
        //sendCommand(FlexCommand.SUB_METER_ALL,"sub meter 15");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSubMeterById(int id) {
        sendCommand(FlexCommand.SUB_METER_ID, String.format("sub meter %d", id));
    }

    public synchronized void commandSubPanAll() {
        sendCommand(FlexCommand.SUB_PAN_ALL, "sub pan all");
    }

    public synchronized void commandSubSliceAll() {
        sendCommand(FlexCommand.SUB_METER_ALL, "sub slice all");
    }

    public synchronized void commandSubAudioStreamAll() {
        sendCommand(FlexCommand.SUB_AUDIO_STREAM_ALL, "sub audio_stream all");
    }

    public synchronized void commandSubDaxIqAll() {
        sendCommand(FlexCommand.SUB_DAX_IQ_ALL, "sub daxiq all");
    }

    public synchronized void commandSubDaxAll() {
        sendCommand(FlexCommand.SUB_DAX_ALL, "sub dax all");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetRfPower(int power) {
        sendCommand(FlexCommand.TRANSMIT_MAX_POWER, String.format("transmit set max_power_level=%d", power));
        sendCommand(FlexCommand.TRANSMIT_POWER, String.format("transmit set rfpower=%d", power));
        //sendCommand(FlexCommand.TRANSMIT_MAX_POWER,"info");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetTunePower(int power) {
        sendCommand(FlexCommand.AUT_TUNE_MAX_POWER, String.format("transmit set tunepower=%d", power));
    }

    public synchronized void commandPTTOnOff(boolean on) {
        if (on) {
            sendCommand(FlexCommand.PTT_ON, "xmit 1");
        } else {
            sendCommand(FlexCommand.PTT_ON, "xmit 0");
        }
    }

    public synchronized void commandTuneTransmitOnOff(boolean on) {
        if (on) {
            sendCommand(FlexCommand.PTT_ON, "transmit tune on");
        } else {
            sendCommand(FlexCommand.PTT_ON, "transmit tune off");
        }
    }


    @SuppressLint("DefaultLocale")
    public synchronized void commandDisplayPan(int x, int y) {
        sendCommand(FlexCommand.DISPLAY_PAN, String.format("display pan set 0x%X xpixels=%d", 0x40000000, x));
        sendCommand(FlexCommand.DISPLAY_PAN, String.format("display pan set 0x%X ypixels=%d", 0x40000000, y));
    }
    //**************封装FlexRadio各种指令*结束***********************

    @Override
    protected void finalize() throws Throwable {
        closeStreamPort();
        super.finalize();
    }


    //**************各种接口**********************

    /**
     * 当TCP接收到数据
     */
    public interface OnReceiveDataListener {
        void onDataReceive(byte[] data);
    }

    /**
     * 当接收到指令回复
     */
    public interface OnCommandListener {
        void onResponse(FlexResponse response);
    }

    public interface OnStatusListener {
        void onStatus(FlexResponse response);
    }

    public interface OnMessageListener {
        void onMessage(FlexResponse response);
    }

    /**
     * 当TCP连接状态变化
     */
    public interface OnTcpConnectStatus {
        void onConnectSuccess(RadioTcpClient tcpClient);

        void onConnectFail(RadioTcpClient tcpClient);
    }

    /**
     * 当接收到流数据时的事件
     */
    public interface OnReceiveStreamData {
        void onReceiveAudio(byte[] data);//音频数据

        void onReceiveIQ(byte[] data);//IQ数据

        void onReceiveFFT(VITA vita);//频谱数据

        void onReceiveMeter(VITA vita);//仪表数据

        void onReceiveUnKnow(byte[] data);//未知数据
    }
    //*******************************************


    /**
     * 电台TCP回复数据的基础类
     */
    public static class FlexResponse {
        private static final String TAG = "FlexResponse";
        public FlexResponseStyle responseStyle;
        public String head;//消息头
        public String content;//消息内容
        public String exContent;//扩展潇潇兮，有的返回消息分为3段，取第3段消息
        public String rawData;//原始数据
        public int seq_number;//32位int,指令序号
        public int handle;//句柄，32位，16进制
        public String version;//版本信息
        public int message_num;//消息号，32位，16进制。其中位24-25包含消息的严重性（0=信息，1=警告，2=错误，3=致命错误）
        public long daxStreamId = 0;
        public long daxTxStreamId = 0;
        public long panadapterStreamId = 0;
        public FlexCommand flexCommand = FlexCommand.UNKNOW;
        public long resultValue = 0;

        public FlexResponse(String line) {
            //Log.e(TAG, "FlexResponse: line--->"+line );
            rawData = line;
            char header;
            if (line.length() > 0) {
                header = line.toUpperCase().charAt(0);
            } else {
                header = 0;
            }
            switch (header) {
                case 'S':
                    responseStyle = FlexResponseStyle.STATUS;
                    getHeadAndContent(line, "\\|");
                    try {
                        this.handle = Integer.parseInt(head.substring(1), 16);//解析16进制
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FlexResponse status handle exception: " + e.getMessage());
                    }
                    break;
                case 'R':
                    responseStyle = FlexResponseStyle.RESPONSE;
                    getHeadAndContent(line, "\\|");
                    try {
                        seq_number = Integer.parseInt(head.substring(1));//解析指令序号
                        flexCommand = FlexCommand.values()[seq_number % 1000];
                        switch (flexCommand) {
                            case STREAM_CREATE_DAX_RX:
                                this.daxStreamId = getStreamId(line);
                                break;
                            case PANADAPTER_CREATE:
                                this.panadapterStreamId = getStreamId(line);
                                break;
                            case STREAM_CREATE_DAX_TX:
                                this.daxTxStreamId = getStreamId(line);
                                break;
                        }
                        resultValue = Integer.parseInt(content, 16);//取命令的返回值

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FlexResponse parseInt seq_number exception: " + e.getMessage());
                    }
                    break;
                case 'H':
                    responseStyle = FlexResponseStyle.HANDLE;
                    head = line;
                    content = line;
                    Log.e(TAG, "FlexResponse: handle:" + line.substring(1));
                    try {
                        this.handle = Integer.parseInt(line.substring(1), 16);//解析16进制
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FlexResponse parseInt handle exception: " + e.getMessage());
                    }

                    break;
                case 'V':
                    responseStyle = FlexResponseStyle.VERSION;
                    head = line;
                    content = line;
                    this.version = line.substring(1);
                    break;
                case 'M':
                    responseStyle = FlexResponseStyle.MESSAGE;
                    getHeadAndContent(line, "\\|");
                    try {
                        //Log.e(TAG, "FlexResponse: "+line );
                        this.message_num = Integer.parseInt(head.substring(2), 16);//消息号，32位，16进制
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FlexResponse parseInt message_num exception: " + e.getMessage());
                    }

                    break;
                case 'C':
                    responseStyle = FlexResponseStyle.COMMAND;
                    getHeadAndContent(line, "\\|");
                    int index = 1;
                    if (head.length() > 2) {
                        if (head.toUpperCase().charAt(1) == 'D') index = 2;
                    }
                    try {
                        seq_number = Integer.parseInt(head.substring(index));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FlexResponse parseInt seq_number exception: " + e.getMessage());
                    }

                    break;
                case 0:
                default:
                    responseStyle = FlexResponseStyle.UNKNOW;
                    break;
            }
        }

        private long getStreamId(String line) {
            String[] lines = line.split("\\|");
            if (lines.length > 2) {
                if (lines[1].equals("0")) {
                    try {
                        return Long.parseLong(lines[2], 16);//stream id，16进制
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "getDaxStreamId exception: " + e.getMessage());
                    }
                }
            }
            return 0;
        }

        /**
         * 分割消息的头和内容，并分别负值给head和content
         *
         * @param line  消息
         * @param split 分隔符
         */
        private void getHeadAndContent(String line, String split) {
            String[] temp = line.split(split);
            if (line.length() > 1) {
                head = temp[0];
                content = temp[1];
            } else {
                head = "";
                content = "";

            }

            if (temp.length > 2) {
                exContent = temp[2];
            } else {
                exContent = "";
            }

        }

        public String resultStatus() {
            if (resultValue == 0) {
                return String.format(GeneralVariables.getStringFromResource(
                        R.string.instruction_success), flexCommand.toString());
            } else {
                return String.format(GeneralVariables.getStringFromResource(
                        R.string.instruction_failed), flexCommand.toString(), rawData);
            }
        }

    }


    // ********事件的Getter和Setter*********

    public OnReceiveDataListener getOnReceiveDataListener() {
        return onReceiveDataListener;
    }

    public void setOnReceiveDataListener(OnReceiveDataListener onReceiveDataListener) {
        this.onReceiveDataListener = onReceiveDataListener;
    }

    public OnCommandListener getOnCommandListener() {
        return onCommandListener;
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        this.onCommandListener = onCommandListener;
    }

    public RadioTcpClient getTcpClient() {
        return tcpClient;
    }

    public String getVersion() {
        return version;
    }

    public int getHandle() {
        return handle;
    }

    public String getHandleStr() {
        return String.format("%X", handle);
    }

    public void setHandle(int handle) {
        this.handle = handle;
    }

    public boolean isAllFlexRadioStatusEvent() {
        return allFlexRadioStatusEvent;
    }

    public void setAllFlexRadioStatusEvent(boolean allFlexRadioStatusEvent) {
        this.allFlexRadioStatusEvent = allFlexRadioStatusEvent;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        clientID = clientID;
    }

    public int getCommandSeq() {
        return commandSeq * 1000 + flexCommand.ordinal();
    }

    public String getCommandStr() {
        return commandStr;
    }

    public long getDaxAudioStreamId() {
        return daxAudioStreamId;
    }

    public String getDiscovery_protocol_version() {
        return discovery_protocol_version;
    }

    public String getModel() {
        return model;
    }

    public String getSerial() {
        return serial;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCallsign() {
        return callsign;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getStatus() {
        return status;
    }

    public String getInUse_ip() {
        return inUse_ip;
    }

    public String getInUse_host() {
        return inUse_host;
    }

    public String getMax_licensed_version() {
        return max_licensed_version;
    }

    public String getRadio_license_id() {
        return radio_license_id;
    }

    public String getRequires_additional_license() {
        return requires_additional_license;
    }

    public String getFpc_mac() {
        return fpc_mac;
    }

    public int getWan_connected() {
        return wan_connected;
    }

    public int getLicensed_clients() {
        return licensed_clients;
    }

    public int getAvailable_clients() {
        return available_clients;
    }

    public int getMax_panadapters() {
        return max_panadapters;
    }

    public int getAvailable_panadapters() {
        return available_panadapters;
    }

    public int getMax_slices() {
        return max_slices;
    }

    public int getAvailable_slices() {
        return available_slices;
    }

    public String getGui_client_ips() {
        return gui_client_ips;
    }

    public String getGui_client_hosts() {
        return gui_client_hosts;
    }

    public String getGui_client_programs() {
        return gui_client_programs;
    }

    public String getGui_client_stations() {
        return gui_client_stations;
    }

    public String getGui_client_handles() {
        return gui_client_handles;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public OnTcpConnectStatus getOnTcpConnectStatus() {
        return onTcpConnectStatus;
    }

    public void setOnTcpConnectStatus(OnTcpConnectStatus onTcpConnectStatus) {
        this.onTcpConnectStatus = onTcpConnectStatus;
    }

    public OnReceiveStreamData getOnReceiveStreamData() {
        return onReceiveStreamData;
    }

    public void setOnReceiveStreamData(OnReceiveStreamData onReceiveStreamData) {
        this.onReceiveStreamData = onReceiveStreamData;
    }

    public OnStatusListener getOnStatusListener() {
        return onStatusListener;
    }

    public void setOnStatusListener(OnStatusListener onStatusListener) {
        this.onStatusListener = onStatusListener;
    }

    public RadioUdpClient getStreamClient() {
        return streamClient;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }


    public static float[] getFloatFromBytes(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        for (int i = 0; i < floats.length; i++) {
            try {
                floats[i] = dis.readFloat();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getFloat: ------>>" + e.getMessage());
                break;
            }
        }
        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return floats;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
