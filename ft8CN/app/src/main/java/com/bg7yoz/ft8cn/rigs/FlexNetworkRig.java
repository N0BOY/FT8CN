package com.bg7yoz.ft8cn.rigs;

import android.annotation.SuppressLint;
import android.util.Log;

import com.bg7yoz.ft8cn.flex.FlexCommand;
import com.bg7yoz.ft8cn.flex.FlexRadio;

public class FlexNetworkRig extends BaseRig{
    private static final String TAG="FlexNetworkRig";
    private int commandSeq = 1;//指令的序列
    private FlexCommand flexCommand;
    private String commandStr;
    //private final int ctrAddress=0xE0;//接收地址，默认0xE0;电台回复命令有时也可以是0x00
    //private byte[] dataBuffer=new byte[0];//数据缓冲区
    @SuppressLint("DefaultLocale")
    public void sendCommand(FlexCommand command, String cmdContent) {
        if (getConnector().isConnected()) {
            commandSeq++;
            flexCommand = command;
            commandStr = String.format("C%d%03d|%s\n", commandSeq, command.ordinal()
                    , cmdContent);
            getConnector().sendData(commandStr.getBytes());
        }
    }


    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceTune(int sliceOder, String freq) {
        sendCommand(FlexCommand.SLICE_TUNE, String.format("slice t %d %s", sliceOder, freq));
    }
    @SuppressLint("DefaultLocale")
    public synchronized void commandSliceSetMode(int sliceOder, FlexRadio.FlexMode mode) {
        sendCommand(FlexCommand.SLICE_SET_TX_ANT, String.format("slice s %d mode=%s", sliceOder, mode.toString()));
    }
    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);

    }

    @Override
    public boolean isConnected() {
        if (getConnector()==null) {
            return false;
        }
        return getConnector().isConnected();
    }

    @Override
    public void setUsbModeToRig() {
        if (getConnector()!=null){
            commandSliceSetMode(0, FlexRadio.FlexMode.DIGU);//设置操作模式
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void setFreqToRig() {
        if (getConnector()!=null){
            commandSliceTune(0,String.format("%.3f", getFreq()/1000000f));
        }
    }



    @Override
    public void onReceiveData(byte[] data) {
        //ToastMessage.show("--"+byteToStr(data));

    }

    @Override
    public void readFreqFromRig() {
        if (getConnector()!=null){
            //getConnector().sendData(IcomRigConstant.setReadFreq(ctrAddress, getCivAddress()));
        }
    }

    @Override
    public String getName() {
        return "FlexRadio series";
    }


    public String getFrequencyStr() {
        return BaseRigOperation.getFrequencyStr(getFreq());
    }

    public FlexNetworkRig() {
        Log.d(TAG, "FlexRadio: Create.");
    }
}
