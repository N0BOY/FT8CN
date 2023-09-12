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

/**
 * 3代的指令，不同电台还有不同，频率长度981，991是9位，其它的长度是8位
 */
public class Yaesu39Rig extends BaseRig {
    private static final String TAG = "Yaesu3Rig";
    private final StringBuilder buffer = new StringBuilder();
    private int swr=0;
    private int alc=0;
    private boolean alcMaxAlert = false;
    private boolean swrAlert = false;

    private boolean isDataUsb=false;//是不是DATA-USB模式

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
                    if (isPttOn()) {
                        readMeters();
                    } else {
                        readFreqFromRig();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "readFreq error:" + e.getMessage());
                }
            }
        };
    }

    /**
     * 读取Meter RM;
     */
    private void readMeters() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(Yaesu3RigConstant.setRead39Meters_ALC());
            getConnector().sendData(Yaesu3RigConstant.setRead39Meters_SWR());
        }
    }
    private void showAlert() {
        if (swr >= Yaesu3RigConstant.swr_39_alert_max) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
        }
        if (alc > Yaesu3RigConstant.alc_39_alert_max) {//网络模式下不警告ALC
            if (!alcMaxAlert) {
                alcMaxAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_high_alert));
            }
        } else {
            alcMaxAlert = false;
        }

    }
    /**
     * 清空缓存数据
     */
    private void clearBufferData() {
        buffer.setLength(0);
    }

    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);
        if (getConnector() != null) {
            switch (getControlMode()) {
                case ControlMode.CAT://以CIV指令
                    getConnector().setPttOn(Yaesu3RigConstant.setPTTState(on));
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
            if (isDataUsb) {//使用DATA-USB模式
                getConnector().sendData(Yaesu3RigConstant.setOperationUSB_Data_Mode());
            }else {
                getConnector().sendData(Yaesu3RigConstant.setOperationUSBMode());
            }
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(Yaesu3RigConstant.setOperationFreq9Byte(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        String s = new String(data);

        if (!s.contains(";")) {
            buffer.append(s);
            if (buffer.length() > 1000) clearBufferData();
            // return;//说明数据还没接收完。
        } else {
            if (s.indexOf(";") > 0) {//说明接到结束的数据了，并且不是第一个字符是;
                buffer.append(s.substring(0, s.indexOf(";")));
            }

            //开始分析数据
            Yaesu3Command yaesu3Command = Yaesu3Command.getCommand(buffer.toString());
            clearBufferData();//清一下缓存
            //要把剩下的数据放到缓存里
            buffer.append(s.substring(s.indexOf(";") + 1));

            if (yaesu3Command == null) {
                return;
            }
            if (yaesu3Command.getCommandID().equalsIgnoreCase("FA")
                    || yaesu3Command.getCommandID().equalsIgnoreCase("FB")) {
                long tempFreq = Yaesu3Command.getFrequency(yaesu3Command);
                if (tempFreq != 0) {//如果tempFreq==0，说明频率不正常
                    setFreq(Yaesu3Command.getFrequency(yaesu3Command));
                }
            }else if (yaesu3Command.getCommandID().equalsIgnoreCase("RM")){//METER
                if (Yaesu3Command.isSWRMeter39(yaesu3Command)){
                    swr=Yaesu3Command.getSWROrALC39(yaesu3Command);
                }
                if (Yaesu3Command.isALCMeter39(yaesu3Command)){
                    alc=Yaesu3Command.getSWROrALC39(yaesu3Command);
                }
                showAlert();
            }

        }

    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(Yaesu3RigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "YAESU FT-891";
    }

    public Yaesu39Rig(boolean isDataUsb) {
        this.isDataUsb=isDataUsb;
        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY, QUERY_FREQ_TIMEOUT);
    }
}
