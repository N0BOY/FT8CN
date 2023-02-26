package com.bg7yoz.ft8cn.connector;


import com.bg7yoz.ft8cn.rigs.OnConnectReceiveData;
import com.bg7yoz.ft8cn.rigs.OnRigStateChanged;


public class BaseRigConnector {
    private boolean connected;//是否处于连接状态
    private OnConnectReceiveData onConnectReceiveData;//当接收到数据后的动作
    private int controlMode;//控制模式
    private OnRigStateChanged onRigStateChanged;
    private OnConnectorStateChanged onConnectorStateChanged=new OnConnectorStateChanged() {
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

    public void sendWaveData(short[] data){
        //留给网络方式发送音频流
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
}
