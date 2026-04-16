package com.bg7yoz.ft8cn.wave;
/**
 * 用于 Mic 录音的类。
 *
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
    private static final int sampleRateInHz = 12000;
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;

    private int bufferSize = 0;
    private AudioRecord audioRecord = null;
    private boolean isRunning = false;
    private OnDataListener onDataListener;

    public interface OnDataListener{
        void onDataReceived(float[] data,int len);
    }

    @SuppressLint("MissingPermission")
    public MicRecorder(){
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (bufferSize <= 0) {
            bufferSize = sampleRateInHz;
        }
        ensureAudioRecord();
    }

    @SuppressLint("MissingPermission")
    private synchronized boolean ensureAudioRecord() {
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            return true;
        }
        releaseAudioRecord();
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRateInHz
                    , channelConfig, audioFormat, bufferSize);
        } catch (Exception e) {
            Log.e(TAG, "ensureAudioRecord: " + e.getMessage());
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
        for (int i = 0; i < 2; i++) {
            if (!ensureAudioRecord()) {
                continue;
            }
            try {
                audioRecord.startRecording();
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    return true;
                }
                Log.d(TAG, "startRecordingInternal: invalid recording state="
                        + audioRecord.getRecordingState());
            } catch (Exception e) {
                Log.d(TAG, "startRecordingInternal: " + e.getMessage());
            }
            releaseAudioRecord();
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                R.string.recorder_cannot_record), "AudioRecord is not initialized"));
        return false;
    }

    public synchronized void start(){
        if (isRunning) return;
        if (!startRecordingInternal()) {
            isRunning = false;
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
                            || currentRecord.getState() != AudioRecord.STATE_INITIALIZED
                            || currentRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        isRunning = false;
                        Log.d(TAG, "record loop: AudioRecord is unavailable.");
                        break;
                    }

                    int bufferReadResult = currentRecord.read(buffer, 0, bufferSize, AudioRecord.READ_BLOCKING);
                    if (bufferReadResult > 0) {
                        if (onDataListener!=null){
                            onDataListener.onDataReceived(buffer,bufferReadResult);
                        }
                        continue;
                    }

                    Log.d(TAG, "record loop read error: " + bufferReadResult);
                    if (bufferReadResult == AudioRecord.ERROR_DEAD_OBJECT
                            || bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION
                            || bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                        isRunning = false;
                        break;
                    }
                }

                AudioRecord currentRecord = audioRecord;
                if (currentRecord != null) {
                    try {
                        if (currentRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                            currentRecord.stop();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "stop after loop: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 停止录音。这里只停止录音循环，实际资源会在录音线程退出后释放。
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
