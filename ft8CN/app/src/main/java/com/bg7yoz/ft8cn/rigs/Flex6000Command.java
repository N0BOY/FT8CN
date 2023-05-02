package com.bg7yoz.ft8cn.rigs;


import android.util.Log;

public class Flex6000Command {
    private static final String TAG = "Flex6000Command";
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

    public Flex6000Command(String commandID, String data) {
        this.commandID = commandID;
        this.data = data;
    }
    //解析接收的指令

    /**
     * 从串口中接到的数据解析出指令的数据:指令头+内容+分号
     *
     * @param buffer  从串口接收到的数据
     * @return 返回电台指令对象，如果不符合指令的格式，返回null。
     */
    public static Flex6000Command getCommand(String buffer) {
        if (buffer.length() < 4) {//指令的长度必须大于等于4,ZZFA
            return null;
        }
        if (buffer.substring(0,4).matches("[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]")) {
                return new Flex6000Command(buffer.substring(0, 4), buffer.substring(4));
        }
        return null;
    }


    /**
     * 计算频率
     * @param command 指令
     * @return 频率
     */
    public static long getFrequency(Flex6000Command command) {
        try {
            if(command.getCommandID().equals("ZZFA")||command.getCommandID().equals("ZZFB")) {
                return Long.parseLong(command.getData());
            }else {
                return 0;
            }
        }catch (Exception e){
            Log.e(TAG, "获取频率失败: "+command.getData()+"\n"+e.getMessage() );
        }
       return 0;
    }




}