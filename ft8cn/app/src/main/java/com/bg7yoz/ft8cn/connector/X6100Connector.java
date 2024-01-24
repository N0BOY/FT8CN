package com.bg7yoz.ft8cn.connector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.flex.FlexCommand;
import com.bg7yoz.ft8cn.flex.FlexMeterInfos;
import com.bg7yoz.ft8cn.flex.FlexMeterList;
import com.bg7yoz.ft8cn.flex.FlexRadio;
import com.bg7yoz.ft8cn.flex.RadioTcpClient;
import com.bg7yoz.ft8cn.flex.VITA;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.ui.ToastMessage;
import com.bg7yoz.ft8cn.x6100.X6100Meters;
import com.bg7yoz.ft8cn.x6100.X6100Radio;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * 网络连接方式连接xiegu ft8cns
 * @author BGY70Z
 * @date 2023-12-01
 */
public class X6100Connector extends BaseRigConnector {

    public interface OnWaveDataReceived{
        void OnDataReceived(int bufferLen,float[] buffer);
    }
    //public int maxRfPower;
    //public int maxTunePower;

    private static final String TAG = "X6100Connector";

    private X6100Radio xieguRadio;

    private OnWaveDataReceived onWaveDataReceived;

    private BaseRig baseRig;
    private boolean streamIsOn =false;

    public float maxTXPower=10.0f;
    public MutableLiveData<Float> mutableMaxTxPower = new MutableLiveData<>();


    public X6100Connector(Context context, X6100Radio xiegRadio, int controlMode) {
        super(controlMode);
        this.xieguRadio = xiegRadio;
        setXieguRadioInterface();

    }

    public static short[] byteDataTo16BitData(byte[] buffer){
        short[] data=new short[buffer.length /2];
        for (int i = 0; i < buffer.length/2; i++) {
            short  res = (short) ((buffer[i*2+1] & 0x00FF) | (((short) buffer[i*2]) << 8));
            data[i]=res;
        }
        return data;
    }

    /**
     * 把原始的声音数据转换成16位的数组数据。
     * @param buffer 原始的声音数据(8位)
     * @return 返回16位的int格式数组
     */
    private float[] byteDataToFloatData(byte[] buffer){
        float[] data=new float[buffer.length /2];
        for (int i = 0; i < buffer.length/2; i++) {
            int  res = (buffer[i*2] & 0x000000FF) | (((int) buffer[i*2+1]) << 8);
            data[i]=res/32768.0f;
        }
        return data;
    }
    private void setXieguRadioInterface() {
        xieguRadio.setOnReceiveStreamData(new X6100Radio.OnReceiveStreamData() {
            @Override
            public void onReceiveAudio(byte[] data) {
                if (onWaveDataReceived!=null){
                    float[] waveFloat = byteDataToFloatData(data);
                    onWaveDataReceived.OnDataReceived(waveFloat.length,waveFloat);
                }
            }

            @Override
            public void onReceiveIQ(byte[] data) {

            }

            @Override
            public void onReceiveFFT(VITA vita) {

            }

            @Override
            public void onReceiveMeter(X6100Meters meters) {
                maxTXPower= meters.max_power;
                mutableMaxTxPower.postValue(maxTXPower);
            }

            @Override
            public void onReceiveUnKnow(byte[] data) {

            }
        });


        //当有命令返回值时的事件
        xieguRadio.setOnCommandListener(new X6100Radio.OnCommandListener() {
            @Override
            public void onResponse(X6100Radio.XieguResponse response) {
                Log.d(TAG, String.format("onResponse(%s): %s"
                        ,response.xieguCommand.toString(),response.rawData ));
                if (response.xieguCommand == X6100Radio.XieguCommand.STREAM){
                   if (response.resultContent.toUpperCase().contains("PORT=")){//说明流端口打开了
                        streamIsOn =true;
                   }
                }

                if (response.resultCode!=0) {//只显示失败的命令
                    ToastMessage.show(response.resultContent);
                    Log.e(TAG, "onResponse: "+response.resultContent);
                }

            }
        });

        //当有状态信息接收到时
        xieguRadio.setOnStatusListener(new X6100Radio.OnStatusListener() {
            @Override
            public void onStatus(X6100Radio.XieguResponse response) {
                //显示状态消息
                if (response.resultCode == 0){//说明是电台状态变化了
                    String status[] = response.resultContent.split(" ");
                    for (int i = 0; i < status.length; i++) {
                        if (status[i].startsWith("active_freq")){//找出频率状态，设置频率
                            String temp[]=status[i].split("=");
                            if (baseRig != null) {
                                baseRig.setFreq(Long.parseLong(temp[1].trim()));
                            }
                        }
                    }
                }
                Log.d(TAG, "onStatus: "+response.rawData );
            }
        });



        xieguRadio.setOnTcpConnectStatus(new X6100Radio.OnTcpConnectStatus() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onConnectSuccess(RadioTcpClient tcpClient) {
                ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.init_flex_operation)
                        ,xieguRadio.getModelName()));
                new Thread(new Runnable() {//此处使用线程方式，是防止tcp对象阻塞
                    @Override
                    public void run() {
                        while (!streamIsOn) {//等待电台打开流端口
                            xieguRadio.commandOpenStream();//设置UDP端口
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                            }
                            //todo 此处经常丢命令
                            xieguRadio.commandGetAudioInfo();//读取6100播放的参数
                            //xieguRadio.commandSubGetMeter();//查阅仪表索引编号
                            xieguRadio.commandSubAllMeter();//订阅仪表流数据
                            //xieguRadio.commandSetTxPower(1);//订阅仪表流数据
                        }
                    }
                }).start();
            }

            @Override
            public void onConnectFail(RadioTcpClient tcpClient) {
                ToastMessage.show(String.format(GeneralVariables.getStringFromResource
                        (R.string.xiegu_connect_failed),xieguRadio.getModelName()));
            }

            @Override
            public void onConnectionClosed(RadioTcpClient tcpClient) {
                if (baseRig.getOnRigStateChanged()!=null) {
                    baseRig.getOnRigStateChanged().onDisconnected();
                }
            }
        });

    }


    @Override
    public void sendData(byte[] data) {
        xieguRadio.sendData(data);
    }


    @Override
    public void setPttOn(boolean on) {
        xieguRadio.isPttOn=on;
        xieguRadio.commandPTTOnOff(on);
    }

    @Override
    public void setPttOn(byte[] command) {
    }

    @Override
    public void sendWaveData(float[] data) {
        xieguRadio.sendWaveData(data);
    }


    public void setMaxTXPower(int power){
        maxTXPower=power;
        mutableMaxTxPower.postValue(maxTXPower);
        GeneralVariables.flexMaxRfPower=power;
        xieguRadio.commandSetTxPower(power);//设置发射功率

    }

    //传送a91数据包的方式
    @Override
    public void sendFt8A91(byte[] a91,float baseFreq){
        Log.d(TAG,String.format("A91:%s", BaseRig.byteToStr(a91)));
        //xieguRadio.commandSendA91(a91,GeneralVariables.volumePercent,baseFreq);
        xieguRadio.commandSendA91(a91,0.95f,baseFreq);
    }

    @Override
    public void setRFVolume(int volume) {
        xieguRadio.commandSetTxVol(volume);
    }

    @Override
    public void connect() {
        super.connect();
        xieguRadio.openAudio();
        xieguRadio.connect();
        xieguRadio.openStreamPort();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        xieguRadio.closeAudio();
        xieguRadio.closeStreamPort();
        xieguRadio.disConnect();
    }

    public OnWaveDataReceived getOnWaveDataReceived() {
        return onWaveDataReceived;
    }

    public void setOnWaveDataReceived(OnWaveDataReceived onWaveDataReceived) {
        this.onWaveDataReceived = onWaveDataReceived;
    }

    public BaseRig getBaseRig() {
        return baseRig;
    }

    public void setBaseRig(BaseRig baseRig) {
        this.baseRig = baseRig;
    }

    @Override
    public boolean isConnected() {
        return xieguRadio.isConnect();
    }

    public X6100Radio getXieguRadio() {
        return xieguRadio;
    }
}
