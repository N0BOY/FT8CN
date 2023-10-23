package com.bg7yoz.ft8cn.rigs;

/**
 * IcomRig是通用的Icom电台控制类。对于wifi模式，实际的控制是通过IComWifiConnector(继承于WifiConnector)
 * 在IComWifiConnector中，有IComWifiRig具体操作电台
 */

import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;
import com.bg7yoz.ft8cn.icom.IComPacketTypes;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.Timer;
import java.util.TimerTask;

public class IcomRig extends BaseRig {
    private static final String TAG = "IcomRig";

    private final int ctrAddress = 0xE0;//接收地址，默认0xE0;电台回复命令有时也可以是0x00
    private byte[] dataBuffer = new byte[0];//数据缓冲区
    private int alc = 0;
    private int swr = 0;
    private boolean alcMaxAlert = false;
    private boolean swrAlert = false;
    private Timer meterTimer;//查询meter的Timer
    //private boolean isPttOn = false;

    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);
        //isPttOn = on;
        alcMaxAlert = false;
        swrAlert = false;
        if (on) {
            //修正连接方式0x03是wlan,01是usb，0x02是usb+mic，确保声音能发送到电台
            if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
                sendCivData(IcomRigConstant.setConnectorDataMode(ctrAddress, getCivAddress(), (byte) 0x03));
            } else if (GeneralVariables.connectMode == ConnectMode.USB_CABLE) {
                sendCivData(IcomRigConstant.setConnectorDataMode(ctrAddress, getCivAddress(), (byte) 0x01));
            } else {
                sendCivData(IcomRigConstant.setConnectorDataMode(ctrAddress, getCivAddress(), (byte) 0x02));
            }
        }

        if (getConnector() != null) {
            if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
                getConnector().setPttOn(on);
                return;
            }

            switch (getControlMode()) {
                case ControlMode.CAT://以CIV指令
                    try{
                        Thread.sleep(100);
                        getConnector().setPttOn(IcomRigConstant.setPTTState(ctrAddress, getCivAddress()
                                , on ? IcomRigConstant.PTT_ON : IcomRigConstant.PTT_OFF));
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    break;
                //case ControlMode.NETWORK:
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
            //因为担心老的ICOM电台不一定支持USB-D，所以，先做个一USB模式，再进入USB-D模式，
            // 这样，如果USB-D模式不支持，USB-D的指令就是无效的，电台就停留在USB模式下了
            //getConnector().sendData(IcomRigConstant.setOperationMode(ctrAddress
            // , getCivAddress(), IcomRigConstant.USB));//usb
            getConnector().sendData(IcomRigConstant.setOperationDataMode(ctrAddress
                    , getCivAddress(), IcomRigConstant.USB));//usb-d
        }
    }

    private void sendCivData(byte[] data) {
        if (getConnector() != null) {
            getConnector().sendData(data);
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

    @Override
    public void sendWaveData(Ft8Message message) {//发送音频数据到电台，用于网络方式
        if (getConnector() != null) {//把生成的具体音频数据传递到Connector，
            float[] data = GenerateFT8.generateFt8(message, GeneralVariables.getBaseFrequency()
                    ,12000);//此处icom电台发射音频的采样率是12000
            if (data==null){
                setPTT(false);
                return;
            }
            getConnector().sendWaveData(data);
        }
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
                setFreq(icomCommand.getFrequency(false));
                break;
            case IcomRigConstant.CMD_SEND_MODE_DATA://获取到的是模式数据
            case IcomRigConstant.CMD_READ_OPERATING_MODE:
                break;
            case IcomRigConstant.CMD_READ_METER://读meter//此处的指令，只在网络模式实现，以后可能会在串口方面实现
                if (icomCommand.getSubCommand() == IcomRigConstant.CMD_READ_METER_ALC) {
                    alc = IcomRigConstant.twoByteBcdToInt(icomCommand.getData(true));
                }
                if (icomCommand.getSubCommand() == IcomRigConstant.CMD_READ_METER_SWR) {
                    swr = IcomRigConstant.twoByteBcdToInt(icomCommand.getData(true));
                }
                showAlert();//检查meter值是否在告警范围
                break;
            case IcomRigConstant.CMD_CONNECTORS:
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
        if (alc > IcomRigConstant.alc_alert_max) {//网络模式下不警告ALC
            if (!alcMaxAlert) {
                alcMaxAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_high_alert));
            }
        } else {
            alcMaxAlert = false;
        }

    }

    @Override
    public void onReceiveData(byte[] data) {

        //ToastMessage.show("--"+byteToStr(data));


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

    @Override
    public String getName() {
        return "ICOM series";
    }

    public void startMeterTimer() {
        meterTimer = new Timer();
        meterTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isPttOn()) {//当Ptt被按下去的时候测量
                    sendCivData(IcomRigConstant.getSWRState(ctrAddress, getCivAddress()));
                    sendCivData(IcomRigConstant.getALCState(ctrAddress, getCivAddress()));
                }
            }
        }, 0, IComPacketTypes.METER_TIMER_PERIOD_MS);
    }


    public String getFrequencyStr() {
        return BaseRigOperation.getFrequencyStr(getFreq());
    }

    public IcomRig(int civAddress) {
        Log.d(TAG, "IcomRig: Create.");
        setCivAddress(civAddress);
        startMeterTimer();
    }
}
