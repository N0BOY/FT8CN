package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 与发射信号有关的类，包括自动通联流程的分析与控制。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.audio.AudioRouteHelper;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.rigs.InstructionSet;
import com.bg7yoz.ft8cn.timer.OnUtcTimer;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FT8TransmitSignal {
    private static final String TAG = "FT8TransmitSignal";

    private boolean transmitFreeText = false;
    private String freeText = "FREE TEXT";

    private final DatabaseOpr databaseOpr;//配置与相关数据的数据库
    private TransmitCallsign toCallsign;//目标呼号
    public MutableLiveData<TransmitCallsign> mutableToCallsign = new MutableLiveData<>();

    private int functionOrder = 6;
    public MutableLiveData<Integer> mutableFunctionOrder = new MutableLiveData<>();//指令顺序变化
    private boolean activated = false;//是否处于可发射状态
    public MutableLiveData<Boolean> mutableIsActivated = new MutableLiveData<>();
    public int sequential;//发射时序
    public MutableLiveData<Integer> mutableSequential = new MutableLiveData<>();
    private boolean isTransmitting = false;
    public MutableLiveData<Boolean> mutableIsTransmitting = new MutableLiveData<>();//是否正在发射
    public MutableLiveData<String> mutableTransmittingMessage = new MutableLiveData<>();//当前发射消息

    //public MutableLiveData<Integer> currentOrder = new MutableLiveData<>();//当前要发射的指令

    //********************************************
    // 下面这些信息用于保存 QSL
    private long messageStartTime = 0;//消息开始时间
    private long messageEndTime = 0;//消息结束时间
    private String toMaidenheadGrid = "";//目标网格
    private int sendReport = 0;//我发送给对方的报告
    private int sentTargetReport = -100;//


    private int receivedReport = 0;//我接收到的报告
    private int receiveTargetReport = -100;//对方发给我的报告
    //********************************************
    private final OnTransmitSuccess onTransmitSuccess;//一般用于保存 QSL 数据


    // 为防止播放过程中被回收，这些变量不能放在方法内部
    private AudioAttributes attributes = null;
    private AudioFormat myFormat = null;
    private AudioTrack audioTrack = null;
    private final Object audioFinishLock = new Object();
    private boolean audioFinished = false;

    public UtcTimer utcTimer;


    public ArrayList<FunctionOfTransmit> functionList = new ArrayList<>();
    public MutableLiveData<ArrayList<FunctionOfTransmit>> mutableFunctions = new MutableLiveData<>();

    private final OnDoTransmitted onDoTransmitted;//一般用于打开和关闭 PTT
    private final ExecutorService doTransmitThreadPool = Executors.newCachedThreadPool();
    private final DoTransmitRunnable doTransmitRunnable = new DoTransmitRunnable(this);

    static {
        System.loadLibrary("ft8cn");
    }

    private boolean isFt710TxCompatibilityMode() {
        return GeneralVariables.instructionSet == InstructionSet.YAESU_FT710;
    }

    private int getTxSampleRate() {
        return GeneralVariables.audioSampleRate;
    }

    private boolean useFloatAudioOutput() {
        return GeneralVariables.audioOutput32Bit;
    }

    private int getTrackMode() {
        return AudioTrack.MODE_STATIC;
    }

    private int getChannelMask() {
        return AudioFormat.CHANNEL_OUT_MONO;
    }

    private int getChannelCount() {
        return 1;
    }

    private int getTrackBufferSizeInBytes(int sampleRate, int channelMask, boolean useFloatOutput) {
        int encoding = useFloatOutput ? AudioFormat.ENCODING_PCM_FLOAT : AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding);
        int bytesPerSample = useFloatOutput ? 4 : 2;
        int fallbackSize = sampleRate * getChannelCount() * bytesPerSample;
        if (minBufferSize > 0) {
            return Math.max(minBufferSize, fallbackSize);
        }
        return fallbackSize;
    }

    /**
     * 发射模块的构造函数。
     * 需要两个回调：一个用于发射前后控制 PTT，另一个用于发射成功后保存 QSL。
     *
     * @param databaseOpr       数据库
     * @param doTransmitted     发射前后的回调
     * @param onTransmitSuccess 发射成功后的回调
     */
    public FT8TransmitSignal(DatabaseOpr databaseOpr
            , OnDoTransmitted doTransmitted, OnTransmitSuccess onTransmitSuccess) {
        this.onDoTransmitted = doTransmitted;//用于打开关闭 PTT 的事件
        this.onTransmitSuccess = onTransmitSuccess;//用于保存 QSL 的事件
        this.databaseOpr = databaseOpr;

        setTransmitting(false);
        setActivated(false);


        // 监听音量设置变化
        GeneralVariables.mutableVolumePercent.observeForever(new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                if (audioTrack != null) {
                    audioTrack.setVolume(aFloat);
                }
            }
        });

        utcTimer = new UtcTimer(FT8Common.FT8_SLOT_TIME_M, false, new OnUtcTimer() {
            @Override
            public void doHeartBeatTimer(long utc) {

            }

            //@RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void doOnSecTimer(long utc) {
                // 超过自动监管时间就停止
                if (GeneralVariables.isLaunchSupervisionTimeout()) {
                    setActivated(false);
                    return;
                }
                if (UtcTimer.getNowSequential() == sequential && activated) {
                    if (GeneralVariables.myCallsign.length() < 3) {
                        // 我的呼号不正确，不能发射
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
                        return;
                    }
                    doTransmit();// 发射动作按时间准确执行，延迟主要来自音频链路
                }
            }
        });

        utcTimer.start();

    }

    /**
     * 立即发射。
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void transmitNow() {
        if (GeneralVariables.myCallsign.length() < 3) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return;
        }
        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.adjust_call_target)
                , toCallsign.callsign));

        // 复位信号报告相关状态
        resetTargetReport();

        if (UtcTimer.getNowSequential() == sequential) {
            if ((UtcTimer.getSystemTime() % 15000) < 2500) {
                setTransmitting(false);
                doTransmit();
            }
        }
    }

    // 发射信号
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void doTransmit() {
        if (!activated) {
            return;
        }
        // 检查是否为黑名单频率，例如 WSPR-2；总频率 = 电台频率 + 音频频率
        if (BaseRigOperation.checkIsWSPR2(
                GeneralVariables.band + Math.round(GeneralVariables.getBaseFrequency()))) {
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.use_wspr2_error)
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
            setActivated(false);
            return;
        }
        Log.d(TAG, "doTransmit: 开始发射...");
        doTransmitThreadPool.execute(doTransmitRunnable);

        mutableFunctions.postValue(functionList);
    }

    /**
     * 设置呼叫并生成发射消息列表。
     *
     * @param transmitCallsign 目标呼号
     * @param functionOrder    指令顺序
     * @param toMaidenheadGrid 目标网格
     */
    @SuppressLint("DefaultLocale")
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void setTransmit(TransmitCallsign transmitCallsign
            , int functionOrder, String toMaidenheadGrid) {

        messageStartTime = 0;//复位起始时间

        Log.d(TAG, "准备发射数据...");
        if (GeneralVariables.checkFun1(toMaidenheadGrid)) {
            this.toMaidenheadGrid = toMaidenheadGrid;
        } else {
            this.toMaidenheadGrid = "";
        }
        mutableToCallsign.postValue(transmitCallsign);// 设定呼叫目标对象，含报告、时序、频率和呼号
        toCallsign = transmitCallsign;// 设定呼叫目标
        //mutableToCallsign.postValue(toCallsign);// 设定呼叫目标

        if (functionOrder == -1) {// 说明这是回复消息
            // 此时 toMaidenheadGrid 实际上传入的是 extraInfo
            this.functionOrder = GeneralVariables.checkFunOrderByExtraInfo(toMaidenheadGrid) + 1;
            if (this.functionOrder == 6) {// 如果已经是 73，就回到消息 1
                this.functionOrder = 1;
            }
        } else {
            this.functionOrder = functionOrder;// 当前指令序号
        }

        if (transmitCallsign.frequency == 0) {
            transmitCallsign.frequency = GeneralVariables.getBaseFrequency();
        }
        if (GeneralVariables.synFrequency) {// 如果是同频发送，就与目标呼号频率一致
            setBaseFrequency(transmitCallsign.frequency);
        }

        sequential = (toCallsign.sequential + 1) % 2;// 发射时序
        mutableSequential.postValue(sequential);// 通知发射时序变化
        generateFun();
        mutableFunctionOrder.postValue(functionOrder);

    }

    @SuppressLint("DefaultLocale")
    public void setBaseFrequency(float freq) {
        GeneralVariables.setBaseFrequency(freq);
        // 写入数据库
        databaseOpr.writeConfig("freq", String.format("%.0f", freq), null);
    }

    /**
     * 根据消息序号生成对应的 FT8 消息。
     *
     * @param order 消息序号
     * @return FT8 消息
     */
    public Ft8Message getFunctionCommand(int order) {
        switch (order) {
            // 发射模式 1：BG7YOY BG7YOZ OL50
            case 1:
                resetTargetReport();// 把对方报告记录复位成 -100
                return new Ft8Message(1, 0, toCallsign.callsign, GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
            // 发射模式 2：BG7YOY BG7YOZ -10
            case 2:
                sentTargetReport = toCallsign.snr;

                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, toCallsign.getSnr());
            // 发射模式 3：BG7YOY BG7YOZ R-10
            case 3:
                sentTargetReport = toCallsign.snr;
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "R" + toCallsign.getSnr());
            // 发射模式 4：BG7YOY BG7YOZ RRR
            case 4:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "RR73");
            // 发射模式 5：BG7YOY BG7YOZ 73
            case 5:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "73");
            // 发射模式 6：CQ BG7YOZ OL50
            case 6:
                resetTargetReport();// 把双方的信号报告都复位成 -100
                Ft8Message msg = new Ft8Message(1, 0, "CQ", GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
                msg.modifier = GeneralVariables.toModifier;
                return msg;
        }

        return new Ft8Message("CQ", GeneralVariables.myCallsign
                , GeneralVariables.getMyMaidenhead4Grid());
    }

    /**
     * 生成指令序列。
     */
    public void generateFun() {
        //ArrayList<FunctionOfTransmit> functions = new ArrayList<>();
        GeneralVariables.noReplyCount = 0;
        functionList.clear();
        for (int i = 1; i <= 6; i++) {
            if (functionOrder == 6) {// 当前是 6(CQ) 时，只生成一条消息
                functionList.add(new FunctionOfTransmit(6, getFunctionCommand(6), false));
                break;
            } else {
                functionList.add(new FunctionOfTransmit(i, getFunctionCommand(i), false));
            }
        }
        mutableFunctions.postValue(functionList);
        setCurrentFunctionOrder(functionOrder);// 设置当前消息
    }

    /**
     * 为了最大限度兼容，把 32 位浮点音频转换成 16 位整型。
     *
     * @param buffer 32 位浮点音频
     * @return 16 位整型音频
     */
    private short[] float2Short(float[] buffer) {
        return float2Short(buffer, 1.0f);
    }

    private short[] float2Short(float[] buffer, float gain) {
        short[] temp = new short[buffer.length + 8];//多出8个为0的数据包，是为了兼容QP-7C的RP2040音频判断
        for (int i = 0; i < buffer.length; i++) {
            float x = buffer[i] * gain;
            if (x > 1.0)
                x = 1.0f;
            else if (x < -1.0)
                x = -1.0f;
            temp[i] = (short) (x * 32767.0);
        }
        return temp;
    }

    private int getFrameCountForShorts(short[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return 0;
        }
        return audioData.length / getChannelCount();
    }

    private int writeAllShorts(short[] audioData) {
        int totalWritten = 0;
        while (audioTrack != null && totalWritten < audioData.length) {
            int written = audioTrack.write(audioData, totalWritten, audioData.length - totalWritten,
                    AudioTrack.WRITE_BLOCKING);
            if (written <= 0) {
                return written;
            }
            totalWritten += written;
        }
        return totalWritten;
    }

    private long getAudioDurationMillis(int frameCount, int sampleRate) {
        if (frameCount <= 0 || sampleRate <= 0) {
            return 0;
        }
        return Math.round(frameCount * 1000.0 / sampleRate);
    }

    private void resetAudioFinished() {
        synchronized (audioFinishLock) {
            audioFinished = false;
        }
    }

    private void finishPlaybackOnce() {
        synchronized (audioFinishLock) {
            if (audioFinished) {
                return;
            }
            audioFinished = true;
        }
        GeneralVariables.debugLog(TAG, "finishPlaybackOnce");
        afterPlayAudio();
    }

    private void schedulePlaybackFinishFallback(long durationMillis) {
        if (durationMillis <= 0) {
            return;
        }
        GeneralVariables.debugLog(TAG, "schedule finish fallback " + durationMillis + "ms");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(durationMillis + 400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                finishPlaybackOnce();
            }
        }).start();
    }

    private void playFT8Signal(Ft8Message msg) {
        resetAudioFinished();
        GeneralVariables.debugLog(TAG, "playFT8Signal msg=" + msg.getMessageText()
                + ", baseHz=" + Math.round(GeneralVariables.getBaseFrequency())
                + ", ft710=" + isFt710TxCompatibilityMode());

        if (GeneralVariables.connectMode == ConnectMode.NETWORK) {// 网络方式不在本地播放音频
            Log.d(TAG, "playFT8Signal: 进入网络发射流程，等待音频发送。");


            if (onDoTransmitted != null) {// 处理音频数据，供 ICOM 网络模式发送
                onDoTransmitted.onTransmitByWifi(msg);
            }


            long now = System.currentTimeMillis();
            while (isTransmitting) {// 等待网络音频发送完成，再触发 afterTransmit
                try {
                    Thread.sleep(1);
                    long current = System.currentTimeMillis() - now;
                    if (current > 13100) {// 实际发射时长
                        isTransmitting = false;
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "playFT8Signal: 网络音频发送结束。");
            finishPlaybackOnce();
            return;
        }

        // 进入 CAT 串口传输音频方式
        // 2023-08-16 由 DS1UFX 提交修改（基于 0.9 版），用于支持 (tr)uSDX audio over CAT
        if (GeneralVariables.controlMode == ControlMode.CAT) {
            Log.d(TAG, "playFT8Signal: try to transmit over CAT");

            if (onDoTransmitted != null) {// 处理音频数据，供 truSDX 的 CAT 模式发送
                if (onDoTransmitted.supportTransmitOverCAT()) {
                    onDoTransmitted.onTransmitOverCAT(msg);

                    long now = System.currentTimeMillis();
                    while (isTransmitting) {// 等待音频发送完毕，再触发 afterTransmit
                        try {
                            Thread.sleep(1);
                            long current = System.currentTimeMillis() - now;
                            if (current > 13000) {// 实际发射时长
                                isTransmitting = false;
                                break;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "playFT8Signal: transmitting over CAT is finished.");
                    finishPlaybackOnce();
                    return;
                }
            }
        }


        // 进入声卡播放模式
        float[] buffer;
        int txSampleRate = getTxSampleRate();
        boolean useFloatOutput = useFloatAudioOutput();
        int trackMode = getTrackMode();
        int channelMask = getChannelMask();
        buffer = GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency()
                , txSampleRate);
        if (buffer == null) {
            GeneralVariables.debugLog(TAG, "generateFt8 returned null");
            finishPlaybackOnce();
            return;
        }
        GeneralVariables.debugLog(TAG, "generated samples=" + buffer.length
                + ", sampleRate=" + txSampleRate
                + ", trackMode=" + trackMode);

        Log.d(TAG, String.format("playFT8Signal: 准备声卡播放....位数：%s,采样率：%d"
                , useFloatOutput ? "Float32" : "Int16"
                , txSampleRate));
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        myFormat = new AudioFormat.Builder().setSampleRate(txSampleRate)
                .setEncoding(useFloatOutput
                        ? AudioFormat.ENCODING_PCM_FLOAT : AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelMask).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                , getTrackBufferSizeInBytes(txSampleRate, channelMask, useFloatOutput)
                , trackMode
                , mySession);
        AudioRouteHelper.bindTrackToPreferredOutput(audioTrack, "TX track created default");
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "playFT8Signal: AudioTrack init failed.");
            AudioRouteHelper.publishDeviceReport("TX track init failed");
            finishPlaybackOnce();
            return;
        }

        int writeResult;
        int expectedWriteLength;
        int markerPosition;
        long durationMillis;
        if (trackMode == AudioTrack.MODE_STREAM) {
            short[] audioData = float2Short(buffer);
            expectedWriteLength = audioData.length;
            markerPosition = getFrameCountForShorts(audioData);
            durationMillis = getAudioDurationMillis(markerPosition, txSampleRate);
            audioTrack.setNotificationMarkerPosition(markerPosition);
            audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack audioTrack) {
                    finishPlaybackOnce();
                }

                @Override
                public void onPeriodicNotification(AudioTrack audioTrack) {

                }
            });
            audioTrack.setVolume(GeneralVariables.volumePercent);
            audioTrack.play();
            writeResult = writeAllShorts(audioData);
        } else if (useFloatOutput) {
            expectedWriteLength = buffer.length;
            markerPosition = buffer.length;
            durationMillis = getAudioDurationMillis(markerPosition, txSampleRate);
            writeResult = audioTrack.write(buffer, 0, buffer.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        } else {
            short[] audioData = float2Short(buffer);
            expectedWriteLength = audioData.length;
            markerPosition = getFrameCountForShorts(audioData);
            durationMillis = getAudioDurationMillis(markerPosition, txSampleRate);
            writeResult = audioTrack.write(audioData, 0, audioData.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        }

        if (expectedWriteLength > writeResult) {
            Log.e(TAG, String.format("播放缓冲区不足：%d--->%d", expectedWriteLength, writeResult));
        }

        if (writeResult == AudioTrack.ERROR_INVALID_OPERATION
                || writeResult == AudioTrack.ERROR_BAD_VALUE
                || writeResult == AudioTrack.ERROR_DEAD_OBJECT
                || writeResult == AudioTrack.ERROR) {
            Log.e(TAG, String.format("播放出错：%d", writeResult));
            AudioRouteHelper.publishDeviceReport("TX write failed:" + writeResult);
            finishPlaybackOnce();
            return;
        }
        schedulePlaybackFinishFallback(durationMillis);
        if (audioTrack != null) {
            if (trackMode != AudioTrack.MODE_STREAM) {
                audioTrack.setNotificationMarkerPosition(markerPosition);
                audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack audioTrack) {
                        finishPlaybackOnce();
                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack audioTrack) {

                    }
                });
                audioTrack.setVolume(GeneralVariables.volumePercent);
                audioTrack.play();
            }
        }
    }

    private void afterPlayAudio() {
        GeneralVariables.debugLog(TAG, "afterPlayAudio release track");
        if (onDoTransmitted != null) {
            onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
        }
        isTransmitting = false;
        mutableIsTransmitting.postValue(false);
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }

    // 当通联成功时的动作
    private void doComplete() {
        messageEndTime = UtcTimer.getSystemTime();// 获取结束时间

        // 如果对方没有网格，就从历史呼号与网格对应表中查找
        toMaidenheadGrid = GeneralVariables.getGridByCallsign(toCallsign.callsign, databaseOpr);

        if (messageStartTime == 0) {// 如果起始时间为空，就取当前时间
            messageStartTime = UtcTimer.getSystemTime();
        }


        // 从历史记录中查信号报告
        // 这里单独处理信号报告，是因为保存时的记录不一定与实际通联一致
        // 遍历接收到的历史消息，查找对方发给我的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignFrom.equals(toCallsign.callsign)
                    && GeneralVariables.checkIsMyCallsign(message.callsignTo))) {
                    //&& message.callsignTo.equals(GeneralVariables.myCallsign))) {
                receiveTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }
        // 遍历历史消息，查找我发给对方的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignTo.equals(toCallsign.callsign)
                    && GeneralVariables.checkIsMyCallsign(message.callsignFrom))) {
                    //&& message.callsignFrom.equals(GeneralVariables.myCallsign))) {
                sentTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }


        messageEndTime = UtcTimer.getSystemTime();
        if (onDoTransmitted != null) {// 保存通联记录
            onTransmitSuccess.doAfterTransmit(new QSLRecord(
                    messageStartTime,
                    messageEndTime,
                    GeneralVariables.myCallsign,
                    GeneralVariables.getMyMaidenhead4Grid(),
                    toCallsign.callsign,
                    toMaidenheadGrid,
                    sentTargetReport != -100 ? sentTargetReport : sendReport,
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,// 如果目标报告有效，就优先使用目标报告
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency())
            ));

            GeneralVariables.addQSLCallsign(toCallsign.callsign);// 把通联成功的呼号加入列表
            ToastMessage.show(String.format("QSO : %s , at %s", toCallsign.callsign
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
        }

    }

    /**
     * ?靹??????????莊????鑈???
     *
     * @param order ????
     */
    public void setCurrentFunctionOrder(int order) {
        functionOrder = order;
        for (int i = 0; i < functionList.size(); i++) {
            functionList.get(i).setCurrentOrder(order);
        }
        if (order == 1) {
            resetTargetReport();//????????????
        }
        if (order == 4 || order == 5) {
            updateQSlRecordList(order, toCallsign);
        }
        mutableFunctions.postValue(functionList);
    }


    /**
     * ????????????????????????????JTDX?????????睛?
     *
     * @param fromCall ????????
     * @param toCall   ?????????
     * @return ??????
     */
    private boolean checkCallsignIsCallTo(String fromCall, String toCall) {
        if (toCall.contains("/")) {//????????????????????JTDX?皺?/?豬????鉙???
            return toCall.contains(fromCall);
        } else {
            return fromCall.equals(toCall);
        }
    }

    /**
     * ??鰕鋧???from?????????????????????????????????鋧???????0???郢鈺??????????????????????1
     *
     * @param messages ?鋧?????
     * @return 0???????????????1????????????????????鋧???>1????????????????鋧?
     */
    private int checkTargetCallMe(ArrayList<Ft8Message> messages) {
        int fromCount = 1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//???????????鋧????簧?
            if (toCallsign == null) {
                continue;
            }
            //if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                return 0;
            }
            if (checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                fromCount++;//??????from??????????鯀?
            }
        }
        return fromCount;
    }

    /**
     * ??箜??鋧????櫢??????????鋧????????郢鈊???,????-1
     *
     * @param messages ?鋧?????
     * @return ?鋧?????
     */
    private int checkFunctionOrdFromMessages(ArrayList<Ft8Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//???????????鋧????簧?
            if (toCallsign == null) {
                continue;
            }
            //??????????????
            //if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                //--TODO ----??鯣????萍?????0???郢鈹?0?????菷????茖??????????瞞?????????

                //??簗?????????????????????
                if (GeneralVariables.checkFun3(ft8Message.extraInfo)
                        || GeneralVariables.checkFun2(ft8Message.extraInfo)) {
                    //???鋧????????????譽??郢軏???????-100?????????????鋧?????????????
                    receivedReport = getReportFromExtraInfo(ft8Message.extraInfo);
                    receiveTargetReport = receivedReport;//???????????????譽??????賺???
                    if (receivedReport == -100) {//?郢軏????????????鋧???????
                        receivedReport = ft8Message.report;
                    }
                }
                sendReport = messages.get(i).snr;//?????????????????賺???

                int order = GeneralVariables.checkFunOrder(ft8Message);//??鰕鋧?????
                if (order != -1) return order;//???????簧?
            }
        }

        return -1;//?????????鋧?
    }

    /**
     * 从扩展消息中提取信号报告，失败时返回 -100。
     *
     * @param extraInfo 扩展消息
     * @return 信号报告
     */
    private int getReportFromExtraInfo(String extraInfo) {
        String s = extraInfo.replace("R", "").trim();
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -100;
        }
    }

    /**
     * 判断消息是否应该排除：
     * 1. 与当前发射时序相同
     * 2. 不在同一波段
     * 3. 呼号命中了排除前缀
     *
     * @param msg 消息
     * @return 是否排除
     */
    private boolean isExcludeMessage(Ft8Message msg) {
        return msg.getSequence() == sequential || msg.band != GeneralVariables.band
                || GeneralVariables.checkIsExcludeCallsign(msg.callsignFrom);
    }

    /**
     * 检查是否有人 CQ 我，或者我关注的呼号正在 CQ。
     *
     * @param messages 消息列表
     * @return `false` 表示没有匹配消息，`true` 表示有匹配消息
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkCQMeOrFollowCQMessage(ArrayList<Ft8Message> messages) {
        // 这里的 messages 是刚解码出的消息
        // 先检查是否有人 CQ 我，且优先判断是否是当前目标呼号，避免回复不聚焦
        for (int i = messages.size() - 1; i >= 0; i--) {// 检查是否有人 CQ 我，且不能是 73
            Ft8Message msg = messages.get(i);
            if (isExcludeMessage(msg)) continue;// 先过滤掉不参与判断的消息
            if (toCallsign == null) break;

            //if (msg.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(msg.getCallsignTo())
                    && msg.getCallsignFrom().equals(toCallsign.callsign)// todo：后续补测复合呼号场景
                    && !GeneralVariables.checkFun5(msg.extraInfo)) {// CQ 我，且不是 73，且发送方是当前目标
                // 根据当前消息内容确定下一个发射序号，避免从头开始
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                                , msg.getSequence(), msg.snr)
                        , GeneralVariables.checkFunOrder(msg) + 1
                        , msg.extraInfo);
                return true;
            }
        }

        // 再检查有没有人 CQ 我，且不是 73
        for (int i = messages.size() - 1; i >= 0; i--) {// 检查有没有人 CQ 我，且不能是 73
            Ft8Message msg = messages.get(i);
            if (isExcludeMessage(msg)) continue;// 先过滤掉不参与判断的消息
            //if ((msg.getCallsignTo().equals(GeneralVariables.myCallsign)
            if ((GeneralVariables.checkIsMyCallsign(msg.getCallsignTo())
                    && !GeneralVariables.checkFun5(msg.extraInfo))) {// CQ 我，且不是 73
                // 根据当前消息内容确定下一个发射序号，避免从头开始
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                                , msg.getSequence(), msg.snr)
                        , GeneralVariables.checkFunOrder(msg) + 1
                        , msg.extraInfo);
                return true;
            }
        }


        // 如果不自动呼叫我关注的消息，就直接退出
        if (!GeneralVariables.autoCallFollow) {
            return false;
        }

        if (toCallsign == null) {
            return false;
        }
        // 当已经有目标呼号时，不再对关注呼号作出反应
        if (toCallsign.haveTargetCallsign()) {
            return false;
        }

        // 我关注的呼号次之，在已解码的历史消息里查找正在 CQ 的对象
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message msg = GeneralVariables.transmitMessages.get(i);
            if (isExcludeMessage(msg)) continue;// 过滤不参与判断的消息

            // 处于 CQ，且 FROM 是我关注的呼号，并且不在通联成功列表中
            if ((msg.checkIsCQ()//??CQ
                    && ((GeneralVariables.autoCallFollow && GeneralVariables.autoFollowCQ)// 自动呼叫 CQ
                    || GeneralVariables.callsignInFollow(msg.getCallsignFrom()))// 是我关注的呼号
                    && !GeneralVariables.checkQSLCallsign(msg.getCallsignFrom())// 之前没有通联成功过
                    && !GeneralVariables.checkIsMyCallsign(msg.callsignFrom))) {// 不是我自己
                    //&& !msg.callsignFrom.equals(GeneralVariables.myCallsign))) {// 不是我自己

                resetTargetReport();
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                        , msg.getSequence(), msg.snr), 1, msg.extraInfo);

                return true;
            }
        }

        return false;

    }


    public void updateQSlRecordList(int order, TransmitCallsign toCall) {
        if (toCall == null) return;
        if (toCall.callsign.equals("CQ")) return;

        QSLRecord record = GeneralVariables.qslRecordList.getRecordByCallsign(toCall.callsign);
        if (record == null) {
            toMaidenheadGrid = GeneralVariables.getGridByCallsign(toCallsign.callsign, databaseOpr);
            record = GeneralVariables.qslRecordList.addQSLRecord(new QSLRecord(
                    messageStartTime,
                    messageEndTime,
                    GeneralVariables.myCallsign,
                    GeneralVariables.getMyMaidenhead4Grid(),
                    toCallsign.callsign,
                    toMaidenheadGrid,
                    sentTargetReport != -100 ? sentTargetReport : sendReport,
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,//?郢鄕????????????賁?????-100?????????????????????貍???
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency()
                    )));
        }
        //???鋧??????????
        switch (order) {
            case 1://????????????鋧???SNR
                record.setToMaidenGrid(toMaidenheadGrid);
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            case 2://????????????????????譽?????????????????
            case 3:
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                record.setReceivedReport(receiveTargetReport != -100 ? receiveTargetReport : receivedReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            //??RR73??3???????????????貶?????
            case 4:
            case 5:
                if (!record.saved) {
                    doComplete();//???豬???????
                    record.saved = true;
                }

                break;
        }

    }

    /**
     * ?????????晳篦???鋧????????????莉????莎??????
     *
     * @param msgList ?鋧?????
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void parseMessageToFunction(ArrayList<Ft8Message> msgList) {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        if (msgList.size() == 0) return;//?????鋧??簧?????

        if (msgList.get(0).getSequence() == sequential) {
            return;
        }
        ArrayList<Ft8Message> messages = new ArrayList<>(msgList);//?????????袁?


        int newOrder = checkFunctionOrdFromMessages(messages);//??鰕鋧??????????????鋧?????-1??????????
        if (newOrder != -1) {//?郢﨧??鋧????????????????????﨔?????
            GeneralVariables.noReplyCount = 0;
        }

        //????????????????晴?鯡??????????????櫢????郢鈊???????????????????
        updateQSlRecordList(newOrder, toCallsign);


        // ????????????????????73??5??||????73??5????????????????-1??
        // ??????RR73(4),???????????????耙??????????????????
        // ????RR73(4),????????????????,?篝R73??????????
        if (newOrder == 5//?鋧????????????RR73??
                || (functionOrder == 5 && newOrder == -1)// ????????????????????73??5??||????73??5????????????????-1??
                || (functionOrder == 4 &&
                (GeneralVariables.noReplyCount > GeneralVariables.noReplyLimit * 2)
                && (GeneralVariables.noReplyLimit > 0)) // ??????RR73(4),???????????????耙??????????????????

                || (functionOrder == 4 && checkTargetCallMe(messages) > 1)// ????RR73(4),??????????????????>1???????????????

                || (functionOrder == 4 && (GeneralVariables.noReplyCount > 20)
                && (GeneralVariables.noReplyLimit == 0))//??????????????????????????RR73(4)??????????????????10????????????????RR73????

        ) {
            //???CQ????
            resetToCQ();

            //?????鰕鋧???????????????????????????CQ
            checkCQMeOrFollowCQMessage(messages);
            setCurrentFunctionOrder(functionOrder);//?靹??????鋧?
            mutableFunctionOrder.postValue(functionOrder);
            return;
        }


        if (newOrder != -1) {//???????鋧??????????????
            //??????newOrder == 1???????????????????????????譽????????鋧?2.
            if (newOrder == 1 || newOrder == 2) {//??????????????????
                resetTargetReport();//?????????貂???????
                generateFun();
            }

            functionOrder = newOrder + 1;//?????????????鋧?
            mutableFunctions.postValue(functionList);
            mutableFunctionOrder.postValue(functionOrder);
            setCurrentFunctionOrder(functionOrder);//?靹??????鋧?
            return;
        }


        //????????????????????6???鋧?????????鯀?????????????
        // 2022-09-22?郢鋠簗????????????????晙????????????靹????????蔆鋧?????
        if (checkCQMeOrFollowCQMessage(messages)) {
            return;
        }


        //???????????????????????????鋧?
        //???郢鉸?????CQ??????newOrder??????-1
        if (functionOrder == 6) {//??????CQ????
            checkCQMeOrFollowCQMessage(messages);
            return;
        }


        //???????????????????????﨔???????1,??????箚?????????
        if (!messages.get(0).isWeakSignal) {
            GeneralVariables.noReplyCount++;
        }
        //?郢﨤?????????????????????CQ????
        if ((GeneralVariables.noReplyCount > GeneralVariables.noReplyLimit) && (GeneralVariables.noReplyLimit > 0)) {
            //??鮖????鋧????惞??郢鈊???????CQ???????CQ???????郢﨧???????????????????遙?
            if (!getNewTargetCallsign(messages)) {//??鮖??????櫢???CQ?鋧????郢﨧????????凜?????TRUE;
                functionOrder = 6;
                toCallsign.callsign = "CQ";
            }
            generateFun();
            setCurrentFunctionOrder(functionOrder);//?靹??????鋧?
            mutableToCallsign.postValue(toCallsign);
            mutableFunctionOrder.postValue(functionOrder);

        }

    }

    /**
     * ??鮖??????櫢?????????????CQ???鋧??????????????????????
     *
     * @param messages ???????鋧?????
     * @return ???????????????NULL
     */
    public boolean getNewTargetCallsign(ArrayList<Ft8Message> messages) {
        if (toCallsign == null) return false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.band != GeneralVariables.band) {//?郢鋧鋧??????猩??????????????晙?
                continue;
            }
            //????CQ,???晙?
            if (!ft8Message.checkIsCQ()) {
                continue;
            }
            //?????????????????????????????????????
            if ((!ft8Message.getCallsignFrom().equals(toCallsign.callsign)
                    && (!GeneralVariables.checkQSLCallsign(ft8Message.getCallsignFrom())))) //??????????????????
            {
                functionOrder = 1;
                toCallsign.callsign = ft8Message.getCallsignFrom();
                return true;
            }


        }
        return false;
    }

    public boolean isSynFrequency() {
        return GeneralVariables.synFrequency;
    }


    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        if (!this.activated) {//????????????
            setTransmitting(false);
        }
        mutableIsActivated.postValue(activated);
    }

    public boolean isTransmitting() {
        return isTransmitting;
    }

    public void setTransmitting(boolean transmitting) {
        if (GeneralVariables.myCallsign.length() < 3 && transmitting) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return;
        }

        if (!transmitting) {//????????
            if (audioTrack != null) {
                if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.pause();
                }
                if (onDoTransmitted != null) {//???????????????????菽?
                    onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
                }
            }
        }

        mutableIsTransmitting.postValue(transmitting);
        isTransmitting = transmitting;
    }

    /**
     * 将发射流程复位到 6，时序也会重新计算。
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void restTransmitting() {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        // 需要根据我的呼号类型来判断 i3/n3
        int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
        setTransmit(new TransmitCallsign(i3, 0, "CQ", UtcTimer.getNowSequential())
                , 6, "");

    }

    /**
     * 将双方信号报告复位为 -100。
     */
    public void resetTargetReport() {
        receiveTargetReport = -100;
        sentTargetReport = -100;
    }

    /**
     * 将发射流程复位到 6，但不改变时序。
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void resetToCQ() {
        resetTargetReport();
        if (toCallsign == null) {
            // 需要根据我的呼号类型来判断 i3/n3
            int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
            setTransmit(new TransmitCallsign(i3, 0, "CQ", (UtcTimer.getNowSequential() + 1) % 2)
                    , 6, "");
        } else {
            functionOrder = 6;
            toCallsign.callsign = "CQ";
            mutableToCallsign.postValue(toCallsign);// 设定呼叫目标
            generateFun();
        }
    }

    /**
     * 设置发射延迟时间，这个延迟也给上一个周期的解码留出处理时间。
     *
     * @param sec 毫秒
     */
    public void setTimer_sec(int sec) {
        utcTimer.setTime_sec(sec);
    }

    public boolean isTransmitFreeText() {
        return transmitFreeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public void setTransmitFreeText(boolean transmitFreeText) {
        this.transmitFreeText = transmitFreeText;
        if (transmitFreeText) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.trans_free_text_mode));
        } else {
            ToastMessage.show((GeneralVariables.getStringFromResource(R.string.trans_standard_messge_mode)));
        }
    }


    private static class DoTransmitRunnable implements Runnable {
        FT8TransmitSignal transmitSignal;

        public DoTransmitRunnable(FT8TransmitSignal transmitSignal) {
            this.transmitSignal = transmitSignal;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            // todo：后续可以维护一个通联上下文列表，把呼号、网格、时间和波段完整记录下来
            if (transmitSignal.functionOrder == 1 || transmitSignal.functionOrder == 2) {// 说明通联已经开始
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }
            if (transmitSignal.messageStartTime == 0) {// 如果起始时间为空，就取当前时间
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }

            // 用于显示即将发射的消息内容
            Ft8Message msg;
            if (transmitSignal.transmitFreeText) {
                msg = new Ft8Message("CQ", GeneralVariables.myCallsign, transmitSignal.freeText);
                msg.i3 = 0;
                msg.n3 = 0;
            } else {
                msg = transmitSignal.getFunctionCommand(transmitSignal.functionOrder);
            }
            msg.modifier = GeneralVariables.toModifier;
            GeneralVariables.debugLog(TAG, "DoTransmit msg=" + msg.getMessageText()
                    + ", pttDelay=" + GeneralVariables.pttDelay);

            if (transmitSignal.onDoTransmitted != null) {
                // 这里用于处理 PTT 等事件
                transmitSignal.onDoTransmitted.onBeforeTransmit(msg, transmitSignal.functionOrder);
            }

            transmitSignal.isTransmitting = true;
            transmitSignal.mutableIsTransmitting.postValue(true);


            transmitSignal.mutableTransmittingMessage.postValue(String.format(" (%.0fHz) %s"
                    , GeneralVariables.getBaseFrequency()
                    , msg.getMessageText()));
            // 生成待发射信号
//            float[] buffer=GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency());
//            if (buffer==null) {
//                return;
//            }

            // 电台动作可能需要一个延迟时间，因此时序未必完全与理论一致
            try {// 给电台一个约 100ms 的响应时间
                Thread.sleep(GeneralVariables.pttDelay);// PTT 指令发送后，给电台一个响应时间，默认 100ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            if (transmitSignal.onDoTransmitted != null) {// 处理音频数据，可供 ICOM 网络模式发送
//                transmitSignal.onDoTransmitted.onAfterGenerate(buffer);
//            }
            // 播放音频
            //transmitSignal.playFT8Signal(buffer);
            transmitSignal.playFT8Signal(msg);
        }
    }
}
