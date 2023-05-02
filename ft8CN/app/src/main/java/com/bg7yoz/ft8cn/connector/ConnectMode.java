package com.bg7yoz.ft8cn.connector;

/**
 * 连接的模式
 * @author BGY70Z
 * @date 2023-03-20
 */
public class ConnectMode {
    public static final int USB_CABLE=0;
    public static final int BLUE_TOOTH=1;
    public static final int NETWORK=2;
    public static String getModeStr(int mode){
        switch (mode){
            case  USB_CABLE:
                return "USB Cable";
            case BLUE_TOOTH:
                return "Bluetooth";
            case NETWORK:
                return "Network";
            default:
                return "-";
        }
    }
}
