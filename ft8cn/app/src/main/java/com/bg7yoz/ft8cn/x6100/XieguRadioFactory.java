package com.bg7yoz.ft8cn.x6100;

/**
 * XieguRadioFactory 当前发现的所有收音机。
 * RadioFactory: 实例化这个类来创建一个 Radio Factory，通过它发现在相同局域网内地协谷电台。
 *
 * 通过Upd协议，在7001端口的广播数据中获取vita协议数据，并解析电台信息。
 *
 * @author BGY70Z
 * @date 2023-11-29
 */


import android.util.Log;

import com.bg7yoz.ft8cn.flex.FlexRadio;
import com.bg7yoz.ft8cn.flex.RadioUdpClient;
import com.bg7yoz.ft8cn.flex.VITA;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;




public class XieguRadioFactory {
    private static final String TAG="XieguRadioFactory";
    private static final int XIEGU_DISCOVERY_PORT =7001;
    private static XieguRadioFactory instance=null;
    private final RadioUdpClient broadcastClient ;
    private OnXieguRadioEvents onXieguRadioEvents;

    private Timer refreshTimer=null;
    private TimerTask refreshTask=null;

    public ArrayList<X6100Radio> xieguRadios=new ArrayList<>();

    /**
     * 获取电台列表实例
     * @return 电台列表实例
     */
    public static XieguRadioFactory getInstance(){
        if (instance==null){
            instance= new XieguRadioFactory();
        }
        instance.xieguRadios.clear();
        return instance;
    }



    public XieguRadioFactory() {
        broadcastClient = new RadioUdpClient(XIEGU_DISCOVERY_PORT);


        broadcastClient.setOnUdpEvents(new RadioUdpClient.OnUdpEvents() {
            @Override
            public void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data) {
                VITA vita = new VITA(data);

                if (vita.isAvailable//如果数据包有效
                        &&vita.classId64 == VITA.XIEGU_Discovery_Class_Id
                        &&vita.streamId==VITA.XIEGU_Discovery_Stream_Id){
                    InetAddress address = packet.getAddress();//获取ip地址
                    updateXieguRadioList(new String(vita.payload),address.getHostAddress());
                }
            }
        });
        try {
            broadcastClient.setActivated(true);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "XieguRadioFactory: "+e.getMessage());
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
     * 从数据中查找电台的MAC地址
     * @param s 数据
     * @return mac地址
     */
    private String getMacAddress(String s){
        String[] strings=s.split(" ");
        for (int i = 0; i <strings.length ; i++) {
            if (strings[i].toLowerCase().startsWith("mac")){
                return strings[i].substring("mac".length()+1);
            }
        }
        return "";
    }

    /**
     * 在电台列表中查找有没有指定MAC的电台
     * @param mac MAC地址
     * @return 电台实例
     */
    public X6100Radio checkXieguRadioByMac(String mac){
        for (X6100Radio radio:xieguRadios) {
            if (radio.isEqual(mac)){
                return radio;
            }
        }
        return null;
    }

    private synchronized void updateXieguRadioList(String s,String ip){
       String mac =  getMacAddress(s);
       if (mac.equals("")) {return;}
        X6100Radio radio=checkXieguRadioByMac(mac);
        if (radio!=null){
            radio.updateLastSeen();
        }else {
            radio=new X6100Radio(s,ip);
            if (onXieguRadioEvents!=null){
                onXieguRadioEvents.onXieguRadioAdded(radio);
            }
            xieguRadios.add(radio);
        }
    }

    /**
     * 检查电台是不是离线，如果离线，触发离线事件
     */
    private void checkOffLineRadios(){
        for (X6100Radio radio:xieguRadios) {
            if (radio.isInvalidNow()){
               if (onXieguRadioEvents!=null){
                   onXieguRadioEvents.onXieguRadioInvalid(radio);
               }
            }
        }
    }

    //***********Getter****************
    public RadioUdpClient getBroadcastClient() {
        return broadcastClient;
    }

    public OnXieguRadioEvents getOnFlexRadioEvents() {
        return onXieguRadioEvents;
    }

    public void setOnXieguRadioEvents(OnXieguRadioEvents onXieguRadioEvents) {
        this.onXieguRadioEvents = onXieguRadioEvents;
    }
    //*********************************


    /**
     * 电台列表变化的接口
     */
    public static interface OnXieguRadioEvents{
        void onXieguRadioAdded(X6100Radio flexRadio);
        void onXieguRadioInvalid(X6100Radio flexRadio);
    }

}
