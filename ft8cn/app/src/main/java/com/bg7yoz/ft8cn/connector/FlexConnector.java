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
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * 有线连接方式的Connector，这里是指USB方式的
 * @author BGY70Z
 * @date 2023-03-20
 */
public class FlexConnector extends BaseRigConnector {
    public MutableLiveData<FlexMeterList> mutableMeterList=new MutableLiveData<>();
    public FlexMeterInfos flexMeterInfos =new FlexMeterInfos("");
    public FlexMeterList meterList=new FlexMeterList();
    public interface OnWaveDataReceived{
        void OnDataReceived(int bufferLen,float[] buffer);
    }
    public int maxRfPower;
    public int maxTunePower;

    private static final String TAG = "CableConnector";

    private FlexRadio flexRadio;

    private OnWaveDataReceived onWaveDataReceived;


    public FlexConnector(Context context, FlexRadio flexRadio, int controlMode) {
        super(controlMode);
        this.flexRadio = flexRadio;
        maxTunePower=GeneralVariables.flexMaxTunePower;
        maxRfPower=GeneralVariables.flexMaxRfPower;
        setFlexRadioInterface();
        //connect();
    }

    public static int[] byteDataTo16BitData(byte[] buffer){
        int[] data=new int[buffer.length /2];
        for (int i = 0; i < buffer.length/2; i++) {
            int  res = (buffer[i*2+1] & 0x000000FF) | (((int) buffer[i*2]) << 8);
            data[i]=res;
        }
        return data;
    }


    private void setFlexRadioInterface() {
        flexRadio.setOnReceiveStreamData(new FlexRadio.OnReceiveStreamData() {
            @Override
            public void onReceiveAudio(byte[] data) {
                if (onWaveDataReceived!=null){
                    float[] buffer=getMonoFloatFromBytes(data);//把24000转成12000，立体声转成单声道
                    onWaveDataReceived.OnDataReceived(buffer.length,buffer);
                }
            }

            @Override
            public void onReceiveIQ(byte[] data) {

            }

            @Override
            public void onReceiveFFT(VITA vita) {
                //if (vita.streamId==0x40000000) {
                //    mutableVita.postValue(vita.showHeadStr() + "\n" + vita.showPayloadHex());
                //}
            }

            @Override
            public void onReceiveMeter(VITA vita) {
                //Log.e(TAG, "onReceiveMeter: "+vita.showPayloadHex() );
                meterList.setMeters(vita.payload,flexMeterInfos);

                mutableMeterList.postValue(meterList);

            }

            @Override
            public void onReceiveUnKnow(byte[] data) {

            }
        });


        //当有命令返回值时的事件
        flexRadio.setOnCommandListener(new FlexRadio.OnCommandListener() {
            @Override
            public void onResponse(FlexRadio.FlexResponse response) {
                if (response.resultValue!=0) {//只显示失败的命令
                    //ToastMessage.show(response.resultStatus());
                    //Log.e(TAG, "onResponse: "+response.resultStatus());
                }

                Log.e(TAG, "onResponse: command:"+response.flexCommand.toString());
                //Log.e(TAG, "onResponse: "+response.resultStatus());
                Log.e(TAG, "onResponse: "+response.rawData );

                if (response.flexCommand== FlexCommand.METER_LIST){
                    //FlexMeters flexMeters=new FlexMeters(response.exContent);
                    flexMeterInfos.setMeterInfos(response.exContent);
                    flexRadio.commandSubMeterAll();//订阅全部仪表消息
                    //flexMeters.getAllMeters();
                    //Log.e(TAG, "onResponse: ----->>>"+flexMeters.getAllMeters() );
                }
                if (response.flexCommand==FlexCommand.STREAM_CREATE_DAX_TX){
                    flexRadio.streamTxId=response.daxTxStreamId;
                }

//                if (response.flexCommand== FlexCommand.METER_LIST){
//                    Log.e(TAG, "onResponse: ."+response.rawData.replace("#","\n") );
//                }
            }
        });

        //当有状态信息接收到时
        flexRadio.setOnStatusListener(new FlexRadio.OnStatusListener() {
            @Override
            public void onStatus(FlexRadio.FlexResponse response) {
                //显示状态消息
                //ToastMessage.show(response.content);
                Log.e(TAG, "onStatus: "+response.rawData );
            }
        });



        flexRadio.setOnTcpConnectStatus(new FlexRadio.OnTcpConnectStatus() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onConnectSuccess(RadioTcpClient tcpClient) {
                ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.init_flex_operation)
                        ,flexRadio.getModel()));

                flexRadio.commandClientDisconnect();//断开之前的全部连接
                flexRadio.commandClientGui();//创建GUI

                flexRadio.commandSubDaxAll();//注册全部DAX流



                flexRadio.commandClientSetEnforceNetWorkGui();//对网络MTU做设置

                //flexRadio.commandSliceList();//列slice
                flexRadio.commandSliceCreate();//创建slice





                //todo 防止流的端口没有释放，把端口变换一下？
                //FlexRadio.streamPort++;

                flexRadio.commandUdpPort();//设置UDP端口


                flexRadio.commandStreamCreateDaxRx(1);//创建流数据到DAX通道1
                flexRadio.commandStreamCreateDaxTx(1);//创建流数据到DAX通道1

                flexRadio.commandSetDaxAudio(1, 0, true);//打开DAX

                //TODO 是否设置？？？ dax tx T 或者 dax tx 1
                flexRadio.commandSliceTune(0,String.format("%.3f",GeneralVariables.band/1000000f));
                flexRadio.commandSliceSetMode(0, FlexRadio.FlexMode.DIGU);//设置操作模式
                flexRadio.commandSetFilter(0, 0, 3000);//设置滤波为3000HZ


                flexRadio.commandMeterList();//列一下仪表
                //flexRadio.commandSubMeterAll();//此处订阅指令放到了接收响应部分

                setMaxRfPower(maxRfPower);//设置发射功率
                setMaxTunePower(maxTunePower);//设置调谐功率

                //flexRadio.commandSubMeterById(5);//列指定的仪表

                //flexRadio.commandSliceSetNR(0, true);
                //flexRadio.commandSliceSetNB(0, true);

                //flexRadio.commandDisplayPan(10, 10);
                //flexRadio.commandSetFilter(0,0,3000);
                // flexRadio.sendCommand(FlexCommand.FILT_SET, "filt 0 0 3000");
                //flexRadio.commandMeterCreateAmp();
                //flexRadio.commandMeterList();


                //flexRadio.sendCommand(FlexCommand.INFO, "info");
                //flexRadio.commandGetInfo();
                //flexRadio.commandSliceGetError(0);

                //flexRadio.sendCommand(FlexCommand.SLICE_GET_ERROR, "slice get_error 0");
                //flexRadio.sendCommand(FlexCommand.REMOTE_RADIO_RX_ON, "remote_audio rx_on on");
                //
                //flexRadio.sendCommand("c1|client gui\n");
                //playData();


            }

            @Override
            public void onConnectFail(RadioTcpClient tcpClient) {
                ToastMessage.show(String.format(GeneralVariables.getStringFromResource
                        (R.string.flex_connect_failed),flexRadio.getModel()));
            }
        });

    }
    public void setMaxRfPower(int power){
        maxRfPower=power;
        GeneralVariables.flexMaxRfPower=power;
        flexRadio.commandSetRfPower(maxRfPower);//设置发射功率

    }
    public void setMaxTunePower(int power){
        maxTunePower=power;
        GeneralVariables.flexMaxTunePower=power;
        flexRadio.commandSetTunePower(maxTunePower);//设置调谐功率

    }
    public void startATU(){
        flexRadio.commandStartATU();
    }
    public void tuneOnOff(boolean on){
        flexRadio.commandTuneTransmitOnOff(on);
    }
    public void subAllMeters(){
        if (flexMeterInfos.size()==0) {
            //todo commandMeterList()是否可以不使用？
            flexRadio.commandMeterList();//列一下仪表
            flexRadio.commandSubMeterAll();//显示全部仪表消息
        }
    }

    @Override
    public void sendData(byte[] data) {
        flexRadio.sendData(data);
    }


    @Override
    public void setPttOn(boolean on) {
        flexRadio.isPttOn=on;
        flexRadio.commandPTTOnOff(on);
    }

    @Override
    public void setPttOn(byte[] command) {
        //cableSerialPort.sendData(command);//以CAT指令发送PTT
    }

    @Override
    public void sendWaveData(float[] data) {
        //Log.e(TAG, "sendWaveData: flexConnector:"+data.length );
        flexRadio.sendWaveData(data);
    }

    @Override
    public void connect() {
        super.connect();
        flexRadio.openAudio();
        flexRadio.connect();
        flexRadio.openStreamPort();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        flexRadio.closeAudio();
        flexRadio.closeStreamPort();
        flexRadio.disConnect();
    }

    public OnWaveDataReceived getOnWaveDataReceived() {
        return onWaveDataReceived;
    }

    public void setOnWaveDataReceived(OnWaveDataReceived onWaveDataReceived) {
        this.onWaveDataReceived = onWaveDataReceived;
    }

    /**
     * 获取单声道的数据,24000hz采样率改为12000采样率，把立体声改为单声道
     * @param bytes 原始声音数据
     * @return 单声道数据
     */
    public static float[] getMonoFloatFromBytes(byte[] bytes) {
        float[] floats = new float[bytes.length / 16];
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        for (int i = 0; i < floats.length; i++) {
            try {
                float f1,f2;
                f1=dis.readFloat();
                dis.readFloat();//放弃一个声道
                f2=dis.readFloat();
                floats[i] = Math.max(f1,f2);//取最大值
                dis.readFloat();//放弃1个声道
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return floats;
    }

    @Override
    public boolean isConnected() {
        return flexRadio.isConnect();
    }
}
