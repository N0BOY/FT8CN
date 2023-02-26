package com.bg7yoz.ft8cn.connector;

import android.content.Context;
import android.util.Log;

import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.serialport.util.SerialInputOutputManager;

/**
 * 有线连接方式的Connector，这里是指USB方式的
 */
public class CableConnector extends BaseRigConnector {
    private static final String TAG="CableConnector";

    private final CableSerialPort cableSerialPort;


    public CableConnector(Context context,CableSerialPort.SerialPort serialPort, int baudRate
            , int controlMode) {
        super(controlMode);
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
    public void connect() {
        super.connect();
        cableSerialPort.connect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        cableSerialPort.disconnect();
    }
}
