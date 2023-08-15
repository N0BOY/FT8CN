package com.bg7yoz.ft8cn.rigs;


import android.util.Log;

public class IcomCommand {
    private static final String TAG = "RigCommand";
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
    public static IcomCommand getCommand(int ctrAddr, int rigAddr, byte[] buffer) {
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
                    && buffer[i + 3] == (byte) rigAddr) {//电台地址，705的默认值是A4
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

        IcomCommand icomCommand = new IcomCommand();
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