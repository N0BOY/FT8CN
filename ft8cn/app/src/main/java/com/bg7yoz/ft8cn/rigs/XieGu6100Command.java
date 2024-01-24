package com.bg7yoz.ft8cn.rigs;


import android.util.Log;


/**
 * X6100_V1.1.6版新增指令说明
 *
 *
 * 1. 新增CI-V指令:
 *    1A 01 (C1) (C2)
 *    C1: 波段号, See IC-705 CI-V Command Table
 *    C2: 寄存器代码(未用), See IC-705 CI-V Command Table
 *    X6100回发数据           说明
 *    FE FE                   # 2 byte, CI-V header
 *    E0 XX 1A 01 01 01       # 6 bytes, The command payload, XX is the rig's address
 *    00 00 80 01 00          # 5 bytes, Operating frequency setting
 *    03 02                   # 2 bytes, Operating mode setting
 *    00                      # 1 byte, Data mode setting
 *    00                      # 1 byte, Duplex and Tone settings
 *    00                      # 1 byte, Digital squelch setting
 *    00 08 85                # 3 bytes, Repeater tone frequency setting
 *    00 08 85                # 3 bytes, Repeater tone frequency setting
 *    00 00 23                # 3 bytes, DTCS code setting
 *    00                      # 1 byte, DV Digital code squelch setting
 *    00 50 00                # 3 bytes, Duplex offset frequency setting
 *    58 36 31 30 30 20 20 20 # 8 bytes, UR (Destination) call sign setting
 *    20 20 20 20 20 20 20 20 # 8 bytes, R1 (Access repeater) call sign setting
 *    20 20 20 20 20 20 20 20 # 8 bytes, R2 (Gateway/Link repeater) call sign setting
 *    FD                      # 1 byte, CI-V tail
 * 2. 新增CI-V指令:
 *    1A 06
 *    See IC-705 CI-V Command Table
 * 3. 新增CI-V指令:
 *    21 00
 *    21 01
 *    21 02
 *    See IC-705 CI-V Command Table
 * 4. 新增CI-V指令:
 *    26 (C1) (C2) (C3) (C4)
 *    C1: VFO序号 (VFO index)
 *        0:     Foreground VFO
 *        other: Background VFO
 *    C2: 工作模式 (Operating mode)
 *        See IC-705 CI-V Command Table
 *    C3: 数据模式 (Data mode)
 *        0:     OFF
 *        other: ON
 *    C4: 滤波器号 (Filter setting)
 *        1:     FILTER1
 *        2:     FILTER2
 *        3:     FILTER3
 *        other: Invalid
 *    *Note: [LSB/USB mode]         with Data mode ON -> L-DIG/U-DIG
 *           [Other operating mode] with Data mode ON -> No effect
 * 5. 新增蓝牙SPP,可以使用FLRIG,Omni-Rig等PC软件无线控制X6100
 *    蓝牙连接计算机后,组合键Win+R,输入bthprops.cpl按回车,在弹出的界面里点击"更多蓝牙设置",
 *    在弹出的"蓝牙设置"窗口中点击"COM端口"选项卡,名称为"X6100 Bluetooth 'Serial Port'"的端口即为蓝牙CI-V接口,
 *    例子:
 *    端口  方向  名称
 *    COM3  传出  X6100 Bluetooth 'Serial Port'
 */
public class XieGu6100Command {
    private static final String TAG = "6100RigCommand";
    private byte[] rawData;

    /**
     * 获取主命令
     *
     * @return 主命令值
     */
    public int getCommandID() {//获取主命令
        if (rawData.length < 5) {
            return -1;
        }
        return rawData[4];
    }

    /**
     * 获取子命令，有的指令没有子命令，要注意。
     *
     * @return 子命令
     */
    public int getSubCommand() {//获取子命令
        if (rawData.length < 7) {
            return -1;
        }
        return rawData[5];
    }

    /**
     * 获取带2字节的子命令，有的指令没有子命令，有的指令只有1个字节，要注意。
     * @return 子指令
     */
    public int getSubCommand2() {//获取子命令
        if (rawData.length < 8) {
            return -1;
        }
        return readShortData(rawData,6);
    }
    /**
     * 获取带3字节的子命令，有的指令没有子命令，有的指令只有1个字节，要注意。
     * @return 子指令
     */
    public int getSubCommand3() {//获取子命令
        if (rawData.length < 9) {
            return -1;
        }
        return  ((int) rawData[7] & 0xff)
                | ((int) rawData[6] & 0xff) << 8
                | ((int) rawData[5] & 0xff) << 16;


    }

    /**
     * 获取数据区，有的指令有子命令，有的没有子命令，所以要区分出来。子命令占一个字节
     *
     * @param hasSubCommand 是否有子命令
     * @return 返回数据区
     */
    public byte[] getData(boolean hasSubCommand) {
        int pos;

        if (hasSubCommand) {
            pos = 6;
        } else {
            pos = 5;
        }
        if (rawData.length < pos + 1) {//没有数据区了
            return null;
        }

        byte[] data = new byte[rawData.length - pos];

        for (int i = 0; i < rawData.length - pos; i++) {
            data[i] = rawData[pos + i];
        }
        return data;
    }

    public byte[] getData2Sub() {
        if (rawData.length < 9) {//没有数据区了
            return null;
        }

        byte[] data = new byte[rawData.length - 8];

        System.arraycopy(rawData, 8, data, 0, rawData.length - 8);
        return data;
    }
    //解析接收的指令

    /**
     * 从串口中接到的数据解析出指令的数据:FE FE E0 A4 Cn Sc data FD
     *
     * @param ctrAddr 控制者地址，默认E0或00
     * @param rigAddr 电台地址，705默认是A4
     * @param buffer  从串口接收到的数据
     * @return 返回电台指令对象，如果不符合指令的格式，返回null。
     */
    public static XieGu6100Command getCommand(int ctrAddr, int rigAddr, byte[] buffer) {
        Log.d(TAG, "getCommand: "+BaseRig.byteToStr(buffer) );
        if (buffer.length <= 5) {//指令的长度不可能小于等5
            return null;
        }
        int position = -1;//指令的位置
        for (int i = 0; i < buffer.length; i++) {
            if (i + 6 > buffer.length) {//说明没找到指令
                return null;
            }
            if (buffer[i] == (byte) 0xfe
                    && buffer[i + 1] == (byte) 0xfe//命令头0xfe 0xfe
                    && (buffer[i + 2] == (byte) ctrAddr || buffer[i + 2] == (byte) 0x00)//控制者地址默认E0或00
                    //&& buffer[i + 3] == (byte) rigAddr
            ) {//协谷CIV默认地址是0x70,但是在测试1.1.7固件的时候，发现回复频率地址始终是0xA4，这似乎是个BUG，暂时忽略CIV地址的判断
                position = i;
                break;
            }
        }
        //说明没找到
        if (position == -1) {
            return null;
        }

        int dataEnd = -1;
        //从命令头之后查起。所以i=position
        for (int i = position; i < buffer.length; i++) {
            if (buffer[i] == (byte) 0xfd) {//是否到结尾了
                dataEnd = i;
                break;
            }
        }
        if (dataEnd == -1) {//说明没找到结尾
            return null;
        }

        XieGu6100Command icomCommand = new XieGu6100Command();
        icomCommand.rawData = new byte[dataEnd - position];
        int pos = 0;
        for (int i = position; i < dataEnd; i++) {//把指令数据搬到rawData中
            //icomCommand.rawData[i] = buffer[i];
            icomCommand.rawData[pos] = buffer[i];//定位错误
            pos++;
        }
        return icomCommand;
    }


    /**
     * 从数据区中计算频率BCD码
     *
     * @param hasSubCommand 是否含有子命令
     * @return 返回频率值
     */
    public long getFrequency(boolean hasSubCommand) {
        byte[] data = getData(hasSubCommand);
        if (data == null) return -1;
        if (data.length < 5) {
            return -1;
        }
        return (int) (data[0] & 0x0f)//取个位 1hz
                + ((int) (data[0] >> 4) & 0xf) * 10//取十位 10hz
                + (int) (data[1] & 0x0f) * 100//百位 100hz
                + ((int) (data[1] >> 4) & 0xf) * 1000//千位  1khz
                + (int) (data[2] & 0x0f) * 10000//万位 10khz
                + ((int) (data[2] >> 4) & 0xf) * 100000//十万位 100khz
                + (int) (data[3] & 0x0f) * 1000000//百万位 1Mhz
                + ((int) (data[3] >> 4) & 0xf) * 10000000//千万位 10Mhz
                + (int) (data[4] & 0x0f) * 100000000//亿位 100Mhz
                + ((int) (data[4] >> 4) & 0xf) * 100000000;//十亿位 1Ghz
    }


    /**
     * 把字节转换成short，不做小端转换！！
     *
     * @param data 字节数据
     * @return short
     */
    public static short readShortData(byte[] data, int start) {
        if (data.length - start < 2) return 0;
        return (short) ((short) data[start + 1] & 0xff
                | ((short) data[start] & 0xff) << 8);
    }


}