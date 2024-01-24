package com.bg7yoz.ft8cn.x6100;

import static com.bg7yoz.ft8cn.flex.VITA.XIEGU_METER_CLASS_ID;
import static com.bg7yoz.ft8cn.flex.VITA.XIEGU_METER_Stream_Id;
import static com.bg7yoz.ft8cn.flex.VITA.XIEGU_PING_CLASS_ID;
import static com.bg7yoz.ft8cn.flex.VITA.XIEGU_PING_Stream_Id;
import static com.bg7yoz.ft8cn.flex.VITA.byteToStr;
import static com.bg7yoz.ft8cn.flex.VITA.readShortData;
import static com.bg7yoz.ft8cn.x6100.X6100Radio.XieguResponseStyle.RESPONSE;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.flex.FlexCommand;
import com.bg7yoz.ft8cn.flex.FlexRadio;
import com.bg7yoz.ft8cn.flex.FlexResponseStyle;
import com.bg7yoz.ft8cn.flex.RadioTcpClient;
import com.bg7yoz.ft8cn.flex.RadioUdpClient;
import com.bg7yoz.ft8cn.flex.VITA;
import com.bg7yoz.ft8cn.flex.VitaPacketType;
import com.bg7yoz.ft8cn.flex.VitaTSF;
import com.bg7yoz.ft8cn.flex.VitaTSI;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class X6100Radio {
    public enum XieguCommand {
        UNKNOW,//未知指令
        AUDIO,//音频指令
        STREAM,//数据流指令
        SUB,//订阅仪表
        UNSUB,//取消订阅仪表
        A91,//ft8符号
        ATU,//自动天调
        TUNE,//设置频率
        MODE,//操作模式
        PTT,//PTT操作
        SET//设置操作
    }

    public enum XieguResponseStyle {
        STATUS,//状态信息，S+HANDLE
        RESPONSE,//命令的响应，R+客户端命令序列号
        HANDLE,//电台给定的句柄，H+句柄（32位的16进制表示）
        VERSION,//版本号，V+版本号
        COMMAND,//发送命令，C+序列号|命令
        UNKNOW//未知的回复类型
    }

    private static final String TAG = "X6100Radio";
    private static int lossCount = 0;
    private static int currentCount = -1;

    private String modelName;//电台型号
    private String version;//电台版本号
    private String rig_ip;//电台的IP
    private String mac;//mac地址
    public boolean isPttOn = false;
    private int control_port = 7002;//电台的控制端口
    private int stream_port = 7003;//电台端流数据的端口
    private int discovery_port = 7001;//发现协议的端口
    private long lastSeen;//最后一次消息的时间
    private boolean isAvailable = true;//电台是不是有效
    private final StringBuilder buffer = new StringBuilder();//指令的缓存
    private final RadioTcpClient tcpClient = new RadioTcpClient();
    private RadioUdpClient streamClient;
    private int commandSeq = 1;//指令的序列
    private XieguCommand xieguCommand;
    private int handle = 0;
    private String commandStr;
    private int frames = 768;//每个周期的帧数
    private int period = 64;//每个周期的时长，毫秒


    //************************事件处理接口*******************************
    private OnReceiveDataListener onReceiveDataListener;//当前接收到的数据事件
    private OnTcpConnectStatus onTcpConnectStatus;//当TCP连接状态变化的事件
    private OnReceiveStreamData onReceiveStreamData;//当接收到流数据后的处理事件
    private OnCommandListener onCommandListener;//触发命令事件
    private OnStatusListener onStatusListener;//触发状态事件
    //*****************************************************************
    private AudioTrack audioTrack = null;


    ///******************用于仪表信息显示*************
    public MutableLiveData<Long> mutablePing = new MutableLiveData<>();//ping值
    public MutableLiveData<Integer> mutableLossPackets = new MutableLiveData<>();//丢失的包数量
    public MutableLiveData<X6100Meters> mutableMeters = new MutableLiveData<>();
    private X6100Meters meters = new X6100Meters();

    private boolean swrAlert = false;
    private boolean alcAlert = false;


    private Timer pingTimer = new Timer();

    private TimerTask pingTask() {
        return new TimerTask() {
            @Override
            public void run() {

                try {
                    if (!streamClient.isActivated() || !isConnect()) {
                        pingTimer.cancel();
                        pingTimer.purge();
                        pingTimer = null;
                        return;
                    }
                    VITA vita = new VITA(VitaPacketType.EXT_DATA_WITH_STREAM
                            , VitaTSI.TSI_OTHER
                            , VitaTSF.TSF_REALTIME
                            , 0
                            , XIEGU_PING_Stream_Id
                            , XIEGU_PING_CLASS_ID);

                    vita.packetCount = 0;
                    vita.packetSize = 7;
                    vita.integerTimestamp = 0;//0是发送包，1是接收包
                    vita.fracTimeStamp = System.currentTimeMillis();
                    streamClient.sendData(vita.pingDataToVita(), rig_ip, stream_port);

                } catch (Exception e) {
                    Log.e(TAG, "ping timer error:" + e.getMessage());
                }
            }
        };
    }

    /**
     * 更新最后看到的时间
     */
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public X6100Radio() {
        updateLastSeen();
    }

    public X6100Radio(String s, String ip) {
        mutableLossPackets.postValue(0);
        update(s, ip);
    }

    public void update(String discoverStr, String ip) {
        Log.d(TAG, discoverStr);
        rig_ip = ip;

        String[] paras = discoverStr.replace("\0", " ").split(" ");
        version = getParameterStr(paras, "ft8cn_server_version");
        modelName = getParameterStr(paras, "model");
        mac = getParameterStr(paras, "mac");
        control_port = getParameterInt(paras, "control_port");
        stream_port = getParameterInt(paras, "stream_port");
        discovery_port = getParameterInt(paras, "discovery_port");

        updateLastSeen();
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
     * 检查是不是 刚刚 离线，离线条件：5秒内没有收到电台的广播数据包
     *
     * @return 是否
     */
    public boolean isInvalidNow() {
        if (isAvailable) {//如果标记在线，而大于5秒的时间没有收到数据包，就视为刚刚离线。
            isAvailable = System.currentTimeMillis() - lastSeen < 1000 * 5;//小于5秒，就视为在线
            return !isAvailable;
        } else {//如果已经标记不在线了，就不是刚刚离线的。
            return false;
        }
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getVersion() {
        return version;
    }

    public String getRig_ip() {
        return rig_ip;
    }

    public void setRig_ip(String rig_ip) {
        this.rig_ip = rig_ip;
    }

    public String getMac() {
        return mac;
    }

    public boolean isEqual(String madAddress) {
        return this.mac.equalsIgnoreCase(madAddress);
    }

    /**
     * 连接到控制电台
     */
    public void connect() {
        this.connect(this.rig_ip, this.control_port);
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
                if (onReceiveDataListener != null) {//此处把数据传递给XieGu6100NetRig
                    onReceiveDataListener.onDataReceive(buffer);
                }
                onReceiveData(buffer);
            }

            @Override
            public void onConnectionClosed() {
                tcpClient.disconnect();
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.tcp_connect_closed));
                if (onTcpConnectStatus != null){
                    onTcpConnectStatus.onConnectionClosed(tcpClient);
                }
            }
        });
        clearBufferData();//清除一下缓存的指令数据
        tcpClient.connect(ip, port);//连接TCP

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

    @SuppressLint("DefaultLocale")
    public synchronized void commandAtuOn() {
        sendCommand(XieguCommand.ATU, "atu on");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandAtuOff() {
        sendCommand(XieguCommand.ATU, "atu off");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandAtuStart() {
        sendCommand(XieguCommand.ATU, "atu start");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandOpenStream() {
        sendCommand(XieguCommand.STREAM, String.format("stream on %d", stream_port));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSendA91(byte[] a91, float vol, float freq) {
        sendCommand(XieguCommand.A91, String.format("a91 %.2f %.0f %s", vol, freq, BaseRig.byteToStr(a91)));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandGetAudioInfo() {
        sendCommand(XieguCommand.AUDIO, "audio get all");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandGetStreamInfo() {
        sendCommand(XieguCommand.STREAM, "stream get");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSubAllMeter() {
        sendCommand(XieguCommand.SUB, "sub all");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSubGetMeter() {
        sendCommand(XieguCommand.SUB, "sub get");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandUnubMeter() {
        sendCommand(XieguCommand.UNSUB, "unsub");
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandTuneFreq(long freq) {
        sendCommand(XieguCommand.TUNE, String.format("tune %d", freq));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetMode(String mode, int filter) {
        sendCommand(XieguCommand.TUNE, String.format("mode %s %d", mode, filter));
    }

    @SuppressLint("DefaultLocale")
    public synchronized void commandSetTxPower(int power) {
        sendCommand(XieguCommand.SET, String.format("set tx %d", power));
    }
    @SuppressLint("DefaultLocale")
    public synchronized void commandSetTxVol(int volume) {
        sendCommand(XieguCommand.SET, String.format("set tx_vol %d", volume));
    }

    private void showAlert() {
        Log.e(TAG, String.format("ALC:%f", meters.alc));
        if (meters.swr >= 5) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
        }


        if (meters.alc > 50 || meters.alc < 20) {
            if (!alcAlert) {
                alcAlert = true;
                if (meters.alc > 50) {
                    ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_high_alert));
                } else {
                    ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_low_alert));
                }
            }
        } else {
            alcAlert = false;
        }
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
                    ToastMessage.show(e.getMessage());
                    e.printStackTrace();
                }

            }
        }


        RadioUdpClient.OnUdpEvents onUdpEvents = new RadioUdpClient.OnUdpEvents() {
            @SuppressLint("DefaultLocale")
            @Override
            public void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data) {
                VITA vita = new VITA(data);
                if (vita.classId64 == VITA.XIEGU_AUDIO_CLASS_ID) {//音频数据

                    //判断数据包丢失情况
                    int temp = lossCount;
                    if (currentCount <= -1) {
                        currentCount = vita.packetCount;
                    }
                    if (currentCount > vita.packetCount) {
                        lossCount = lossCount + vita.packetCount + 16 - currentCount - 1;
                    } else if (currentCount < vita.packetCount) {
                        lossCount = lossCount + vita.packetCount - currentCount - 1;
                    }
                    currentCount = vita.packetCount;
                    if (lossCount > temp) {
                        Log.e(TAG, String.format("丢包数量:%d", lossCount));

                        for (int i = 0; i < (lossCount - temp); i++) {
                            Log.d(TAG, String.format("补发数据,%d,size:%d", i, vita.payload.length));
                            sendReceivedAudio(vita.payload);//把当前的数据补发给录音对象
                        }
                        mutableLossPackets.postValue(lossCount);
                    }
                    sendReceivedAudio(vita.payload);//把音频发给录音对象
                    playReceiveAudio(vita.payload);//发送当前的音频数据
                } else if (vita.classId64 == XIEGU_PING_CLASS_ID//ping数据
                        && vita.streamId == XIEGU_PING_Stream_Id
                        && vita.integerTimestamp == 1) {//ping的回包
                    mutablePing.postValue(System.currentTimeMillis() - vita.fracTimeStamp);
                } else if (vita.classId64 == XIEGU_METER_CLASS_ID//仪表数据
                        && vita.streamId == XIEGU_METER_Stream_Id) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            meters.update(vita.payload);
                            mutableMeters.postValue(meters);
                            if (isPttOn) {
                                showAlert();
                            } else {
                                alcAlert = false;
                                swrAlert = false;
                            }
                            if (onReceiveStreamData != null){
                                onReceiveStreamData.onReceiveMeter(meters);
                            }
                        }
                    }).start();

                }


            }
        };

        //此处要确定stream的udp端口
        streamClient = new RadioUdpClient(stream_port);
        streamClient.setOnUdpEvents(onUdpEvents);
        try {
            streamClient.setActivated(true);
            pingTimer.schedule(pingTask(), 1000, 1000);//启动ping计时器
        } catch (SocketException e) {
            ToastMessage.show(e.getMessage());
            e.printStackTrace();
            Log.d(TAG, "streamClient: " + e.getMessage());
        }


    }

    /**
     * 当接收到音频数据后，发送给录音对象的操作
     *
     * @param data 音频数据
     */
    private void sendReceivedAudio(byte[] data) {
        if (onReceiveStreamData != null) {
            onReceiveStreamData.onReceiveAudio(data);
        }
    }

    /**
     * 当接收到音频数据时的处理
     *
     * @param data 音频数据
     */
    private void playReceiveAudio(byte[] data) {
        if (audioTrack != null) {//如果音频播放已经打开，就写音频流数据
            audioTrack.write(data, 0, data.length, AudioTrack.WRITE_NON_BLOCKING);
        }
    }

    /**
     * 断开与电台的连接
     */
    public synchronized void disConnect() {
        if (tcpClient.isConnect()) {
            tcpClient.disconnect();
        }
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer.purge();
            pingTimer = null;
        }
    }

    /**
     * 电台是否连接
     *
     * @return 是否
     */
    public boolean isConnect() {
        return tcpClient.isConnect();
    }

    /**
     * 关闭音频
     */
    public void closeAudio() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack = null;
        }
    }



    /**
     * 当接收到数据时触发的事件，此处是TCP连接得到的数据
     *
     * @param data 数据
     */
    private void onReceiveData(byte[] data) {

        if (data.length > 4) {//判断是不是老式的icom指令
            if ((data[0] == (byte) 0xfe) && (data[1] == (byte) 0xfe)
                    && ((data[2] == (byte) 0xe0) || data[3] == (byte) 0xe0)) {
                clearBufferData();
                return;
            }
        }
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
     * <p>
     * //* @param line 数据行
     */
    private void doReceiveLineEvent(String line) {

        XieguResponse response = new XieguResponse(line);
        //更新一下句柄
        switch (response.responseStyle) {
            case VERSION:
                this.version = response.head.substring(1);
                break;
            case HANDLE:
                this.handle = Integer.parseInt(response.head.substring(1), 16);
                break;
            case RESPONSE:
                if (XieguCommand.AUDIO == response.xieguCommand) {//是音频指令回复的信息
                    setAudioInfo(response.resultContent);
                }
                if (onCommandListener != null) {
                    onCommandListener.onResponse(response);
                }
                break;
            case STATUS:

                if (response.resultCode == 0) {//说明是电台状态变化了
                    String status[] = response.resultContent.split(" ");
                    for (int i = 0; i < status.length; i++) {//找出ptt的状体，设置ptt
                        if (status[i].startsWith("ptt")) {//判断PTT
                            String temp[] = status[i].split("=");
                            isPttOn = temp[1].equalsIgnoreCase("on");
                        }

                        if (status[i].startsWith("play_volume")) {//判断PTT
                            String temp[] = status[i].split("=");
                            float vol = Integer.parseInt(temp[1].trim())*1.0f/100f;
                            GeneralVariables.volumePercent = vol;
                            GeneralVariables.mutableVolumePercent.postValue(vol);
                        }
                    }
                }

                if (onStatusListener != null) {
                    onStatusListener.onStatus(response);
                }
                break;
        }

    }

    /**
     * 获取电台的音频信息
     *
     * @param result 返回信息
     */
    private void setAudioInfo(String result) {
        String[] keys = result.split(" ");
        for (int i = 0; i < keys.length; i++) {
            String[] val = keys[i].split("=");
            if (val[0].equalsIgnoreCase("period")) period = Integer.parseInt(val[1]) / 1000;
            if (val[0].equalsIgnoreCase("frames")) frames = Integer.parseInt(val[1]);
        }
        Log.d(TAG, String.format("set audio para:frames=%d,period=%d", frames, period));
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
    public void sendCommand(XieguCommand command, String cmdContent) {
        if (tcpClient.isConnect()) {
            commandSeq++;
            xieguCommand = command;
            commandStr = String.format("C%05d%03d|%s\n", commandSeq, command.ordinal()
                    , cmdContent);
            tcpClient.sendByte(commandStr.getBytes());
            Log.d(TAG, "sendCommand: " + commandStr);
        }
    }

    public synchronized void commandPTTOnOff(boolean on) {
        if (on) {
            sendCommand(XieguCommand.PTT, "ptt on");
        } else {
            sendCommand(XieguCommand.PTT, "ptt off");
        }
    }

    /**
     * 发射的采样率为12000采样率，单声道,16位
     *
     * @param data 音频
     */
    public void sendWaveData(float[] data) {
        Log.d(TAG, String.format("send wav data,len:%d....", data.length));
        short[] temp = new short[data.length];
        //传递过来的音频是LPCM,32 float，12000Hz
        //x6100的音频格式是LPCM 16 Int，12000Hz
        //要做一下浮点到16位int的转换
        for (int i = 0; i < data.length; i++) {
            float x = data[i];
            if (x > 1.0)
                x = 1.0f;
            else if (x < -1.0)
                x = -1.0f;
            temp[i] = (short) (x * 32767.0);
        }
        short[] payload = new short[frames];

        VITA vita = new VITA(VitaPacketType.EXT_DATA_WITH_STREAM
                , VitaTSI.TSI_OTHER
                , VitaTSF.TSF_SAMPLE_COUNT
                , 0
                , 0x84000001
                , 0x584945475500A1L);

        vita.packetCount = 0;
        vita.integerTimestamp = 0;
        vita.fracTimeStamp = payload.length * 2L;


        try {
            int count = 0;
            int a = 0;
            while (count < temp.length) {
                long now = System.currentTimeMillis();//获取当前时间
                Arrays.fill(payload, (short) 0);//数组清零

                if (!isPttOn) break;
                if (data.length - count > frames) {
                    System.arraycopy(temp, count, payload, 0, frames);
                    count = count + frames;
                } else {
                    System.arraycopy(temp, count, payload, 0, temp.length - count);
                    count = temp.length;
                }
                streamClient.sendData(vita.audioShortDataToVita(vita.packetCount, payload), rig_ip, stream_port);
                while (isPttOn) {
                    if (System.currentTimeMillis() - now >= period) {//64毫秒一个周期
                        break;
                    }
                }
                a++;
            }


        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 清空缓存数据
     */
    private void clearBufferData() {
        buffer.setLength(0);
    }

    /**
     * 打开音频，流方式。当收到音频流的时候，播放数据
     */
    public void openAudio() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat myFormat = new AudioFormat.Builder().setSampleRate(12000)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                //, 12000 * 4, AudioTrack.MODE_STREAM
                , 768 * 2 * 4, AudioTrack.MODE_STREAM//x6100一个声音周期是64毫秒，共计768*2个字节
                , mySession);
        audioTrack.play();
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

    public OnCommandListener getOnCommandListener() {
        return onCommandListener;
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        this.onCommandListener = onCommandListener;
    }

    public OnReceiveDataListener getOnReceiveDataListener() {
        return onReceiveDataListener;
    }

    public void setOnReceiveDataListener(OnReceiveDataListener onReceiveDataListener) {
        this.onReceiveDataListener = onReceiveDataListener;
    }


//**************各种接口**********************

    /**
     * 当TCP接收到数据
     */
    public interface OnReceiveDataListener {
        void onDataReceive(byte[] data);
    }

    /**
     * 当TCP连接状态变化
     */
    public interface OnTcpConnectStatus {
        void onConnectSuccess(RadioTcpClient tcpClient);

        void onConnectFail(RadioTcpClient tcpClient);
        void onConnectionClosed(RadioTcpClient tcpClient);
    }

    /**
     * 当接收到流数据时的事件
     */
    public interface OnReceiveStreamData {
        void onReceiveAudio(byte[] data);//音频数据

        void onReceiveIQ(byte[] data);//IQ数据

        void onReceiveFFT(VITA vita);//频谱数据

        void onReceiveMeter(X6100Meters meters);//仪表数据

        void onReceiveUnKnow(byte[] data);//未知数据
    }

    /**
     * 当接收到指令回复
     */
    public interface OnCommandListener {
        void onResponse(XieguResponse response);
    }

    public interface OnStatusListener {
        void onStatus(XieguResponse response);
    }
    //*******************************************


    /**
     * 电台TCP回复数据的基础类
     */
    public static class XieguResponse {
        private static final String TAG = "XieguResponse";
        public XieguResponseStyle responseStyle;
        public String head;//消息头
        public int resultCode;//消息代码
        public String resultContent;//扩展消息，有的返回消息分为3段，取第3段消息
        public String rawData;//原始数据
        public int seq_number;//32位int,指令序号

        public XieguCommand xieguCommand = XieguCommand.UNKNOW;


        public XieguResponse(String line) {
            rawData = line;
            char header;
            if (line.length() > 0) {
                header = line.toUpperCase().charAt(0);
            } else {
                header = 0;
            }
            switch (header) {
                case 'S':
                    responseStyle = XieguResponseStyle.STATUS;
                    getHeadAndContent(line, "\\|");//获取指令的头、值、内容

                    break;
                case 'R':
                    responseStyle = RESPONSE;
                    getHeadAndContent(line, "\\|");
                    try {
                        seq_number = Integer.parseInt(head.substring(1));//解析指令序号
                        xieguCommand = XieguCommand.values()[seq_number % 1000];
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.e(TAG, "XieguResponse parseInt seq_number exception: " + e.getMessage());
                    }
                    break;
                case 'H':
                    responseStyle = XieguResponseStyle.HANDLE;
                    head = line;
                    resultContent = line;
                    Log.d(TAG, "XieguResponse: handle:" + line.substring(1));

                    break;
                case 'V':
                    responseStyle = XieguResponseStyle.VERSION;
                    head = line;
                    resultContent = line;
                    break;

                case 0:
                default:
                    responseStyle = XieguResponseStyle.UNKNOW;
                    break;
            }
        }


        /**
         * 分割消息的头和内容，并分别负值给head和content
         *
         * @param line  消息
         * @param split 分隔符
         */
        private void getHeadAndContent(String line, String split) {
            String[] temp = line.split(split);
            if (temp.length > 1) {
                head = temp[0];

                resultCode = Integer.parseInt(temp[1]);

            } else {
                head = "";
            }

            if (temp.length > 2) {
                resultContent = temp[2];
            } else {
                resultContent = "";
            }
        }
    }

}
