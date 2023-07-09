package com.bg7yoz.ft8cn;
/**
 * 常用变量。关于mainContext有内存泄漏的风险，以后解决。
 * mainContext
 */

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.callsign.CallsignDatabase;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.ft8transmit.QslRecordList;
import com.bg7yoz.ft8cn.html.HtmlContext;
import com.bg7yoz.ft8cn.icom.IcomAudioUdp;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class GeneralVariables {
    private static final String TAG = "GeneralVariables";
    public static String VERSION = BuildConfig.VERSION_NAME;//版本号"0.62（Beta 4）";
    public static String BUILD_DATE = BuildConfig.apkBuildTime;//编译的时间
    public static int MESSAGE_COUNT = 3000;//消息的最大缓存数量
    public static boolean saveSWLMessage=false;//保存解码消息开关
    public static boolean saveSWL_QSO=false;//保存解码消息消息中的QSO开关

    public static boolean deepDecodeMode=false;//是否开启深度解码

    public static boolean audioOutput32Bit =true;//音频输出类型true=float,false=int16
    public static int audioSampleRate=12000;//发射音频的采样率

    public static MutableLiveData<Float> mutableVolumePercent = new MutableLiveData<>();
    public static float volumePercent = 0.5f;//播放音频的音量,是百分比

    public static int flexMaxRfPower=10;//flex电台的最大发射功率
    public static int flexMaxTunePower=10;//flex电台的最大调谐功率

    private Context mainContext;
    public static CallsignDatabase callsignDatabase = null;

    public void setMainContext(Context context) {
        mainContext = context;
    }

    public static boolean isChina = true;//语言是不是中国
    public static boolean isTraditionalChinese = true;//语言是不是繁体中文
    //public static double maxDist = 0;//最远距离

    //各已经通联的分区列表
    public static final Map<String, String> dxccMap = new HashMap<>();
    public static final Map<Integer, Integer> cqMap = new HashMap<>();
    public static final Map<Integer, Integer> ituMap = new HashMap<>();

    private static final Map<String, Integer> excludedCallsigns = new HashMap<>();

    /**
     * 添加排除的字头
     *
     * @param callsigns 呼号
     */
    public static synchronized void addExcludedCallsigns(String callsigns) {
        excludedCallsigns.clear();
        String[] s = callsigns.toUpperCase().replace(" ", ",")
                .replace("|", ",")
                .replace("，", ",").split(",");
        for (int i = 0; i < s.length; i++) {
            if (s[i].length() > 0) {
                excludedCallsigns.put(s[i], 0);
            }
        }
    }

    /**
     * 查找是否含有排除的字头
     *
     * @param callsign 呼号
     * @return 是否
     */
    public static synchronized boolean checkIsExcludeCallsign(String callsign) {
        Iterator<String> iterator = excludedCallsigns.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (callsign.toUpperCase().indexOf(key) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取排除呼号前缀的列表
     *
     * @return 列表
     */
    public static synchronized String getExcludeCallsigns() {
        StringBuilder calls = new StringBuilder();
        Iterator<String> iterator = excludedCallsigns.keySet().iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (i == 0) {
                calls.append(key);
            } else {
                calls.append(",").append(key);
            }
            i++;
        }
        return calls.toString();
    }


    //通联记录列表，包括成功与不成功的
    public static QslRecordList qslRecordList = new QslRecordList();

    //此处有内存泄露警告，但Application Context不应该会内存泄露，所以注释掉
    @SuppressLint("StaticFieldLeak")
    private static GeneralVariables generalVariables = null;

    public static GeneralVariables getInstance() {
        if (generalVariables == null) {
            generalVariables = new GeneralVariables();
        }
        return generalVariables;
    }

    public static Context getMainContext() {
        return GeneralVariables.getInstance().mainContext;
    }


    public static MutableLiveData<String> mutableDebugMessage = new MutableLiveData<>();
    public static int QUERY_FREQ_TIMEOUT = 2000;//轮询频率变化的时间间隔。2秒
    public static int START_QUERY_FREQ_DELAY = 2000;//开始轮询频率的时间延迟

    public static final int DEFAULT_LAUNCH_SUPERVISION = 10 * 60 * 1000;//发射监管默认值,10分钟
    private static String myMaidenheadGrid = "";
    public static MutableLiveData<String> mutableMyMaidenheadGrid = new MutableLiveData<>();

    public static int connectMode = ConnectMode.USB_CABLE;//连接方式USB==0,BLUE_TOOTH==1

    //public static String bluetoothDeviceAddress=null;//可以用于连接的蓝牙设备地址


    //用于记录呼号于网格的对应关系 todo---应当把此处列表也放到后台追踪信息里
    //public static ArrayList<CallsignMaidenheadGrid> callsignMaidenheadGrids=new ArrayList<>();
    public static final Map<String, String> callsignAndGrids = new ConcurrentHashMap<>();
    //private static final Map<String,String> callsignAndGrids=new HashMap<>();

    public static String myCallsign = "";//我的呼号
    public static String toModifier = "";//呼叫的修饰符
    private static float baseFrequency = 1000;//声音频率
    public static MutableLiveData<Float> mutableBaseFrequency = new MutableLiveData<>();

    public static boolean synFrequency = false;//同频发射
    public static int transmitDelay = 500;//发射延迟时间，这个时间也是给上一个周期的解码时间
    public static int pttDelay = 100;//PTT的响应时间，在给电台PTT指令后，一般电台会有一个响应时间，此处默认是100毫秒
    public static int civAddress = 0xa4;//civ地址
    public static int baudRate = 19200;//波特率
    public static long band = 14074000;//载波频段
    public static int instructionSet = 0;//指令集，0:icom，1:yaesu 2 代，2:yaesu 3代。
    public static int bandListIndex = -1;//电台波段的索引值
    public static MutableLiveData<Integer> mutableBandChange = new MutableLiveData<>();//波段索引值变化
    public static int controlMode = ControlMode.VOX;
    public static int modelNo = 0;
    public static int launchSupervision = DEFAULT_LAUNCH_SUPERVISION;//发射监管
    public static long launchSupervisionStart = UtcTimer.getSystemTime();//自动发射的起始时间
    public static int noReplyLimit = 0;//呼叫无回应次数0==忽略

    public static int noReplyCount = 0;//没有回应的次数

    //下面4个参数是ICOM网络方式连接的参数
    public static String icomIp = "255.255.255.255";
    public static int icomUdpPort = 50001;
    public static String icomUserName = "ic705";
    public static String icomPassword = "";


    public static boolean autoFollowCQ = true;//自动关注CQ
    public static boolean autoCallFollow = true;//自动呼叫关注的呼号
    public static ArrayList<String> QSL_Callsign_list = new ArrayList<>();//QSL成功的呼号
    public static ArrayList<String> QSL_Callsign_list_other_band = new ArrayList<>();//在其它波段QSL成功的呼号


    public static final ArrayList<String> followCallsign = new ArrayList<>();//关注的呼号

    public static ArrayList<Ft8Message> transmitMessages = new ArrayList<>();//放在呼叫界面，关注的列表

    public static void setMyMaidenheadGrid(String grid) {
        myMaidenheadGrid = grid;
        mutableMyMaidenheadGrid.postValue(grid);
    }

    public static String getMyMaidenheadGrid() {
        return myMaidenheadGrid;
    }

    public static float getBaseFrequency() {
        return baseFrequency;
    }

    public static void setBaseFrequency(float baseFrequency) {
        mutableBaseFrequency.postValue(baseFrequency);
        GeneralVariables.baseFrequency = baseFrequency;
    }

    @SuppressLint("DefaultLocale")
    public static String getBaseFrequencyStr() {
        return String.format("%.0f", baseFrequency);
    }

    public static String getCivAddressStr() {
        return String.format("%2X", civAddress);
    }

    public static String getTransmitDelayStr() {
        return String.valueOf(transmitDelay);
    }

    public static String getBandString() {
        return BaseRigOperation.getFrequencyAllInfo(band);
    }

    /**
     * 查有没有通联成功的呼号
     *
     * @param callsign 呼号
     * @return 是否存在
     */
    public static boolean checkQSLCallsign(String callsign) {
        return QSL_Callsign_list.contains(callsign);
    }

    /**
     * 查别的波段有没有通联成功的呼号
     *
     * @param callsign 呼号
     * @return 是否存在
     */
    public static boolean checkQSLCallsign_OtherBand(String callsign) {
        return QSL_Callsign_list_other_band.contains(callsign);
    }


    /**
     * 查该呼号是不是在关注的呼号列表中
     *
     * @param callsign 呼号
     * @return 是否存在
     */
    public static boolean callsignInFollow(String callsign) {
        return followCallsign.contains(callsign);
    }

    /**
     * 向通联成功的呼号列表添加
     *
     * @param callsign 呼号
     */
    public static void addQSLCallsign(String callsign) {
        if (!checkQSLCallsign(callsign)) {
            QSL_Callsign_list.add(callsign);
        }
    }

    public static String getMyMaidenhead4Grid() {
        if (myMaidenheadGrid.length() > 4) {
            return myMaidenheadGrid.substring(0, 4);
        }
        return myMaidenheadGrid;
    }

    /**
     * 自动程序运行起始时间
     */
    public static void resetLaunchSupervision() {
        launchSupervisionStart = UtcTimer.getSystemTime();
    }

    /**
     * 或取自动程序的运行时长
     *
     * @return 毫秒
     */
    public static int launchSupervisionCount() {
        return (int) (UtcTimer.getSystemTime() - launchSupervisionStart);
    }

    public static boolean isLaunchSupervisionTimeout() {
        if (launchSupervision == 0) return false;//0是不监管
        return launchSupervisionCount() > launchSupervision;
    }

    /**
     * 从extraInfo中查消息顺序
     *
     * @param extraInfo 消息中的扩展内容
     * @return 返回消息序号
     */
    public static int checkFunOrderByExtraInfo(String extraInfo) {
        if (checkFun5(extraInfo)) return 5;
        if (checkFun4(extraInfo)) return 4;
        if (checkFun3(extraInfo)) return 3;
        if (checkFun2(extraInfo)) return 2;
        if (checkFun1(extraInfo)) return 1;
        return -1;
    }

    /**
     * 检查消息的序号，如果解析不出来，就-1
     *
     * @param message 消息
     * @return 消息序号
     */
    public static int checkFunOrder(Ft8Message message) {
        if (message.checkIsCQ()) return 6;
        return checkFunOrderByExtraInfo(message.extraInfo);

    }


    //是不是网格报告
    public static boolean checkFun1(String extraInfo) {
        //网格报告必须是4位,或没有网格
        return (extraInfo.trim().matches("[A-Z][A-Z][0-9][0-9]") && !extraInfo.equals("RR73"))
                || (extraInfo.trim().length() == 0);

    }

    //是不是信号报告,如-10
    public static boolean checkFun2(String extraInfo) {
        if (extraInfo.trim().length() < 2) {
            return false;
        }//信号报告必须至少2位
        try {
            return Integer.parseInt(extraInfo.trim()) != 73;//如果是73，说明是消息6，不是消息2
            //return true;
        } catch (Exception e) {
            return false;
        }
    }

    //是不是带R的信号报告,如R-10
    public static boolean checkFun3(String extraInfo) {
        if (extraInfo.trim().length() < 3) {
            return false;
        }//带R信号报告必须至少3位
        //第一位如果不是R，或者第二位是R，说明不是消息3
        if ((extraInfo.trim().charAt(0) != 'R') || (extraInfo.trim().charAt(1) == 'R')) {
            return false;
        }

        try {
            Integer.parseInt(extraInfo.trim().substring(1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //是不是RRR或RR73值
    public static boolean checkFun4(String extraInfo) {
        return extraInfo.trim().equals("RR73") || extraInfo.trim().equals("RRR");
    }

    //是不是73值
    public static boolean checkFun5(String extraInfo) {
        return extraInfo.trim().equals("73");
    }


    /**
     * 判断是不是信号报告，如果是，把值赋给 report
     * @param extraInfo 消息扩展
     * @return 信号报告值,没找到是-100
     */
    public static int checkFun2_3(String extraInfo){
        if (extraInfo.equals("73")) return -100;
        if (extraInfo.matches("[R]?[+-]?[0-9]{1,2}")){
            try {
                return Integer.parseInt(extraInfo.replace("R",""));
            } catch (Exception e) {
                return -100;
            }
        }
        return -100;
    }

    /**
     * 判断是不是网格报告，如果是，把值赋给 report
     * @param extraInfo 消息扩展
     * @return 信号报告
     */
    public static boolean checkFun1_6(String extraInfo){
        return  extraInfo.trim().matches("[A-Z][A-Z][0-9][0-9]")
                && !extraInfo.trim().equals("RR73");
    }
    /**
     * 检查是否是通联结束：RRR、RR73、73
     * @param extraInfo 消息后缀
     * @return 是否
     */
    public static boolean checkFun4_5(String extraInfo){
        return extraInfo.trim().equals("RR73")
                || extraInfo.trim().equals("RRR")
                ||extraInfo.trim().equals("73");
    }

    /**
     * 从String.xml中提取字符串
     *
     * @param id id
     * @return 字符串
     */
    public static String getStringFromResource(int id) {
        if (getMainContext() != null) {
            return getMainContext().getString(id);
        } else {
            return "";
        }
    }


    /**
     * 把已经通联的DXCC分区添加到集合中
     *
     * @param dxccPrefix DXCC前缀
     */
    public static void addDxcc(String dxccPrefix) {
        dxccMap.put(dxccPrefix, dxccPrefix);
    }

    /**
     * 查看是不是已经通联的DXCC分区
     *
     * @param dxccPrefix DXCC前缀
     * @return 是否
     */
    public static boolean getDxccByPrefix(String dxccPrefix) {
        return dxccMap.containsKey(dxccPrefix);
    }

    /**
     * 把CQ分区加到列表里
     *
     * @param cqZone cq分区编号
     */
    public static void addCqZone(int cqZone) {
        cqMap.put(cqZone, cqZone);
    }

    /**
     * 查是否存在已经通联的CQ分区
     *
     * @param cq cq分区编号
     * @return 是否存在
     */
    public static boolean getCqZoneById(int cq) {
        return cqMap.containsKey(cq);
    }

    /**
     * 把itu分区添加到已通联的ITU列表中
     *
     * @param itu itu编号
     */
    public static void addItuZone(int itu) {
        ituMap.put(itu, itu);
    }

    /**
     * 查Itu分区在不在已通联的列表中
     *
     * @param itu itu编号
     * @return 是否存在
     */
    public static boolean getItuZoneById(int itu) {
        return ituMap.containsKey(itu);
    }

    //用于触发新的网格
    public static MutableLiveData<String> mutableNewGrid = new MutableLiveData<>();

    /**
     * 把呼号与网格的对应关系添加到呼号--网格对应表，
     *
     * @param callsign 呼号
     * @param grid     网格
     */
    public static void addCallsignAndGrid(String callsign, String grid) {
        if (grid.length() >= 4) {
            callsignAndGrids.put(callsign, grid);
            mutableNewGrid.postValue(grid);
        }
    }

    /**
     * 呼号--网格对应表。以呼号查网格
     * 如果内存中没有，应当到数据库中查一下。
     *
     * @param callsign 呼号
     * @return 是否有对应的网格
     */
    public static boolean getCallsignHasGrid(String callsign) {
        return callsignAndGrids.containsKey(callsign);
    }

    /**
     * 呼号--网格对应表。以呼号查网格，条件是呼号和网格都对应的上。
     * 此函数的目的是，为了更新对应表的数据库
     *
     * @param callsign 呼号
     * @param grid     网格
     * @return 是否有对应的网格
     */
    public static boolean getCallsignHasGrid(String callsign, String grid) {
        if (!callsignAndGrids.containsKey(callsign)) return false;//说明根本没有这个呼号
        String s = callsignAndGrids.get(callsign);
        if (s == null) return false;
        return s.equals(grid);
    }

    public static String getGridByCallsign(String callsign, DatabaseOpr db) {
        String s = callsign.replace("<", "").replace(">", "");
        if (getCallsignHasGrid(s)) {
            return callsignAndGrids.get(s);
        } else {
            db.getCallsignQTH(callsign);
            return "";
        }
    }

    /**
     * 遍历呼号--网格对应表，生成HTML
     *
     * @return HTML
     */
    public static String getCallsignAndGridToHTML() {
        StringBuilder result = new StringBuilder();
        int order = 0;
        for (String key : callsignAndGrids.keySet()) {
            order++;
            HtmlContext.tableKeyRow(result,order % 2 != 0,key,callsignAndGrids.get(key));
        }
        return result.toString();
    }

    public static synchronized void deleteArrayListMore(ArrayList<Ft8Message> list) {
        if (list.size() > GeneralVariables.MESSAGE_COUNT) {
            while (list.size() > GeneralVariables.MESSAGE_COUNT) {
                list.remove(0);
            }
        }
    }

    /**
     * 判断是否为整数
     *
     * @param str 传入的字符串
     * @return 是整数返回true, 否则返回false
     */

    public static boolean isInteger(String str) {
        if (str != null && !"".equals(str.trim()))
            return str.matches("^[0-9]*$");
        else
            return false;
    }

    /**
     * 输出音频的数据类型，网络模式不可用
     */
    public  enum AudioOutputBitMode{
        Float32,
        Int16
    }
}
