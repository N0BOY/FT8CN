package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 与发射信号有关的类。包括分析通联过程的自动程序。
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
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
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

    private final DatabaseOpr databaseOpr;//配置信息，和相关数据的数据库
    private TransmitCallsign toCallsign;//目标呼号
    public MutableLiveData<TransmitCallsign> mutableToCallsign = new MutableLiveData<>();

    private int functionOrder = 6;
    public MutableLiveData<Integer> mutableFunctionOrder = new MutableLiveData<>();//指令的顺序变化
    private boolean activated = false;//是否处于可以发射的模式
    public MutableLiveData<Boolean> mutableIsActivated = new MutableLiveData<>();
    public int sequential;//发射的时序
    public MutableLiveData<Integer> mutableSequential = new MutableLiveData<>();
    private boolean isTransmitting = false;
    public MutableLiveData<Boolean> mutableIsTransmitting = new MutableLiveData<>();//是否处于发射状态
    public MutableLiveData<String> mutableTransmittingMessage = new MutableLiveData<>();//当前消息的内容

    //public MutableLiveData<Integer> currentOrder = new MutableLiveData<>();//当前要发射的指令

    //********************************************
    //此处的信息是用于保存QSL的
    private long messageStartTime = 0;//消息开始的时间
    private long messageEndTime = 0;//消息结束的时间
    private String toMaidenheadGrid = "";//目标的网格信息
    private int sendReport = 0;//我发送到对方的报告
    private int sentTargetReport = -100;//


    private int receivedReport = 0;//我接收到的报告
    private int receiveTargetReport = -100;//发送给对方的信号报告
    //********************************************
    private final OnTransmitSuccess onTransmitSuccess;//一般是用于保存QSL数据


    //防止播放中止，变量不能放在方法中
    private AudioAttributes attributes = null;
    private AudioFormat myFormat = null;
    private AudioTrack audioTrack = null;

    public UtcTimer utcTimer;


    public ArrayList<FunctionOfTransmit> functionList = new ArrayList<>();
    public MutableLiveData<ArrayList<FunctionOfTransmit>> mutableFunctions = new MutableLiveData<>();

    private final OnDoTransmitted onDoTransmitted;//一般是用于打开关闭PTT
    private final ExecutorService doTransmitThreadPool = Executors.newCachedThreadPool();
    private final DoTransmitRunnable doTransmitRunnable = new DoTransmitRunnable(this);

    static {
        System.loadLibrary("ft8cn");
    }

    /**
     * 发射模块的构造函数，需要两个回调，一个是当发射时（有两个动作，用于打开/关闭PTT），另一个时当成功时(用于保存QSL)。
     *
     * @param databaseOpr       数据库
     * @param doTransmitted     当发射前后时的回调
     * @param onTransmitSuccess 当发射成功时的回调
     */
    public FT8TransmitSignal(DatabaseOpr databaseOpr
            , OnDoTransmitted doTransmitted, OnTransmitSuccess onTransmitSuccess) {
        this.onDoTransmitted = doTransmitted;//用于打开关闭PTT的事件
        this.onTransmitSuccess = onTransmitSuccess;//用于保存QSL的事件
        this.databaseOpr = databaseOpr;

        setTransmitting(false);
        setActivated(false);


        //观察音量设置的变化
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
                //超过自动监管时间，就停止
                if (GeneralVariables.isLaunchSupervisionTimeout()) {
                    setActivated(false);
                    return;
                }
                if (UtcTimer.getNowSequential() == sequential && activated) {
                    if (GeneralVariables.myCallsign.length() < 3) {
                        //我的呼号不正确，不能发射！
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
                        return;
                    }
                    doTransmit();//发射动作还是准确按时间来，延迟是音频信号的延迟
                }
            }
        });


        //utcTimer.setTime_sec(GeneralVariables.transmitDelay);//默认晚500毫秒发射，确保上一时序解码结束
        utcTimer.start();

    }

    /**
     * 立即发射
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void transmitNow() {
        if (GeneralVariables.myCallsign.length() < 3) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return;
        }
        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.adjust_call_target)
                , toCallsign.callsign));

        //把信号报告相关的复位
        resetTargetReport();

        if (UtcTimer.getNowSequential() == sequential) {
            if ((UtcTimer.getSystemTime() % 15000) < 2500) {
                setTransmitting(false);
                doTransmit();
            }
        }
    }

    //发射信号
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void doTransmit() {
        if (!activated) {
            return;
        }
        //检测是不是黑名单频率，WSPR-2的频率，频率=电台频率+声音频率
        if (BaseRigOperation.checkIsWSPR2(
                GeneralVariables.band + Math.round(GeneralVariables.getBaseFrequency()))) {
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.use_wspr2_error)
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
            setActivated(false);
            return;
        }
        Log.d(TAG, "doTransmit: 开始发射...");
        doTransmitThreadPool.execute(doTransmitRunnable);
//        new Thread(new Runnable() {
//            @SuppressLint("DefaultLocale")
//            @Override
//            public void run() {
//                //此处可能要修改，维护一个列表。把每个呼号，网格，时间，波段，记录下来
//                if (functionOrder == 1 || functionOrder == 2) {//当消息处于1或2时，说明开始了通联
//                    messageStartTime = UtcTimer.getSystemTime();
//                }
//                if (messageStartTime == 0) {//如果起始时间没有，就取现在的
//                    messageStartTime = UtcTimer.getSystemTime();
//                }
//
//                //用于显示将要发射的消息内容
//                Ft8Message msg;
//                if (transmitFreeText){
//                    msg=new Ft8Message("CQ",GeneralVariables.myCallsign,freeText);
//                    msg.i3=0;
//                    msg.n3=0;
//                }else {
//                    msg = getFunctionCommand(functionOrder);
//                }
//
//                if (onDoTransmitted != null) {
//                    //此处用于处理PTT等事件
//                    onDoTransmitted.onBeforeTransmit(msg, functionOrder);
//                }
//                //short[] buffer = new short[FT8Common.SAMPLE_RATE * FT8Common.FT8_SLOT_TIME];
//                //79个符号，每个符号0.16秒，采样率12000，
//                short[] buffer = new short[(int) (0.5f +
//                        GenerateFT8.num_tones * GenerateFT8.symbol_period
//                                * GenerateFT8.sample_rate)]; // 数据信号中的采样数0.5+79*0.16*12000];
//
//
//                isTransmitting = true;
//                mutableIsTransmitting.postValue(true);
//
//
//                mutableTransmittingMessage.postValue(String.format(" (%.0fHz) %s"
//                        , GeneralVariables.getBaseFrequency()
//                        , msg.getMessageText()));
//                if (!GenerateFT8.generateFt8(msg
//                        , GeneralVariables.getBaseFrequency(), buffer)) {
//                    return;
//                }
//                ;
//                //电台动作可能有要有个延迟时间，所以时间并不一定完全准确
//                try {//给电台一个100毫秒的响应时间
//                    Thread.sleep(GeneralVariables.pttDelay);//给PTT指令后，电台一个响应时间，默认100毫秒
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                if (onDoTransmitted != null) {//处理音频数据，可以给ICOM的网络模式发送
//                    onDoTransmitted.onAfterGenerate(buffer);
//                }
//                //播放音频
//                playFT8Signal(buffer);
//            }
//        }).start();
        mutableFunctions.postValue(functionList);
    }

    /**
     * 设置呼叫，生成发射消息列表
     *
     * @param transmitCallsign 目标呼号
     * @param functionOrder    命令顺序
     * @param toMaidenheadGrid 目标网格
     */
    @SuppressLint("DefaultLocale")
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void setTransmit(TransmitCallsign transmitCallsign
            , int functionOrder, String toMaidenheadGrid) {

        messageStartTime = 0;//复位起始的时间

        Log.d(TAG, "准备发射数据...");
        if (GeneralVariables.checkFun1(toMaidenheadGrid)) {
            this.toMaidenheadGrid = toMaidenheadGrid;
        } else {
            this.toMaidenheadGrid = "";
        }
        mutableToCallsign.postValue(transmitCallsign);//设定呼叫的目标对象（含报告、时序，频率，呼号）
        toCallsign = transmitCallsign;//设定呼叫的目标
        //mutableToCallsign.postValue(toCallsign);//设定呼叫的目标

        if (functionOrder == -1) {//说明是回复消息
            //此时的toMaidenheadGrid是extraInfo
            this.functionOrder = GeneralVariables.checkFunOrderByExtraInfo(toMaidenheadGrid) + 1;
            if (this.functionOrder == 6) {//如果已经是73了，就改到消息1
                this.functionOrder = 1;
            }
        } else {
            this.functionOrder = functionOrder;//当前指令的序号
        }

        if (transmitCallsign.frequency == 0) {
            transmitCallsign.frequency = GeneralVariables.getBaseFrequency();
        }
        if (GeneralVariables.synFrequency) {//如果是同频发送，就与目标呼号频率一致
            setBaseFrequency(transmitCallsign.frequency);
        }

        sequential = (toCallsign.sequential + 1) % 2;//发射的时序
        mutableSequential.postValue(sequential);//通知发射时序改变
        generateFun();
        mutableFunctionOrder.postValue(functionOrder);

    }

    @SuppressLint("DefaultLocale")
    public void setBaseFrequency(float freq) {
        GeneralVariables.setBaseFrequency(freq);
        //写到数据中
        databaseOpr.writeConfig("freq", String.format("%.0f", freq), null);
    }

    /**
     * 根据消息号，生成对应的消息
     *
     * @param order 消息号
     * @return FT8消息
     */
    public Ft8Message getFunctionCommand(int order) {
        switch (order) {
            //发射模式1，BG7YOY BG7YOZ OL50
            case 1:
                resetTargetReport();//把给对方的信号报告记录复位成-100
                return new Ft8Message(1, 0, toCallsign.callsign, GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
            //发射模式2，BG7YOY BG7YOZ -10
            case 2:
                sentTargetReport = toCallsign.snr;

                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, toCallsign.getSnr());
            //发射模式3，BG7YOY BG7YOZ R-10
            case 3:
                sentTargetReport = toCallsign.snr;
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "R" + toCallsign.getSnr());
            //发射模式4，BG7YOY BG7YOZ RRR
            case 4:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "RR73");
            //发射模式5，BG7YOY BG7YOZ 73
            case 5:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "73");
            //发射模式6，CQ BG7YOZ OL50
            case 6:
                resetTargetReport();//把给对方的信号报告,接收到对方的信号报告记录复位成-100
                Ft8Message msg = new Ft8Message(1, 0, "CQ", GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
                msg.modifier = GeneralVariables.toModifier;
                return msg;
        }

        return new Ft8Message("CQ", GeneralVariables.myCallsign
                , GeneralVariables.getMyMaidenhead4Grid());
    }

    /**
     * 生成指令序列
     */
    public void generateFun() {
        //ArrayList<FunctionOfTransmit> functions = new ArrayList<>();
        GeneralVariables.noReplyCount = 0;
        functionList.clear();
        for (int i = 1; i <= 6; i++) {
            if (functionOrder == 6) {//如果当前的指令序列是6(CQ)，那么就只生成一个消息
                functionList.add(new FunctionOfTransmit(6, getFunctionCommand(6), false));
                break;
            } else {
                functionList.add(new FunctionOfTransmit(i, getFunctionCommand(i), false));
            }
        }
        mutableFunctions.postValue(functionList);
        setCurrentFunctionOrder(functionOrder);//设置当前消息
    }

    /**
     * 为了最大限度兼容，把32位浮点转换成16位整型，有些声卡不支持32位的浮点。
     * @param buffer 32位浮点音频
     * @return 16位整型
     */
    private short[] float2Short(float[] buffer) {
        short[] temp = new short[buffer.length + 8];//多出8个为0的数据包，是为了兼容QP-7C的RP2040音频判断
        for (int i = 0; i < buffer.length; i++) {
            float x = buffer[i];
            if (x > 1.0)
                x = 1.0f;
            else if (x < -1.0)
                x = -1.0f;
            temp[i] = (short) (x * 32767.0);
        }
        return temp;
    }

    //private void playFT8Signal(float[] buffer) {
    private void playFT8Signal(Ft8Message msg) {

        if (GeneralVariables.connectMode == ConnectMode.NETWORK) {//网络方式就不播放音频了
            Log.d(TAG, "playFT8Signal: 进入网络发射程序，等待音频发送。");


            if (onDoTransmitted != null) {//处理音频数据，可以给ICOM的网络模式发送
                onDoTransmitted.onTransmitByWifi(msg);
            }


            long now = System.currentTimeMillis();
            while (isTransmitting) {//等待音频数据包发送完毕再退出，以触发afterTransmitting
                try {
                    Thread.sleep(1);
                    long current = System.currentTimeMillis() - now;
                    if (current > 13000) {//实际发射的时长
                        isTransmitting = false;
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "playFT8Signal: 退出网络音频发送。");
            afterPlayAudio();
            return;
        }


        //进入声卡模式
        float[] buffer;
        buffer = GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency()
                , GeneralVariables.audioSampleRate);
        if (buffer == null) {
            afterPlayAudio();
            return;
        }

        Log.d(TAG, String.format("playFT8Signal: 准备声卡播放....位数：%s,采样率：%d"
                , GeneralVariables.audioOutput32Bit ? "Float32" : "Int16"
                , GeneralVariables.audioSampleRate));
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        //myFormat = new AudioFormat.Builder().setSampleRate(FT8Common.SAMPLE_RATE)
        myFormat = new AudioFormat.Builder().setSampleRate(GeneralVariables.audioSampleRate)
                .setEncoding(GeneralVariables.audioOutput32Bit ? //浮点与整型
                        AudioFormat.ENCODING_PCM_FLOAT : AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                , GeneralVariables.audioOutput32Bit ? GeneralVariables.audioSampleRate * 15 * 4
                : GeneralVariables.audioSampleRate * 15 * 2//浮点与整型
                , AudioTrack.MODE_STATIC
                , mySession);

        //区分32浮点和整型
        int writeResult;
        if (GeneralVariables.audioOutput32Bit) {
            writeResult = audioTrack.write(buffer, 0, buffer.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        } else {
            short[] audio_data = float2Short(buffer);
            writeResult = audioTrack.write(audio_data, 0, audio_data.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        }

        if (buffer.length > writeResult) {
            Log.e(TAG, String.format("播放缓冲区不足：%d--->%d", buffer.length, writeResult));
        }

        //检查写入的结果，如果是异常情况，则直接需要释放资源
        if (writeResult == AudioTrack.ERROR_INVALID_OPERATION
                || writeResult == AudioTrack.ERROR_BAD_VALUE
                || writeResult == AudioTrack.ERROR_DEAD_OBJECT
                || writeResult == AudioTrack.ERROR) {
            //出异常情况
            Log.e(TAG, String.format("播放出错：%d", writeResult));
            afterPlayAudio();
            return;
        }
        audioTrack.setNotificationMarkerPosition(buffer.length);
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack audioTrack) {
                afterPlayAudio();
            }

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {

            }
        });
        if (audioTrack != null) {
            audioTrack.play();
            audioTrack.setVolume(GeneralVariables.volumePercent);//设置播放的音量
        }
    }

    /**
     * 播放完声音后的处理动作。包括回调onAfterTransmit,用于关闭PTT
     */
    private void afterPlayAudio() {
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

    //当通联成功时的动作
    private void doComplete() {
        messageEndTime = UtcTimer.getSystemTime();//获取结束的时间

        //如对方没有网格，就从历史呼号与网格对应表中查找
        toMaidenheadGrid = GeneralVariables.getGridByCallsign(toCallsign.callsign, databaseOpr);

        if (messageStartTime == 0) {//如果起始时间没有，就取现在的
            messageStartTime = UtcTimer.getSystemTime();
        }


        //从历史记录中查信号报告
        //此处处理信号报告，是因为保存的信号报告经常与实际通联的信号报告不一致。
        //遍历接收到对方的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignFrom.equals(toCallsign.callsign)
                    && message.callsignTo.equals(GeneralVariables.myCallsign))) {
                receiveTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }
        //遍历我发送给对方的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignTo.equals(toCallsign.callsign)
                    && message.callsignFrom.equals(GeneralVariables.myCallsign))) {
                sentTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }


        messageEndTime = UtcTimer.getSystemTime();
        if (onDoTransmitted != null) {//用于保存通联记录
            onTransmitSuccess.doAfterTransmit(new QSLRecord(
                    messageStartTime,
                    messageEndTime,
                    GeneralVariables.myCallsign,
                    GeneralVariables.getMyMaidenhead4Grid(),
                    toCallsign.callsign,
                    toMaidenheadGrid,
                    sentTargetReport != -100 ? sentTargetReport : sendReport,
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,//如果给对方的信号报告是不是-100，就用发给对方的信号报告记录
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency())
            ));

            GeneralVariables.addQSLCallsign(toCallsign.callsign);//把通联成功的呼号添加到列表中
            ToastMessage.show(String.format("QSO : %s , at %s", toCallsign.callsign
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
        }

    }

    /**
     * 设置当前要发射的指令顺序
     *
     * @param order 顺序
     */
    public void setCurrentFunctionOrder(int order) {
        functionOrder = order;
        for (int i = 0; i < functionList.size(); i++) {
            functionList.get(i).setCurrentOrder(order);
        }
        if (order == 1) {
            resetTargetReport();//复位信号报告
        }
        if (order == 4 || order == 5) {
            updateQSlRecordList(order, toCallsign);
        }
        mutableFunctions.postValue(functionList);
    }


    /**
     * 当目标是复合呼号（非标准信号），JTDX回复可能会缩短
     *
     * @param fromCall 对方的呼号
     * @param toCall   我的目标呼号
     * @return 是不是
     */
    private boolean checkCallsignIsCallTo(String fromCall, String toCall) {
        if (toCall.contains("/")) {//当对方的呼号在斜线时，JTDX会把/后面的字符去掉
            return toCall.contains(fromCall);
        } else {
            return fromCall.equals(toCall);
        }
    }

    /**
     * 检查消息中from中有目标呼号的数量。当有目标呼号呼叫我的消息，返回0，
     * @param messages 消息列表
     * @return 0：有目标呼叫我的，1：没有任何目标呼号发出的消息，>1：有目标呼号呼叫别人的消息
     */
    private int checkTargetCallMe(ArrayList<Ft8Message> messages){
        int fromCount=1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//同一个时序下的消息不做解析
            if (toCallsign == null) {
                continue;
            }
            if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                return 0;
            }
            if (checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)){
                fromCount++;//计数器，from是目标呼号的情况
            }
        }
        return fromCount;
    }
    /**
     * 检测本消息列表中对方回复消息的序号，如果没有,返回-1
     *
     * @param messages 消息列表
     * @return 消息的序号
     */
    private int checkFunctionOrdFromMessages(ArrayList<Ft8Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//同一个时序下的消息不做解析
            if (toCallsign == null) {
                continue;
            }
            //是双方的呼叫信息
            if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                //--TODO ----检查起始时间是不是0，如果是0，补充起始时间。因为有的呼叫会越过第一步

                //检测是不是对方给我的信号报告
                if (GeneralVariables.checkFun3(ft8Message.extraInfo)
                        || GeneralVariables.checkFun2(ft8Message.extraInfo)) {
                    //从消息中取信号报告，如果不正确（-100），那么就取消息中的信号报告
                    receivedReport = getReportFromExtraInfo(ft8Message.extraInfo);
                    receiveTargetReport = receivedReport;//对方给我的信号报告，要保存下来
                    if (receivedReport == -100) {//如果不正确，就取消息的报告
                        receivedReport = ft8Message.report;
                    }
                }
                sendReport = messages.get(i).snr;//把接收到的信号保存下来

                int order = GeneralVariables.checkFunOrder(ft8Message);//检查消息的序号
                if (order != -1) return order;//说明成功解析出序号
            }
        }

        return -1;//说明没找到消息
    }

    /**
     * 从扩展消息中获取对方给的信号报告，获取失败，值-100
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
     * 检查有没有人CQ我，或我关注的呼号在CQ
     *
     * @param messages 消息列表
     * @return false=没有符合的消息，TRUE=有符合的消息
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkCQMeOrFollowCQMessage(ArrayList<Ft8Message> messages) {
        //此message是刚刚解码出的消息
        //检查CQ我，且是我呼叫的目标
        for (int i = messages.size() - 1; i >= 0; i--) {//此处是检查有没有CQ我。（TO:ME,且不能是73）
            Ft8Message msg = messages.get(i);
            if (msg.getSequence() == sequential) {//如果与发射时序相同，不理会
                continue;
            }
            if (msg.band != GeneralVariables.band) {//如果消息不在相同的波段内，不呼叫
                continue;
            }
            if (GeneralVariables.checkIsExcludeCallsign(msg.callsignFrom)) {//如果是在过滤范围内的呼叫，不理会
                continue;
            }

            if ((msg.getCallsignTo().equals(GeneralVariables.myCallsign)
                    && !GeneralVariables.checkFun5(msg.extraInfo))) {//不能是73
                //设置发射之前，确定消息的序号，避免从头开始
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                                , msg.getSequence(), msg.snr)
                        , GeneralVariables.checkFunOrder(msg) + 1
                        , msg.extraInfo);
                return true;
            }
        }


        //如果不自动呼叫我关注的消息，就退出
        if (!GeneralVariables.autoCallFollow) {
            return false;
        }

        if (toCallsign == null) {
            return false;
        }
        //当已经有目标呼号的时候，不对关注的呼号做反应
        if (toCallsign.haveTargetCallsign()) {
            return false;
        }

        //我关注的呼号次之，！！！到关注的消息列表中找
        //此处是检查关注的呼号在CQ。（TO:CQ,且不能本次通联能成功的呼号）
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message msg = GeneralVariables.transmitMessages.get(i);
            if (msg.getSequence() == sequential) {//如果与发射时序相同，不理会
                continue;
            }
            if (msg.band != GeneralVariables.band) {//如果消息不在相同的波段内，不呼叫
                continue;
            }

            //处于CQ,FROM是我的关注呼号,并且不在通联成功的呼号列表中
            if ((msg.checkIsCQ()//在CQ
                    && ((GeneralVariables.autoCallFollow && GeneralVariables.autoFollowCQ)//自动呼叫CQ
                    || GeneralVariables.callsignInFollow(msg.getCallsignFrom()))//是我关注的
                    && !GeneralVariables.checkQSLCallsign(msg.getCallsignFrom())//之前没有联通成功过
                    && !msg.callsignFrom.equals(GeneralVariables.myCallsign))) {//不是我自己

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
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,//如果给对方的信号报告是不是-100，就用发给对方的信号报告记录
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency()
                    )));
        }
        //根据消息序列更新内容
        switch (order) {
            case 1://更新网格，和对方消息的SNR
                record.setToMaidenGrid(toMaidenheadGrid);
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            case 2://更新对方返回的信号报告，和对方的信号报告
            case 3:
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                record.setReceivedReport(receiveTargetReport != -100 ? receiveTargetReport : receivedReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            //当RR73或73的状态下，就保存日志。
            case 4:
            case 5:
                if (!record.saved) {
                    doComplete();//保存到数据库
                    record.saved = true;
                }

                break;
        }

    }

    /**
     * 从关注列表解码的消息中，此处是变化发射程序的入口
     *
     * @param msgList 消息列表
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void parseMessageToFunction(ArrayList<Ft8Message> msgList) {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        if (msgList.size() == 0) return;//没有消息解析，返回

        if (msgList.get(0).getSequence() == sequential) {
            return;
        }
        ArrayList<Ft8Message> messages =new ArrayList<>(msgList);//防止线程冲突


        int newOrder = checkFunctionOrdFromMessages(messages);//检查消息中对方回复的消息序号，-1为没有收到
        if (newOrder != -1) {//如果有消息序号，说明有回应，复位错误计数器
            GeneralVariables.noReplyCount = 0;
        }

        //更新一下通联的列表检查是不是在通联列表中，如果没有记录下来，就保存
        updateQSlRecordList(newOrder, toCallsign);


        // 判断通联成功：对方回73（5）||我是73（5），且对方没回（-1）
        // 或者我是RR73(4),且已经达到无回应阈值，且有无回应限制
        // 或我是RR73(4),且对方开始呼叫别人了,解决RR73卡死的问题
        if (newOrder == 5
                || (functionOrder == 5 && newOrder == -1)// 判断通联成功：对方回73（5）||我是73（5），且对方没回（-1）
                || (functionOrder == 4 &&
                (GeneralVariables.noReplyCount > GeneralVariables.noReplyLimit * 2)
                && (GeneralVariables.noReplyLimit > 0)) // 或者我是RR73(4),且已经达到无回应阈值，且有无回应限制
                || (functionOrder ==4 && checkTargetCallMe(messages)>1)
        ) { // 或我是RR73(4),且对方开始呼叫别人了
            //doComplete();//做保存的动作
            //进入到CQ状态
            resetToCQ();

            //加入检查消息中有没有呼号我的，或关注的呼号在CQ
            checkCQMeOrFollowCQMessage(messages);
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            mutableFunctionOrder.postValue(functionOrder);
            return;
        }


        if (newOrder != -1) {//说明收到消息，且未完成通联
            //原来是newOrder == 1，但有的时候，对方直接给信号报告，也就是消息2.
            if (newOrder == 1 || newOrder == 2) {//说明是别人第一次回复我
                resetTargetReport();//把信号报告复位一下
                generateFun();
            }

            functionOrder = newOrder + 1;//执行下一个序号的消息
            mutableFunctions.postValue(functionList);
            mutableFunctionOrder.postValue(functionOrder);
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            return;
        }


        //到此位置，我还没有在6号消息状态，检查看有没有人呼叫我
        // 2022-09-22如果这时有人呼叫我，或自动跟踪状态，我就设置新的发射消息列表
        if (checkCQMeOrFollowCQMessage(messages)) {
            return;
        }


        //到此位置，说明没有收到回复的消息
        //之明如果我是在CQ，那么newOrder必然是-1
        if (functionOrder == 6) {//我处于CQ状态
            checkCQMeOrFollowCQMessage(messages);
            return;
        }


        //到此位置，说明没有回应，错误次数要加1,弱信号检测不记无回应
        if (!messages.get(0).isWeakSignal) {
            GeneralVariables.noReplyCount++;
        }
        //如果超出无反应限定值，复位到CQ状态
        if ((GeneralVariables.noReplyCount > GeneralVariables.noReplyLimit) && (GeneralVariables.noReplyLimit > 0)) {
            //检查关注消息列表，如果没有新的CQ，就进入到CQ状态，如果有，就转入到呼叫新的目标。
            if (!getNewTargetCallsign(messages)) {//检查关注列表中的CQ消息，如果有新的目标，返回TRUE;
                functionOrder = 6;
                toCallsign.callsign = "CQ";
            }
            generateFun();
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            mutableToCallsign.postValue(toCallsign);
            mutableFunctionOrder.postValue(functionOrder);

        }

    }

    /**
     * 检查关注列表中，有没有正在CQ的消息，且不是我现在的目标呼号
     *
     * @param messages 关注的消息列表
     * @return 目标呼号，没有返回NULL
     */
    public boolean getNewTargetCallsign(ArrayList<Ft8Message> messages) {
        if (toCallsign == null) return false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.band != GeneralVariables.band) {//如果消息不在相同的波段内，不理会
                continue;
            }
            //不是CQ,不理会
            if (!ft8Message.checkIsCQ()) {
                continue;
            }
            //不是当前的目标呼号，且之前没有通联成功过
            if ((!ft8Message.getCallsignFrom().equals(toCallsign.callsign)
                    && (!GeneralVariables.checkQSLCallsign(ft8Message.getCallsignFrom())))) //之前没有联通成功过
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
        if (!this.activated) {//强制关闭发射
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

        if (!transmitting) {//停止发射
            if (audioTrack != null) {
                if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.pause();
                }
                if (onDoTransmitted != null) {//通知一下，已经不发射了
                    onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
                }
            }
        }

        mutableIsTransmitting.postValue(transmitting);
        isTransmitting = transmitting;
    }

    /**
     * 复位发射程序到6,时序也会改变
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void restTransmitting() {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        //要判断我的呼号类型，才能确定i3n3 !!!
        int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
        setTransmit(new TransmitCallsign(i3, 0, "CQ", UtcTimer.getNowSequential())
                , 6, "");

    }

    /**
     * 把给对方的信号记录复位成-100；
     */
    public void resetTargetReport() {
        receiveTargetReport = -100;
        sentTargetReport = -100;
    }

    /**
     * 复位发射程序到6，不会改变时序
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void resetToCQ() {
        resetTargetReport();
        if (toCallsign == null) {
            //要判断我的呼号类型，才能确定i3n3 !!!
            int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
            setTransmit(new TransmitCallsign(i3, 0, "CQ", (UtcTimer.getNowSequential() + 1) % 2)
                    , 6, "");
        } else {
            functionOrder = 6;
            toCallsign.callsign = "CQ";
            mutableToCallsign.postValue(toCallsign);//设定呼叫的目标
            generateFun();
        }
    }

    /**
     * 设置发射时间延迟，这个延迟时间，也是给上一个周期解码的一个时间
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
            //此处可能要修改，维护一个列表。把每个呼号，网格，时间，波段，记录下来
            if (transmitSignal.functionOrder == 1 || transmitSignal.functionOrder == 2) {//当消息处于1或2时，说明开始了通联
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }
            if (transmitSignal.messageStartTime == 0) {//如果起始时间没有，就取现在的
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }

            //用于显示将要发射的消息内容
            Ft8Message msg;
            if (transmitSignal.transmitFreeText) {
                msg = new Ft8Message("CQ", GeneralVariables.myCallsign, transmitSignal.freeText);
                msg.i3 = 0;
                msg.n3 = 0;
            } else {
                msg = transmitSignal.getFunctionCommand(transmitSignal.functionOrder);
            }
            msg.modifier = GeneralVariables.toModifier;

            if (transmitSignal.onDoTransmitted != null) {
                //此处用于处理PTT等事件
                transmitSignal.onDoTransmitted.onBeforeTransmit(msg, transmitSignal.functionOrder);
            }

            transmitSignal.isTransmitting = true;
            transmitSignal.mutableIsTransmitting.postValue(true);


            transmitSignal.mutableTransmittingMessage.postValue(String.format(" (%.0fHz) %s"
                    , GeneralVariables.getBaseFrequency()
                    , msg.getMessageText()));
            //生成信号
//            float[] buffer=GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency());
//            if (buffer==null) {
//                return;
//            }

            //电台动作可能有要有个延迟时间，所以时间并不一定完全准确
            try {//给电台一个100毫秒的响应时间
                Thread.sleep(GeneralVariables.pttDelay);//给PTT指令后，电台一个响应时间，默认100毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            if (transmitSignal.onDoTransmitted != null) {//处理音频数据，可以给ICOM的网络模式发送
//                transmitSignal.onDoTransmitted.onAfterGenerate(buffer);
//            }
            //播放音频
            //transmitSignal.playFT8Signal(buffer);
            transmitSignal.playFT8Signal(msg);
        }
    }
}
