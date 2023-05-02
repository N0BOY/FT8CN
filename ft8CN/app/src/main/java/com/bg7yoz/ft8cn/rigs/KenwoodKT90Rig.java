package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.os.Handler;
import android.util.Log;

import com.bg7yoz.ft8cn.database.ControlMode;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 3代的指令，不同电台还有不同，频率长度981，991是9位，其它的长度是8位
 */
public class KenwoodKT90Rig extends BaseRig {
    private static final String TAG = "KenwoodKT90Rig";
    private final StringBuilder buffer = new StringBuilder();

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
                    readFreqFromRig();
                } catch (Exception e) {
                    Log.e(TAG, "readFreq error:" + e.getMessage());
                }
            }
        };
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
                    getConnector().setPttOn(KenwoodTK90RigConstant.setPTTState(on));
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
            getConnector().sendData(KenwoodTK90RigConstant.setOperationUSBMode());
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(KenwoodTK90RigConstant.setOperationFreq(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        String s = new String(data);

        if (!s.contains("\r"))
        {
            buffer.append(s);
            if (buffer.length()>1000) clearBufferData();
            //说明数据还没接收完。
        }else {
            if (s.indexOf("\r")>0){//说明接到结束的数据了，并且不是第一个字符是;
              buffer.append(s.substring(0,s.indexOf("\r")));
            }
            //开始分析数据
            Yaesu3Command yaesu3Command = Yaesu3Command.getCommand(buffer.toString());
            clearBufferData();//清一下缓存
            //要把剩下的数据放到缓存里
            buffer.append(s.substring(s.indexOf("\r")+1));

            if (yaesu3Command == null) {
                return;
            }
            if (yaesu3Command.getCommandID().equalsIgnoreCase("FA")) {
                long tempFreq=Yaesu3Command.getFrequency(yaesu3Command);
                if (tempFreq!=0) {//如果tempFreq==0，说明频率不正常
                    setFreq(Yaesu3Command.getFrequency(yaesu3Command));
                }
            }

        }

    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(KenwoodTK90RigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "KENWOOD TK90";
    }

    public KenwoodKT90Rig() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getConnector()!=null){
                    getConnector().sendData(KenwoodTK90RigConstant.setVFOMode());
                }
            }
        },START_QUERY_FREQ_DELAY-500);
        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY,QUERY_FREQ_TIMEOUT);
    }
}
