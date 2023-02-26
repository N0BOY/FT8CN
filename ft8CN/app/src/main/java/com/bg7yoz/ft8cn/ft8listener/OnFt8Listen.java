package com.bg7yoz.ft8cn.ft8listener;

import com.bg7yoz.ft8cn.Ft8Message;

import java.util.ArrayList;

public interface OnFt8Listen {
    /**
     * 当开始监听时触发的事件
     * @param utc 当前的UTC时间
     */
    void beforeListen(long utc);

    /**
     *当解码结束后触发的事件
     * @param utc 当前周期的UTC时间
     * @param time_sec 此次平均的偏移时间（秒）
     * @param sequential 当前的时序
     * @param messages 消息列表
     */
    void afterDecode(long utc,float time_sec,int sequential, ArrayList<Ft8Message> messages);
}
