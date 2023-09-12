package com.bg7yoz.ft8cn.connector;
/**
 * 用于连接电台的基础类，蓝牙、USB线、FLEX网络、ICOM网络都是继承于此
 *
 * @author BG7YOZ
 * @date 2023-03-20
 */

import com.bg7yoz.ft8cn.rigs.OnConnectReceiveData;
import com.bg7yoz.ft8cn.rigs.OnRigStateChanged;


public class BaseRigConnector {
    private boolean connected;//是否处于连接状态
    private OnConnectReceiveData onConnectReceiveData;//当接收到数据后的动作
    private int controlMode;//控制模式
    private OnRigStateChanged onRigStateChanged;
    private final OnConnectorStateChanged onConnectorStateChanged=new OnConnectorStateChanged() {
        @Override
        public void onDisconnected() {
            if (onRigStateChanged!=null){
                onRigStateChanged.onDisconnected();
            }
            connected=false;
        }

        @Override
        public void onConnected() {
            if (onRigStateChanged!=null){
                onRigStateChanged.onConnected();
            }
            connected=true;
        }

        @Override
        public void onRunError(String message) {
            if (onRigStateChanged!=null){
                onRigStateChanged.onRunError(message);
            }
            connected=false;
        }
    };
    public BaseRigConnector(int controlMode) {
        this.controlMode=controlMode;
    }

    /**
     * 发送数据
     * @param data 数据
     */
    public synchronized void sendData(byte[] data){};

    /**
     * 设置PTT状态，ON OFF,如果是RTS和DTR，这个是在有线方式才有的，在CableConnector中会重载此方法
     * @param on 是否ON
     */
    public void setPttOn(boolean on){};

    /**
     * 使用发送数据的方式设置PTT状态
     * @param command 指令数据
     */
    public void setPttOn(byte[] command){};

    public void setControlMode(int mode){
        controlMode=mode;
    }

    public int getControlMode() {
        return controlMode;
    }

    public void setOnConnectReceiveData(OnConnectReceiveData receiveData){
        onConnectReceiveData=receiveData;
    }


    /**
     * 2023-08-16 由DS1UFX提交修改（基于0.9版），用于(tr)uSDX audio over cat的支持。
     * 发送音频数据流，把16位int格式转为32位float格式
     * @param data byte格式，实际上是16位的int
     */
    public void sendWaveData(byte[] data){
        float[] waveFloat=new float[data.length/2];
        for (int i = 0; i <waveFloat.length ; i++) {
            waveFloat[i]=readShortBigEndianData(data,i*2)/32768.0f;
        }
        sendWaveData(waveFloat);
    }

    public void sendWaveData(float[] data){
        //留给网络方式发送音频流
    }

    //2023-08-16 由DS1UFX提交修改（基于0.9版），用于(tr)uSDX audio over cat的支持。
    public void receiveWaveData(byte[] data){
        float[] waveFloat=new float[data.length/2];
        for (int i = 0; i <waveFloat.length ; i++) {
            waveFloat[i]=readShortBigEndianData(data,i*2)/32768.0f;
        }
        receiveWaveData(waveFloat);
    }
    public void receiveWaveData(short[] data){
        float[] waveFloat=new float[data.length];
        for (int i = 0; i <waveFloat.length ; i++) {
            waveFloat[i]=data[i]/32768.0f;
        }
        receiveWaveData(waveFloat);
    }
    public void receiveWaveData(float[] data){
    }

    public OnConnectReceiveData getOnConnectReceiveData() {
        return onConnectReceiveData;
    }
    public void connect(){
    }
    public void disconnect(){
    }

    public OnRigStateChanged getOnRigStateChanged() {
        return onRigStateChanged;
    }

    public void setOnRigStateChanged(OnRigStateChanged onRigStateChanged) {
        this.onRigStateChanged = onRigStateChanged;
    }

    public OnConnectorStateChanged getOnConnectorStateChanged() {
        return onConnectorStateChanged;
    }
    public boolean isConnected(){
        return connected;
    }

    /**
     * 从流数据中读取小端模式的Short
     *
     * @param data  流数据
     * @param start 起始点
     * @return Int16
     */
    public static short readShortBigEndianData(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start] & 0xff
                | ((short) data[start + 1] & 0xff) << 8);
    }
}
