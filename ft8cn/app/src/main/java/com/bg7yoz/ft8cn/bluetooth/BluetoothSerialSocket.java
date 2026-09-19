package com.bg7yoz.ft8cn.bluetooth;
/**
 * 蓝牙串口的SOCKET
 * BG7YOZ
 * 2023-03
 * BLE 串口（GATT UART/NUS）支持及写入队列，由 BG7RHK 提供修改，2026-09
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;

public class BluetoothSerialSocket implements Runnable {

    private final BroadcastReceiver disconnectBroadcastReceiver;

    private final Context context;
    private BluetoothSerialListener listener;
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic txCharacteristic;
    private boolean connected;
    private boolean isBleSerial;
    private final Queue<byte[]> bleWriteQueue = new LinkedList<>();
    private boolean bleWriteInProgress;

    public BluetoothSerialSocket(Context context, BluetoothDevice device) {
        if(context instanceof Activity)
            throw new InvalidParameterException("expected non UI context");
        this.context = context;
        this.device = device;
        this.isBleSerial = BluetoothConstants.checkIsBleSerial(device);
        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(listener != null)
                    listener.onSerialIoError(new IOException("background disconnect"));
                disconnect();
            }
        };
    }

    @SuppressLint("MissingPermission")
    String getName() {
        return device.getName() != null ? device.getName() : device.getAddress();
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    void connect(BluetoothSerialListener listener) throws IOException {
        this.listener = listener;
        context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(BluetoothConstants.INTENT_ACTION_DISCONNECT));
        if (isBleSerial) {
            connectBle();
            return;
        }
        Executors.newCachedThreadPool().submit(this);
    }

    private void connectBle() {
        if (device == null) {
            if (listener != null) {
                listener.onSerialConnectError(new IOException("Bluetooth device is null"));
            }
            return;
        }

        connected = false;
        gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connected = false;
                    if (listener != null) {
                        listener.onSerialConnectError(new IOException("BLE connect status=" + status));
                    }
                    return;
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connected = false;
                    if (listener != null) {
                        listener.onSerialIoError(new IOException("BLE disconnected"));
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connected = false;
                    if (listener != null) {
                        listener.onSerialConnectError(new IOException("BLE services discovery failed"));
                    }
                    return;
                }

                BluetoothGattService service = null;
                for (BluetoothGattService s : gatt.getServices()) {
                    UUID serviceUuid = s.getUuid();
                    if (serviceUuid.equals(BluetoothConstants.BLE_UART_SERVICE_UUID)
                            || serviceUuid.equals(BluetoothConstants.BLE_NUS_SERVICE_UUID)) {
                        service = s;
                        break;
                    }
                }
                if (service == null) {
                    if (listener != null) {
                        listener.onSerialConnectError(new IOException("BLE serial service not found"));
                    }
                    return;
                }

                BluetoothGattCharacteristic rxCharacteristic = null;
                txCharacteristic = null;
                for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                    UUID uuid = ch.getUuid();
                    if (uuid.equals(BluetoothConstants.BLE_UART_TX_RX_UUID)) {
                        if ((ch.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            rxCharacteristic = ch;
                        }
                        if ((ch.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
                                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                            txCharacteristic = ch;
                        }
                    } else if (uuid.equals(BluetoothConstants.BLE_NUS_TX_RX_UUID)) {
                        if ((ch.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            rxCharacteristic = ch;
                        }
                    } else if (uuid.equals(BluetoothConstants.BLE_NUS_RX_UUID)
                            && (ch.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
                            | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                        txCharacteristic = ch;
                    }
                }
                if (rxCharacteristic == null || txCharacteristic == null) {
                    connected = false;
                    if (listener != null) {
                        listener.onSerialConnectError(new IOException("BLE serial characteristic not found"));
                    }
                    return;
                }

                BluetoothGattDescriptor descriptor = rxCharacteristic.getDescriptor(BluetoothConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (descriptor != null) {
                    gatt.setCharacteristicNotification(rxCharacteristic, true);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean ok = gatt.writeDescriptor(descriptor);
                    if (!ok) {
                        connected = false;
                        if (listener != null) {
                            listener.onSerialConnectError(new IOException("BLE descriptor write failed"));
                        }
                    }
                } else {
                    connected = true;
                    if (listener != null) {
                        listener.onSerialConnect();
                    }
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                connected = true;
                if (listener != null) {
                    listener.onSerialConnect();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] data = characteristic.getValue();
                if (listener != null && data != null && data.length > 0) {
                    listener.onSerialRead(data);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    bleWriteInProgress = false;
                    if (listener != null) {
                        listener.onSerialIoError(new IOException("BLE write failed, status=" + status));
                    }
                    return;
                }
                bleWriteInProgress = false;
                writeNextBlePacket(gatt);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void writeNextBlePacket(BluetoothGatt bluetoothGatt) {
        if (!connected || txCharacteristic == null || bleWriteInProgress) {
            return;
        }
        byte[] nextPacket;
        synchronized (bleWriteQueue) {
            nextPacket = bleWriteQueue.poll();
            if (nextPacket == null) {
                return;
            }
            bleWriteInProgress = true;
        }
        txCharacteristic.setValue(nextPacket);
        int properties = txCharacteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                && (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
            txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
        if (!bluetoothGatt.writeCharacteristic(txCharacteristic)) {
            bleWriteInProgress = false;
            if (listener != null) {
                listener.onSerialIoError(new IOException("BLE write failed to start"));
            }
            return;
        }
        if (txCharacteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            bleWriteInProgress = false;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    () -> writeNextBlePacket(bluetoothGatt), 25);
        }
    }

    void disconnect() {
        connected = false;
        listener = null;
        synchronized (bleWriteQueue) {
            bleWriteQueue.clear();
            bleWriteInProgress = false;
        }
        if (gatt != null) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (Exception ignored) {
            }
            gatt = null;
        }
        if(socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        try {
            context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    void write(byte[] data) throws IOException {
        if (!connected) {
            throw new IOException("not connected");
        }
        if (isBleSerial) {
            if (gatt == null || txCharacteristic == null) {
                throw new IOException("BLE serial channel not ready");
            }
            synchronized (bleWriteQueue) {
                bleWriteQueue.offer(Arrays.copyOf(data, data.length));
            }
            writeNextBlePacket(gatt);
            return;
        }
        socket.getOutputStream().write(data);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() { // connect & read
        try {
            socket = device.createRfcommSocketToServiceRecord(BluetoothConstants.BLUETOOTH_SPP_UUID);
            socket.connect();
            if(listener != null)
                listener.onSerialConnect();
        } catch (Exception e) {
            if(listener != null)
                listener.onSerialConnectError(e);
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            return;
        }
        connected = true;
        try {
            byte[] buffer = new byte[1024];
            int len;
            while (true) {
                len = socket.getInputStream().read(buffer);
                byte[] data = Arrays.copyOf(buffer, len);
                if(listener != null)
                    listener.onSerialRead(data);
            }
        } catch (Exception e) {
            connected = false;
            if (listener != null)
                listener.onSerialIoError(e);
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

}
