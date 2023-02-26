package com.bg7yoz.ft8cn.connector;

import com.bg7yoz.ft8cn.icom.IComWifiRig;

public class IComWifiConnector extends BaseRigConnector{
    private static final String TAG = "IComWifiConnector";
    public interface OnWifiDataReceived{
        void OnWaveReceived(int bufferLen,float[] buffer);
        void OnCivReceived(byte[] data);
    }

    private IComWifiRig iComWifiRig;
    private OnWifiDataReceived onWifiDataReceived;


    public IComWifiConnector(int controlMode,IComWifiRig iComWifiRig) {
        super(controlMode);
        this.iComWifiRig=iComWifiRig;

        this.iComWifiRig.setOnIComDataEvents(new IComWifiRig.OnIComDataEvents() {
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

    @Override
    public void sendWaveData(short[] data) {
        if (iComWifiRig.opened) {
            iComWifiRig.sendWaveData(data);
        }
    }

    @Override
    public void connect() {
        super.connect();
        iComWifiRig.start();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        iComWifiRig.close();
    }

    @Override
    public void sendData(byte[] data) {
        iComWifiRig.sendCivData(data);
    }

    @Override
    public void setPttOn(byte[] command) {
        iComWifiRig.sendCivData(command);
    }

    @Override
    public void setPttOn(boolean on) {
        if (iComWifiRig.opened){
            iComWifiRig.setPttOn(on);
        }
    }
    public OnWifiDataReceived getOnWifiDataReceived() {
        return onWifiDataReceived;
    }

    @Override
    public boolean isConnected() {
        return iComWifiRig.opened;
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
