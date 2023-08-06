package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.os.Handler;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * (tr)uSDX, fork from KENWOOD TS590.
 */
public class TrUSDXRig extends BaseRig {
    private static final String TAG = "TrUSDXRig";
    private final StringBuilder buffer = new StringBuilder();

    private Timer readFreqTimer = new Timer();
    private int swr=0;
    private int alc=0;
    private boolean alcMaxAlert = false;
    private boolean swrAlert = false;
    private boolean rxStreaming = false;

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
                        readMeters();//读METER
                    }else {
                        readFreqFromRig();//读频率
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
    private void readMeters(){
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(KenwoodTK90RigConstant.setRead590Meters());
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
                    if (on) {
                        rxStreaming = false;
                    }
                    getConnector().setPttOn(KenwoodTK90RigConstant.setTS590PTTState(on));
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
            getConnector().sendData(KenwoodTK90RigConstant.setTS590OperationUSBMode());
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(KenwoodTK90RigConstant.setTS590OperationFreq(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        byte[] remain = data;
        String s = new String(data);
        Log.d(TAG, "data received: " + s);
        while (s.contains(";")) { // ;
            // TODO apply effective way
            int idx = s.indexOf(";");
            byte[] cutted = Arrays.copyOf(remain, idx);
            remain = Arrays.copyOfRange(remain, idx + 1, remain.length);
            s = new String(remain);

            if (rxStreaming) {
                if (cutted.length > 0) {
                    getConnector().receiveWaveData(cutted);
                }
                rxStreaming = false;
            } else {
                buffer.append(new String(cutted));
                //开始分析数据
                Yaesu3Command yaesu3Command = Yaesu3Command.getCommand(buffer.toString());
                clearBufferData();//清一下缓存

                if (yaesu3Command == null) {
                    return;
                }
                String cmd=yaesu3Command.getCommandID();
                Log.i(TAG, "command: " + cmd);
                if (cmd.equalsIgnoreCase("FA")) {//频率
                    long tempFreq=Yaesu3Command.getFrequency(yaesu3Command);
                    if (tempFreq!=0) {//如果tempFreq==0，说明频率不正常
                        setFreq(Yaesu3Command.getFrequency(yaesu3Command));
                    }
                }else if (cmd.equalsIgnoreCase("RM")){//meter
                    if (Yaesu3Command.is590MeterSWR(yaesu3Command)) {
                        swr = Yaesu3Command.get590ALCOrSWR(yaesu3Command);
                    }
                    if (Yaesu3Command.is590MeterALC(yaesu3Command)) {
                        alc = Yaesu3Command.get590ALCOrSWR(yaesu3Command);
                    }
                    showAlert();
                }
            }
        }
        if (remain.length <= 0) {
            return;
        }
        if (remain.length >= 2 && remain[0] == 0x55 && remain[1] == 0x53) {// US
            clearBufferData();
            rxStreaming = true;
            byte[] wave = Arrays.copyOfRange(remain, 2, remain.length);
            if (getConnector() != null && wave.length > 0) {
                getConnector().receiveWaveData(wave);
            }
        } else {
            buffer.append(s);
        }
    }
    private void showAlert() {
        if (swr >= KenwoodTK90RigConstant.ts_590_swr_alert_max) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
        }
        if (alc > KenwoodTK90RigConstant.ts_590_alc_alert_max) {//网络模式下不警告ALC
            if (!alcMaxAlert) {
                alcMaxAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_high_alert));
            }
        } else {
            alcMaxAlert = false;
        }

    }
    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(KenwoodTK90RigConstant.setTS590ReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "(tr)uSDX";
    }

    @Override
    public boolean supportWaveOverCAT() {
        return getConnector() != null && getControlMode() == ControlMode.CAT;
    }

    @Override
    public void onDisconnecting() {
        if (getConnector() != null) {
            clearBufferData();
            getConnector().sendData(KenwoodTK90RigConstant.setTrUSDXStreaming(false));
        }
    }

    public TrUSDXRig() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getConnector()!=null){
                    getConnector().sendData(KenwoodTK90RigConstant.setTS590VFOMode());
                    getConnector().sendData(KenwoodTK90RigConstant.setTrUSDXStreaming(true));
                }
            }
        },START_QUERY_FREQ_DELAY-500);
        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY,QUERY_FREQ_TIMEOUT);
    }
}
