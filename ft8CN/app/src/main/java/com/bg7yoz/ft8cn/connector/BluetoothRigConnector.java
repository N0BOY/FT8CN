package com.bg7yoz.ft8cn.connector;
/**
 * 用于蓝牙连接的Connector,继承于BaseRigConnector
 *
 * @author BG7YOZ
 * @date 2023-03-20
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.bluetooth.BluetoothSerialListener;
import com.bg7yoz.ft8cn.bluetooth.BluetoothSerialService;
import com.bg7yoz.ft8cn.bluetooth.BluetoothSerialSocket;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.IOException;

public class BluetoothRigConnector extends BaseRigConnector implements ServiceConnection, BluetoothSerialListener {
    private enum Connected {False, Pending, True}
    private static BluetoothRigConnector connector=null;

    public static BluetoothRigConnector getInstance(Context context, String address, int controlMode){
        if (connector!=null){
            if (!connector.getDeviceAddress().equals(address)) {
                if (connector.connected== Connected.True) {
                    connector.socketDisconnect();
                }
                connector.setDeviceAddress(address);
                connector.socketConnect();
            }

            return connector;
        }else {
            return new BluetoothRigConnector(context,address,controlMode);
        }
    }

    private static final String TAG = "BluetoothRigConnector";
    //private static ServiceConnection connection;
    private boolean initialStart = true;
    private BluetoothSerialService service = null;
    private Connected connected = Connected.False;
    private String deviceAddress;
    private Context context;


    public BluetoothRigConnector(Context context, String address, int controlMode) {
        super(controlMode);
        connector=this;
        deviceAddress = address;
        this.context = context;

        context.stopService(new Intent(context, BluetoothSerialService.class));
        context.bindService(new Intent(context, BluetoothSerialService.class), this, Context.BIND_AUTO_CREATE);

    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        service = ((BluetoothSerialService.SerialBinder) iBinder).getService();
        service.attach(this);
        if (initialStart) {
            initialStart = false;
            //getActivity().runOnUiThread(this::connect);
            socketConnect();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        socketDisconnect();
        service = null;
    }

    @Override
    public void onSerialConnect() {
        Log.d(TAG, "onSerialConnect: connected");
        connected = Connected.True;
        getOnConnectorStateChanged().onConnected();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        Log.e(TAG, "onSerialConnectError: " + e.getMessage());
        getOnConnectorStateChanged().onRunError(e.getMessage());
        socketDisconnect();
    }


    @Override
    public void onSerialRead(byte[] data) {
        if (data.length > 0) {
            //Log.d(TAG, "onSerialRead: " + BaseRig.byteToStr(data));
            if (getOnConnectReceiveData()!=null){
                getOnConnectReceiveData().onData(data);
            }
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        Log.e(TAG, "onSerialIoError: " + e.getMessage());
        getOnConnectorStateChanged().onRunError(e.getMessage());
        socketDisconnect();
    }

    public void socketDisconnect() {
        connected = Connected.False;
        getOnConnectorStateChanged().onDisconnected();
        service.disconnect();
    }

    /*
     * Serial + UI
     */
    public void socketConnect() {
        try {
            ToastMessage.show(String.format(
                    GeneralVariables.getStringFromResource(R.string.connect_bluetooth_spp)
                    ,deviceAddress));
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            Log.d(TAG, "connecting...");
            connected = Connected.Pending;
            BluetoothSerialSocket socket = new BluetoothSerialSocket(context, device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    public void sendCommand(byte[] data) {
        //Log.d(TAG, "sendCommand: "+BaseRig.byteToStr(data) );
        if (connected != Connected.True) {
            Log.e(TAG, "sendCommand: 蓝牙没连接");
            socketConnect();
            return;
        }

        try {
            service.write(data);
        } catch (IOException e) {
            getOnConnectorStateChanged().onRunError(e.getMessage());
        }
    }

    @Override
    public synchronized void sendData(byte[] data) {
         sendCommand(data);
    }

    @Override
    public void setPttOn(byte[] command) {
        sendData(command);//以CAT指令发送PTT
    }

    @Override
    public void connect() {
        super.connect();
        socketConnect();
    }
    @Override
    public void disconnect() {
        super.disconnect();
        socketDisconnect();
    }
}
