package com.bg7yoz.ft8cn.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import com.bg7yoz.ft8cn.BuildConfig;

/**
 * 与蓝牙有关的常量
 */

public class BluetoothConstants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;


    public static boolean checkBluetoothIsOpen(){
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if (adapter==null){
            return false;
        }else {
            return adapter.isEnabled();
        }
    }

    public static boolean checkIsSpp(BluetoothDevice device) {
        @SuppressLint("MissingPermission") ParcelUuid[] parcelUuids = device.getUuids();

        if (parcelUuids != null) {
            for (int i = 0; i < parcelUuids.length; i++) {//只保留UUID是串口的
                if (parcelUuids[i].getUuid().toString().toUpperCase().equals("00001101-0000-1000-8000-00805F9B34FB")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkIsHeadSet(BluetoothDevice device){
        @SuppressLint("MissingPermission") ParcelUuid[] parcelUuids = device.getUuids();
        boolean audioSinkService=false;
        boolean handsFreeService=false;
        if (parcelUuids != null) {
            for (int i = 0; i < parcelUuids.length; i++) {//只保留UUID是串口的
                if (parcelUuids[i].getUuid().toString().toLowerCase().equals("0000111e-0000-1000-8000-00805f9b34fb")) {
                    handsFreeService=true;
                }
                if (parcelUuids[i].getUuid().toString().toLowerCase().equals("0000110b-0000-1000-8000-00805f9b34fb")) {
                    audioSinkService=true;
                }
            }
        }
        return audioSinkService&&handsFreeService;
    }
}
