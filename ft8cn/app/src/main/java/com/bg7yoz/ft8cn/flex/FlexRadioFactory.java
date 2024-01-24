package com.bg7yoz.ft8cn.flex;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// VITA 形成的发现消息解析器的枚举定义
//enum VitaTokens {
//    nullToken ,
//    ipToken,
//    portToken,
//    modelToken,
//    serialToken,
//    callsignToken,
//    nameToken,
//    dpVersionToken,
//    versionToken,
//    statusToken,
//};
/**
 * RadioFactory 当前发现的所有收音机。
 * RadioFactory: 实例化这个类来创建一个 Radio Factory，它将为网络上发现的无线电维护FlexRadio列表flexRadios。
 *
 * 通过Upd协议，在4992端口的广播数据中获取vita协议数据，并解析出序列号，用于更新电台列表flexRadios。
 * @author BGY70Z
 * @date 2023-03-20
 */
public class FlexRadioFactory {
    private static final String TAG="FlexRadioFactory";
    private static final int FLEX_DISCOVERY_PORT =4992;
    private static FlexRadioFactory instance=null;
    private final RadioUdpClient broadcastClient ;
    private OnFlexRadioEvents onFlexRadioEvents;

    private Timer refreshTimer=null;
    private TimerTask refreshTask=null;

    public ArrayList<FlexRadio> flexRadios=new ArrayList<>();

    /**
     * 获取电台列表实例
     * @return 电台列表实例
     */
    public static FlexRadioFactory getInstance(){
        if (instance==null){
            instance= new FlexRadioFactory();
        }
        return instance;
    }



    public FlexRadioFactory() {
        broadcastClient = new RadioUdpClient(FLEX_DISCOVERY_PORT);

        broadcastClient.setOnUdpEvents(new RadioUdpClient.OnUdpEvents() {
            @Override
            public void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data) {
                VITA vita = new VITA(data);
                if (vita.isAvailable//如果数据包有效，且classId=0x534CFFFF,StreamId=0x800，更新电台列表
                        &&vita.informationClassCode==VITA.FLEX_CLASS_ID
                        &&vita.packetClassCode==VITA.VS_Discovery
                        &&vita.streamId==VITA.FLEX_Discovery_stream_ID){
                    updateFlexRadioList(new String(vita.payload));
                }
            }
        });
        try {
            broadcastClient.setActivated(true);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "FlexRadioFactory: "+e.getMessage());
        }

    }


    public void startRefreshTimer(){
        if (refreshTimer==null) {
            refreshTask=new TimerTask() {
                @Override
                public void run() {
                    Log.e(TAG, "run: 检查离线" );
                    checkOffLineRadios();
                }
            };
            refreshTimer=new Timer();
            refreshTimer.schedule(refreshTask, 1000, 1000);//检查电台列表中的电台是否在线（每一秒）
        }
    }
    public void cancelRefreshTimer(){
        if (refreshTimer!=null){
            refreshTimer.cancel();
            refreshTimer=null;
            refreshTask.cancel();
            refreshTask=null;
        }
    }

    /**
     * 从数据中查找电台的序列号
     * @param s 数据
     * @return 序列号
     */
    private String getSerialNum(String s){
        String[] strings=s.split(" ");
        for (int i = 0; i <strings.length ; i++) {
            if (strings[i].toLowerCase().startsWith("serial")){
                return strings[i].substring("serial".length()+1);
            }
        }
        return "";
    }

    /**
     * 在电台列表中查找有没有指定序列号的电台
     * @param serial 序列号
     * @return 电台实例
     */
    public FlexRadio checkFlexRadioBySerial(String serial){
        for (FlexRadio radio:flexRadios) {
            if (radio.isEqual(serial)){
                return radio;
            }
        }
        return null;
    }

    private synchronized void updateFlexRadioList(String s){
        String serial=getSerialNum(s);
        if (serial.equals("")){return;}
        FlexRadio radio=checkFlexRadioBySerial(serial);
        if (radio!=null){
            radio.updateLastSeen();
        }else {
            radio=new FlexRadio(s);
            if (onFlexRadioEvents!=null){
                onFlexRadioEvents.OnFlexRadioAdded(radio);
            }
            flexRadios.add(radio);
        }
    }

    /**
     * 检查电台是不是离线，如果离线，触发离线事件
     */
    private void checkOffLineRadios(){
        for (FlexRadio radio:flexRadios) {
            if (radio.isInvalidNow()){
               if (onFlexRadioEvents!=null){
                   onFlexRadioEvents.OnFlexRadioInvalid(radio);
               }
            }
        }
    }

    //***********Getter****************
    public RadioUdpClient getBroadcastClient() {
        return broadcastClient;
    }

    public OnFlexRadioEvents getOnFlexRadioEvents() {
        return onFlexRadioEvents;
    }

    public void setOnFlexRadioEvents(OnFlexRadioEvents onFlexRadioEvents) {
        this.onFlexRadioEvents = onFlexRadioEvents;
    }
    //*********************************


    /**
     * 电台列表变化的接口
     */
    public static interface OnFlexRadioEvents{
        void OnFlexRadioAdded(FlexRadio flexRadio);
        void OnFlexRadioInvalid(FlexRadio flexRadio);
    }

}
