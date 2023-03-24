package com.bg7yoz.ft8cn.wave;

/**
 *定义音频录音结束后的回调接口。
 *回调接口主要有2个，录音开始前，录音开始后。
 * 注意！！！录音采用多线程的方式，此处的回调不在主线程中，如果回调中有UI操作的话，要使用runOnUiThread方法，防止界面锁死。
 *
 * @author BG7YOZ
 * @date 2022.5.7
 */

public interface OnAudioRecorded {
    /**
     * 录音开始前的回调函数。
     * @param audioFileName 生成的Wav文件名
     */
    void beginAudioRecord(String audioFileName);

    /**
     * 录音结束后的回调函数。
     * @param audioFileName Wav文件名
     * @param dataSize 录音数据的大小，byte[]格式，不包括wav文件头的长度，如果要算wav文件长度，在此基础上+44。
     * @param duration 实际录音的时长（秒）
     */
    void endAudioRecorded(String audioFileName,long dataSize,float duration);
}
