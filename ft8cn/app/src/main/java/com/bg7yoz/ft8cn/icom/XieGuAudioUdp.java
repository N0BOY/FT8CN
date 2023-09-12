package com.bg7yoz.ft8cn.icom;
/**
 * 处理协谷的音频流，继承至AudioUdp。
 *
 * @author BGY70Z
 * @date 2023-08-26
 */

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XieGuAudioUdp extends AudioUdp {
    private static final String TAG = "XieGuAudioUdp";

    private final ExecutorService doTXThreadPool = Executors.newCachedThreadPool();
//    private final DoTXAudioRunnable doTXAudioRunnable = new DoTXAudioRunnable(this);


    private final AudioRunnable audioRunnable = new AudioRunnable(this);
    private boolean audioIsRunning = false;
    //private DatagramPacket packet;
    //private byte[] data;

    @Override
    public void sendTxAudioData(float[] audioData) {
        if (audioData == null) return;

        short[] temp = new short[audioData.length];//12000
        //传递过来的音频是LPCM,32 float，12000Hz
        //要做一下浮点到16位int的转换
        for (int i = 0; i < audioData.length; i++) {
            float x = audioData[i];
            if (x > 0.999999f)
                temp[i] = 32767;
            else if (x < -0.999999f)
                temp[i] = -32766;
            else
                temp[i] = (short) (x * 32767.0);
        }

        audioRunnable.setAudioData(temp);
        //doTXAudioRunnable.audioData = temp;
        //doTXThreadPool.execute(doTXAudioRunnable);
    }

    @Override
    public void startTxAudio() {
        if (!audioIsRunning) {
            audioIsRunning = true;
            doTXThreadPool.execute(audioRunnable);

        }
    }


    @Override
    public void stopTXAudio() {
        audioIsRunning = false;
        audioRunnable.stop();
    }


    private static class AudioRunnable implements Runnable {
        private final int partialLen = (int) (IComPacketTypes.AUDIO_SAMPLE_RATE * 0.02);//20ms的数据包的长度
        private final byte[] audioPacket = new byte[partialLen * 2];
        private final byte[] ft8Audio = new byte[15 * IComPacketTypes.AUDIO_SAMPLE_RATE * 2];//15秒，采样率*2（16位，所以2倍）
        private int index = 0;
        XieGuAudioUdp audioUdp;
        private boolean isRunning = true;

        public AudioRunnable(XieGuAudioUdp audioUdp) {

            this.audioUdp = audioUdp;
            Log.e(TAG, "AudioRunnable: create runnable");
        }

        public void setAudioData(short[] audioData) {
            for (int i = 0; i < audioData.length; i++) {
                System.arraycopy(IComPacketTypes.shortToBigEndian((short)
                                (audioData[i]
                                        * GeneralVariables.volumePercent))//乘以信号量的比率
                        , 0, ft8Audio, i * 2, 2);
            }
            index = 0;

        }

        @Override
        public void run() {
            while (isRunning) {
                long now = System.currentTimeMillis() - 1;//获取当前时间
                if (audioUdp.isPttOn) {
                    System.arraycopy(ft8Audio, index, audioPacket, 0, audioPacket.length);
                    index = index + partialLen * 2;
                    if (index >= ft8Audio.length) index = 0;
                }


                audioUdp.sendTrackedPacket(IComPacketTypes.AudioPacket.getTxAudioPacket(audioPacket
                        , (short) 0, audioUdp.localId, audioUdp.remoteId, audioUdp.innerSeq));
                audioUdp.innerSeq++;
                while (isRunning) {
                    if (System.currentTimeMillis() - now >= 21) {//20毫秒一个周期
                        break;
                    }
                }
            }
        }

        public void stop() {
            isRunning = false;
        }
    }
//
//    private static class DoTXAudioRunnable implements Runnable {
//        XieGuAudioUdp audioUdp;
//        short[] audioData;//传递过来的音频是LPCM 16bit Int,12000hz
//
//        public DoTXAudioRunnable(XieGuAudioUdp audioUdp) {
//            this.audioUdp = audioUdp;
//        }
//
//        @Override
//        public void run() {
//            if (audioData == null) return;
//
//            final int partialLen = (int) (IComPacketTypes.AUDIO_SAMPLE_RATE * 0.02);//20ms的数据包的长度
//
//            //要转换一下到BYTE,小端模式
//            //先播放，是给出空的声音，for i 循环，做了一个判断，是给前面的空声音，for j循环，做得判断，是让后面发送空声音
//            byte[] audioPacket = new byte[partialLen * 2];
//            for (int i = 0; i < (audioData.length / partialLen) + 8; i++) {//多出6个周期，前面3个，后面3个多
//                if (!audioUdp.isPttOn) break;
//                long now = System.currentTimeMillis() - 1;//获取当前时间
//
//                audioUdp.sendTrackedPacket(IComPacketTypes.AudioPacket.getTxAudioPacket(audioPacket
//                        , (short) 0, audioUdp.localId, audioUdp.remoteId, audioUdp.innerSeq));
//                audioUdp.innerSeq++;
//
//                Arrays.fill(audioPacket, (byte) 0x00);
//                if (i >= 3) {//让前两个空数据发送出去
//                    for (int j = 0; j < partialLen; j++) {
//                        if ((i - 3) * partialLen + j < audioData.length) {
//                            System.arraycopy(IComPacketTypes.shortToBigEndian((short)
//                                            (audioData[(i - 3) * partialLen + j]
//                                                    * GeneralVariables.volumePercent))//乘以信号量的比率
//                                    , 0, audioPacket, j * 2, 2);
//                        }
//                    }
//                }
//                while (audioUdp.isPttOn) {
//                    if (System.currentTimeMillis() - now >= 21) {//20毫秒一个周期
//                        break;
//                    }
//                }
//            }
//            Log.d(TAG, "run: 音频发送完毕！！");
//            Thread.currentThread().interrupt();
//        }
//
//    }

    /**
     * 接收到电台发过来的音频数据
     *
     * @param packet 数据包
     * @param data   数据
     */
    @Override
    public void onDataReceived(DatagramPacket packet, byte[] data) {
        super.onDataReceived(packet, data);
        if (IComPacketTypes.CONTROL_SIZE == data.length) {
            if (IComPacketTypes.ControlPacket.getType(data) == IComPacketTypes.CMD_I_AM_READY) {
                startTxAudio();
            }
        }
        if (!IComPacketTypes.AudioPacket.isAudioPacket(data)) return;
        byte[] audioData = IComPacketTypes.AudioPacket.getAudioData(data);
        if (onStreamEvents != null) {
            onStreamEvents.OnReceivedAudioData(audioData);
        }
    }


}
