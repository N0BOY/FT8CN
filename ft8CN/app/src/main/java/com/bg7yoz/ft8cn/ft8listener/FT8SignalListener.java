package com.bg7yoz.ft8cn.ft8listener;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.timer.OnUtcTimer;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.wave.OnGetVoiceDataDone;

import java.util.ArrayList;

public class FT8SignalListener {
    private static final String TAG = "FT8SignalListener";
    private final UtcTimer utcTimer;
    //private HamRecorder hamRecorder;
    private OnFt8Listen onFt8Listen;//当开始监听，解码结束后触发的事件
    //private long band;
    public MutableLiveData<Long> decodeTimeSec = new MutableLiveData<>();//解码的时长
    private OnWaveDataListener onWaveDataListener;

    private DatabaseOpr db;

    //用于保存解码数据的地址
//    private final long ft8Decoder=InitDecoder(0, FT8Common.SAMPLE_RATE
//            , 15*24000, true);;

    //private String myCallsign;

    static {
        System.loadLibrary("ft8cn");
    }

    public interface OnWaveDataListener {
        void getVoiceData(int duration, boolean afterDoneRemove, OnGetVoiceDataDone getVoiceDataDone);
    }

    public FT8SignalListener(DatabaseOpr db, OnFt8Listen onFt8Listen) {
        //this.hamRecorder = hamRecorder;
        this.onFt8Listen = onFt8Listen;
        this.db = db;


        //创建动作触发器，与UTC时间同步，以15秒一个周期，DoOnSecTimer是在周期起始时触发的事件。150是15秒
        utcTimer = new UtcTimer(FT8Common.FT8_SLOT_TIME_M, false, new OnUtcTimer() {
            @Override
            public void doHeartBeatTimer(long utc) {//不触发时的时钟信息
            }

            @Override
            public void doOnSecTimer(long utc) {//当指定间隔时触发时
                Log.d(TAG, String.format("触发录音,%d", utc));
                runRecorde(utc);
            }
        });
    }

    public void startListen() {
        utcTimer.start();
    }

    public void stopListen() {
        utcTimer.stop();
    }

    public boolean isListening() {
        return utcTimer.isRunning();
    }

    /**
     * 获取当前时间的偏移量，这里包括总的时钟偏移，也包括本实例的偏移
     *
     * @return
     */
    public int time_Offset() {
        return utcTimer.getTime_sec() + UtcTimer.delay;
    }

    /**
     * 录音。在后台以多线程的方式录音，录音自动生成一个临时的Wav格式文件。
     * 有两个回调函数，用于开始录音时和结束录音时。当结束录音时，激活解码程序。
     *
     * @param utc 当前解码的UTC时间
     */
    private void runRecorde(long utc) {
        Log.d(TAG, "开始录音...");

        if (onWaveDataListener != null) {
            onWaveDataListener.getVoiceData(FT8Common.FT8_SLOT_TIME_MILLISECOND, true
                    , new OnGetVoiceDataDone() {
                        @Override
                        public void onGetDone(float[] data) {
                            Log.d(TAG, "开始解码...###");
                            decodeFt8(utc, data);
                        }
                    });
        }
//
//        hamRecorder.getVoiceData(FT8Common.FT8_SLOT_TIME_MILLISECOND, true
//                , new OnGetVoiceDataDone() {
//                    @Override
//                    public void onGetDone(float[] data) {
//                        Log.d(TAG, "开始解码...");
//                        //testFt8(utc, HamRecorder.byteDataTo16BitData(data));
//                        //decodeFt8(utc, HamRecorder.getFloatFromBytes(data));
//                        decodeFt8(utc, data);
//                    }
//                });
    }

    public void decodeFt8(long utc, float[] voiceData) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        long time = System.currentTimeMillis();
        if (onFt8Listen != null) {
            onFt8Listen.beforeListen(utc);
        }
        ArrayList<Ft8Message> ft8Messages = new ArrayList<>();
        //long ft8Decoder;

        Ft8Message ft8Message = new Ft8Message(FT8Common.FT8_MODE);

        ft8Message.utcTime = utc;
        ft8Message.band = GeneralVariables.band;

        //此处，改为reset,是因为在cpp部分，改为一个变量，不是以指针，申请新内存的方式处理了。
        //其实这种方式要注意一个问题，在一个周期之内，必须解码完毕，否则新的解码又要开始了
        long ft8Decoder = InitDecoder(ft8Message.utcTime, FT8Common.SAMPLE_RATE
                , voiceData.length, true);

        //DecoderFt8Reset(ft8Decoder, ft8Message.utcTime, voiceData.length);

        DecoderMonitorPressFloat(voiceData, ft8Decoder);


        int num_candidates = DecoderFt8FindSync(ft8Decoder);
        float dt = 0;
        int dtAverage = 0;
        for (int idx = 0; idx < num_candidates; ++idx) {

            try {//做一下解码失败保护
                if (DecoderFt8Analysis(idx, ft8Decoder, ft8Message)) {
                    if (ft8Message.isValid) {
                        Ft8Message msg = new Ft8Message(ft8Message);//此处使用msg，是因为有的哈希呼号会把<...>替换掉
                        if (checkMessageSame(ft8Messages, msg)) {
                            continue;
                        }
                        dt += ft8Message.time_sec;
                        dtAverage++;
                        //ft8Messages.add(new Ft8Message(ft8Message));
                        ft8Messages.add(msg);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "run: " + e.getMessage());
            }

        }
        float time_sec = 0f;
        if (dtAverage != 0) {
            time_sec = dt / dtAverage;
            //utcTimer.setTime_sec(Math.round(time_sec * 1000));
        } else {//当检测不到时，不要偏移时间
            utcTimer.setTime_sec(Math.round(0));
        }

        //移到finalize() 方法中调用了
        DeleteDecoder(ft8Decoder);


        if (onFt8Listen != null) {
            onFt8Listen.afterDecode(utc, time_sec, UtcTimer.sequential(utc), ft8Messages);
        }

        decodeTimeSec.postValue(System.currentTimeMillis() - time);//解码耗时

        db.writeMessage(ft8Messages, GeneralVariables.myCallsign);//把消息写到数据库

        Log.d(TAG, String.format("解码耗时:%d毫秒", System.currentTimeMillis() - time));
//            }
//        }).start();
    }


    /**
     * 检查消息列表里同样的内容是否存在
     *
     * @param ft8Messages 消息列表
     * @param ft8Message  消息
     * @return
     */
    private boolean checkMessageSame(ArrayList<Ft8Message> ft8Messages, Ft8Message ft8Message) {
        for (Ft8Message msg : ft8Messages) {
            if (msg.getMessageText().equals(ft8Message.getMessageText())) {
                if (msg.snr < ft8Message.snr) {
                    msg.snr = ft8Message.snr;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        //DeleteDecoder(ft8Decoder);
        super.finalize();
    }

    public OnWaveDataListener getOnWaveDataListener() {
        return onWaveDataListener;
    }

    public void setOnWaveDataListener(OnWaveDataListener onWaveDataListener) {
        this.onWaveDataListener = onWaveDataListener;
    }

    /**
     * 解码的第一步，初始化解码器，获取解码器的地址。
     *
     * @param utcTime     UTC时间
     * @param sampleRat   采样率，12000
     * @param num_samples 缓冲区数据的长度
     * @param isFt8       是否是FT8信号
     * @return 返回解码器的地址
     */
    public native long InitDecoder(long utcTime, int sampleRat, int num_samples, boolean isFt8);

    /**
     * 解码的第二步，读取Wav数据。
     *
     * @param buffer  Wav数据缓冲区
     * @param decoder 解码器数据的地址
     */
    public native void DecoderMonitorPress(int[] buffer, long decoder);

    public native void DecoderMonitorPressFloat(float[] buffer, long decoder);


    /**
     * 解码的第三步，同步数据。
     *
     * @param decoder 解码器地址
     * @return 中标信号的数量
     */
    public native int DecoderFt8FindSync(long decoder);

    /**
     * 解码的第四步，分析出消息。（需要在一个循环里）
     *
     * @param idx        中标信号的序号
     * @param decoder    解码器的地址
     * @param ft8Message 解出来的消息
     * @return
     */
    public native boolean DecoderFt8Analysis(int idx, long decoder, Ft8Message ft8Message);

    /**
     * 解码的最后一步，删除解码器数据
     *
     * @param decoder 解码器数据的地址
     */
    public native void DeleteDecoder(long decoder);

    public native void DecoderFt8Reset(long decoder, long utcTime, int num_samples);


}
