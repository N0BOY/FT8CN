package com.bg7yoz.ft8cn.bluetooth;

public interface BluetoothSerialListener {
    void onSerialConnect      ();
    void onSerialConnectError (Exception e);
    void onSerialRead         (byte[] data);
    void onSerialIoError      (Exception e);
}
