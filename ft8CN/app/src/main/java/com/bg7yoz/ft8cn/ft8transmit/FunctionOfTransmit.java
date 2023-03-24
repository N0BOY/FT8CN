package com.bg7yoz.ft8cn.ft8transmit;
/**
 * FT8通联的6步
 * @author BGY70Z
 * @date 2023-03-20
 */

import com.bg7yoz.ft8cn.Ft8Message;

public class FunctionOfTransmit {
    private int functionOrder;//消息的序号
    private String functionMessage;//消息内容
    private boolean completed;//是否完成
    private boolean isCurrentOrder;//是不是当前要发射的消息
    private Ft8Message ft8Message;

//    /**
//     * 老的发送消息方法
//     * @param functionOrder 消息序号
//     * @param functionMessage 消息内容
//     * @param completed 是否结束
//     */
//    @Deprecated
//    public FunctionOfTransmit(int functionOrder, String functionMessage, boolean completed) {
//        this.functionOrder = functionOrder;
//        this.functionMessage = functionMessage;
//        this.completed = completed;
//    }

    /**
     * 新版发送消息方法
     * @param functionOrder 消息序号
     * @param message FT8消息
     * @param completed 是否结束
     */
    public FunctionOfTransmit(int functionOrder, Ft8Message message, boolean completed) {
        this.functionOrder = functionOrder;
        ft8Message=message;
        this.completed = completed;
        this.functionMessage = message.getMessageText();
    }

    public int getFunctionOrder() {
        return functionOrder;
    }

    public void setFunctionOrder(int functionOrder) {
        this.functionOrder = functionOrder;
    }

    public String getFunctionMessage() {
        return functionMessage;
    }

    public void setFunctionMessage(String functionMessage) {
        this.functionMessage = functionMessage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCurrentOrder() {
        return isCurrentOrder;
    }

    public void setCurrentOrder(int currentOrder) {
        isCurrentOrder = currentOrder==functionOrder;
    }
}
