package com.bg7yoz.ft8cn.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import com.bg7yoz.ft8cn.BuildConfig;

import java.util.UUID;

/**
 * 与蓝牙有关的常量
 * BLE 串口相关常量与检测方法，由 BG7RHK 提供修改，2026-09
 */

public class BluetoothConstants {

    // classic SPP UUID
    public static final UUID BLUETOOTH_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // BLE UART serial UUIDs
    public static final UUID BLE_UART_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    public static final UUID BLE_UART_TX_RX_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    public static final UUID BLE_NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID BLE_NUS_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID BLE_NUS_TX_RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;


    public static boolean checkBluetoothIsOpen() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        } else {
            return adapter.isEnabled();
        }
    }

    public static boolean checkIsSpp(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        @SuppressLint("MissingPermission") ParcelUuid[] parcelUuids = device.getUuids();
        if (parcelUuids != null) {
            for (ParcelUuid parcelUuid : parcelUuids) {
                if (parcelUuid != null && parcelUuid.getUuid() != null) {
                    String uuid = parcelUuid.getUuid().toString().toUpperCase();
                    if (uuid.equals(BLUETOOTH_SPP_UUID.toString().toUpperCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean checkIsBleSerial(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        int type = device.getType();
        if (type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
            return true;
        }

        @SuppressLint("MissingPermission") ParcelUuid[] parcelUuids = device.getUuids();
        if (parcelUuids != null) {
            for (ParcelUuid parcelUuid : parcelUuids) {
                if (parcelUuid != null && parcelUuid.getUuid() != null) {
                    String uuid = parcelUuid.getUuid().toString().toUpperCase();
                    if (uuid.equals(BLE_UART_SERVICE_UUID.toString().toUpperCase())
                            || uuid.equals(BLE_NUS_SERVICE_UUID.toString().toUpperCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSerialDevice(BluetoothDevice device) {
        return checkIsSpp(device) || checkIsBleSerial(device);
    }

    public static boolean checkIsHeadSet(BluetoothDevice device) {
        @SuppressLint("MissingPermission") ParcelUuid[] parcelUuids = device.getUuids();
        boolean audioSinkService = false;
        boolean handsFreeService = false;
        if (parcelUuids != null) {
            for (int i = 0; i < parcelUuids.length; i++) {//只保留UUID是串口的
                if (parcelUuids[i].getUuid().toString().toLowerCase().equals("0000111e-0000-1000-8000-00805f9b34fb")) {
                    handsFreeService = true;
                }
                if (parcelUuids[i].getUuid().toString().toLowerCase().equals("0000110b-0000-1000-8000-00805f9b34fb")) {
                    audioSinkService = true;
                }
            }
        }
        return audioSinkService && handsFreeService;
    }
}
