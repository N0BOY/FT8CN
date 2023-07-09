package com.bg7yoz.ft8cn.rigs;


import android.util.Log;

public class ElecraftCommand {
    private static final String TAG = "ElecraftCommand";
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

    public ElecraftCommand(String commandID, String data) {
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
    public static ElecraftCommand getCommand(String buffer) {
        if (buffer.length() < 2) {//指令的长度必须大于等于2
            return null;
        }
        if (buffer.substring(0,2).matches("[a-zA-Z][a-zA-Z]")) {
                return new ElecraftCommand(buffer.substring(0, 2), buffer.substring(2));
        }
        return null;
    }


    /**
     * 计算频率
     * @param command 指令
     * @return 频率
     */
    public static long getFrequency(ElecraftCommand command) {
        try {
            if(command.getCommandID().equals("FA")||command.getCommandID().equals("FB")) {
                return Long.parseLong(command.getData());
            }else {
                return 0;
            }
        }catch (Exception e){
            Log.e(TAG, "获取频率失败: "+command.getData()+"\n"+e.getMessage() );
        }
       return 0;
    }

    public static boolean isSWRMeter(ElecraftCommand command) {
        return  command.data.length() >= 3;
    }

    public static int getSWRMeter(ElecraftCommand command) {
        return  Integer.parseInt(command.data.substring(0,3));
    }


}