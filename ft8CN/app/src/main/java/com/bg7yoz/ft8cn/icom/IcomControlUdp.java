package com.bg7yoz.ft8cn.icom;

import android.util.Log;

import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

public class IcomControlUdp extends IcomUdpBase {
    private static final String TAG = "IcomControlUdp";
    public final String APP_NAME = "FT8CN";

    //与采样率有关，每20ms发送的样本数12000/50=240=F0，实际字节数是（16bit），还要乘以2，也就是480字节


    public Timer tokenTimer;//续订令牌的时钟

    public String userName;
    public String password;
    public String rigName = "";
    public String audioName = "";
    public byte[] rigMacAddress=new byte[6];//0xA8、0x90包中提供
    public String connectionMode = "";

    public boolean gotAuthOK = false;//token认证通过了
    public boolean isAuthenticated = false;//登录成功
    public boolean rigIsBusy = false;

    public IcomCivUdp civUdp;
    public IcomAudioUdp audioUdp;


    public IcomControlUdp(String userName, String password, String remoteIp, int remotePort) {
        udpStyle=IcomUdpStyle.ControlUdp;
        this.userName = userName;
        this.password = password;

        this.rigIp = remoteIp;
        this.rigPort = remotePort;

        civUdp = new IcomCivUdp();
        audioUdp = new IcomAudioUdp();
        civUdp.rigIp = remoteIp;
        audioUdp.rigIp = remoteIp;
        civUdp.openStream();
        audioUdp.openStream();
    }

    @Override
    public void onDataReceived(DatagramPacket packet,byte[] data) {
        super.onDataReceived(packet,data);
        switch (data.length) {
            case IComPacketTypes.CONTROL_SIZE://在父类中已经实现0x04,0x01指令
                if (IComPacketTypes.ControlPacket.getType(data) == IComPacketTypes.CMD_I_AM_HERE) {
                    rigIp=packet.getAddress().getHostAddress();
                    //civUdp.rigIp=packet.getAddress().getHostAddress();
                    //audioUdp.rigIp=packet.getAddress().getHostAddress();
                }  //如果电台回复I'm ready,就发起login
                if (IComPacketTypes.ControlPacket.getType(data) == IComPacketTypes.CMD_I_AM_READY) {
                    sendLoginPacket();//电台准备好了，申请登录
                    startIdleTimer();//打开发送空包时钟
                    }
                break;
            case IComPacketTypes.TOKEN_SIZE://处理令牌的续订之类的事情
                onReceiveTokenPacket(data);
                break;
            case IComPacketTypes.STATUS_SIZE://0x50电台回复我它的参数：CivPort,AudioPort等
                onReceiveStatusPacket(data);
                break;
            case IComPacketTypes.LOGIN_RESPONSE_SIZE://0x60电台回复登录的请求
                onReceiveLoginResponse(data);
                break;
            case IComPacketTypes.CONNINFO_SIZE://电台会回复2次0x90包，区别在于busy字段
                onReceiveConnInfoPacket(data);
                break;
            case IComPacketTypes.CAP_CAPABILITIES_SIZE://0xA8数据包,返回civ地址
                byte[] audioCap = IComPacketTypes.CapCapabilitiesPacket.getRadioCapPacket(data, 0);
                if (audioCap!=null) {
                    civUdp.supportTX = IComPacketTypes.RadioCapPacket.getSupportTX(audioCap);
                    civUdp.civAddress = IComPacketTypes.RadioCapPacket.getCivAddress(audioCap);
                    audioName = IComPacketTypes.RadioCapPacket.getAudioName(audioCap);
                }
                break;
        }
    }

    /**
     * 处理电台发送过来的connInfo（0x90）数据包，电台发送0x90包有两次，第一次busy=0,第二次busy=1。
     * 在0x90数据包中取macAddress，电台名称
     *
     * @param data 0x90数据包
     */
    public void onReceiveConnInfoPacket(byte[] data) {
        rigMacAddress = IComPacketTypes.ConnInfoPacket.getMacAddress(data);
        rigIsBusy = IComPacketTypes.ConnInfoPacket.getBusy(data);
        rigName = IComPacketTypes.ConnInfoPacket.getRigName(data);
        //if (!rigIsBusy) {//说明是第一次收到0x90数据包，要回复一个x090数据包
            Log.e(TAG, "onReceiveConnInfoPacket: send 0x90");
            sendTrackedPacket(
                    IComPacketTypes.ConnInfoPacket.connInfoPacketData(data, (short) 0
                            , localId, remoteId
                            , (byte) 0x01, (byte) 0x03, innerSeq, localToken, rigToken
                            , rigName, userName
                            , IComPacketTypes.AUDIO_SAMPLE_RATE//48000采样率
                            , civUdp.localPort, audioUdp.localPort
                            , IComPacketTypes.TX_BUFFER_SIZE));
            innerSeq++;
        //}
    }

    /**
     * 处理电台回复登录数据包
     *
     * @param data 0x60数据包
     */
    public void onReceiveLoginResponse(byte[] data) {
        if (IComPacketTypes.ControlPacket.getType(data) == 0x01) return;
        connectionMode = IComPacketTypes.LoginResponsePacket.getConnection(data);
        Log.d(TAG, "connection mode:" + connectionMode);
        if (IComPacketTypes.LoginResponsePacket.authIsOK(data)) {//errorCode=0x00,认证成功
            Log.d(TAG, "onReceiveLoginResponse: Login succeed!");
            if (!isAuthenticated) {
                rigToken = IComPacketTypes.LoginResponsePacket.getToken(data);
                Log.d(TAG, "onReceiveLoginResponse: send token confirm 0x02");
                sendTokenPacket(IComPacketTypes.TOKEN_TYPE_CONFIRM);//发送令牌确认包
                startTokenTimer();//启动令牌续订时钟
                isAuthenticated = true;
            }
        }
        if (onStreamEvents!=null){//触发认证事件
            onStreamEvents.OnLoginResponse(IComPacketTypes.LoginResponsePacket.authIsOK(data));
        }
    }

    /**
     * 处理电台回复我的参数。0x50数据包
     *
     * @param data 0x50数据包
     */
    public void onReceiveStatusPacket(byte[] data) {
        if (IComPacketTypes.ControlPacket.getType(data) == 0x01) return;
        if (IComPacketTypes.StatusPacket.getAuthOK(data)
                && IComPacketTypes.StatusPacket.getIsConnected(data)) {//令牌认证成功，且处于连接状态
            audioUdp.rigPort = IComPacketTypes.StatusPacket.getRigAudioPort(data);
            audioUdp.rigIp = rigIp;
            civUdp.rigPort = IComPacketTypes.StatusPacket.getRigCivPort(data);
            civUdp.rigIp = rigIp;
            Log.e(TAG, String.format("onReceiveStatusPacket: Status packet 0x50: civRigPort:%d,audioRigPort:%d"
                    ,civUdp.rigPort,audioUdp.rigPort ));
            civUdp.startAreYouThereTimer();//civ端口启动连接电台
            audioUdp.startAreYouThereTimer();//audio端口启动连接电台
        }//else处理关闭连接？？？
    }

    /**
     * 处理令牌数据包
     *
     * @param data 0x40数据包
     */
    public void onReceiveTokenPacket(byte[] data) {
        //看是不是续订令牌包
        if (IComPacketTypes.TokenPacket.getRequestType(data) == IComPacketTypes.TOKEN_TYPE_RENEWAL
                && IComPacketTypes.TokenPacket.getRequestReply(data) == 0x02
                && IComPacketTypes.ControlPacket.getType(data) != IComPacketTypes.CMD_RETRANSMIT) {
            int response = IComPacketTypes.TokenPacket.getResponse(data);
            if (response == 0x0000) {//说明续订成功了
                gotAuthOK = true;
            } else if (response == 0xffffffff) {
                remoteId = IComPacketTypes.ControlPacket.getSentId(data);
                localToken = IComPacketTypes.TokenPacket.getTokRequest(data);
                rigToken = IComPacketTypes.TokenPacket.getToken(data);
                sendConnectionRequest();//申请连接
            } else {
                Log.e(TAG, "Token renewal failed,unknow response");
            }
        }
    }

    /**
     * 发送音频数据到电台
     * @param data 数据
     */
    public void sendWaveData(short[] data){
        audioUdp.sendTxAudioData(data);
    }

    /**
     * 发送0x90数据包，向电台请求连接
     */
    public void sendConnectionRequest() {
        sendTrackedPacket(IComPacketTypes.ConnInfoPacket.connectRequestPacket((short) 0
                , localId, remoteId, (byte) 0x01, (byte) 0x03, innerSeq, localToken, rigToken
                , rigMacAddress, rigName, userName, IComPacketTypes.AUDIO_SAMPLE_RATE
                , civUdp.getLocalPort(), audioUdp.getLocalPort()
                , IComPacketTypes.TX_BUFFER_SIZE));
        innerSeq++;
    }

    /**
     * 发送登录数据包
     */
    public void sendLoginPacket() {
        sendTrackedPacket(IComPacketTypes.LoginPacket.loginPacketData((short) 0
                , localId, remoteId, innerSeq, localToken, rigToken, userName, password, APP_NAME));
        innerSeq++;
    }

    @Override
    public void setOnStreamEvents(OnStreamEvents onStreamEvents) {
        super.setOnStreamEvents(onStreamEvents);
        audioUdp.onStreamEvents=onStreamEvents;
        civUdp.onStreamEvents=onStreamEvents;
    }

    /**
     * 启动令牌续订时钟
     */
    public void startTokenTimer() {
        stopTimer(tokenTimer);
        Log.d(TAG, String.format("start Toke Timer: local port:%d,remote port %d", localPort, rigPort));
        tokenTimer = new Timer();
        tokenTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendTokenPacket(IComPacketTypes.TOKEN_TYPE_RENEWAL);
            }
        }, IComPacketTypes.TOKEN_RENEWAL_PERIOD_MS, IComPacketTypes.TOKEN_RENEWAL_PERIOD_MS);
    }
    public void closeAll(){
        sendTrackedPacket(IComPacketTypes.TokenPacket.getTokenPacketData((short)0
                ,localId,remoteId,IComPacketTypes.TOKEN_TYPE_DELETE,innerSeq,localToken,rigToken));
        innerSeq++;
        this.close();
        civUdp.close();
        audioUdp.close();

        civUdp.sendOpenClose(false);
    }
}
