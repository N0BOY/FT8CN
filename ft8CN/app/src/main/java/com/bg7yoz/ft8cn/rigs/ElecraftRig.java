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
public class ElecraftRig extends BaseRig {
    private static final String TAG = "ElecraftRig";
    private final StringBuilder buffer = new StringBuilder();
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
            getConnector().sendData(ElecraftRigConstant.setReadMetersSWR());
        }
    }

    private void showAlert() {
        if (swr >= ElecraftRigConstant.swr_alert_max) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
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
                    getConnector().setPttOn(ElecraftRigConstant.setPTTState(on));
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
            getConnector().sendData(ElecraftRigConstant.setOperationUSBMode());
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(ElecraftRigConstant.setOperationFreq11Byte(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        String s = new String(data);

        if (!s.contains(";"))
        {
            buffer.append(s);
            if (buffer.length()>1000) clearBufferData();
           // return;//说明数据还没接收完。
        }else {
            if (s.indexOf(";")>0){//说明接到结束的数据了，并且不是第一个字符是;
              buffer.append(s.substring(0,s.indexOf(";")));
            }

            //开始分析数据
            ElecraftCommand elecraftCommand = ElecraftCommand.getCommand(buffer.toString());
            clearBufferData();//清一下缓存
            //要把剩下的数据放到缓存里
            buffer.append(s.substring(s.indexOf(";")+1));

            if (elecraftCommand == null) {
                return;
            }
            if (elecraftCommand.getCommandID().equalsIgnoreCase("FA")
                    || elecraftCommand.getCommandID().equalsIgnoreCase("FB")) {
                long tempFreq=ElecraftCommand.getFrequency(elecraftCommand);
                if (tempFreq!=0) {//如果tempFreq==0，说明频率不正常
                    setFreq(ElecraftCommand.getFrequency(elecraftCommand));
                }
            }else if (elecraftCommand.getCommandID().equalsIgnoreCase("SW")) {//METER
                if (ElecraftCommand.isSWRMeter(elecraftCommand)) {
                    swr = ElecraftCommand.getSWRMeter(elecraftCommand);
                }

                showAlert();
            }



        }

    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(ElecraftRigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "Elecraft series";
    }

    public ElecraftRig() {
        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY,QUERY_FREQ_TIMEOUT);
    }
}
