package com.bg7yoz.ft8cn.rigs;


import android.util.Log;

public class Yaesu3Command {
    private static final String TAG = "Yaesu3Command";
    private final String commandID;
    private final String data;

    /**
     * 获取命令(两字节字符串)
     *
     * @return 主命令值
     */
    public String getCommandID() {//获取主命令
        return commandID;
    }

    /**
     * 获取命令数据，字符串，没有分号
     *
     * @return 命令数据
     */
    public String getData() {//获取命令数据
        return data;
    }

    public Yaesu3Command(String commandID, String data) {
        this.commandID = commandID;
        this.data = data;
    }
    //解析接收的指令

    /**
     * 从串口中接到的数据解析出指令的数据:指令头+内容+分号
     *
     * @param buffer 从串口接收到的数据
     * @return 返回电台指令对象，如果不符合指令的格式，返回null。
     */
    public static Yaesu3Command getCommand(String buffer) {
        if (buffer.length() < 2) {//指令的长度必须大于等于2
            return null;
        }
        if (buffer.substring(0, 2).matches("[a-zA-Z][a-zA-Z]")) {
            return new Yaesu3Command(buffer.substring(0, 2), buffer.substring(2));
        }
        return null;
    }


    /**
     * 计算频率
     *
     * @param command 指令
     * @return 频率
     */
    public static long getFrequency(Yaesu3Command command) {
        try {
            if (command.getCommandID().equals("FA") || command.getCommandID().equals("FB")) {
                return Long.parseLong(command.getData());
            } else {
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取频率失败: " + command.getData() + "\n" + e.getMessage());
        }
        return 0;
    }


    /**
     * 获取SWR_YAESU 950
     *
     * @param command 指令
     * @return 值
     */
    public static int getALCOrSWR38(Yaesu3Command command) {
        if (command.data.length() < 7) return 0;
        return Integer.parseInt(command.data.substring(1, 4));
    }

    public static boolean isSWRMeter38(Yaesu3Command command) {
        if (command.data.length() < 7) return false;
        return (command.data.charAt(0) == '6');
    }

    public static boolean isALCMeter38(Yaesu3Command command) {
        if (command.data.length() < 7) return false;
        return (command.data.charAt(0) == '4');
    }


    /**
     * 获取SWR_YAESU 891
     *
     * @param command 指令
     * @return 值
     */
    public static int getSWROrALC39(Yaesu3Command command) {
        if (command.data.length() < 4) return 0;
        return Integer.parseInt(command.data.substring(1, 4));
    }

    public static boolean isSWRMeter39(Yaesu3Command command) {
        if (command.data.length() < 4) return false;
        return (command.data.charAt(0) == '6');
    }

    public static boolean isALCMeter39(Yaesu3Command command) {
        if (command.data.length() < 4) return false;
        return (command.data.charAt(0) == '4');
    }

    /**
     * 获取ts-590的ALC,SWR
     *
     * @param command 指令
     * @return 值
     */
    public static int get590ALCOrSWR(Yaesu3Command command) {
        return Integer.parseInt(command.data.substring(1, 5));
    }

    public static boolean is590MeterALC(Yaesu3Command command){
        if (command.data.length() < 5) return false;
        return command.data.charAt(2) == '3';
    }
    public static boolean is590MeterSWR(Yaesu3Command command){
        if (command.data.length() < 5) return false;
        return command.data.charAt(2) == '1';
    }



}