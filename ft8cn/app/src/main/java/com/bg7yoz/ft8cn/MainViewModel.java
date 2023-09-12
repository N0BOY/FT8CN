package com.bg7yoz.ft8cn;
/**
 * -----2022.5.6-----
 * MainViewModel类，用于解码FT8信号以及保存与解码有关的变量数据。生存于APP的整个生命周期。
 * 1.解码的总条数。decoded_counter和mutable_Decoded_Counter。
 * 2.解码消息的列表。消息以Ft8Message展示，列表用ArrayList泛型实现。ft8Messages，mutableFt8MessageList。
 * 3.解码和录音都需要时间同步，也就是以UTC时间的每15秒为一个周期。同步事件的触发由UtcTimer类来实现。
 * 4.当前的UTC时间。timerSec，更新频率（心跳频率）由UtcTimer确定，暂定100毫秒。
 * 5.通过类方法getInstance获取当前的MainViewModel的实例，确保有唯一的实例。
 * 6.用HamAudioRecorder类实现录音，目前只实现录音成文件，然后读取文件的数据给解码模块，后面要改成直接给数组的方式----TO DO---
 * 7.解码采用JNI接口调用原生C语言。调用接口名时ft8cn，由cpp文件夹下的CMakeLists.txt维护。各函数的调用接口在decode_ft8.cpp中。
 * -----2022.5.9-----
 * 如果系统没有发射信号，触发器会在每一个周期触发录音动作，因录音开始和结束要浪费一些时间，如果不干预上一个录音的动作，将出现
 * 连续的周期内录音动作重叠，造成第二个录音动作失败。所以，第二个周期的录音开始前，要停止前一个周期的录音，造成的结果就是每一次录音
 * 的开始时间要晚于周期开始300毫秒（模拟器的结果），实际录音的长度一般在14.77秒左右
 * <p>
 *
 * 2023-08-16 由DS1UFX提交修改（基于0.9版），增加(tr)uSDX audio over cat的支持。
 *
 * @author BG7YOZ
 * @date 2022.8.22
 */

import static com.bg7yoz.ft8cn.GeneralVariables.getStringFromResource;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.bg7yoz.ft8cn.callsign.CallsignDatabase;
import com.bg7yoz.ft8cn.callsign.CallsignInfo;
import com.bg7yoz.ft8cn.callsign.OnAfterQueryCallsignLocation;
import com.bg7yoz.ft8cn.connector.BluetoothRigConnector;
import com.bg7yoz.ft8cn.connector.CableConnector;
import com.bg7yoz.ft8cn.connector.CableSerialPort;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.connector.FlexConnector;
import com.bg7yoz.ft8cn.connector.IComWifiConnector;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.database.OnAfterQueryFollowCallsigns;
import com.bg7yoz.ft8cn.database.OperationBand;
import com.bg7yoz.ft8cn.flex.FlexRadio;
import com.bg7yoz.ft8cn.ft8listener.FT8SignalListener;
import com.bg7yoz.ft8cn.ft8listener.OnFt8Listen;
import com.bg7yoz.ft8cn.ft8transmit.FT8TransmitSignal;
import com.bg7yoz.ft8cn.ft8transmit.OnDoTransmitted;
import com.bg7yoz.ft8cn.ft8transmit.OnTransmitSuccess;
import com.bg7yoz.ft8cn.html.LogHttpServer;
import com.bg7yoz.ft8cn.icom.WifiRig;
import com.bg7yoz.ft8cn.log.QSLCallsignRecord;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.log.SWLQsoList;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.rigs.ElecraftRig;
import com.bg7yoz.ft8cn.rigs.Flex6000Rig;
import com.bg7yoz.ft8cn.rigs.FlexNetworkRig;
import com.bg7yoz.ft8cn.rigs.GuoHeQ900Rig;
import com.bg7yoz.ft8cn.rigs.IcomRig;
import com.bg7yoz.ft8cn.rigs.InstructionSet;
import com.bg7yoz.ft8cn.rigs.KenwoodKT90Rig;
import com.bg7yoz.ft8cn.rigs.KenwoodTS2000Rig;
import com.bg7yoz.ft8cn.rigs.KenwoodTS570Rig;
import com.bg7yoz.ft8cn.rigs.KenwoodTS590Rig;
import com.bg7yoz.ft8cn.rigs.OnRigStateChanged;
import com.bg7yoz.ft8cn.rigs.TrUSDXRig;
import com.bg7yoz.ft8cn.rigs.Wolf_sdr_450Rig;
import com.bg7yoz.ft8cn.rigs.XieGu6100Rig;
import com.bg7yoz.ft8cn.rigs.XieGuRig;
import com.bg7yoz.ft8cn.rigs.Yaesu2Rig;
import com.bg7yoz.ft8cn.rigs.Yaesu38Rig;
import com.bg7yoz.ft8cn.rigs.Yaesu38_450Rig;
import com.bg7yoz.ft8cn.rigs.Yaesu39Rig;
import com.bg7yoz.ft8cn.rigs.YaesuDX10Rig;
import com.bg7yoz.ft8cn.spectrum.SpectrumListener;
import com.bg7yoz.ft8cn.timer.OnUtcTimer;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.ToastMessage;
import com.bg7yoz.ft8cn.wave.HamRecorder;
import com.bg7yoz.ft8cn.wave.OnGetVoiceDataDone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainViewModel extends ViewModel {
    String TAG = "ft8cn MainViewModel";
    public boolean configIsLoaded = false;

    private static MainViewModel viewModel = null;//当前存在的实例。
    //public static Application application;


    //public int decoded_counter = 0;//解码的总条数
    public final ArrayList<Ft8Message> ft8Messages = new ArrayList<>();//消息列表
    public UtcTimer utcTimer;//同步触发动作的计时器。


    //public CallsignDatabase callsignDatabase = null;//呼号信息的数据库
    public DatabaseOpr databaseOpr;//配置信息，和相关数据的数据库


    public MutableLiveData<Integer> mutable_Decoded_Counter = new MutableLiveData<>();//解码的总条数
    public int currentDecodeCount = 0;//本次解码的条数
    public MutableLiveData<ArrayList<Ft8Message>> mutableFt8MessageList = new MutableLiveData<>();//消息列表
    public MutableLiveData<Long> timerSec = new MutableLiveData<>();//当前UTC时间。更新频率由UtcTimer确定，未触发时约100毫秒。
    public MutableLiveData<Boolean> mutableIsRecording = new MutableLiveData<>();//是否处于录音状态
    public MutableLiveData<Boolean> mutableHamRecordIsRunning = new MutableLiveData<>();//HamRecord是否运转
    public MutableLiveData<Float> mutableTimerOffset = new MutableLiveData<>();//本周期的时间延迟
    public MutableLiveData<Boolean> mutableIsDecoding = new MutableLiveData<>();//会触发频谱图中的标记动作
    public ArrayList<Ft8Message> currentMessages = null;//本周期解码的消息（用于画到频谱上）

    public MutableLiveData<Boolean> mutableIsFlexRadio = new MutableLiveData<>();//是不是flex电台

    private final ExecutorService getQTHThreadPool = Executors.newCachedThreadPool();
    private final ExecutorService sendWaveDataThreadPool = Executors.newCachedThreadPool();
    private final GetQTHRunnable getQTHRunnable = new GetQTHRunnable(this);
    private final SendWaveDataRunnable sendWaveDataRunnable = new SendWaveDataRunnable();


    public HamRecorder hamRecorder;//用于录音的对象
    public FT8SignalListener ft8SignalListener;//用于监听FT8信号并解码的对象
    public FT8TransmitSignal ft8TransmitSignal;//用于发射信号用的对象
    public SpectrumListener spectrumListener;//用于画频谱的对象
    public boolean markMessage = true;//是否标记消息开关

    //控制电台的方式
    public OperationBand operationBand = null;

    private SWLQsoList swlQsoList = new SWLQsoList();//用于记录SWL的QSO对象，对SWL QSO做判断，防止重复。


    public MutableLiveData<ArrayList<CableSerialPort.SerialPort>> mutableSerialPorts = new MutableLiveData<>();
    private ArrayList<CableSerialPort.SerialPort> serialPorts;//串口列表
    public BaseRig baseRig;//电台
    private final OnRigStateChanged onRigStateChanged = new OnRigStateChanged() {
        @Override
        public void onDisconnected() {
            //与电台连接中断
            ToastMessage.show(getStringFromResource(R.string.disconnect_rig));
        }

        @Override
        public void onConnected() {
            //与电台建立连接
            ToastMessage.show(getStringFromResource(R.string.connected_rig));
        }

        @Override
        public void onPttChanged(boolean isOn) {

        }

        @Override
        public void onFreqChanged(long freq) {
            //当前频率:%s
            ToastMessage.show(String.format(getStringFromResource(R.string.current_frequency)
                    , BaseRigOperation.getFrequencyAllInfo(freq)));
            //把频率的变化写回到全局变量中
            GeneralVariables.band = freq;
            GeneralVariables.bandListIndex = OperationBand.getIndexByFreq(freq);
            GeneralVariables.mutableBandChange.postValue(GeneralVariables.bandListIndex);

            databaseOpr.getAllQSLCallsigns();//通联成功的呼号读出来

        }

        @Override
        public void onRunError(String message) {
            //与电台通讯出现错误，
            ToastMessage.show(String.format(getStringFromResource(R.string.radio_communication_error)
                    , message));
        }
    };

    //发射信号用的消息列表
    //public ArrayList<Ft8Message> transmitMessages = new ArrayList<>();
    //public MutableLiveData<ArrayList<Ft8Message>> mutableTransmitMessages = new MutableLiveData<>();
    public MutableLiveData<Integer> mutableTransmitMessagesCount = new MutableLiveData<>();


    public boolean deNoise = false;//在频谱中抑制噪声

    //*********日志查询需要的变量********************
    public boolean logListShowCallsign = false;//在日志查询列表的表现形式
    public String queryKey = "";//查询的关键字
    public int queryFilter = 0;//过滤，0全部，1，确认，2，未确认
    public MutableLiveData<Integer> mutableQueryFilter = new MutableLiveData<>();
    public ArrayList<QSLCallsignRecord> callsignRecords = new ArrayList<>();
    //public ArrayList<QSLRecordStr> qslRecords=new ArrayList<>();
    //********************************************
    //关注呼号的列表
    //public ArrayList<String> followCallsign = new ArrayList<>();


    //日志管理HTTP SERVER
    private final LogHttpServer httpServer;

    /**
     * 获取MainViewModel的实例，确保存在唯一的MainViewModel实例，该实例在APP的全部生存周期中。
     *
     * @param owner ViewModelStoreOwner 所有者，一般为Activity或Fragment。
     * @return MainViewModel 返回一个MainViewModel实例
     */
    public static MainViewModel getInstance(ViewModelStoreOwner owner) {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(owner).get(MainViewModel.class);
        }
        return viewModel;
    }

    /**
     * 获取消息列表中指定的消息
     *
     * @param position 在Mutable类型的列表中的位置
     * @return 返回一个Ft8Message类型的解码后的信息
     */
    public Ft8Message getFt8Message(int position) {
        return Objects.requireNonNull(ft8Messages.get(position));
    }

    /**
     * MainViewModel的构造函数主要完成一下事情：
     * 1.创建与UTC同步的时钟，时钟是UtcTimer类，内核是用Timer和TimerTask实现的。回调函数是多线程的，要考虑线程安全的问题。
     * 2.创建Mutable型的解码消息列表。
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public MainViewModel() {

        //获取配置信息。
        databaseOpr = DatabaseOpr.getInstance(GeneralVariables.getMainContext()
                , "data.db");
        mutableIsDecoding.postValue(false);//解码状态
        //创录音对象
        hamRecorder = new HamRecorder(null);
        hamRecorder.startRecord();

        mutableIsFlexRadio.setValue(false);

        //创建用于显示时间的计时器
        utcTimer = new UtcTimer(10, false, new OnUtcTimer() {
            @Override
            public void doHeartBeatTimer(long utc) {//不触发时的时钟信息

            }

            @Override
            public void doOnSecTimer(long utc) {//当指定间隔时触发时
                timerSec.postValue(utc);//发送当前UTC时间
                mutableIsRecording.postValue(hamRecorder.isRunning());
                mutableHamRecordIsRunning.postValue(hamRecorder.isRunning());//发送当前计时器状态
            }
        });
        utcTimer.start();//启动计时器

        //同步一下时间。microsoft的NTP服务器
        UtcTimer.syncTime(null);

        mutableFt8MessageList.setValue(ft8Messages);

        //创建监听对象，回调中的动作用于处理解码、发射、关注的呼号列表添加等操作
        ft8SignalListener = new FT8SignalListener(databaseOpr, new OnFt8Listen() {
            @Override
            public void beforeListen(long utc) {
                mutableIsDecoding.postValue(true);
            }

            @Override
            public void afterDecode(long utc, float time_sec, int sequential
                    , ArrayList<Ft8Message> messages, boolean isDeep) {
                if (messages.size() == 0) return;//没有解码出消息，不触发动作

                synchronized (ft8Messages) {
                    ft8Messages.addAll(messages);//添加消息到列表
                }
                GeneralVariables.deleteArrayListMore(ft8Messages);//删除多余的消息,FT8CN限定的可展示消息的总数量

                mutableFt8MessageList.postValue(ft8Messages);//触发添加消息的动作，让界面能观察到
                mutableTimerOffset.postValue(time_sec);//本次时间偏移量


                findIncludedCallsigns(messages);//查找符合条件的消息，放到呼叫列表中

                //检查发射程序。从消息列表中解析发射的程序
                //超出周期2秒钟，就不应该解析了
                if (!ft8TransmitSignal.isTransmitting()
                        && !isDeep//屏蔽掉深度解码激活自动程序
                        //深度解码的列表应该加到没有深度解码的新消息列表中
                        && (ft8SignalListener.timeSec
                        + GeneralVariables.pttDelay
                        + GeneralVariables.transmitDelay <= 2000)) {//考虑网络模式，发射时长是13秒
                    ft8TransmitSignal.parseMessageToFunction(messages);//解析消息，并处理
                }

                currentMessages = messages;

                if (isDeep) {
                    currentDecodeCount += messages.size();
                } else {
                    currentDecodeCount = messages.size();
                }

                mutableIsDecoding.postValue(false);//解码的状态，会触发频谱图中的标记动作


                getQTHRunnable.messages = messages;
                getQTHThreadPool.execute(getQTHRunnable);//用线程池的方式查询归属地

                //此变量也是告诉消息列表变化的
                mutable_Decoded_Counter.postValue(
                        currentDecodeCount);//告知界面消息的总数量

                if (GeneralVariables.saveSWLMessage) {
                    databaseOpr.writeMessage(messages);//把SWL消息写到数据库
                }
                //检查QSO of SWL,并保存到SWLQSOTable中的通联列表qsoList中
                if (GeneralVariables.saveSWL_QSO) {
                    swlQsoList.findSwlQso(messages, ft8Messages, new SWLQsoList.OnFoundSwlQso() {
                        @Override
                        public void doFound(QSLRecord record) {
                            databaseOpr.addSWL_QSO(record);//把SWL QSO保存到数据库
                            ToastMessage.show(record.swlQSOInfo());
                        }
                    });
                }
                //从列表中查找呼号和网格对应关系，并添加到表中
                getCallsignAndGrid(messages);
            }
        });

        ft8SignalListener.setOnWaveDataListener(new FT8SignalListener.OnWaveDataListener() {
            @Override
            public void getVoiceData(int duration, boolean afterDoneRemove, OnGetVoiceDataDone getVoiceDataDone) {
                hamRecorder.getVoiceData(duration, afterDoneRemove, getVoiceDataDone);
            }
        });


        ft8SignalListener.startListen();

        //频谱监听对象
        spectrumListener = new SpectrumListener(hamRecorder);


        //创建发射对象，回调：发射前，发射后、QSL成功后。
        ft8TransmitSignal = new FT8TransmitSignal(databaseOpr, new OnDoTransmitted() {
            private boolean needControlSco() {//根据控制模式，确定是不是需要开启SCO
                if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
                    return false;
                }
                if (GeneralVariables.controlMode != ControlMode.CAT) {
                    return true;
                }
                return baseRig != null && !baseRig.supportWaveOverCAT();
            }

            @Override
            public void onBeforeTransmit(Ft8Message message, int functionOder) {
                if (GeneralVariables.controlMode == ControlMode.CAT
                        || GeneralVariables.controlMode == ControlMode.RTS
                        || GeneralVariables.controlMode == ControlMode.DTR) {
                    if (baseRig != null) {
                        //if (GeneralVariables.connectMode != ConnectMode.NETWORK) stopSco();
                        if (needControlSco()) stopSco();
                        baseRig.setPTT(true);
                    }
                }
                if (ft8TransmitSignal.isActivated()) {
                    GeneralVariables.transmitMessages.add(message);
                    //mutableTransmitMessages.postValue(GeneralVariables.transmitMessages);
                    mutableTransmitMessagesCount.postValue(1);
                }
            }

            @Override
            public void onAfterTransmit(Ft8Message message, int functionOder) {
                if (GeneralVariables.controlMode == ControlMode.CAT
                        || GeneralVariables.controlMode == ControlMode.RTS
                        || GeneralVariables.controlMode == ControlMode.DTR) {
                    if (baseRig != null) {
                        baseRig.setPTT(false);
                        //if (GeneralVariables.connectMode != ConnectMode.NETWORK) startSco();
                        if (needControlSco()) startSco();
                    }
                }
            }

            @Override
            public void onTransmitByWifi(Ft8Message msg) {
                if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
                    if (baseRig != null) {
                        if (baseRig.isConnected()) {
                            sendWaveDataRunnable.baseRig = baseRig;
                            sendWaveDataRunnable.message = msg;
                            //以线程池的方式执行网络数据包发送
                            sendWaveDataThreadPool.execute(sendWaveDataRunnable);
                        }
                    }
                }
            }

            //2023-08-16 由DS1UFX提交修改（基于0.9版），用于(tr)uSDX audio over cat的支持。
            @Override
            public boolean supportTransmitOverCAT() {
                if (GeneralVariables.controlMode != ControlMode.CAT) {
                    return false;
                }
                if (baseRig == null) {
                    return false;
                }
                if (!baseRig.isConnected() || !baseRig.supportWaveOverCAT()) {
                    return false;
                }
                return true;
            }

            @Override
            public void onTransmitOverCAT(Ft8Message msg) {//通过CAT发送音频消息
                if (!supportTransmitOverCAT()) {
                    return;
                }
                sendWaveDataRunnable.baseRig = baseRig;
                sendWaveDataRunnable.message = msg;
                sendWaveDataThreadPool.execute(sendWaveDataRunnable);
            }

        }, new OnTransmitSuccess() {//当通联成功时
            @Override
            public void doAfterTransmit(QSLRecord qslRecord) {
                databaseOpr.addQSL_Callsign(qslRecord);//两个操作，把呼号和QSL记录下来
                if (qslRecord.getToCallsign() != null) {//把通联成功的分区加入到分区列表
                    GeneralVariables.callsignDatabase.getCallsignInformation(qslRecord.getToCallsign()
                            , new OnAfterQueryCallsignLocation() {
                                @Override
                                public void doOnAfterQueryCallsignLocation(CallsignInfo callsignInfo) {
                                    GeneralVariables.addDxcc(callsignInfo.DXCC);
                                    GeneralVariables.addItuZone(callsignInfo.ITUZone);
                                    GeneralVariables.addCqZone(callsignInfo.CQZone);
                                }
                            });
                }
            }
        });


        //打开HTTP SERVER
        httpServer = new LogHttpServer(this, LogHttpServer.DEFAULT_PORT);
        try {
            httpServer.start();
        } catch (IOException e) {
            Log.e(TAG, "http server error:" + e.getMessage());
        }
    }

    public void setTransmitIsFreeText(boolean isFreeText) {
        if (ft8TransmitSignal != null) {
            ft8TransmitSignal.setTransmitFreeText(isFreeText);
        }
    }

    public boolean getTransitIsFreeText() {
        if (ft8TransmitSignal != null) {
            return ft8TransmitSignal.isTransmitFreeText();
        }
        return false;
    }


    /**
     * 查找符合条件的消息，放到呼叫列表中
     *
     * @param messages 消息
     */
    private synchronized void findIncludedCallsigns(ArrayList<Ft8Message> messages) {
        Log.d(TAG, "findIncludedCallsigns: 查找关注的呼号");
        if (ft8TransmitSignal.isActivated() && ft8TransmitSignal.sequential != UtcTimer.getNowSequential()) {
            return;
        }
        int count = 0;
        for (Ft8Message msg : messages) {
            //与我的呼号有关，与关注的呼号有关
            if (msg.getCallsignFrom().equals(GeneralVariables.myCallsign)
                    || msg.getCallsignTo().equals(GeneralVariables.myCallsign)
                    || GeneralVariables.callsignInFollow(msg.getCallsignFrom())
                    || (GeneralVariables.callsignInFollow(msg.getCallsignTo()) && (msg.getCallsignTo() != null))
                    || (GeneralVariables.autoFollowCQ && msg.checkIsCQ())) {//是CQ，并且允许关注CQ
                //看不是通联成功的呼号的消息
                msg.isQSL_Callsign = GeneralVariables.checkQSLCallsign(msg.getCallsignFrom());
                if (!GeneralVariables.checkIsExcludeCallsign(msg.callsignFrom)) {//不在排除呼号前缀的，才加入列表
                    count++;
                    GeneralVariables.transmitMessages.add(msg);
                }
            }
        }
        GeneralVariables.deleteArrayListMore(GeneralVariables.transmitMessages);//删除多余的消息
        //mutableTransmitMessages.postValue(GeneralVariables.transmitMessages);
        mutableTransmitMessagesCount.postValue(count);
    }

    /**
     * 清除传输消息列表
     */
    public void clearTransmittingMessage() {
        GeneralVariables.transmitMessages.clear();
        mutableTransmitMessagesCount.postValue(0);
    }


    /**
     * 从消息列表中查找呼号和网格的对应关系
     *
     * @param messages 消息列表
     */
    private void getCallsignAndGrid(ArrayList<Ft8Message> messages) {
        for (Ft8Message msg : messages) {
            if (GeneralVariables.checkFun1(msg.extraInfo)) {//检查是不是网格
                //如果内存表中没有，或不一致，就写入数据库中
                if (!GeneralVariables.getCallsignHasGrid(msg.getCallsignFrom(), msg.maidenGrid)) {
                    databaseOpr.addCallsignQTH(msg.getCallsignFrom(), msg.maidenGrid);//写数据库
                }
                GeneralVariables.addCallsignAndGrid(msg.getCallsignFrom(), msg.maidenGrid);
            }
        }
    }

    /**
     * 清除消息列表
     */
    public void clearFt8MessageList() {
        ft8Messages.clear();
        mutable_Decoded_Counter.postValue(ft8Messages.size());
        mutableFt8MessageList.postValue(ft8Messages);
    }


    /**
     * 删除单个文件
     *
     * @param fileName 要删除的文件的文件名
     */
    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    /**
     * 向关注的呼号列表添加呼号
     *
     * @param callsign 呼号
     */
    public void addFollowCallsign(String callsign) {
        if (!GeneralVariables.followCallsign.contains(callsign)) {
            GeneralVariables.followCallsign.add(callsign);
            databaseOpr.addFollowCallsign(callsign);
        }
    }


    /**
     * 从数据库中获取关注的呼号列表
     */
    public void getFollowCallsignsFromDataBase() {
        databaseOpr.getFollowCallsigns(new OnAfterQueryFollowCallsigns() {
            @Override
            public void doOnAfterQueryFollowCallsigns(ArrayList<String> callsigns) {
                for (String s : callsigns) {
                    if (!GeneralVariables.followCallsign.contains(s)) {
                        GeneralVariables.followCallsign.add(s);
                    }
                }
            }
        });
    }


    /**
     * 设置操作载波频率。如果电台没有连接，就有操作
     */
    public void setOperationBand() {
        if (!isRigConnected()) {
            return;
        }

        //先设置上边带，再设置频率
        baseRig.setUsbModeToRig();//设置上边带

        //此处延迟1秒发送第二个指令，是防止协谷X6100断开连接的问题
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                baseRig.setFreq(GeneralVariables.band);//设置频率
                baseRig.setFreqToRig();
            }
        }, 800);
    }

    public void setCivAddress() {
        if (baseRig != null) {
            baseRig.setCivAddress(GeneralVariables.civAddress);
        }
    }

    public void setControlMode() {
        if (baseRig != null) {
            baseRig.setControlMode(GeneralVariables.controlMode);
        }
    }


    /**
     * 通过USB连接电台
     *
     * @param context context
     * @param port    串口
     */
    public void connectCableRig(Context context, CableSerialPort.SerialPort port) {
        if (GeneralVariables.controlMode == ControlMode.VOX) {//如果当前是VOX，就改成CAT模式
            GeneralVariables.controlMode = ControlMode.CAT;
        }
        connectRig();

        if (baseRig == null) {
            return;
        }
        baseRig.setControlMode(GeneralVariables.controlMode);
        CableConnector connector = new CableConnector(context, port, GeneralVariables.baudRate
                //, GeneralVariables.controlMode);
                , GeneralVariables.controlMode,baseRig);

        //2023-08-16 由DS1UFX提交修改（基于0.9版），用于(tr)uSDX audio over cat的支持。
        connector.setOnCableDataReceived(new CableConnector.OnCableDataReceived() {
            @Override
            public void OnWaveReceived(int bufferLen, float[] buffer) {
                Log.i(TAG, "call hamRecorder.doOnWaveDataReceived");
                hamRecorder.doOnWaveDataReceived(bufferLen, buffer);
            }
        });

        baseRig.setOnRigStateChanged(onRigStateChanged);
        baseRig.setConnector(connector);
        connector.connect();

        //晚1秒钟设置模式，防止有的电台反应不过来
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setOperationBand();//设置载波频率
            }
        }, 1000);

    }

    public void connectBluetoothRig(Context context, BluetoothDevice device) {
        GeneralVariables.controlMode = ControlMode.CAT;//蓝牙控制模式，只能是CAT控制
        connectRig();
        if (baseRig == null) {
            return;
        }
        baseRig.setControlMode(GeneralVariables.controlMode);
        BluetoothRigConnector connector = BluetoothRigConnector.getInstance(context, device.getAddress()
                , GeneralVariables.controlMode);
        baseRig.setOnRigStateChanged(onRigStateChanged);
        baseRig.setConnector(connector);

        new Handler().postDelayed(new Runnable() {//蓝牙连接是需要时间的，等2秒再设置频率
            @Override
            public void run() {
                setOperationBand();//设置载波频率
            }
        }, 5000);
    }

    /**
     * 以网络方式连接到ICOM、协谷X6100系列电台
     * @param wifiRig ICom,XieGu Wifi模式的电台
     */
    public void connectWifiRig(WifiRig wifiRig) {
        if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
            if (baseRig != null) {
                if (baseRig.getConnector() != null) {
                    baseRig.getConnector().disconnect();
                }
            }
        }

        GeneralVariables.controlMode = ControlMode.CAT;//网络控制模式
        //目前Icom与协谷x6100共用同一种连接器
        IComWifiConnector iComWifiConnector = new IComWifiConnector(GeneralVariables.controlMode
                ,wifiRig);
        iComWifiConnector.setOnWifiDataReceived(new IComWifiConnector.OnWifiDataReceived() {
            @Override
            public void OnWaveReceived(int bufferLen, float[] buffer) {
                hamRecorder.doOnWaveDataReceived(bufferLen, buffer);
            }

            @Override
            public void OnCivReceived(byte[] data) {

            }
        });

        iComWifiConnector.connect();
        connectRig();//给baseRig赋值

        baseRig.setControlMode(GeneralVariables.controlMode);
        baseRig.setOnRigStateChanged(onRigStateChanged);
        baseRig.setConnector(iComWifiConnector);

        new Handler().postDelayed(new Runnable() {//蓝牙连接是需要时间的，等2秒再设置频率
            @Override
            public void run() {
                setOperationBand();//设置载波频率
            }
        }, 1000);
    }

    /**
     * 连接到flexRadio
     *
     * @param context   context
     * @param flexRadio flexRadio对象
     */
    public void connectFlexRadioRig(Context context, FlexRadio flexRadio) {
        if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
            if (baseRig != null) {
                if (baseRig.getConnector() != null) {
                    baseRig.getConnector().disconnect();
                }
            }
        }
        GeneralVariables.controlMode = ControlMode.CAT;//网络控制模式
        FlexConnector flexConnector = new FlexConnector(context, flexRadio, GeneralVariables.controlMode);
        flexConnector.setOnWaveDataReceived(new FlexConnector.OnWaveDataReceived() {
            @Override
            public void OnDataReceived(int bufferLen, float[] buffer) {
                hamRecorder.doOnWaveDataReceived(bufferLen, buffer);
            }
        });
        flexConnector.connect();
        connectRig();

        baseRig.setOnRigStateChanged(onRigStateChanged);
        baseRig.setConnector(flexConnector);
//
        new Handler().postDelayed(new Runnable() {//连接是需要时间的，等2秒再设置频率
            @Override
            public void run() {
                setOperationBand();//设置载波频率
            }
        }, 3000);
    }


    /**
     * 根据指令集创建不同型号的电台
     */
    private void connectRig() {

        baseRig = null;
        //此处判断是用什么类型的电台，ICOM,YAESU 2,YAESU 3
        switch (GeneralVariables.instructionSet) {
            case InstructionSet.ICOM:
                baseRig = new IcomRig(GeneralVariables.civAddress);
                break;
            case InstructionSet.YAESU_2:
                baseRig = new Yaesu2Rig();
                break;
            case InstructionSet.YAESU_3_9:
                baseRig = new Yaesu39Rig(false);//yaesu3代指令，9位频率,usb模式
                break;
            case InstructionSet.YAESU_3_9_U_DIG:
                baseRig = new Yaesu39Rig(true);//yaesu3代指令，9位频率,data-usb模式
                break;
            case InstructionSet.YAESU_3_8:
                baseRig = new Yaesu38Rig();//yaesu3代指令，8位频率
                break;
            case InstructionSet.YAESU_3_450:
                baseRig = new Yaesu38_450Rig();//yaesu3代指令，8位频率
                break;
            case InstructionSet.KENWOOD_TK90:
                baseRig = new KenwoodKT90Rig();//建伍TK90
                break;
            case InstructionSet.YAESU_DX10:
                baseRig = new YaesuDX10Rig();//YAESU DX10 DX101
                break;
            case InstructionSet.KENWOOD_TS590:
                baseRig = new KenwoodTS590Rig();//KENWOOD TS590
                break;
            case InstructionSet.GUOHE_Q900:
                baseRig = new GuoHeQ900Rig();//国赫Q900
                break;
            case InstructionSet.XIEGUG90S://协谷，USB模式
                baseRig = new XieGuRig(GeneralVariables.civAddress);//协谷G90S
                break;
            case InstructionSet.ELECRAFT:
                baseRig = new ElecraftRig();//ELECRAFT
                break;
            case InstructionSet.FLEX_CABLE:
                baseRig = new Flex6000Rig();//FLEX6000
                break;
            case InstructionSet.FLEX_NETWORK:
                baseRig = new FlexNetworkRig();
                break;
            case InstructionSet.XIEGU_6100:
                baseRig = new XieGu6100Rig(GeneralVariables.civAddress);//协谷6100
                break;
            case InstructionSet.KENWOOD_TS2000:
                baseRig = new KenwoodTS2000Rig();//建伍TS2000
                break;
            case InstructionSet.WOLF_SDR_DIGU:
                baseRig = new Wolf_sdr_450Rig(false);
                break;
            case InstructionSet.WOLF_SDR_USB:
                baseRig = new Wolf_sdr_450Rig(true);
                break;
            case InstructionSet.TRUSDX:
                baseRig = new TrUSDXRig();//(tr)uSDX
                break;
            case InstructionSet.KENWOOD_TS570:
                baseRig = new KenwoodTS570Rig();//KENWOOD TS-570D
                break;
        }

        if ((GeneralVariables.instructionSet == InstructionSet.FLEX_NETWORK)
                || ((GeneralVariables.instructionSet == InstructionSet.ICOM
                ||GeneralVariables.instructionSet==InstructionSet.XIEGU_6100)
                && GeneralVariables.connectMode == ConnectMode.NETWORK)) {
            hamRecorder.setDataFromLan();
        } else {
            //hamRecorder.setDataFromMic();
            if (GeneralVariables.controlMode != ControlMode.CAT || baseRig == null
                    || !baseRig.supportWaveOverCAT()) {
                hamRecorder.setDataFromMic();
            } else {
                hamRecorder.setDataFromLan();
            }
        }

        mutableIsFlexRadio.postValue(GeneralVariables.instructionSet == InstructionSet.FLEX_NETWORK);

    }


    /**
     * 检察电台是否处于连接状态,两种情况：rigBaseClass没建立，串口没连接成功
     *
     * @return 是否连接
     */
    public boolean isRigConnected() {
        if (baseRig == null) {
            return false;
        } else {
            return baseRig.isConnected();
        }
    }

    /**
     * 获取串口设备列表
     */
    public void getUsbDevice() {
        serialPorts =
                CableSerialPort.listSerialPorts(GeneralVariables.getMainContext());
        mutableSerialPorts.postValue(serialPorts);
    }


    public void startSco() {
        AudioManager audioManager = (AudioManager) GeneralVariables.getMainContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            //蓝牙设备不支持录音
            ToastMessage.show(getStringFromResource(R.string.does_not_support_recording));
            return;
        }
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();//71毫秒
        audioManager.setSpeakerphoneOn(false);//进入耳机模式
    }

    public void stopSco() {
        AudioManager audioManager = (AudioManager) GeneralVariables.getMainContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
            audioManager.setSpeakerphoneOn(true);//退出耳机模式
        }

    }


    public void setBlueToothOn() {
        AudioManager audioManager = (AudioManager) GeneralVariables.getMainContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            //蓝牙设备不支持录音
            ToastMessage.show(getStringFromResource(R.string.does_not_support_recording));
        }

        /*
        播放音乐的对应的就是MODE_NORMAL, 如果使用外放播则调用audioManager.setSpeakerphoneOn(true)即可.
        若使用耳机和听筒,则需要先设置模式为MODE_IN_CALL(3.0以前)或MODE_IN_COMMUNICATION(3.0以后).
         */
        audioManager.setMode(AudioManager.MODE_NORMAL);//178毫秒
        audioManager.setBluetoothScoOn(true);
        audioManager.stopBluetoothSco();
        audioManager.startBluetoothSco();//71毫秒
        audioManager.setSpeakerphoneOn(false);//进入耳机模式

        //进入到蓝牙耳机模式
        ToastMessage.show(getStringFromResource(R.string.bluetooth_headset_mode));

    }

    public void setBlueToothOff() {

        AudioManager audioManager = (AudioManager) GeneralVariables.getMainContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
            audioManager.setSpeakerphoneOn(true);//退出耳机模式
        }
        //离开蓝牙耳机模式
        ToastMessage.show(getStringFromResource(R.string.bluetooth_Headset_mode_cancelled));

    }


    /**
     * 查询蓝牙是否连接
     *
     * @return 是否
     */
    @SuppressLint("MissingPermission")
    public boolean isBTConnected() {
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter == null) return false;

        //蓝牙头戴式耳机，支持语音输入输出
        int headset = blueAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        int a2dp = blueAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        return headset == BluetoothAdapter.STATE_CONNECTED || a2dp == BluetoothAdapter.STATE_CONNECTED;
    }

    private static class GetQTHRunnable implements Runnable {
        MainViewModel mainViewModel;
        ArrayList<Ft8Message> messages;

        public GetQTHRunnable(MainViewModel mainViewModel) {
            this.mainViewModel = mainViewModel;
        }


        @Override
        public void run() {
            CallsignDatabase.getMessagesLocation(
                    GeneralVariables.callsignDatabase.getDb(), messages);
            mainViewModel.mutableFt8MessageList.postValue(mainViewModel.ft8Messages);
        }
    }

    private static class SendWaveDataRunnable implements Runnable {
        BaseRig baseRig;
        //float[] data;
        Ft8Message message;

        @Override
        public void run() {
            if (baseRig != null && message != null) {
                baseRig.sendWaveData(message);//实际生成的数据是12.64+0.04,0.04是生成的0数据
            }
        }
    }

}