package com.bg7yoz.ft8cn.timer;
/**
 * UtcTimer类的一个接口，用于UtcTimer的回调。
 * UtcTimer是一个动作触发器，在一个时钟周期到来时触发动作，触发动作的回调函数是DoOnSecTimer。
 * UtcTimer在以一个固定的频率循环（目前默认时100毫秒），在每一个频率下的回调函数是doHeartBeatTimer。
 * 注意!!!! doHeartBeatTimer不要执行耗时的操作，一定要在心跳间隔内完成，否则可能会造成线程的积压，影响性能。
 *
 * @author BG7YOZ
 * @date 2022.5.6
 */
public interface OnUtcTimer {
    void doHeartBeatTimer(long utc);//心跳回调，在触发器每一个循环时触发，心跳的只处理简单事务，不要过多占用CPU，防止线程叠加
    void doOnSecTimer(long utc);//当指定的时间间隔时触发
}
