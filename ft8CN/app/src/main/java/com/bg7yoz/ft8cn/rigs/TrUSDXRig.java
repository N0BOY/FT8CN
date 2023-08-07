package com.bg7yoz.ft8cn.rigs;

import static com.bg7yoz.ft8cn.GeneralVariables.QUERY_FREQ_TIMEOUT;
import static com.bg7yoz.ft8cn.GeneralVariables.START_QUERY_FREQ_DELAY;

import android.os.Handler;
import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import com.jackz314.resample.Resample;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * (tr)uSDX, fork from KENWOOD TS590.
 */
public class TrUSDXRig extends BaseRig {
    private static final String TAG = "TrUSDXRig";
    private static final int rxSampling = 7812;
    private static final int txSampling = 11520;
    private final StringBuilder buffer = new StringBuilder();
    private final ByteArrayOutputStream rxStreamBuffer = new ByteArrayOutputStream();
    //private final Resample rxResample = new Resample(Resample.ConverterType.SRC_LINEAR, 1, rxSampling, 12000);
    //private final Resample txResample = new Resample(Resample.ConverterType.SRC_SINC_FASTEST, 1, 48000, txSampling);

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
                        clearBufferData();
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
                case ControlMode.CAT:
                    if (on) {
                        rxStreaming = false;
                    }
                    getConnector().setPttOn(KenwoodTK90RigConstant.setTrUSDXPTTState(on));
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
                onReceivedWaveData(cutted, true);
                rxStreaming = false;
            } else {
                buffer.append(new String(cutted));
                //开始分析数据
                Yaesu3Command yaesu3Command = Yaesu3Command.getCommand(buffer.toString());
                clearBufferData();//清一下缓存

                if (yaesu3Command == null) {
                    continue;
                }
                String cmd=yaesu3Command.getCommandID();
                Log.i(TAG, "command: " + cmd);
                if (cmd.equalsIgnoreCase("FA")) {//频率
                    long tempFreq=Yaesu3Command.getFrequency(yaesu3Command);
                    if (tempFreq!=0) {//如果tempFreq==0，说明频率不正常
                        setFreq(Yaesu3Command.getFrequency(yaesu3Command));
                    }
                }else if (cmd.equalsIgnoreCase("US")){
                    rxStreaming = true;
                    byte[] wave = Arrays.copyOfRange(cutted, 2, cutted.length);
                    onReceivedWaveData(wave);
                }
            }
        }
        if (remain.length <= 0) {
            return;
        }
        if (rxStreaming) {
            onReceivedWaveData(remain);
        } else if (remain.length >= 2 && remain[0] == 0x55 && remain[1] == 0x53) {// US
            clearBufferData();
            rxStreaming = true;
            byte[] wave = Arrays.copyOfRange(remain, 2, remain.length);
            onReceivedWaveData(wave);
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
            // force reset
            getConnector().sendData(KenwoodTK90RigConstant.setTrUSDXPTTState(false));
            getConnector().sendData(KenwoodTK90RigConstant.setTS590ReadOperationFreq());
        }
    }

    @Override
    public String getName() {
        return "(tr)uSDX";
    }

    @Override
    public boolean supportWaveOverCAT() {
        return true;
    }

    @Override
    public void onDisconnecting() {
        if (getConnector() != null) {
            clearBufferData();
            getConnector().sendData(KenwoodTK90RigConstant.setTrUSDXStreaming(false));
        }
    }

    public void onReceivedWaveData(byte[] data) {
        onReceivedWaveData(data, false);
    }

    public void onReceivedWaveData(byte[] data, boolean force) {
        if (data.length == 0) {
            return;
        }
        if (getConnector() == null) {
            return;
        }
        Resample rxResample = new Resample(Resample.ConverterType.SRC_LINEAR, 1, rxSampling, 12000);
        rxStreamBuffer.write(data, 0, data.length);
        if (rxStreamBuffer.size() >= 256 || force) {
            byte[] resampled = rxResample.processCopy(toWaveSamples8To16(rxStreamBuffer.toByteArray()));
            rxStreamBuffer.reset();
            getConnector().receiveWaveData(resampled);
        }
        rxResample.close();
    }

    @Override
    public void sendWaveData(Ft8Message message) {
        if (getConnector() == null) {
            return;
        }
        float[] wave = GenerateFT8.generateFt8(message, GeneralVariables.getBaseFrequency()
             //   ,txSampling);
                , 24000);
             //   ,48000);
        //Log.i(TAG, String.format("wave length: %d", wave.length));
        if (wave == null){
            setPTT(false);
            return;
        }
        byte[] pcm16 = toWaveFloatToPCM16(wave);
        Resample txResample = new Resample(Resample.ConverterType.SRC_SINC_FASTEST, 1, 24000, txSampling);
        byte[] resampled = txResample.processCopy(pcm16);
        txResample.close();
        byte[] pcm8 = toWaveSamples16To8(resampled);
        //byte[] pcm8 = resampled;
        //byte[] pcm8 = toWaveFloatToPCM8(wave);
        Log.i(TAG, String.format("pcm8 length: %d", pcm8.length));
        for (int i = 0; i < pcm8.length; i++) {
            if (pcm8[i] == 0x3B) pcm8[i] = 0x3A; // ; to :
        }
        while (pcm8.length > 0) {
            if (pcm8.length <= 256) {
                getConnector().sendData(pcm8);
                break;
            } else {
                getConnector().sendData(Arrays.copyOfRange(pcm8, 0, 256));
                pcm8 = Arrays.copyOfRange(pcm8, 256, pcm8.length);
            }
        }
        /*
        for (int i = 0; i < pcm8.length; i += 64) {
            if (!isPttOn()) {
                return;
            }
            getConnector().sendData(Arrays.copyOfRange(pcm8, i, Math.min(i + 64, pcm8.length)));
            //try {
            //    Thread.sleep(1);
            //} catch (InterruptedException e) {
            //    e.printStackTrace();
            //}
        }
        */
    }

    private static byte[] toWaveSamples8To16(byte[] in) {
        ByteBuffer buf = ByteBuffer.allocate(in.length * 2);
        for (int i = 0; i < in.length; i++) {
            short v = (short)(((short)in[i] - 128) << 8);
            buf.putShort(v);
        }
        return buf.array();
    }

    private static byte[] toWaveFloatToPCM16(float[] in) {
        ByteBuffer buf = ByteBuffer.allocate(in.length * 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < in.length; i++) {
            float x = in[i];
            short v = (short)(x * 32767.0f);
            buf.putShort(v);
        }
        return buf.array();
    }

    private static byte[] toWaveFloatToPCM8(float[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            float x = in[i];
            short v = (short)(x * 32767.0f);
            out[i] = (byte)((byte)(v >> 8) + 128);
        }
        return out;
    }

    private static byte[] toWaveSamples16To8(byte[] in) {
        byte[] out = new byte[in.length / 2];
        for (int i = 0; i < out.length; i++) {
            short v = readShortBigEndianData(in, i * 2);
            //short v = (short)(((short)in[i*2] & 0xFF) << 8 | (short)in[i*2+1] & 0xFF);
            out[i] = (byte)(((byte)(v >> 8)) + 128);
        }
        return out;
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

    /**
     * 从流数据中读取小端模式的Short
     *
     * @param data  流数据
     * @param start 起始点
     * @return Int16
     */
    public static short readShortBigEndianData(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start] & 0xff
                | ((short) data[start + 1] & 0xff) << 8);
    }

}
