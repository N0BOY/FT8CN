/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.bg7yoz.ft8cn.serialport;

/**
 * Registry of USB vendor/product ID constants.
 *
 * Culled from various sources; see
 * <a href="http://www.linux-usb.org/usb.ids">usb.ids</a> for one listing.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public final class UsbId {

    public static final int VENDOR_FTDI = 0x0403;
    public static final int FTDI_FT232R = 0x6001;
    public static final int FTDI_ZERO = 0x0;
    public static final int FTDI_FT2232H = 0x6010;
    public static final int FTDI_FT4232H = 0x6011;
    public static final int FTDI_FT232H = 0x6014;
    public static final int FTDI_FT231X = 0x6015; // same ID for FT230X, FT231X, FT234XD

    public static final int VENDOR_ATMEL = 0x03EB;
    public static final int ATMEL_LUFA_CDC_DEMO_APP = 0x2044;

    public static final int VENDOR_ARDUINO = 0x2341;
    public static final int ARDUINO_UNO = 0x0001;
    public static final int ARDUINO_MEGA_2560 = 0x0010;
    public static final int ARDUINO_SERIAL_ADAPTER = 0x003b;
    public static final int ARDUINO_MEGA_ADK = 0x003f;
    public static final int ARDUINO_MEGA_2560_R3 = 0x0042;
    public static final int ARDUINO_UNO_R3 = 0x0043;
    public static final int ARDUINO_MEGA_ADK_R3 = 0x0044;
    public static final int ARDUINO_SERIAL_ADAPTER_R3 = 0x0044;
    public static final int ARDUINO_LEONARDO = 0x8036;
    public static final int ARDUINO_MICRO = 0x8037;

    public static final int VENDOR_VAN_OOIJEN_TECH = 0x16c0;
    public static final int VAN_OOIJEN_TECH_TEENSYDUINO_SERIAL = 0x0483;

    public static final int CDC_WOLF_PID= 0xF001;// ST CDC WOLF RIG

    public static final int VENDOR_LEAFLABS = 0x1eaf;
    public static final int LEAFLABS_MAPLE = 0x0004;

    public static final int VENDOR_SILABS = 0x10c4;
    public static final int SILABS_CP2102 = 0xea60; // same ID for CP2101, CP2103, CP2104, CP2109
    public static final int SILABS_CP2105 = 0xea70;
    public static final int SILABS_CP2108 = 0xea71;

    public static final int VENDOR_PROLIFIC = 0x067b;
    public static final int PROLIFIC_PL2303 = 0x2303;   // device type 01, T, HX
    public static final int PROLIFIC_PL2303GC = 0x23a3; // device type HXN
    public static final int PROLIFIC_PL2303GB = 0x23b3; // "
    public static final int PROLIFIC_PL2303GT = 0x23c3; // "
    public static final int PROLIFIC_PL2303GL = 0x23d3; // "
    public static final int PROLIFIC_PL2303GE = 0x23e3; // "
    public static final int PROLIFIC_PL2303GS = 0x23f3; // "

    public static final int VENDOR_QINHENG = 0x1a86;
    public static final int QINHENG_CH340 = 0x7523;
    public static final int QINHENG_CH341A = 0x5523;
    public static final int QINHENG_CH341K = 29986;


    // at www.linux-usb.org/usb.ids listed for NXP/LPC1768, but all processors supported by ARM mbed DAPLink firmware report these ids
    public static final int VENDOR_ARM = 0x0d28;
    public static final int ARM_MBED = 0x0204;

    public static final int VENDOR_ST = 0x0483;
    public static final int ST_CDC = 0x5740;
    public static final int ST_CDC2 = 0xA34C;
    public static final int ST_CDC3 = 0x5732;

    public static final int VENDOR_RASPBERRY_PI = 0x2e8a;
    public static final int RASPBERRY_PI_PICO_MICROPYTHON = 0x0005;

    public static final int VENDOR_GUOHE1 = 0x5678;
    public static final int PID_GUOHE1 = 0x6789;

    public static final int VENDOR_ICOM = 0x0c26;
    public static final int IC_R30 = 0x002b;
    public static final int IC_705 = 0x0036;
    public static final int ICOM_USB_SERIAL_CONTROL = 0x0018;
    public static final int XIEGU_X6100 = 0x55D2;//xiegu

    private UsbId() {
        throw new IllegalAccessError("Non-instantiable class");
    }

}
