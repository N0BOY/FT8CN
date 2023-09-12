package com.bg7yoz.ft8cn.icom;
/**
 * ICom的控制流。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

public class IcomControlUdp extends ControlUdp {
    private static final String TAG = "IcomControlUdp";


    public IcomControlUdp(String userName, String password, String remoteIp, int remotePort) {
        super(userName,password,remoteIp,remotePort);

        civUdp = new IcomCivUdp();
        audioUdp = new IcomAudioUdp();

        civUdp.rigIp = remoteIp;
        audioUdp.rigIp = remoteIp;
        civUdp.openStream();
        audioUdp.openStream();
    }


    /**
     * 处理电台发送过来的connInfo（0x90）数据包，电台发送0x90包有两次，第一次busy=0,第二次busy=1。
     * 在0x90数据包中取macAddress，电台名称
     *
     * @param data 0x90数据包
     */
    @Override
    public void onReceiveConnInfoPacket(byte[] data) {
        rigMacAddress = IComPacketTypes.ConnInfoPacket.getMacAddress(data);
        rigIsBusy = IComPacketTypes.ConnInfoPacket.getBusy(data);
        rigName = IComPacketTypes.ConnInfoPacket.getRigName(data);

        if (!rigIsBusy) {//说明是第一次收到0x90数据包，要回复一个x090数据包
            sendTrackedPacket(
                    IComPacketTypes.ConnInfoPacket.connInfoPacketData(data, (short) 0
                            , localId, remoteId
                            , (byte) 0x01, (byte) 0x03, innerSeq, localToken, rigToken
                            , rigName, userName
                            , IComPacketTypes.AUDIO_SAMPLE_RATE//接收12000采样率
                            , IComPacketTypes.AUDIO_SAMPLE_RATE//发射12000采样率
                            , civUdp.localPort, audioUdp.localPort
                            , IComPacketTypes.XIEGU_TX_BUFFER_SIZE));//0x96是wfView常用的缓冲区长度
                            //, IComPacketTypes.TX_BUFFER_SIZE));//0xf0是之前测试的缓冲区长度
            innerSeq++;
        }
    }

}
