package com.bg7yoz.ft8cn.connector;

import android.content.Context;
import android.util.Log;

import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.serialport.util.SerialInputOutputManager;

/**
 * 有线连接方式的Connector，这里是指USB方式的，继承于BaseRigConnector
 *
 * @author BG7YOZ
 * @date 2023-03-20
 */
public class CableConnector extends BaseRigConnector {
    private static final String TAG="CableConnector";
    public interface OnCableDataReceived {
        void OnWaveReceived(int bufferLen,float[] buffer);
    }

    private final CableSerialPort cableSerialPort;
    private final BaseRig cableConnectedRig;
    private OnCableDataReceived onCableDataReceived;

    public CableConnector(Context context,CableSerialPort.SerialPort serialPort, int baudRate
            , int controlMode, BaseRig cableConnectedRig) {
        super(controlMode);
        this.cableConnectedRig = cableConnectedRig;
        cableSerialPort= new CableSerialPort(context,serialPort,baudRate,getOnConnectorStateChanged());
        cableSerialPort.ioListener=new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                if (getOnConnectReceiveData()!=null){
                    getOnConnectReceiveData().onData(data);
                }
            }

            @Override
            public void onRunError(Exception e) {
                Log.e(TAG, "CableConnector error: "+e.getMessage() );
                    getOnConnectorStateChanged().onRunError("与串口失去连接："+e.getMessage());
            }
        }  ;
        //connect();
    }

    @Override
    public synchronized void sendData(byte[] data) {
        cableSerialPort.sendData(data);
    }


    @Override
    public void setPttOn(boolean on) {
        //只处理RTS和DTR
        switch (getControlMode()){
            case ControlMode.DTR:  cableSerialPort.setDTR_On(on);//打开和关闭DTR
                break;
            case ControlMode.RTS:cableSerialPort.setRTS_On(on);//打开和关闭RTS
                break;
        }
    }

    @Override
    public void setPttOn(byte[] command) {
        cableSerialPort.sendData(command);//以CAT指令发送PTT
    }

    @Override
    public void sendWaveData(byte[] data) {
        sendData(data);
    }

    @Override
    public void sendWaveData(float[] data) {
        // TODO float to byte
        byte[] wave = new byte[data.length * 2];
        sendWaveData(data);
    }

    @Override
    public void receiveWaveData(float[] data){
        Log.i(TAG, "received wave data");

        if (onCableDataReceived!=null){
            Log.i(TAG, "call onCableDataReceived.OnWaveReceived");
            onCableDataReceived.OnWaveReceived(data.length,data);
        }
    }

    public void setOnCableDataReceived(OnCableDataReceived onCableDataReceived) {
        this.onCableDataReceived = onCableDataReceived;
    }

    @Override
    public void connect() {
        super.connect();
        cableSerialPort.connect();
    }

    @Override
    public void disconnect() {
        cableConnectedRig.onDisconnecting();
        super.disconnect();
        cableSerialPort.disconnect();
    }
}
