package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.os.Handler;
import android.util.Log;

import com.bg7yoz.ft8cn.database.ControlMode;

import java.util.Timer;
import java.util.TimerTask;

/**
 * KENWOOD TS590,与YAESU3代指令接近，命令结构使用Yaesu3Command,指令在KenwoodTK90RigConstant中。
 */
public class Flex6000Rig extends BaseRig {
    private static final String TAG = "KenwoodTS590Rig";
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
                    getConnector().setPttOn(Flex6000RigConstant.setPTTState(on));
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
            getConnector().sendData(Flex6000RigConstant.setOperationUSB_DIGI_Mode());
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(Flex6000RigConstant.setOperationFreq(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        String s = new String(data);

        if (!s.contains(";"))
        {
            buffer.append(s);
            if (buffer.length()>1000) clearBufferData();
            return;//说明数据还没接收完。
        }else {
            if (s.indexOf(";")>0){//说明接到结束的数据了，并且不是第一个字符是;
              buffer.append(s.substring(0,s.indexOf(";")));
            }
            //开始分析数据
            Flex6000Command flex6000Command = Flex6000Command.getCommand(buffer.toString());
            clearBufferData();//清一下缓存
            //要把剩下的数据放到缓存里
            buffer.append(s.substring(s.indexOf(";")+1));

            if (flex6000Command == null) {
                return;
            }
            if (flex6000Command.getCommandID().equalsIgnoreCase("ZZFA")) {
                long tempFreq=Flex6000Command.getFrequency(flex6000Command);
                if (tempFreq!=0) {//如果tempFreq==0，说明频率不正常
                    setFreq(Flex6000Command.getFrequency(flex6000Command));
                }
            }

        }

    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            clearBufferData();//清空一下缓存
            getConnector().sendData(Flex6000RigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "FLEX 6000 series";
    }

    public Flex6000Rig() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getConnector()!=null){//切换VFO A
                    //getConnector().sendData(Flex6000RigConstant.setVFOMode());
                }
            }
        },START_QUERY_FREQ_DELAY-500);
        readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY,QUERY_FREQ_TIMEOUT);
    }
}
