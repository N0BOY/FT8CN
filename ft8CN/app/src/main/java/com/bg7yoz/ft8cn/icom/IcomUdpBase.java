package com.bg7yoz.ft8cn.icom;

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class IcomUdpBase {
    public enum IcomUdpStyle{//数据流的类型
        UdpBase,
        ControlUdp,
        CivUdp,
        AudioUdp
    }
    public static String getUdpStyle(IcomUdpStyle style){
        switch (style){
            case ControlUdp:
                return GeneralVariables.getStringFromResource(R.string.control_stream);
            case  CivUdp:
                return GeneralVariables.getStringFromResource(R.string.civ_stream);
            case AudioUdp:
                return GeneralVariables.getStringFromResource(R.string.audio_stream);
            default:
                return GeneralVariables.getStringFromResource(R.string.data_stream);
        }
    }
    /**
     * 事件接口
     */
    public interface OnStreamEvents {
        void OnReceivedIAmHere(byte[] data);
        void OnReceivedCivData(byte[] data);
        void OnReceivedAudioData(byte[] audioData);
        void OnUdpSendIOException(IcomUdpStyle style,IOException e);
        void OnLoginResponse(boolean authIsOK);
        //void OnWatchDogAlert(IcomUdpStyle style,boolean isAlerted);
    }
    public IcomUdpStyle udpStyle=IcomUdpStyle.UdpBase;

    private static final String TAG = "IcomUdpBase";
    public int rigPort;
    public String rigIp;
    public int localPort;
    public int localId = (int) System.currentTimeMillis();//随机码，以时间为随机变量
    public int remoteId;
    public boolean authDone = false;//登录
    public boolean rigReadyDone = false;//电台已经ready,control可以执行登录，ci-v可以执行open.
    public short trackedSeq = 1;//因为are you there=0,are you ready=1。从are you ready之后才发track包
    public short pingSeq = 0;//ping的起始值是0
    public short innerSeq = 0x30;
    public int rigToken;//电台提供的令牌
    public short localToken = (short) System.currentTimeMillis();//本地生成的令牌，可以是随机数
    public boolean isPttOn=false;



    public IcomSeqBuffer txSeqBuffer = new IcomSeqBuffer();//发送命令的历史列表
    //public IcomSeqBuffer rxSeqBuffer = new IcomSeqBuffer();//接收命令的历史列表
    public long lastReceivedTime=System.currentTimeMillis();//最后收到数据的时间
    public long lastSentTime=System.currentTimeMillis();//最后收到数据的时间


    public IcomUdpClient udpClient;//用于与电台通讯的udp


    public OnStreamEvents onStreamEvents;//一些事件处理
    //Timer，执行代码部分：TimerTask，具体执行：timer.schedule(task,delay,period)
    public Timer areYouThereTimer;
    public Timer pingTimer;
    public Timer idleTimer;//发送空数据包的时钟


    public void close(){
        onStreamEvents=null;//不用弹出网络异常的消息了
        sendUntrackedPacket(IComPacketTypes.ControlPacket.toBytes(IComPacketTypes.CMD_DISCONNECT
                ,(short)0,localId,remoteId));
        stopTimer(areYouThereTimer);
        stopTimer(pingTimer);
        stopTimer(idleTimer);
    }

    /**
     * 关闭udpClient
     */
    public void closeStream(){
        if (udpClient!=null){
            try {
                udpClient.setActivated(false);
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "closeStream: "+e.getMessage() );
            }
        }
    }
    /**
     * 打开Udp流端口，如果Udp端口已经打开了，会再打开一次，本地端口应该会变化
     */
    public void openStream() {//打开
        if (udpClient == null) {
            udpClient = new IcomUdpClient(-1);
        }
        udpClient.setOnUdpEvents(new IcomUdpClient.OnUdpEvents() {
            @Override
            public void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data) {
                //屏蔽掉非法的数据包
                if (data.length<IComPacketTypes.CONTROL_SIZE) return;//如果小于最小的控制包，退出
                if (IComPacketTypes.ControlPacket.getRcvdId(data)!=localId) return;//如果接收的ID与我的ID不同，也退出

                onDataReceived(packet,data);
            }

            @Override
            public void OnUdpSendIOException(IOException e) {
                if (onStreamEvents!=null){
                    onStreamEvents.OnUdpSendIOException(udpStyle,e);
                }
            }
        });

        try {
            if (udpClient.isActivated()) udpClient.setActivated(false);
            udpClient.setActivated(true);
            localPort = udpClient.getLocalPort();
            Log.d(TAG, "IcomUdpBase: Open udp local port:" + localPort);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "IcomUdpBase: Open udp failed:" + e.getMessage());
        }
    }

    /**
     * 查看udp是否打开
     * @return 是否
     */
    public boolean streamOpened() {
        if (udpClient == null) {
            return false;
        } else {
            return udpClient.isActivated();
        }
    }

    /**
     * 当接收到数据之后的操作，后代类可以重载
     *
     * @param data 数据
     */
    public void onDataReceived(DatagramPacket packet,byte[] data) {

        //此处是对共性的数据接包做处理，不是共性的，可以在后续继承的类中override.
        switch (data.length) {
            case IComPacketTypes.CONTROL_SIZE://控制包,处理I'm here和重新传输
                onReceivedControlPacket(data);
                break;
            case IComPacketTypes.PING_SIZE://ping包
                onReceivedPingPacket(data);//回复ping
                break;
            case IComPacketTypes.RETRANSMIT_RANGE_SIZE://0x18，请求按序号范围重新发送
                break;

        }

        //一次性处理多个重传请求：type=0x01,len!=0x10，多个重传的序号是个short数组，在0x10字节之后。
        if (data.length != IComPacketTypes.CONTROL_SIZE
                && IComPacketTypes.ControlPacket.getType(data) == IComPacketTypes.CMD_RETRANSMIT) {
            retransmitMultiPacket(data);
        }

        //todo-接收数据，如果不是Ping包，且type=0x00&&seq!=0x00，说明是指令，考虑把指令添加到rxSeqBuffer中。
        //todo-接收指令的缓冲区是rxSeqBuffer

    }

    /**
     * 当接收到Control（0x10）包
     *
     * @param data 数据包
     */
    public void onReceivedControlPacket(byte[] data) {
        //todo 应当实现回复type=0x01的指令，也就是retransmit
        switch (IComPacketTypes.ControlPacket.getType(data)) {
            case IComPacketTypes.CMD_I_AM_HERE:
                if (onStreamEvents != null) {
                    onStreamEvents.OnReceivedIAmHere(data);
                }
                remoteId = IComPacketTypes.ControlPacket.getSentId(data);//记录下对方的ID
                stopTimer(areYouThereTimer);//停止are you there Timer
                startPingTimer();//打开Ping时钟，3个端口都有ping
                //发送are you ready?
                sendUntrackedPacket(IComPacketTypes.ControlPacket.toBytes(
                        IComPacketTypes.CMD_ARE_YOU_READY, (short) 1, localId, remoteId
                ));
                //在control流中启动idle timer
                //如果Ping time没有启动，启动500ms,3个端口都有Ping
                break;
            case IComPacketTypes.CMD_I_AM_READY:
                //startIdleTimer();//打开发送空包时钟
                //不同的端口处理方法不同，要在子类的override中实现
                //control 是login
                //civ 是openClose
                //audio 没看到
                break;
            case IComPacketTypes.CMD_RETRANSMIT://说明是有一个需要重传的数据包
                retransmitPacket(data);
                break;
        }

    }

    /**
     * 查找并重新传输单个数据包
     *
     * @param data 电台发送的请求数据包
     */
    public void retransmitPacket(byte[] data) {
        retransmitPacket(IComPacketTypes.ControlPacket.getSeq(data));
    }

    /**
     * 查找并重新传输单个数据包
     *
     * @param retransmitSeq 请求重新传输的序号
     */
    public void retransmitPacket(short retransmitSeq) {
        byte[] packet = txSeqBuffer.get(retransmitSeq);
        if (packet==null){
            Log.e(TAG, String.format("retransmitPacket:remotePort:%d,seq=0x%x,data:null ",rigPort,retransmitSeq) );
        }else {
            //IComPacketTypes.ControlPacket.setRcvdId(packet,remoteId);//如果mei
            Log.e(TAG, String.format("retransmitPacket:remotePort:%d,seq=0x%x,data:%s "
                    , rigPort,retransmitSeq, IComPacketTypes.byteToStr(packet)));
        }

        if (packet != null) {//找到了历史发送的数据包
            sendUntrackedPacket(packet);
        } else {//没有找到数据包，就发送一个空包
            sendUntrackedPacket(IComPacketTypes.ControlPacket.idlePacketData(retransmitSeq, localId, remoteId));
        }
    }

    /**
     * 查找并重新传输多个数据包，数据的格式是：controlPacket+short数组
     *
     * @param data 电台发送的请求数据包
     */
    public void retransmitMultiPacket(byte[] data) {
        if (data.length <= IComPacketTypes.CONTROL_SIZE) return;
        if (IComPacketTypes.ControlPacket.getType(data) != IComPacketTypes.CMD_RETRANSMIT) return;
        for (int i = 0x10; i < data.length; i = i + 2) {
            if (i + 1 > data.length - 1) break;//做一个保护，如果字节数不是偶数，防止数组下标溢出
            //重新传输指令
            retransmitPacket(IComPacketTypes.readShortBigEndianData(data, i));
        }
    }



    /**
     * 启动Are you there 时钟
     */
    public void startAreYouThereTimer() {
        stopTimer(areYouThereTimer);
        areYouThereTimer = new Timer();
        areYouThereTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, String.format("AreYouThereTimer: local port:%d,remote port %d", localPort, rigPort));
                sendUntrackedPacket(
                        IComPacketTypes.ControlPacket.toBytes(IComPacketTypes.CMD_ARE_YOU_THERE
                                , (short) 0, localId, 0));
            }
        }, 0, IComPacketTypes.ARE_YOU_THERE_PERIOD_MS);
    }

    /**
     * 启动ping时钟
     */
    public void startPingTimer() {
        stopTimer(pingTimer);//如果之前有打开的时钟，就关闭
        Log.d(TAG, String.format("start PingTimer: local port:%d,remote port %d", localPort, rigPort));
        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPingPacket();//发送Ping包
            }
        }, 0, IComPacketTypes.PING_PERIOD_MS);//500ms周期
    }
    /**
     * 发送空包时钟
     */
    public void startIdleTimer() {
        stopTimer(idleTimer);
        Log.d(TAG, String.format("start Idle Timer: local port:%d,remote port %d", localPort, rigPort));
        idleTimer = new Timer();
        idleTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (txSeqBuffer.getTimeOut()>200) {//当超过200毫秒没有发送指令，就发送一个空指令
                    sendTrackedPacket(
                            IComPacketTypes.ControlPacket.toBytes(IComPacketTypes.CMD_NULL
                                    , (short) 0, localId, remoteId));
                }
            }
        }, IComPacketTypes.IDLE_PERIOD_MS, IComPacketTypes.IDLE_PERIOD_MS);
    }

    /**
     * 停止时钟
     *
     * @param timer 时钟
     */
    public void stopTimer(Timer timer) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }


    public void onReceivedPingPacket(byte[] data) {
        //两种情况，一种是电台ping我，另一个是电台回复我的Ping包
        if (IComPacketTypes.ControlPacket.getType(data) == IComPacketTypes.CMD_PING) {
            if (IComPacketTypes.PingPacket.getReply(data) == 0x00) {//电台ping我
                sendReplyPingPacket(data);//回复电台Ping
            } else {//回复我的ping，序号++
                if (IComPacketTypes.ControlPacket.getSeq(data) == pingSeq) {
                    pingSeq++;
                }
            }
        }
    }

    /**
     * 发送令牌包0x40
     * @param requestType 令牌类型，0x02确认，0x05续订
     */
    public void sendTokenPacket(byte requestType){
        sendTrackedPacket(IComPacketTypes.TokenPacket.getTokenPacketData((short)0
                ,localId,remoteId,requestType,innerSeq,localToken,rigToken));
        innerSeq++;
    }

    /**
     * 发Ping电台数据包
     */
    public void sendPingPacket() {
        byte[] data = IComPacketTypes.PingPacket.sendPingData(localId, remoteId, pingSeq);
        sendUntrackedPacket(data);//因为Ping包走自己的序列，所以发送unTracked包
        //pingSeq++;要在电台回复我之后，再自增
    }

    /**
     * 回复电台的ping
     *
     * @param data 对方的ping数据
     */
    public void sendReplyPingPacket(byte[] data) {
        byte[] packet = IComPacketTypes.PingPacket.sendReplayPingData(data, localId, remoteId);
        sendUntrackedPacket(packet);
    }

    /**
     * 发送指令数据包
     *
     * @param data 数据包
     */
    public synchronized void sendUntrackedPacket(byte[] data) {
        try {
            udpClient.sendData(data, rigIp, rigPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送tracked数据包
     *
     * @param data 数据包
     */
    public synchronized void sendTrackedPacket(byte[] data) {
        try {
            lastSentTime=System.currentTimeMillis();
            System.arraycopy(IComPacketTypes.shortToBigEndian(trackedSeq), 0
                    , data, 6, 2);//把序号写到数据列表里
            udpClient.sendData(data, rigIp, rigPort);
            txSeqBuffer.add(trackedSeq, data);
            trackedSeq++;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public int getLocalPort() {
        return localPort;
    }

    /**
     * 发送空数据包，是Tracked发送。此函数，一般在idleTimer中调用。放在此处，是为了方便调用。
     */
    public void sendIdlePacket() {
        //seq设置为0，是因为:在sendTrackedPacket中，会把trackedSeq写到数据包中
        sendTrackedPacket(IComPacketTypes.ControlPacket.idlePacketData((short) 0, localPort, remoteId));
    }


    public OnStreamEvents getOnStreamEvents() {
        return onStreamEvents;
    }

    public void setOnStreamEvents(OnStreamEvents onStreamEvents) {
        this.onStreamEvents = onStreamEvents;
    }


}
