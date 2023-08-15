package com.bg7yoz.ft8cn.spectrum;
/**
 * 用于瀑布图的音频接收。以一个FT8符号为颗粒度。
 * @author BGY70Z
 * @date 2023-03-20
 */

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.wave.HamRecorder;
import com.bg7yoz.ft8cn.wave.OnGetVoiceDataDone;

public class SpectrumListener {
    private static final String TAG = "SpectrumListener";
    private HamRecorder hamRecorder;

    private float[] dataBuffer=new float[0];
    public MutableLiveData<float[]> mutableDataBuffer = new MutableLiveData<>();


    private final OnGetVoiceDataDone onGetVoiceDataDone=new OnGetVoiceDataDone() {
        @Override
        public void onGetDone(float[] data) {
                    mutableDataBuffer.postValue(data);
        }
    };

    public SpectrumListener(HamRecorder hamRecorder) {
        this.hamRecorder = hamRecorder;
        doReceiveData();
    }


    private void doReceiveData(){
        hamRecorder.getVoiceData(160,false,onGetVoiceDataDone);
    }

    public float[] getDataBuffer() {
        return dataBuffer;
    }
}
