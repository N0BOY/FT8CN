package com.bg7yoz.ft8cn.ft8listener;
/**
 * 用于监听音频的类。监听通过时钟UtcTimer来控制周期，通过OnWaveDataListener接口来读取音频数据。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;
import com.bg7yoz.ft8cn.timer.OnUtcTimer;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.ToastMessage;
import com.bg7yoz.ft8cn.wave.OnGetVoiceDataDone;
import com.bg7yoz.ft8cn.wave.WaveFileReader;
import com.bg7yoz.ft8cn.wave.WaveFileWriter;

import java.util.ArrayList;

public class FT8SignalListener {
    private static final String TAG = "FT8SignalListener";
    private final UtcTimer utcTimer;
    //private HamRecorder hamRecorder;
    private final OnFt8Listen onFt8Listen;//当开始监听，解码结束后触发的事件
    //private long band;
    public MutableLiveData<Long> decodeTimeSec = new MutableLiveData<>();//解码的时长
    public long timeSec=0;//解码的时长

    private OnWaveDataListener onWaveDataListener;


    private DatabaseOpr db;

    private final A91List a91List = new A91List();//a91列表


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
     * @return int
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
            onWaveDataListener.getVoiceData(13000, true
                    , new OnGetVoiceDataDone() {
                        @Override
                        public void onGetDone(float[] data) {
                            Log.d(TAG, String.format("开始解码...###,数据长度：%d",data.length));
                            decodeFt8(utc, data);
                        }
                    });
        }
    }

    public void decodeFt8(long utc, float[] voiceData) {

        //此处是测试用代码-------------------------
//        String fileName = getCacheFileName("test_01.wav");
//        Log.e(TAG, "onClick: fileName:" + fileName);
//        WaveFileReader reader = new WaveFileReader(fileName);
//        int data[][] = reader.getData();
        //----------------------------------------------------------

        new Thread(new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                if (onFt8Listen != null) {
                    onFt8Listen.beforeListen(utc);
                }

//                float[] tempData = ints2floats(data);


                ///读入音频数据，并做预处理
                //其实这种方式要注意一个问题，在一个周期之内，必须解码完毕，否则新的解码又要开始了
                long ft8Decoder = InitDecoder(utc, FT8Common.SAMPLE_RATE
                        , voiceData.length, true);
//                        , tempData.length, true);
                DecoderMonitorPressFloat(voiceData, ft8Decoder);//读入音频数据
//                DecoderMonitorPressFloat(tempData, ft8Decoder);//读入音频数据


                ArrayList<Ft8Message> allMsg = new ArrayList<>();
//                ArrayList<Ft8Message> msgs = runDecode(utc, voiceData,false);
                ArrayList<Ft8Message> msgs = runDecode(ft8Decoder, utc, false);
                addMsgToList(allMsg, msgs);
                timeSec = System.currentTimeMillis() - time;
                decodeTimeSec.postValue(timeSec);//解码耗时
                if (onFt8Listen != null) {
                    onFt8Listen.afterDecode(utc, averageOffset(allMsg), UtcTimer.sequential(utc), msgs, false);
                }


                if (GeneralVariables.deepDecodeMode) {//进入深度解码模式
                    ArrayList<Ft8Message> deepAll = new ArrayList<>();
                    long nextRxDeadline = utc
                            + (long) FT8Common.FT8_SLOT_TIME_MILLISECOND * 2
                            - 1000L; // leave 1s margin before next RX
                    if (System.currentTimeMillis() >= nextRxDeadline) {
                        DeleteDecoder(ft8Decoder);
                        Log.d(TAG, String.format("深度解码跳过，接近下一次接收(UTC=%d)", utc));
                        return;
                    }
                    // First deep decode pass - clear a91List to start fresh for deep decode
                    msgs = runDecode(ft8Decoder, utc, true, nextRxDeadline, true);
                    addMsgToList(allMsg, msgs);
                    addMsgToList(deepAll, msgs);
                    timeSec = System.currentTimeMillis() - time;
                    decodeTimeSec.postValue(timeSec);//解码耗时

                    do {
                        if (System.currentTimeMillis() >= nextRxDeadline) {
                            break;// leave 1s margin before next RX cycle
                        }
                        //减去解码的信号（从之前的解码中累积的）
                        try {
                            ReBuildSignal.subtractSignal(ft8Decoder, a91List);
                        } catch (Exception e) {
                            Log.e(TAG, "Deep decode: signal subtraction failed: " + e.getMessage());
                            break; // If subtraction fails, stop deep decode to avoid infinite loop
                        }

                        //再做一次解码 - don't clear a91List, accumulate for next subtraction
                        msgs = runDecode(ft8Decoder, utc, true, nextRxDeadline, false);
                        addMsgToList(allMsg, msgs);
                        addMsgToList(deepAll, msgs);
                        timeSec = System.currentTimeMillis() - time;
                        decodeTimeSec.postValue(timeSec);//解码耗时

                    } while (msgs.size() > 0);

                    if (deepAll.size() > 0 && onFt8Listen != null) {
                        onFt8Listen.afterDecode(utc, averageOffset(deepAll), UtcTimer.sequential(utc), deepAll, true);
                    }
                }
                //移到finalize() 方法中调用了
                DeleteDecoder(ft8Decoder);

                long totalDecodeMs = System.currentTimeMillis() - time;
                Log.d(TAG, String.format("解码耗时:%d毫秒", totalDecodeMs));
                long overrunMs = totalDecodeMs - FT8Common.FT8_SLOT_TIME_MILLISECOND;
                if (overrunMs > 0 && GeneralVariables.decode_overrun_toast) {
                    ToastMessage.show(String.format("Decode overrun: %d ms", overrunMs));
                }

            }
        }).start();
    }


    private ArrayList<Ft8Message> runDecode(long ft8Decoder, long utc, boolean isDeep) {
        return runDecode(ft8Decoder, utc, isDeep, null, true);
    }

    private ArrayList<Ft8Message> runDecode(long ft8Decoder, long utc, boolean isDeep, Long deadlineMs, boolean clearA91List) {
        ArrayList<Ft8Message> ft8Messages = new ArrayList<>();
        Ft8Message ft8Message = new Ft8Message(FT8Common.FT8_MODE);

        ft8Message.utcTime = utc;
        ft8Message.band = GeneralVariables.band;
        // Clear a91List only if requested (for initial decode and first deep decode)
        // Deep decode iterations should accumulate signals for subtraction
        if (clearA91List) {
            a91List.clear();
        }

        setDecodeMode(ft8Decoder, isDeep);//设置迭代次数,isDeep==true，迭代次数增加

        final int MAX_CANDIDATES_PER_BATCH = 120;
        final int MAX_BATCH_ITERATIONS = 10; // Prevent infinite loops on very busy bands
        int batchIteration = 0;

        // Process candidates in batches to handle busy bands with >120 signals
        while (batchIteration < MAX_BATCH_ITERATIONS) {
            // Check time budget if provided
            if (deadlineMs != null && System.currentTimeMillis() >= deadlineMs) {
                Log.d(TAG, String.format("Batch decode stopped: time budget exceeded (iteration %d)", batchIteration));
                break;
            }

            int num_candidates = DecoderFt8FindSync(ft8Decoder);//最多120个
            if (num_candidates == 0) {
                break; // No more candidates found
            }

            // Track a91 data for this batch to subtract after processing
            A91List batchA91List = new A91List();

            // Process candidates in this batch
            int candidatesToProcess = Math.min(num_candidates, MAX_CANDIDATES_PER_BATCH);
            for (int idx = 0; idx < candidatesToProcess; ++idx) {
                // Check time budget during candidate processing
                if (deadlineMs != null && System.currentTimeMillis() >= deadlineMs) {
                    Log.d(TAG, String.format("Batch decode stopped: time budget exceeded during candidate processing (idx %d/%d)", idx, candidatesToProcess));
                    break;
                }

                try {//做一下解码失败保护
                    if (DecoderFt8Analysis(idx, ft8Decoder, ft8Message)) {

                        if (ft8Message.isValid) {
                            Ft8Message msg = new Ft8Message(ft8Message);//此处使用msg，是因为有的哈希呼号会把<...>替换掉
                            byte[] a91 = DecoderGetA91(ft8Decoder);
                            batchA91List.add(a91, ft8Message.freq_hz, ft8Message.time_sec);
                            a91List.add(a91, ft8Message.freq_hz, ft8Message.time_sec);

                            if (checkMessageSame(ft8Messages, msg)) {
                                continue;
                            }

                            msg.isWeakSignal = isDeep;//是不是弱信号
                            ft8Messages.add(msg);

                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "runDecode: " + e.getMessage());
                }
            }

            // Subtract decoded signals to avoid re-processing them
            // If we processed a full batch (120 candidates), continue searching for more
            if (batchA91List.list.size() > 0) {
                try {
                    ReBuildSignal.subtractSignal(ft8Decoder, batchA91List);
                } catch (Exception e) {
                    Log.e(TAG, "runDecode: signal subtraction failed: " + e.getMessage());
                    // Continue processing even if subtraction fails
                }
            }
            
            // If we processed a full batch (120 candidates), search again for more candidates
            // This allows us to find additional candidates that were beyond the initial 120 limit
            if (num_candidates >= MAX_CANDIDATES_PER_BATCH) {
                batchIteration++;
            } else {
                // Fewer than 120 candidates, we're done
                break;
            }
        }

        //尝试解析剩余的哈希呼号（使用本解码过程中添加的哈希）
        resolveHashCallsigns(ft8Messages);

        return ft8Messages;
    }

    /**
     * 尝试解析消息列表中剩余的哈希呼号
     * 使用本解码过程中添加到哈希列表的呼号来解析
     *
     * @param messages 消息列表
     */
    private void resolveHashCallsigns(ArrayList<Ft8Message> messages) {
        for (Ft8Message msg : messages) {
            if (msg.callsignFrom != null && msg.callsignFrom.equals("<...>")) {
                String resolved = Ft8Message.hashList.getCallsign(new long[]{msg.callFromHash10, msg.callFromHash12, msg.callFromHash22});
                if (!resolved.equals("<...>")) {
                    msg.callsignFrom = resolved;
                }
            }
            if (msg.callsignTo != null && msg.callsignTo.equals("<...>")) {
                String resolved = Ft8Message.hashList.getCallsign(new long[]{msg.callToHash10, msg.callToHash12, msg.callToHash22});
                if (!resolved.equals("<...>")) {
                    msg.callsignTo = resolved;
                }
            }
        }
    }

    /**
     * 计算平均时间偏移值
     *
     * @param messages 消息列表
     * @return 偏移值
     */
    private float averageOffset(ArrayList<Ft8Message> messages) {
        if (messages.size() == 0) return 0f;
        float dt = 0;
        //int dtAverage = 0;
        for (Ft8Message msg : messages) {
            dt += msg.time_sec;
        }
        return dt / messages.size();
    }

    /**
     * 把消息添加到列表中
     *
     * @param allMsg 消息列表
     * @param newMsg 新的消息
     */
    private void addMsgToList(ArrayList<Ft8Message> allMsg, ArrayList<Ft8Message> newMsg) {
        for (int i = newMsg.size() - 1; i >= 0; i--) {
            if (checkMessageSame(allMsg, newMsg.get(i))) {
                newMsg.remove(i);
            } else {
                allMsg.add(newMsg.get(i));
            }
        }
    }

    /**
     * 检查消息列表里同样的内容是否存在
     *
     * @param ft8Messages 消息列表
     * @param ft8Message  消息
     * @return boolean
     */
    private boolean checkMessageSame(ArrayList<Ft8Message> ft8Messages, Ft8Message ft8Message) {
        return DecodeDuplicateFilter.isDuplicate(ft8Messages, ft8Message);
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


    public String getCacheFileName(String fileName) {
        return GeneralVariables.getMainContext().getCacheDir() + "/" + fileName;
    }

    public float[] ints2floats(int data[][]) {
        float temp[] = new float[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            temp[i] = data[0][i] / 32768.0f;
        }
        return temp;
    }

    public int[] floats2ints(float data[]) {
        int temp[] = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            temp[i] = (int) (data[i] * 32767.0f);
        }
        return temp;
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
     * @return boolean
     */
    public native boolean DecoderFt8Analysis(int idx, long decoder, Ft8Message ft8Message);

    /**
     * 解码的最后一步，删除解码器数据
     *
     * @param decoder 解码器数据的地址
     */
    public native void DeleteDecoder(long decoder);

    public native void DecoderFt8Reset(long decoder, long utcTime, int num_samples);

    public native byte[] DecoderGetA91(long decoder);//获取当前message的a91数据

    public native void setDecodeMode(long decoder, boolean isDeep);//设置解码的模式，isDeep=true是多次迭代，=false是快速迭代
}
