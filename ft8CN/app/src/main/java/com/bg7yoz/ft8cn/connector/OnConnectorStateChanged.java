package com.bg7yoz.ft8cn.connector;

public interface OnConnectorStateChanged {
    void onDisconnected();
    void onConnected();
    void onRunError(String message);
}
