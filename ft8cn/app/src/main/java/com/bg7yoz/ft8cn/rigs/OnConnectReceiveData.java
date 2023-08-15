package com.bg7yoz.ft8cn.rigs;

/**
 * 从电台接收到数据的回调
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnConnectReceiveData {
    void onData(byte[] data);
}
