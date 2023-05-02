package com.bg7yoz.ft8cn.rigs;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.connector.BaseRigConnector;

/**
 * 电台的抽象类。
 * @author BGY70Z
 * @date 2023-03-20
 */
public abstract class BaseRig {
    private long freq;//当前频率值
    public MutableLiveData<Long> mutableFrequency = new MutableLiveData<>();
    private int controlMode;//控制模式
    private OnRigStateChanged onRigStateChanged;//当电台的一些状态发生变化的回调
    private int civAddress;//CIV地址
    private int baudRate;//波特率
    private boolean isPttOn=false;//ptt是否打开
    private BaseRigConnector connector = null;//连接电台的对象

    public abstract boolean isConnected();//确认电台是否连接

    public abstract void setUsbModeToRig();//设置电台上边带方式

    public abstract void setFreqToRig();//设置电台频率

    public abstract void onReceiveData(byte[] data);//当电台发送回数据的动作

    public abstract void readFreqFromRig();//从电台读取频率

    public abstract String getName();//获取电台的名字

    private final OnConnectReceiveData onConnectReceiveData = new OnConnectReceiveData() {
        @Override
        public void onData(byte[] data) {
            onReceiveData(data);
        }
    };

    public void setPTT(boolean on) {//设置PTT打开或关闭
        isPttOn=on;
        if (onRigStateChanged != null) {
            onRigStateChanged.onPttChanged(on);
        }
    }

//    public void sendWaveData(float[] data) {
//        //留给ICOM电台使用
//    }
    public void sendWaveData(Ft8Message message) {
        //留给ICOM电台使用
    }

    public long getFreq() {
        return freq;
    }

    public void setFreq(long freq) {
        if (freq == this.freq) return;
        if (freq == 0) return;
        if (freq == -1) return;
        mutableFrequency.postValue(freq);
        this.freq = freq;
        if (onRigStateChanged != null) {
            onRigStateChanged.onFreqChanged(freq);
        }
    }

    public void setConnector(BaseRigConnector connector) {
        this.connector = connector;

        this.connector.setOnRigStateChanged(onRigStateChanged);
        this.connector.setOnConnectReceiveData(new OnConnectReceiveData() {
            @Override
            public void onData(byte[] data) {
                onReceiveData(data);
            }
        });
    }

    public void setControlMode(int mode) {
        controlMode = mode;
        if (connector != null) {
            connector.setControlMode(mode);
        }
    }

    public int getControlMode() {
        return controlMode;
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }

    public BaseRigConnector getConnector() {
        return connector;
    }

    public OnRigStateChanged getOnRigStateChanged() {
        return onRigStateChanged;
    }

    public void setOnRigStateChanged(OnRigStateChanged onRigStateChanged) {
        this.onRigStateChanged = onRigStateChanged;
    }

    public int getCivAddress() {
        return civAddress;
    }

    public void setCivAddress(int civAddress) {
        this.civAddress = civAddress;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public boolean isPttOn() {
        return isPttOn;
    }
}
