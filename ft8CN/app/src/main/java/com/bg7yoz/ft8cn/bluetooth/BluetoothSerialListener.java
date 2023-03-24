package com.bg7yoz.ft8cn.bluetooth;

/**
 * 蓝牙串口的回调接口
 * BG7YOZ
 * 2023-03
 */
public interface BluetoothSerialListener {
    void onSerialConnect      ();
    void onSerialConnectError (Exception e);
    void onSerialRead         (byte[] data);
    void onSerialIoError      (Exception e);
}
