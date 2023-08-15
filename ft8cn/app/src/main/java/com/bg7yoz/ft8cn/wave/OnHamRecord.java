package com.bg7yoz.ft8cn.wave;

/**
 * 接收到音频的回调。
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnHamRecord {
    void OnReceiveData(float[] data,int size);
}
