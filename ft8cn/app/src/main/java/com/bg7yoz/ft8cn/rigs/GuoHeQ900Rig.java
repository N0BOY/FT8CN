package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * YAESU的部分电台，回送的数据不是连续的，所以，要做一个缓冲区，接受5字节长度。满了就复位。或发送指令时，就复位。
 */
public class GuoHeQ900Rig extends BaseRig {
    private static final String TAG = "GuoHeQ900Rig";
    private Timer readFreqTimer = new Timer();
    private byte[] buffer;
    private int dataCount=-1;

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


    @Override
    public void setPTT(boolean on) {
        super.setPTT(on);
        Log.d(TAG, "setPTT: " + on);

        if (getConnector() != null) {
            getConnector().setPttOn(GuoHeRigConstant.setPTTState(on));
        }
    }

    public synchronized void setPttOn(byte[] command) {

        getConnector().sendData(command);//以CAT指令发送PTT
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
            getConnector().sendData(GuoHeRigConstant.setOperationUSBMode());//USB模式
            //getConnector().sendData(GuoHeRigConstant.setOperationFT8Mode());//FT8模式
        }
    }

    @Override
    public void setFreqToRig() {
        if (getConnector() != null) {
            getConnector().sendData(GuoHeRigConstant.setOperationFreq(getFreq()));
        }
    }


    private int checkHead(byte[] data) {
        int count = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == (byte) 0xa5) {
                count++;
                if (count == 4) {
                    return i+1;
                }
            }
        }
        return -1;
    }

    private void clearBuffer(){
        dataCount=-1;
        buffer=null;
    }

    @Override
    public void onReceiveData(byte[] data) {
        synchronized (this) {
            try {
                int startIndex = checkHead(data);
                if (startIndex != -1) {
                    int len = data[startIndex]+1;

                    buffer = new byte[len];
                    dataCount = 0;
                    for (int i = startIndex ; i < data.length; i++) {
                        buffer[dataCount] = data[i];
                        dataCount++;
                        if (dataCount == buffer.length) {
                            break;
                        }
                    }
                } else {
                    if (buffer == null) {
                        return;
                    }
                    if (dataCount < buffer.length && dataCount > 0) {
                        for (int i = 0; i < data.length; i++) {
                            buffer[dataCount] = data[i];
                            dataCount++;
                            if (dataCount == buffer.length) {
                                break;
                            }
                        }
                    }
                }

                if (buffer.length == dataCount) {//说明已经收取全部指令
                    byte[] crcData=new byte[buffer.length-2];
                    for (int i = 0; i < crcData.length; i++) {
                        crcData[i]=buffer[i];
                    }
                    //Log.e(TAG, "onReceiveData: crc data:"+byteToStr(crcData) );
                    //Log.e(TAG, "onReceiveData: crc --->"+String.format("%x",CRC16.crc16(crcData)) );
                    int crc=CRC16.crc16(crcData);
                    int ttt=((buffer[buffer.length-2]& 0xFF)<<8)|(buffer[buffer.length-1]&0xff);
                    //Log.e(TAG, "onReceiveData:数据内容：" + byteToStr(buffer));
                    if (crc==ttt) {//crc校验成功

                        if (buffer[1] == (byte) 0x0b) {//是电台状态指令

                            long vfoa = ((buffer[5] & 0xFFL) << 24) |
                                    ((buffer[6] & 0xFFL) << 16) |
                                    ((buffer[7] & 0xFFL) << 8) |
                                    ((buffer[8] & 0xFFL));
                            long vfob = ((buffer[9] & 0xFFL) << 24) |
                                    ((buffer[10] & 0xFFL) << 16) |
                                    ((buffer[11] & 0xFFL) << 8) |
                                    ((buffer[12] & 0xFFL));

                            if (buffer[13] == (byte) 0x00) {
                                setFreq(vfoa);
                                //Log.e(TAG, "onReceiveData: is VFO a");
                            } else {
                                setFreq(vfob);
                                //Log.e(TAG, "onReceiveData: is VFO b");
                            }
                            //Log.e(TAG, "onReceiveData: vfoa:" + vfoa);
                            //Log.e(TAG, "onReceiveData: vfob:" + vfob);

                        }
                    }
                    clearBuffer();
                }
            } catch (Exception e) {
                Log.e(TAG, "onReceiveData: " + e.getMessage());
            }
        }
    }

    @Override
    public void readFreqFromRig() {
        if (getConnector() != null) {
            getConnector().sendData(GuoHeRigConstant.setReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "GuoHe series";
    }

    public GuoHeQ900Rig() {
         readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY, QUERY_FREQ_TIMEOUT);
         //readFreqTimer.schedule(readTask(), START_QUERY_FREQ_DELAY, 4000);
    }

}
