package com.bg7yoz.ft8cn.rigs;

public interface OnRigStateChanged {
    void onDisconnected();
    void onConnected();
    void onPttChanged(boolean isOn);
    void onFreqChanged(long freq);
    void onRunError(String message);
}
