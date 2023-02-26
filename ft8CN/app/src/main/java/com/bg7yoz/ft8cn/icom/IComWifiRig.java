package com.bg7yoz.ft8cn.icom;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.icom.IcomUdpBase.IcomUdpStyle;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.IOException;

public class IComWifiRig {
    public interface OnIComDataEvents{
        void onReceivedCivData(byte[] data);
        void onReceivedWaveData(byte[] data);
    }
    private IcomControlUdp controlUdp;
    private AudioTrack audioTrack = null;
    private final String ip;
    private final int port;
    private final String userName;
    private final String password;
    public boolean opened=false;
    public boolean isPttOn=false;

    private OnIComDataEvents onIComDataEvents;



    public IComWifiRig(String ip, int port, String userName, String password) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }


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
                if (onIComDataEvents!=null){
                    onIComDataEvents.onReceivedCivData(data);
                }
            }

            @Override
            public void OnReceivedAudioData(byte[] audioData) {
                if (onIComDataEvents!=null){
                    onIComDataEvents.onReceivedWaveData(audioData);
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

    public void setPttOn(boolean on){//打开PTT
        isPttOn=on;
        controlUdp.civUdp.sendPttAction(on);
        controlUdp.audioUdp.isPttOn=on;
    }
    public void sendCivData(byte[] data){
        controlUdp.civUdp.sendCivData(data);
    }

    public void sendWaveData(short[] data){//发送音频数据到电台
        controlUdp.sendWaveData(data);
    }

    /**
     * 关闭各种连接，以及音频
     */
    public void close(){
        opened=false;
        controlUdp.closeAll();
        closeAudio();
        //controlUdp.closeStream();
    }
    /**
     * 打开音频，流方式。当收到音频流的时候，播放数据
     */
    public void openAudio() {
        if (audioTrack!=null) closeAudio();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat myFormat = new AudioFormat.Builder().setSampleRate(IComPacketTypes.AUDIO_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                , IComPacketTypes.AUDIO_SAMPLE_RATE/4, AudioTrack.MODE_STREAM
                , mySession);
        audioTrack.play();
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

    public void setOnIComDataEvents(OnIComDataEvents onIComDataEvents) {
        this.onIComDataEvents = onIComDataEvents;
    }
}
