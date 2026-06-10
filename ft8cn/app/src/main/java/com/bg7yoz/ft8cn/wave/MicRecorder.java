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

    public interface OnDataListener{
        void onDataReceived(float[] data,int len);
    }

    @SuppressLint("MissingPermission")
    public MicRecorder(){
        //计算最小缓冲区
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (bufferSize <= 0) {
            bufferSize = sampleRateInHz;
        }
    }

    @SuppressLint("MissingPermission")
    private synchronized boolean ensureAudioRecord() {
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            return true;
        }
        releaseAudioRecord();
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRateInHz,
                    channelConfig, audioFormat, bufferSize);
        } catch (Exception e) {
            Log.d(TAG, "ensureAudioRecord: " + e.getMessage());
            audioRecord = null;
        }
        return audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED;
    }

    private synchronized void releaseAudioRecord() {
        if (audioRecord == null) {
            return;
        }
        try {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
        } catch (Exception e) {
            Log.d(TAG, "releaseAudioRecord stop: " + e.getMessage());
        }
        try {
            audioRecord.release();
        } catch (Exception e) {
            Log.d(TAG, "releaseAudioRecord release: " + e.getMessage());
        }
        audioRecord = null;
    }

    @SuppressLint("MissingPermission")
    private boolean startRecordingInternal() {
        if (!ensureAudioRecord()) {
            return false;
        }

        try {
            audioRecord.startRecording();//开始录音
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "startRecord: " + e.getMessage());
        }

        releaseAudioRecord();
        if (!ensureAudioRecord()) {
            return false;
        }

        try {
            audioRecord.startRecording();
            return audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
        } catch (Exception e) {
            Log.d(TAG, "startRecord retry: " + e.getMessage());
            return false;
        }
    }

    public synchronized void start(){
        if (isRunning) return;

        if (!startRecordingInternal()) {
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                    R.string.recorder_cannot_record), "AudioRecord is not initialized"));
            Log.d(TAG, "startRecord: AudioRecord is not initialized");
            return;
        }

        isRunning = true;
        float[] buffer = new float[bufferSize];

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    AudioRecord currentRecord = audioRecord;
                    if (currentRecord == null
                            || currentRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        isRunning = false;
                        Log.d(TAG, String.format("录音失败，状态码：%d",
                                currentRecord == null ? -1 : currentRecord.getRecordingState()));
                        break;
                    }

                    //读录音的数据
                    int bufferReadResult = currentRecord.read(buffer, 0, bufferSize, AudioRecord.READ_BLOCKING);
                    if (bufferReadResult <= 0) {
                        Log.d(TAG, "record loop read error: " + bufferReadResult);
                        if (bufferReadResult == AudioRecord.ERROR_DEAD_OBJECT
                                || bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION
                                || bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                            isRunning = false;
                            break;
                        }
                        continue;
                    }

                    if (onDataListener!=null){
                        onDataListener.onDataReceived(buffer,bufferReadResult);
                    }
                }
                try {
                    AudioRecord currentRecord = audioRecord;
                    if (currentRecord != null
                            && currentRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        currentRecord.stop();//停止录音
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
    public synchronized void stopRecord() {
        isRunning = false;
        releaseAudioRecord();
    }

    public OnDataListener getOnDataListener() {
        return onDataListener;
    }

    public void setOnDataListener(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }
}
