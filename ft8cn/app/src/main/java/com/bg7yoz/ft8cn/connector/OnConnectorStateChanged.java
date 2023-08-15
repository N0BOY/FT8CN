package com.bg7yoz.ft8cn.connector;

/**
 * 连接器的回调
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnConnectorStateChanged {
    void onDisconnected();
    void onConnected();
    void onRunError(String message);
}
