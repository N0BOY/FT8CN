package com.bg7yoz.ft8cn.connector;
/**
 * 用于USB串口操作的类。USB串口驱动在serialport目录中，主要是CDC、CH34x、CP21xx、FTDI等。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.bg7yoz.ft8cn.BuildConfig;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.serialport.CdcAcmSerialDriver;
import com.bg7yoz.ft8cn.serialport.UsbSerialDriver;
import com.bg7yoz.ft8cn.serialport.UsbSerialPort;
import com.bg7yoz.ft8cn.serialport.UsbSerialProber;
import com.bg7yoz.ft8cn.serialport.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;


public class CableSerialPort {
    private static final String TAG = "CableSerialPort";
    private OnConnectorStateChanged onStateChanged;
    public static final int SEND_TIMEOUT = 2000;

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID;

    public enum UsbPermission {Unknown, Requested, Granted, Denied}

    private UsbPermission usbPermission = UsbPermission.Unknown;

    private BroadcastReceiver broadcastReceiver;
    private final Context context;

    private int vendorId = 0x0c26;//设备号
    private int portNum = 0;//端口号
    private int baudRate = 19200;//波特率

    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;
    public SerialInputOutputManager.Listener ioListener = null;

    private UsbManager usbManager;
    private UsbDeviceConnection usbConnection;
    private UsbSerialDriver driver;


    private boolean connected = false;//是否处于连接状态

    public CableSerialPort(Context mContext, SerialPort serialPort, int baud, OnConnectorStateChanged connectorStateChanged) {
        vendorId = serialPort.vendorId;
        portNum = serialPort.portNum;
        baudRate = baud;
        context = mContext;
        this.onStateChanged=connectorStateChanged;
        doBroadcast();
    }

    public CableSerialPort(Context mContext) {
        context = mContext;
        doBroadcast();
    }

    private void doBroadcast() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
    }

    private boolean prepare() {
        registerRigSerialPort(context);
        UsbDevice device = null;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        //此处把connection设成Null,这样在后面来通过是否null判断是否有权限。
        usbConnection = null;
        //此处是不是做个权限判断？
        if (usbManager == null) {
            return false;
        }


        for (UsbDevice v : usbManager.getDeviceList().values()) {
            if (v.getVendorId() == vendorId) {
                device = v;
            }
        }
        if (device == null) {
            Log.e(TAG, String.format("串口设备打开失败: 没有找到设备0x%04x", vendorId));
            return false;
        }
        driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            //试着把未知的设备加入到cdc驱动上
            driver = new CdcAcmSerialDriver(device);
        }
        if (driver.getPorts().size() < portNum) {
            Log.e(TAG, "串口号不存在，无法打开。");
            return false;
        }
        Log.d(TAG, "connect: port size:" + String.valueOf(driver.getPorts().size()));
        usbSerialPort = driver.getPorts().get(portNum);
        usbConnection = usbManager.openDevice(driver.getDevice());

        return true;

    }

    //@RequiresApi(api = Build.VERSION_CODES.S)
    public boolean connect() {
        connected = false;
        if (!prepare()) {
            //return false;
        }
        if (driver == null) {
            if (onStateChanged!=null){
                onStateChanged.onRunError(GeneralVariables.getStringFromResource(R.string.serial_no_driver));
            }
            return false;
        }
        if (usbConnection == null && usbPermission == UsbPermission.Unknown
                && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;

            PendingIntent usbPermissionIntent;

            //在android12 开始，增加了PendingIntent.FLAG_MUTABLE保护机制，所以要做版本判断
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                usbPermissionIntent = PendingIntent.getBroadcast(context, 0
                        , new Intent(INTENT_ACTION_GRANT_USB), PendingIntent.FLAG_MUTABLE);
            } else {
                usbPermissionIntent = PendingIntent.getBroadcast(context, 0
                        , new Intent(INTENT_ACTION_GRANT_USB), 0);
            }


            //PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0
            //        , new Intent(INTENT_ACTION_GRANT_USB), PendingIntent.FLAG_MUTABLE);
            // , new Intent(INTENT_ACTION_GRANT_USB), 0);


            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            prepare();
        }
        if (usbConnection == null) {
            if (onStateChanged!=null){
                onStateChanged.onRunError(GeneralVariables.getStringFromResource(R.string.serial_connect_no_access));
            }

            return false;
        }
        try {
            usbSerialPort.open(usbConnection);
            //波特率、停止位
            //usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            Log.d(TAG,String.format("serial:baud rate：%d,data bits:%d,stop bits:%d,parity bit:%d"
                    ,baudRate,GeneralVariables.serialDataBits
                    ,GeneralVariables.serialStopBits
                    ,GeneralVariables.serialParity));
            usbSerialPort.setParameters(baudRate, GeneralVariables.serialDataBits
                    , GeneralVariables.serialStopBits, GeneralVariables.serialParity);
            usbIoManager = new SerialInputOutputManager(usbSerialPort, new SerialInputOutputManager.Listener() {
                @Override
                public void onNewData(byte[] data) {
                    if (ioListener != null) {
                        ioListener.onNewData(data);
                    }
                }

                @Override
                public void onRunError(Exception e) {
                    if (ioListener != null) {
                        ioListener.onRunError(e);
                    }
                    disconnect();
                }
            });
            usbIoManager.start();
            Log.d(TAG, "串口打开成功！");
            connected = true;

            if (onStateChanged!=null){
                onStateChanged.onConnected();
            }


        } catch (Exception e) {
            Log.e(TAG, "串口打开失败: " + e.getMessage());
            if (onStateChanged!=null){
                onStateChanged.onRunError(GeneralVariables.getStringFromResource(R.string.serial_connect_failed)
                        + e.getMessage());
            }
            disconnect();
            return false;
        }
        return true;
    }

    public boolean sendData(final byte[] src) {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.write(src, SEND_TIMEOUT);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "发送数据出错：" + e.getMessage());
                return false;
            }
            return true;
        } else {
            Log.e(TAG, "无法发送数据，串口没有打开。");
            return false;
        }

    }

    public void disconnect() {
        connected = false;
        if (onStateChanged!=null){
            onStateChanged.onDisconnected();
        }
        if (usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            if (usbSerialPort != null) {
                usbSerialPort.close();
            }
        } catch (IOException ignored) {
        }
        usbSerialPort = null;
    }

    public void registerRigSerialPort(Context context) {
        Log.d(TAG, "registerRigSerialPort: registered!");
        context.registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
    }

    public void unregisterRigSerialPort(Activity activity) {
        Log.d(TAG, "unregisterRigSerialPort: unregistered!");
        activity.unregisterReceiver(broadcastReceiver);
    }


    /**
     * 打开和关闭RTS
     *
     * @param rts_on true：打开，false：关闭
     */
    public void setRTS_On(boolean rts_on) {
        try {
            EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
            if (controlLines.contains(UsbSerialPort.ControlLine.RTS)) {
                usbSerialPort.setRTS(rts_on);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDTR_On(boolean dtr_on) {
        try {
            EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
            if (controlLines.contains(UsbSerialPort.ControlLine.DTR)) {
                usbSerialPort.setDTR(dtr_on);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "setDTR_On: " + e.getMessage());
        }
    }

    public OnConnectorStateChanged getOnStateChanged() {
        return onStateChanged;
    }

    public void setOnStateChanged(OnConnectorStateChanged onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int deviceId) {
        this.vendorId = deviceId;
    }

    public int getPortNum() {
        return portNum;
    }

    public void setPortNum(int portNum) {
        this.portNum = portNum;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    /**
     * 获取本机可用的串口设备串列表
     *
     * @param context context
     * @return 串口设备列表
     */
    public static ArrayList<SerialPort> listSerialPorts(Context context) {
        ArrayList<SerialPort> serialPorts = new ArrayList<>();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
            if (driver == null) {
                continue;
                //试着把未知的设备加入到cdc驱动上
                //driver = new CdcAcmSerialDriver(device);
            }
            for (int i = 0; i < driver.getPorts().size(); i++) {
                serialPorts.add(new SerialPort(device.getDeviceId(), device.getVendorId()
                        , device.getProductId(), i));
            }
        }
        return serialPorts;
    }

    public boolean isConnected() {
        return connected;
    }


    public static class SerialPort {
        public int deviceId = 0;
        public int vendorId = 0x0c26;//厂商号
        public int productId = 0;//设备号
        public int portNum = 0;//端口号

        public SerialPort(int deviceId, int vendorId, int productId, int portNum) {
            this.deviceId = deviceId;
            this.vendorId = vendorId;
            this.productId = productId;
            this.portNum = portNum;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("SerialPort:deviceId=0x%04X, vendorId=0x%04X, portNum=%d"
                    , deviceId, vendorId, portNum);
        }

        @SuppressLint("DefaultLocale")
        public String information() {
            return String.format("\\0x%04X\\0x%04X\\0x%04X\\0x%d", deviceId, vendorId, productId, portNum);
        }
    }
}
