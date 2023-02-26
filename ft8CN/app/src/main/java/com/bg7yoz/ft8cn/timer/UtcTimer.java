package com.bg7yoz.ft8cn.timer;
/**
 * UtcTimer类，用于实现FT8在各通联周期开始时触发的动作。FT8的通联因为需要时钟同步，以UTC时间为基准，每15秒一个周期（FT4为7.5秒）。
 * 该类采用Timer和TimerTask来实现定时触发动作。
 * 由于FT8需要时钟同步（精度为秒），在每一个周期开始触发动作，所以，目前以100毫秒为心跳，检测是否处于周期（对UTC时间以周期的秒数取模）的开始，
 * 如果是，则回调doHeartBeatTimer函数，为防止重复动作，触发后会等待1秒钟后再进入新的心跳周期（因为是以秒数取模）。
 * 注意！！为防止回调动作占用时间过长，影响下一个动作的触发，所以，回调都是以多线程的方式调用，在使用时要注意线程安全。
 * <p>
 * BG7YOZ
 * 2022.5.7
 */

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UtcTimer {
    private final int sec;
    private final boolean doOnce;
    private final OnUtcTimer onUtcTimer;


    private long utc;
    public static int delay=0;//时钟总的延时，（毫秒）
    private boolean running = false;//用来判断是否触发周期的动作

    private final Timer secTimer = new Timer();
    private final Timer heartBeatTimer = new Timer();
    private int time_sec = 0;//时间的偏移量；
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private final Runnable doSomething=new Runnable() {
        @Override
        public void run() {
            onUtcTimer.doOnSecTimer(utc);
        }
    };
    private final ExecutorService heartBeatThreadPool=Executors.newCachedThreadPool();
    private final Runnable doHeartBeat=new Runnable() {
        @Override
        public void run() {
            onUtcTimer.doHeartBeatTimer(utc);
        }
    };

    /**
     * 类方法。获得UTC时间的字符串表示结果。
     * @param time 时间。
     * @return String 以字符串方式显示UTC时间。
     */
    @SuppressLint("DefaultLocale")
    public static String getTimeStr(long time) {
        long curtime = time / 1000;
        long hour = ((curtime) / (60 * 60)) % 24;//小时
        long sec = (curtime) % 60;//秒
        long min = ((curtime) % 3600) / 60;//分
        return String.format("UTC : %02d:%02d:%02d", hour, min, sec);
    }

    /**
     * 以HHMMSS格式显示UTC时间
     * @param time
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static String getTimeHHMMSS(long time) {
        long curtime = time / 1000;
        long hour = ((curtime) / (60 * 60)) % 24;//小时
        long sec = (curtime) % 60;//秒
        long min = ((curtime) % 3600) / 60;//分
        return String.format("%02d%02d%02d", hour, min, sec);
    }
    public static String getYYYYMMDD(long time){
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(new Date(time));
    }
    public static String getDatetimeStr(long time){
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(new Date(time));
    }

    /**
     * 时钟触发器的构建方法。需要确定时钟的周期，周期一般是15秒或7.5秒，因为周期的参数是int，所以参数的单位是十分之一秒。
     * 由于心跳频率较快（暂时定为100毫秒），心跳的动作越简练越好，要在下一个心跳开始之前处理完，防止造成线程叠加，影响性能。
     * 心跳动作不会因周期动作不触发（running==false）而影响，只要UtcTimer的实例存在，心跳动作就运行（方便显示时钟数据）。
     * 该触发器需要调用delete函数彻底停止（心跳动作也停止了）。
     *
     * @param sec        时钟的周期，单位是十分之一秒，如：15秒，值150，7.5秒，值75。
     * @param doOnce     是否只触发一次。
     * @param onUtcTimer 回调函数，包括心跳回调，和周期起始触发动作的回调。
     */
    public UtcTimer(int sec, boolean doOnce, OnUtcTimer onUtcTimer) {
        this.sec = sec;
        this.doOnce = doOnce;
        this.onUtcTimer = onUtcTimer;

        //初始化Timer的任务。
        //TimerTask timerTask = initTask();
        //执行timer，延时0执行，周期100毫秒

        secTimer.schedule(secTask(), 0, 10);
        heartBeatTimer.schedule(heartBeatTask(), 0, 1000);
    }

    /**
     * 定义时钟触发的动作。
     * 时钟触发器的构建方法。需要确定时钟的周期，周期一般是15秒或7.5秒，因为周期的参数是int，所以参数的单位是十分之一秒。
     * 由于心跳频率较快（暂时定为100毫秒），心跳的动作越简练越好，要在下一个心跳开始之前处理完，防止造成线程叠加，影响性能。
     * 心跳动作不会因周期动作不触发（running==false）而影响，只要UtcTimer的实例存在，心跳动作就运行（方便显示时钟数据）。
     *
     * @return TimerTask 返回动作的实例。
     */


    private TimerTask heartBeatTask() {
        return new TimerTask() {
            @Override
            public void run() {
                //心跳动作
                doHeartBeatEvent(onUtcTimer);
            }
        };
    }

    private TimerTask secTask() {
        return new TimerTask() {


            @Override
            public void run() {

                try {
                    utc = getSystemTime();//获取当前的UTC时间
                    //utc/100是取十分之一秒为单位，所以取模应该是600，而非60，切记！
                    //running是判断是否需要触发周期动作。
                    //+80是因为触发后一些动作影响，补偿的时间
                    //time_sec是时间的偏移量
                    if (running && (((utc - time_sec) / 100) % 600) % sec == 0) {
                        //周期动作
                        //注意!!!! doHeartBeatTimer不要执行耗时的操作，一定要在心跳间隔内完成，否则可能会造成线程的积压，影响性能。
                        cachedThreadPool.execute(doSomething);//用线程池的方式调用，减少系统消耗
                        //thread.run();

                        //如果只执行一次触发动作
                        if (doOnce) {
                            running = false;
                            return;
                        }

                        //等待1秒钟，防止重复触发动作。
                        Thread.sleep(1000);
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 触发心跳时的动作。由Timer调用，写此函数是方便阅读。动作是在新创建的线程中执行。
     *
     * @param onUtcTimer 触发时钟的回调函数。
     */
    private void doHeartBeatEvent(OnUtcTimer onUtcTimer) {
        //心跳动作
        heartBeatThreadPool.execute(doHeartBeat);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //注意!!!! doHeartBeatTimer不要执行耗时的操作，一定要在心跳间隔内完成，否则可能会造成线程的积压，影响性能。
//                onUtcTimer.doHeartBeatTimer(utc);
//            }
//        }).start();
    }


    public void stop() {
        running = false;
    }

    public void start() {
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public void delete() {
        secTimer.cancel();
        heartBeatTimer.cancel();
    }

    /**
     * 设置时间偏移量，正值是向后偏移
     * @param time_sec 向前的偏移量
     */
    public void setTime_sec(int time_sec) {
        this.time_sec = time_sec;
    }

    /**
     * 获取时间偏移
     * @return 时间偏移值（毫秒）
     */
    public int getTime_sec() {
        return time_sec;
    }

    public long getUtc() {
        return utc;
    }

    /**
     * 根据UTC时间计算时序
     * @param utc UTC时间
     * @return 时序:0,1
     */
    public static int sequential(long utc){
        return (int) ((((utc) / 1000) / 15) % 2);
    }
    public static int getNowSequential(){
      return   sequential(getSystemTime());
    }
    public static long getSystemTime(){
        return delay+System.currentTimeMillis();
    }


}
