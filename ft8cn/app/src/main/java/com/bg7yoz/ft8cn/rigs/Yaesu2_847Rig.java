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
 * YAESU的部分电台，回送的数据不是连续的，所以，要做一个缓冲区，接受5字节长度。满了就复位。或发送指令时，就复位。
 * ft848在连接成功后，必须发送5个0，结束后发送4个0加80
 */
public class Yaesu2_847Rig extends BaseRig{
    private static final String TAG="Yaesu2_847Rig";
    private Timer readFreqTimer = new Timer();

    private int swr = 0;
    private int alc = 0;
    private boolean alcMaxAlert = false;
    private boolean swrAlert = false;
    private boolean sentConnect =false;

    private TimerTask readTask(){
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isConnected()){
                        readFreqTimer.cancel();
                        readFreqTimer.purge();
                        readFreqTimer=null;
                        return;
                    }
                    if (!sentConnect) {//发送连接头数据，5个0，只发送1次
                        sendConnectData();
                        sentConnect =!sentConnect;
                    }

                    if (isPttOn()) {
                        readMeters();
                    } else {
                        readFreqFromRig();
                    }
                }catch (Exception e)
                {
                    Log.e(TAG, "readFreq error:"+e.getMessage() );
                }
            }
        };
    }


    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);

        if (getConnector()!=null){
            switch (getControlMode()){
                case ControlMode.CAT://以CIV指令
                    getConnector().setPttOn(Yaesu2RigConstant.setPTTState(on));
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
        if (getConnector()==null) {
            return false;
        }
        return getConnector().isConnected();
    }

    @Override
    public void setUsbModeToRig() {
        if (getConnector()!=null){
            getConnector().sendData(Yaesu2RigConstant.setOperationUSB847Mode());
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector()!=null){
            getConnector().sendData(Yaesu2RigConstant.setOperationFreq(getFreq()));
        }
    }

    @Override
    public void onReceiveData(byte[] data) {
        //YAESU 817的指令，返回频率是5字节的，METER是2字节的。
        //Meter是2字节的，第一字节高位功率，0-A，低位ALC 0-9,第二字节高位驻波比，0-C，0为高驻波，低位音频输入0-8
        if (data.length == 5) {//频率
            long freq = Yaesu2Command.getFrequency(data);
            if (freq > -1) {
                setFreq(freq);
            }
        } else if (data.length == 2) {//METERS
            alc = (data[0] & 0x0f);
            swr = (data[1] & 0x0f0) >> 4;
            showAlert();
        }

    }

    /**
     * 读取Meter RM;
     */
    private void readMeters() {
        if (getConnector() != null) {
            getConnector().sendData(Yaesu2RigConstant.readMeter());
        }
    }

    private void sendConnectData() {//连接电台后，要发送5个0
        if (getConnector() != null) {
            getConnector().sendData(Yaesu2RigConstant.sendConnectData());
        }
    }

    @Override
    public void onDisconnecting() {//断开电台前，要发送4个0加80
        if (getConnector() != null) {
            getConnector().sendData(Yaesu2RigConstant.sendDisconnectData());
        }
        super.onDisconnecting();
    }

    private void showAlert() {
        if (swr > Yaesu2RigConstant.swr_817_alert_min) {
            if (!swrAlert) {
                swrAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.swr_high_alert));
            }
        } else {
            swrAlert = false;
        }
        if (alc >= Yaesu2RigConstant.alc_817_alert_max) {//网络模式下不警告ALC
            if (!alcMaxAlert) {
                alcMaxAlert = true;
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.alc_high_alert));
            }
        } else {
            alcMaxAlert = false;
        }

    }
    @Override
    public void readFreqFromRig(){
        if (getConnector()!=null){
            //clearBuffer();//清除一下缓冲区
            getConnector().sendData(Yaesu2RigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "YAESU 847 series";
    }

    public Yaesu2_847Rig() {
        readFreqTimer.schedule(readTask(),START_QUERY_FREQ_DELAY,QUERY_FREQ_TIMEOUT);
    }

}
