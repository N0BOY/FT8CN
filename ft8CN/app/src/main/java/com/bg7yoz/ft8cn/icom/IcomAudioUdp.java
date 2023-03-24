package com.bg7yoz.ft8cn.icom;
/**
 * 处理ICom的音频流。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class IcomAudioUdp extends IcomUdpBase {
    private static final String TAG = "IcomAudioUdp";

    public IcomAudioUdp() {
        udpStyle = IcomUdpStyle.AudioUdp;
    }
    private final ExecutorService doTXThreadPool =Executors.newCachedThreadPool();
    private final DoTXAudioRunnable doTXAudioRunnable=new DoTXAudioRunnable(this);

    @Override
    public void onDataReceived(DatagramPacket packet, byte[] data) {
        super.onDataReceived(packet, data);

        if (!IComPacketTypes.AudioPacket.isAudioPacket(data)) return;
        byte[] audioData = IComPacketTypes.AudioPacket.getAudioData(data);
        if (onStreamEvents != null) {
            onStreamEvents.OnReceivedAudioData(audioData);
        }
    }


    public void sendTxAudioData(float[] audioData) {
        if (audioData==null) return;

        short[] temp=new short[audioData.length];
        //要做一下浮点到16位int的转换
        for (int i = 0; i < audioData.length; i++) {
            float x = audioData[i];
            if (x > 1.0)
                x = 1.0f;
            else if (x < -1.0)
                x = -1.0f;
            temp[i] = (short) (0.5 + (x * 32767.0));
        }
        doTXAudioRunnable.audioData=temp;
        doTXThreadPool.execute(doTXAudioRunnable);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final int partialLen = IComPacketTypes.TX_BUFFER_SIZE * 2;//数据包的长度
//                //要转换一下到BYTE,小端模式
//
//                //byte[] data = new byte[audioData.length * 2 + partialLen * 4];//多出一点空声音放在前后各20ms*2共80ms
//                //先播放，是给出空的声音，for i 循环，做了一个判断，是给前面的空声音，for j循环，做得判断，是让后面发送空声音
//                byte[] audioPacket = new byte[partialLen];
//                for (int i = 0; i < (audioData.length / IComPacketTypes.TX_BUFFER_SIZE) + 8; i++) {//多出6个周期，前面3个，后面3个多
//                    if (!isPttOn) break;
//                    long now = System.currentTimeMillis() - 1;//获取当前时间
//
//                    sendTrackedPacket(IComPacketTypes.AudioPacket.getTxAudioPacket(audioPacket
//                            , (short) 0, localId, remoteId, innerSeq));
//                    innerSeq++;
//
//                    Arrays.fill(audioPacket,(byte)0x00);
//                    if (i>=3) {//让前两个空数据发送出去
//                        for (int j = 0; j < IComPacketTypes.TX_BUFFER_SIZE; j++) {
//                            if ((i-3) * IComPacketTypes.TX_BUFFER_SIZE + j < audioData.length) {
//                                System.arraycopy(IComPacketTypes.shortToBigEndian((short)
//                                                (audioData[(i-3) * IComPacketTypes.TX_BUFFER_SIZE + j]
//                                                        * GeneralVariables.volumePercent))
//                                        , 0, audioPacket, j * 2, 2);
//                            }
//                        }
//                    }
//                        while (isPttOn) {
//                            if (System.currentTimeMillis() - now >= 21) {//20毫秒一个周期
//                                break;
//                            }
//                        }
//                }
//                Log.e(TAG, "run: 音频发送完毕！！" );
//                Thread.currentThread().interrupt();
//            }
//        }).start();
    }
    private static class DoTXAudioRunnable implements Runnable{
        IcomAudioUdp icomAudioUdp;
        short[] audioData;

        public DoTXAudioRunnable(IcomAudioUdp icomAudioUdp) {
            this.icomAudioUdp = icomAudioUdp;
        }

        @Override
        public void run() {
            if (audioData==null) return;

            final int partialLen = IComPacketTypes.TX_BUFFER_SIZE * 2;//数据包的长度
            //要转换一下到BYTE,小端模式

            //byte[] data = new byte[audioData.length * 2 + partialLen * 4];//多出一点空声音放在前后各20ms*2共80ms
            //先播放，是给出空的声音，for i 循环，做了一个判断，是给前面的空声音，for j循环，做得判断，是让后面发送空声音
            byte[] audioPacket = new byte[partialLen];
            for (int i = 0; i < (audioData.length / IComPacketTypes.TX_BUFFER_SIZE) + 8; i++) {//多出6个周期，前面3个，后面3个多
                if (!icomAudioUdp.isPttOn) break;
                long now = System.currentTimeMillis() - 1;//获取当前时间

                icomAudioUdp.sendTrackedPacket(IComPacketTypes.AudioPacket.getTxAudioPacket(audioPacket
                        , (short) 0, icomAudioUdp.localId, icomAudioUdp.remoteId, icomAudioUdp.innerSeq));
                icomAudioUdp.innerSeq++;

                Arrays.fill(audioPacket,(byte)0x00);
                if (i>=3) {//让前两个空数据发送出去
                    for (int j = 0; j < IComPacketTypes.TX_BUFFER_SIZE; j++) {
                        if ((i-3) * IComPacketTypes.TX_BUFFER_SIZE + j < audioData.length) {
                            System.arraycopy(IComPacketTypes.shortToBigEndian((short)
                                            (audioData[(i-3) * IComPacketTypes.TX_BUFFER_SIZE + j]
                                                    * GeneralVariables.volumePercent))//乘以信号量的比率
                                    , 0, audioPacket, j * 2, 2);
                        }
                    }
                }
                while (icomAudioUdp.isPttOn) {
                    if (System.currentTimeMillis() - now >= 21) {//20毫秒一个周期
                        break;
                    }
                }
            }
            Log.e(TAG, "run: 音频发送完毕！！" );
            Thread.currentThread().interrupt();
        }

    }
}
