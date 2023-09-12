package com.bg7yoz.ft8cn.connector;
/**
 * 网络方式的连接器的基本类。
 * 注：基本兼容ICom网络方式，但有些差异的音频数据包是Int类型，需要转换成Float类型
 *
 * @author BGY70Z
 * @date 2023-08-19
 */


import android.util.Log;

import com.bg7yoz.ft8cn.icom.WifiRig;

public class WifiConnector extends BaseRigConnector{
    private static final String TAG = "WifiConnector";
    public interface OnWifiDataReceived{
        void OnWaveReceived(int bufferLen,float[] buffer);
        void OnCivReceived(byte[] data);
    }


    public WifiRig wifiRig;
    public OnWifiDataReceived onWifiDataReceived;


    public WifiConnector(int controlMode, WifiRig wifiRig) {
        super(controlMode);
        this.wifiRig=wifiRig;

    }

    @Override
    public void sendWaveData(float[] data) {
        if (wifiRig.opened) {
            wifiRig.sendWaveData(data);
        }
    }

    @Override
    public void connect() {
        super.connect();
        wifiRig.start();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        wifiRig.close();
    }

    @Override
    public void sendData(byte[] data) {
        wifiRig.sendCivData(data);
    }

    @Override
    public void setPttOn(byte[] command) {
        wifiRig.sendCivData(command);
    }

    @Override
    public void setPttOn(boolean on) {
        if (wifiRig.opened){
            wifiRig.setPttOn(on);
        }
    }
    public OnWifiDataReceived getOnWifiDataReceived() {
        return onWifiDataReceived;
    }

    @Override
    public boolean isConnected() {
        return wifiRig.opened;
    }

    public void setOnWifiDataReceived(OnWifiDataReceived onDataReceived) {
        this.onWifiDataReceived = onDataReceived;
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
