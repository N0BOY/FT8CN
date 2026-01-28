package com.bg7yoz.ft8cn.wave;
/**
 * 使用Mic录音的操作。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

public class MicRecorder {
    private static final String TAG = "MicRecorder";
    private int bufferSize = 0;//最小缓冲区大小
    private static final int sampleRateInHz = 12000;//采样率
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO; //单声道
    //private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //量化位数
    private static final int audioFormat = AudioFormat.ENCODING_PCM_FLOAT; //量化位数

    private AudioRecord audioRecord = null;//AudioRecord对象
    private boolean isRunning = false;//是否处于录音的状态。
    private OnDataListener onDataListener;
    
    // dB level calculation variables
    private static final long UPDATE_INTERVAL_MS = 100; // Update dB level every 100ms
    private long lastUpdateTime = 0;

    public interface OnDataListener{
        void onDataReceived(float[] data,int len);
    }

    @SuppressLint("MissingPermission")
    public MicRecorder(){
        //计算最小缓冲区
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRateInHz
                , channelConfig, audioFormat, bufferSize);//创建AudioRecorder对象
    }

    public void start(){
        if (isRunning) return;

        float[] buffer = new float[bufferSize];
        lastUpdateTime = 0; // Reset update timer
        try {
            audioRecord.startRecording();//开始录音
        }catch (Exception e){
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                    R.string.recorder_cannot_record),e.getMessage()));
            Log.d(TAG, "startRecord: "+e.getMessage() );
        }

        isRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    //判断是否处于录音状态，state!=3，说明没有处于录音的状态
                    if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        isRunning = false;
                        Log.d(TAG, String.format("录音失败，状态码：%d", audioRecord.getRecordingState()));
                        break;
                    }

                    //读录音的数据
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize,AudioRecord.READ_BLOCKING);

                    // Apply mic input gain to the audio samples
                    float gain = GeneralVariables.micInputGain;
                    if (gain != 1.0f && bufferReadResult > 0) {
                        for (int i = 0; i < bufferReadResult; i++) {
                            buffer[i] *= gain;
                            // Clamp to prevent clipping (float PCM range is -1.0 to 1.0)
                            if (buffer[i] > 1.0f) {
                                buffer[i] = 1.0f;
                            } else if (buffer[i] < -1.0f) {
                                buffer[i] = -1.0f;
                            }
                        }
                    }

                    if (onDataListener!=null){
                        onDataListener.onDataReceived(buffer,bufferReadResult);
                    }
                    
                    // Calculate and update dB level periodically (after gain is applied)
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                        float dBLevel = calculateDbLevel(buffer, bufferReadResult);
                        GeneralVariables.mutableMicDbLevel.postValue(dBLevel);
                        lastUpdateTime = currentTime;
                    }
                }
                try {
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop();//停止录音
                    }
                }catch (Exception e){
                    ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                            R.string.recorder_stop_record_error),e.getMessage()));
                    Log.d(TAG, "startRecord: "+e.getMessage() );
                }
            }
        }).start();
    }

    /**
     * 停止录音。当录音停止后，监听列表中的监听器全部删除。
     */
    public void stopRecord() {
        isRunning = false;
        // Clear dB level when recording stops
        GeneralVariables.mutableMicDbLevel.postValue(Float.NEGATIVE_INFINITY);
    }

    public OnDataListener getOnDataListener() {
        return onDataListener;
    }

    public void setOnDataListener(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }
    
    /**
     * Calculate dB level (dBFS - decibels relative to full scale) from audio buffer
     * @param buffer Audio buffer
     * @param len Length of valid data in buffer
     * @return dB level (typically ranges from -infinity to 0 dBFS)
     */
    private float calculateDbLevel(float[] buffer, int len) {
        if (len <= 0) {
            return Float.NEGATIVE_INFINITY;
        }
        
        // Calculate RMS (Root Mean Square)
        double sumSquares = 0.0;
        for (int i = 0; i < len; i++) {
            double sample = buffer[i];
            sumSquares += sample * sample;
        }
        
        double rms = Math.sqrt(sumSquares / len);
        
        // Convert to dBFS (decibels relative to full scale)
        // For float PCM, full scale is 1.0, so we use that as reference
        if (rms <= 0.0) {
            return Float.NEGATIVE_INFINITY;
        }
        
        // dB = 20 * log10(rms / reference)
        // For dBFS, reference is 1.0 (full scale)
        double dB = 20.0 * Math.log10(rms);
        
        // Clamp to reasonable range (typically -60 to 0 dBFS for practical purposes)
        // But we'll allow lower values for very quiet signals
        return (float) Math.max(dB, -120.0);
    }
}
