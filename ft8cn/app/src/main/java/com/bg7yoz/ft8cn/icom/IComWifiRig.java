package com.bg7yoz.ft8cn.icom;
/**
 * WIFI模式下iCom电台操作。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.media.AudioTrack;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.icom.IcomUdpBase.IcomUdpStyle;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.IOException;

public class IComWifiRig extends WifiRig{

    public IComWifiRig(String ip, int port, String userName, String password) {
        super(ip,port,userName,password);
    }

    @Override
    public void start(){
        opened=true;
        openAudio();//打开音频
        controlUdp=new IcomControlUdp(userName,password,ip,port);

        //设置事件，这里可以处理电台状态，和接收电台送来的音频数据
        controlUdp.setOnStreamEvents(new IcomUdpBase.OnStreamEvents() {
            @Override
            public void OnReceivedIAmHere(byte[] data) {

            }

            @Override
            public void OnReceivedCivData(byte[] data) {
                if (onDataEvents!=null){
                    onDataEvents.onReceivedCivData(data);
                }
            }

            @Override
            public void OnReceivedAudioData(byte[] audioData) {
                if (onDataEvents!=null){
                    onDataEvents.onReceivedWaveData(audioData);
                }
                if (audioTrack!=null){
                   // if (!isPttOn) {//如果ptt没有按下
                        audioTrack.write(audioData, 0, audioData.length
                                , AudioTrack.WRITE_NON_BLOCKING);
                 //   }
                }
            }

            @Override
            public void OnUdpSendIOException(IcomUdpStyle style,IOException e) {
                ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                        R.string.network_exception),IcomUdpBase.getUdpStyle(style),e.getMessage()));
                close();
            }

            @Override
            public void OnLoginResponse(boolean authIsOK) {
                if (authIsOK){
                    ToastMessage.show(GeneralVariables.getStringFromResource(R.string.login_succeed));
                }else {
                    ToastMessage.show(GeneralVariables.getStringFromResource(R.string.loging_failed));
                    controlUdp.closeAll();
                }
            }

        });
        controlUdp.openStream();//打开端口
        controlUdp.startAreYouThereTimer();//开始连接电台
    }

    @Override
    public void setPttOn(boolean on){//打开PTT
        isPttOn=on;
        controlUdp.civUdp.sendPttAction(on);
        controlUdp.audioUdp.isPttOn=on;
    }

    @Override
    public void sendCivData(byte[] data){
        controlUdp.sendCivData(data);
    }

    @Override
    public void sendWaveData(float[] data){//发送音频数据到电台
        controlUdp.sendWaveData(data);
    }

    /**
     * 关闭各种连接，以及音频
     */
    @Override
    public void close(){
        opened=false;
        controlUdp.closeAll();
        closeAudio();
    }


}
