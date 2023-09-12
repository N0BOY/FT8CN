package com.bg7yoz.ft8cn.icom;
/**
 * WIFI模式下iCom电台操作。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.icom.IcomUdpBase.IcomUdpStyle;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.IOException;

public abstract class WifiRig {
    public interface OnDataEvents {
        void onReceivedCivData(byte[] data);

        void onReceivedWaveData(byte[] data);
    }

    public ControlUdp controlUdp;
    public AudioTrack audioTrack = null;
    public final String ip;
    public final int port;
    public final String userName;
    public final String password;
    public boolean opened = false;
    public boolean isPttOn = false;

    public OnDataEvents onDataEvents;


    public WifiRig(String ip, int port, String userName, String password) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }


    public abstract void start();

    public abstract void setPttOn(boolean on);

    public abstract void sendCivData(byte[] data);

    public abstract void sendWaveData(float[] data);

    /**
     * 关闭各种连接，以及音频
     */
    public abstract void close();


    /**
     * 打开音频，流方式。当收到音频流的时候，播放数据
     */
    public void openAudio() {
        if (audioTrack != null) closeAudio();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat myFormat = new AudioFormat.Builder().setSampleRate(IComPacketTypes.AUDIO_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int mySession = 0;

        audioTrack = new AudioTrack(attributes, myFormat
                , IComPacketTypes.AUDIO_SAMPLE_RATE * 4, AudioTrack.MODE_STREAM
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

    public void setOnDataEvents(OnDataEvents onDataEvents) {
        this.onDataEvents = onDataEvents;
    }
}
