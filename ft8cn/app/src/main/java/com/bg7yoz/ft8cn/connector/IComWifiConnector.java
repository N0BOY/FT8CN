package com.bg7yoz.ft8cn.connector;
/**
 * ICom网络方式的连接器。
 * 注：ICom网络方式的音频数据包是Int类型，需要转换成Float类型
 *
 * @author BGY70Z
 * @date 2023-08-19
 */


import com.bg7yoz.ft8cn.icom.WifiRig;

public class IComWifiConnector extends WifiConnector{
    private static final String TAG = "IComWifiConnector";

    public IComWifiConnector(int controlMode,WifiRig wifiRig) {
        super(controlMode,wifiRig);

        this.wifiRig.setOnDataEvents(new WifiRig.OnDataEvents() {
            @Override
            public void onReceivedCivData(byte[] data) {
                if (getOnConnectReceiveData()!=null){
                    getOnConnectReceiveData().onData(data);
                }
                if (onWifiDataReceived!=null) {
                    onWifiDataReceived.OnCivReceived(data);
                }
            }

            @Override
            public void onReceivedWaveData(byte[] data) {//接收音频数据事件，把音频数据转换成float格式的。
                if (onWifiDataReceived!=null){
                    float[] waveFloat=new float[data.length/2];
                    for (int i = 0; i <waveFloat.length ; i++) {
                        waveFloat[i]=readShortBigEndianData(data,i*2)/32768.0f;
                    }
                    onWifiDataReceived.OnWaveReceived(waveFloat.length,waveFloat);
                }
            }
        });
    }


}
