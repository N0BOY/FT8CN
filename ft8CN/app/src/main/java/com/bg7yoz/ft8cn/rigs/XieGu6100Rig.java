package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.Timer;
import java.util.TimerTask;

public class XieGu6100Rig extends BaseRig {
    private static final String TAG = "IcomRig";

    private final int ctrAddress = 0xE0;//接收地址，默认0xE0;电台回复命令有时也可以是0x00
    private byte[] dataBuffer = new byte[0];//数据缓冲区
    private int swr = 0;
    private boolean swrAlert = false;
    private Timer readFreqTimer = new Timer();

    private TimerTask readTask() {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isConnected()) {
                        readFreqTimer.cancel();
                        readFreqTimer.purge();
                        readFreqTimer = null;
                        return;
                    }
                    if (isPttOn()){
                        readSWRMeter();
                    }else {
                        readFreqFromRig();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "readFreq or meter error:" + e.getMessage());
                }
            }
        };
    }


    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);

        if (getConnector() != null) {
            switch (getControlMode()) {
                case ControlMode.CAT://以CIV指令
                    getConnector().setPttOn(IcomRigConstant.setPTTState(ctrAddress, getCivAddress()
                            , on ? IcomRigConstant.PTT_ON : IcomRigConstant.PTT_OFF));
                    break;
                case ControlMode.RTS:
                case ControlMode.DTR:
                    getConnector().setPttOn(on);
                    break;
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (getConnector() == null) {
            return false;
        }
        return getConnector().isConnected();
    }

    @Override
    public void setUsbModeToRig() {
        if (getConnector() != null) {
//            getConnector().sendData(IcomRigConstant.setOperationMode(ctrAddress
//                    , getCivAddress(), 1));//usb=1
            getConnector().sendData(IcomRigConstant.setOperationDataMode(ctrAddress, getCivAddress(), IcomRigConstant.USB));//usb-d
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(IcomRigConstant.setOperationFrequency(ctrAddress
                    , getCivAddress(), getFreq()));
        }
    }

    /**
     * 查找指令的结尾的位置，如果没找到，值是-1。
     *
     * @param data 数据
     * @return 位置
     */
    private int getCommandEnd(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == (byte) 0xFD) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找指令头，没找到返回-1，找到返回FE FE的第一个位置
     *
     * @param data 数据
     * @return 位置
     */
    private int getCommandHead(byte[] data) {
        if (data.length < 2) return -1;
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == (byte) 0xFE && data[i + 1] == (byte) 0xFE) {
                return i;
            }
        }
        return -1;
    }

    private void analysisCommand(byte[] data) {
        int headIndex = getCommandHead(data);
        if (headIndex == -1) {//说明没有指令头
            return;
        }
        IcomCommand icomCommand;
        if (headIndex == 0) {
            icomCommand = IcomCommand.getCommand(ctrAddress, getCivAddress(), data);
        } else {
            byte[] temp = new byte[data.length - headIndex];
            System.arraycopy(data, headIndex, temp, 0, temp.length);
            icomCommand = IcomCommand.getCommand(ctrAddress, getCivAddress(), temp);
        }
        if (icomCommand == null) {
            return;
        }

        //目前只对频率和模式消息作反应
        switch (icomCommand.getCommandID()) {
            case IcomRigConstant.CMD_SEND_FREQUENCY_DATA://获取到的是频率数据
            case IcomRigConstant.CMD_READ_OPERATING_FREQUENCY:
                //获取频率
                long freqTemp = icomCommand.getFrequency(false);
                if (freqTemp >= 500000 && freqTemp <= 250000000) {//协谷的频率范围
                    setFreq(freqTemp);
                }
                break;
            case IcomRigConstant.CMD_SEND_MODE_DATA://获取到的是模式数据
            case IcomRigConstant.CMD_READ_OPERATING_MODE:
                break;
            case IcomRigConstant.CMD_READ_METER://读meter//此处的指令，只在网络模式实现，以后可能会在串口方面实现
                if (icomCommand.getSubCommand() == IcomRigConstant.CMD_READ_METER_SWR) {
                    //协谷的小端模式
                    int temp=IcomRigConstant.twoByteBcdToIntBigEnd(icomCommand.getData(true));
                    if (temp!=255) {
                        swr = temp;//
                    }
                }
                showAlert();//检查meter值是否在告警范围

                break;
        }
    }


    private void showAlert() {
        if (swr >= IcomRigConstant.swr_alert_max) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
        }
    }



    @Override
    public void onReceiveData(byte[] data) {
        int commandEnd = getCommandEnd(data);
        if (commandEnd <= -1) {//这是没有指令结尾
            byte[] temp = new byte[dataBuffer.length + data.length];
            System.arraycopy(dataBuffer, 0, temp, 0, dataBuffer.length);
            System.arraycopy(data, 0, temp, dataBuffer.length, data.length);
            dataBuffer = temp;
        } else {
            byte[] temp = new byte[dataBuffer.length + commandEnd + 1];
            System.arraycopy(dataBuffer, 0, temp, 0, dataBuffer.length);
            dataBuffer = temp;
            System.arraycopy(data, 0, dataBuffer, dataBuffer.length - commandEnd - 1, commandEnd + 1);
        }
        if (commandEnd != -1) {
            analysisCommand(dataBuffer);
        }
        dataBuffer = new byte[0];//清空缓冲区
        if (commandEnd <= -1 || commandEnd < data.length) {
            byte[] temp = new byte[data.length - commandEnd + 1];
            for (int i = 0; i < data.length - commandEnd - 1; i++) {
                temp[i] = data[commandEnd + i + 1];
            }
            dataBuffer = temp;
        }


    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            getConnector().sendData(IcomRigConstant.setReadFreq(ctrAddress, getCivAddress()));
        }
    }

    private void readSWRMeter() {
        if (getConnector() != null) {
            getConnector().sendData(IcomRigConstant.getSWRState(ctrAddress, getCivAddress()));
        }
    }

    @Override
    public String getName() {
        return "XIEGU X6100 series";
    }


    public String getFrequencyStr() {
        return BaseRigOperation.getFrequencyStr(getFreq());
    }

    public XieGu6100Rig(int civAddress) {
        Log.d(TAG, "XieGuRig: Create.");
        setCivAddress(civAddress);

        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY, QUERY_FREQ_TIMEOUT);
        //readFreqTimer.schedule(readTask(),START_QUERY_FREQ_DELAY,1000);
    }
}
