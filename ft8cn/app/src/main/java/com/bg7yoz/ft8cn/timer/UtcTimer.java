package com.bg7yoz.ft8cn.timer;
/**
 * UtcTimer类，用于实现FT8在各通联周期开始时触发的动作。FT8的通联因为需要时钟同步，以UTC时间为基准，每15秒一个周期（FT4为7.5秒）。
 * 该类采用Timer和TimerTask来实现定时触发动作。
 * 由于FT8需要时钟同步（精度为秒），在每一个周期开始触发动作，所以，目前以100毫秒为心跳，检测是否处于周期（对UTC时间以周期的秒数取模）的开始，
 * 如果是，则回调doHeartBeatTimer函数，为防止重复动作，触发后会等待1秒钟后再进入新的心跳周期（因为是以秒数取模）。
 * 注意！！为防止回调动作占用时间过长，影响下一个动作的触发，所以，回调都是以多线程的方式调用，在使用时要注意线程安全。
 * <p>
 * @author BG7YOZ
 * @date 2022.5.7
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class UtcTimer {
    private final int sec;
    private final boolean doOnce;
    private final OnUtcTimer onUtcTimer;


    private long utc;
    public static int delay = 0;//时钟总的延时，（毫秒）
    private boolean running = false;//用来判断是否触发周期的动作

    private final Timer secTimer = new Timer();
    private final Timer heartBeatTimer = new Timer();
    private int time_sec = 0;//时间的偏移量；
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private final Runnable doSomething = new Runnable() {
        @Override
        public void run() {
            onUtcTimer.doOnSecTimer(utc);
        }
    };
    private final ExecutorService heartBeatThreadPool = Executors.newCachedThreadPool();
    private final Runnable doHeartBeat = new Runnable() {
        @Override
        public void run() {
            onUtcTimer.doHeartBeatTimer(utc);
        }
    };

    /**
     * 类方法。获得UTC时间的字符串表示结果。
     *
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
     *
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

    public static String getYYYYMMDD(long time) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(new Date(time));
    }

    public static String getDatetimeStr(long time) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(new Date(time));
    }

    public static String getDatetimeYYYYMMDD_HHMMSS(long time) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
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
     *
     * @param time_sec 向前的偏移量
     */
    public void setTime_sec(int time_sec) {
        this.time_sec = time_sec;
    }

    /**
     * 获取时间偏移
     *
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
     *
     * @param utc UTC时间
     * @return 时序:0,1
     */
    public static int sequential(long utc) {
        return (int) ((((utc) / 1000) / 15) % 2);
    }

    public static int getNowSequential() {
        return sequential(getSystemTime());
    }

    public static long getSystemTime() {
        return delay + System.currentTimeMillis();
    }

    /**
     * 同步时间：优先使用GPS时间，如果GPS失败则回退到NTP时间服务器
     * Time synchronization: Try GPS first (if enabled), fall back to NTP if GPS fails or is disabled
     */
    public static void syncTime(AfterSyncTime afterSyncTime) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Try GPS time sync first only if enabled
                SyncResult gpsResult;
                if (GeneralVariables.enableGpsTimeSync) {
                    gpsResult = syncTimeFromGPS(afterSyncTime);
                    
                    // If GPS failed, fall back to NTP
                    if (!gpsResult.success) {
                        if (afterSyncTime != null) {
                            afterSyncTime.gpsFailedFallingBackToNTP(gpsResult.failureReason);
                        }
                        syncTimeFromNTP(afterSyncTime);
                    }
                } else {
                    // GPS sync is disabled, go directly to NTP
                    syncTimeFromNTP(afterSyncTime);
                }
            }
        }).start();
    }
    
    /**
     * Result of a sync operation
     */
    private static class SyncResult {
        boolean success;
        String failureReason;
        
        SyncResult(boolean success, String failureReason) {
            this.success = success;
            this.failureReason = failureReason;
        }
    }

    /**
     * 从GPS获取时间同步
     * Get time synchronization from GPS
     * 
     * @param afterSyncTime Callback interface
     * @return SyncResult with success status and failure reason
     */
    @SuppressLint("MissingPermission")
    private static SyncResult syncTimeFromGPS(AfterSyncTime afterSyncTime) {
        Context context = GeneralVariables.getMainContext();
        if (context == null) {
            String reason = "Context not available";
            logExternalCall("GPS", "GPS", "FAILED", reason);
            return new SyncResult(false, reason);
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return new SyncResult(false, "Location service not available");
        }

        // Check if GPS provider is available
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return new SyncResult(false, "GPS provider disabled");
        }

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return new SyncResult(false, "Location permission denied");
        }

        // First try to get last known location (fast, synchronous)
        try {
            @SuppressLint("MissingPermission")
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null && lastLocation.getTime() > 0) {
                long gpsTime = lastLocation.getTime();
                long currentTime = System.currentTimeMillis();
                long timeDiff = Math.abs(gpsTime - currentTime);
                
                // Use last known location if it's recent (within 5 minutes)
                // GPS time is very accurate, so even slightly stale is OK for time sync
                if (timeDiff < 5 * 60 * 1000) { // Within 5 minutes
                    int trueDelay = (int) (gpsTime - currentTime);
                    UtcTimer.delay = trueDelay;
                    if (afterSyncTime != null) {
                        afterSyncTime.doAfterSyncTimer(trueDelay, "GPS", "GPS");
                    }
                    return new SyncResult(true, null);
                } else {
                    // Last known location is too stale
                    return new SyncResult(false, "Last GPS location too old (" + (timeDiff / 1000) + " seconds)");
                }
            } else {
                return new SyncResult(false, "No GPS location available");
            }
        } catch (Exception e) {
            // Continue to request fresh location
            // Don't return failure yet, try requesting fresh location
        }

        // If last known location is stale or unavailable, request fresh location update
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final long[] gpsTime = {0};
        final int[] calculatedDelay = {0};

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null && location.getTime() > 0) {
                    // GPS time is in UTC milliseconds
                    gpsTime[0] = location.getTime();
                    long currentTime = System.currentTimeMillis();
                    int trueDelay = (int) (gpsTime[0] - currentTime);
                    calculatedDelay[0] = trueDelay;
                    UtcTimer.delay = trueDelay;
                    success[0] = true;
                }
                latch.countDown();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {
                latch.countDown();
            }
        };

        try {
            // Request location updates using main thread's looper
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0, // minTime - 0 means get update as soon as possible
                    0, // minDistance - 0 means no distance requirement
                    locationListener,
                    Looper.getMainLooper());

            // Wait up to 10 seconds for GPS fix
            boolean received = latch.await(10, TimeUnit.SECONDS);
            
            // Remove listener to prevent memory leaks
            locationManager.removeUpdates(locationListener);

            if (received && success[0]) {
                // GPS sync succeeded - use the delay calculated in the callback (not recalculate with stale time)
                if (afterSyncTime != null) {
                    afterSyncTime.doAfterSyncTimer(calculatedDelay[0], "GPS", "GPS");
                }
                
                // Log successful GPS call
                logExternalCall("GPS", "GPS", "SUCCESS", String.format("Time offset: %d ms", calculatedDelay[0]));
                
                return new SyncResult(true, null);
            } else if (received) {
                // Received but no valid location
                String reason = "GPS location received but invalid";
                logExternalCall("GPS", "GPS", "FAILED", reason);
                return new SyncResult(false, reason);
            } else {
                // Timeout
                String reason = "GPS timeout (10 seconds)";
                logExternalCall("GPS", "GPS", "FAILED", reason);
                return new SyncResult(false, reason);
            }
        } catch (InterruptedException e) {
            // Timeout or interrupted - fall back to NTP
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                // Ignore
            }
            String reason = "GPS interrupted";
            logExternalCall("GPS", "GPS", "FAILED", reason);
            return new SyncResult(false, reason);
        } catch (Exception e) {
            // Any other error - fall back to NTP
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                // Ignore
            }
            String reason = "GPS error: " + (e.getMessage() != null ? e.getMessage() : "Unknown");
            logExternalCall("GPS", "GPS", "FAILED", reason);
            return new SyncResult(false, reason);
        }
    }

    /**
     * 使用配置的NTP服务器同步时间（带重试逻辑和指数退避）
     * Synchronize time using configured NTP server with retry logic and exponential backoff
     */
    private static void syncTimeFromNTP(AfterSyncTime afterSyncTime) {
        String ntpServer = GeneralVariables.ntpServer;
        if (ntpServer == null || ntpServer.isEmpty()) {
            ntpServer = "time.windows.com";
        }
        
        final int MAX_RETRIES = 3;
        final int[] backoffDelays = {2000, 4000, 8000}; // Exponential backoff: 2s, 4s, 8s
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            NTPUDPClient timeClient = new NTPUDPClient();
            // Set timeout to 10 seconds per attempt
            timeClient.setDefaultTimeout(10000);
            
            try {
                InetAddress inetAddress = InetAddress.getByName(ntpServer);
                TimeInfo timeInfo = timeClient.getTime(inetAddress);
                long serverTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
                int trueDelay = (int) ((serverTime - System.currentTimeMillis()));
                UtcTimer.delay = trueDelay % 15000;//延迟的周期
                
                // Success - close client and return
                try {
                    timeClient.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
                
                if (afterSyncTime != null) {
                    afterSyncTime.doAfterSyncTimer(trueDelay, "NTP", ntpServer);
                }
                
                // Log successful NTP call
                logExternalCall("NTP", ntpServer, "SUCCESS", String.format("Time offset: %d ms", trueDelay));
                
                return; // Success - exit retry loop
                
            } catch (IOException e) {
                lastException = e;
                // Close client before retry
                try {
                    timeClient.close();
                } catch (Exception ex) {
                    // Ignore close errors
                }
                
                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoffDelays[attempt - 1]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        if (afterSyncTime != null) {
                            afterSyncTime.syncFailed("NTP", ntpServer, "Interrupted during retry");
                        }
                        return;
                    }
                    // Continue to next retry attempt
                }
            } catch (Exception e) {
                lastException = e;
                // Close client before retry
                try {
                    timeClient.close();
                } catch (Exception ex) {
                    // Ignore close errors
                }
                
                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoffDelays[attempt - 1]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        if (afterSyncTime != null) {
                            afterSyncTime.syncFailed("NTP", ntpServer, "Interrupted during retry");
                        }
                        return;
                    }
                    // Continue to next retry attempt
                }
            }
        }
        
        // All retries failed
        String reason = "Failed after " + MAX_RETRIES + " attempts";
        if (lastException != null) {
            String errorMsg = lastException.getMessage();
            if (errorMsg != null && !errorMsg.isEmpty()) {
                if (errorMsg.contains("timeout") || errorMsg.contains("Timeout") || errorMsg.contains("timed out")) {
                    reason = "NTP timeout after " + MAX_RETRIES + " attempts (10s per attempt)";
                } else {
                    reason = errorMsg + " (after " + MAX_RETRIES + " attempts)";
                }
            }
        }
        if (afterSyncTime != null) {
            afterSyncTime.syncFailed("NTP", ntpServer, reason);
        }
        
        // Log failed NTP call
        logExternalCall("NTP", ntpServer, "FAILED", reason);
    }
    
    /**
     * Log external service call
     */
    private static void logExternalCall(String service, String endpoint, String status, String details) {
        try {
            android.content.Context context = GeneralVariables.getMainContext();
            if (context != null) {
                com.bg7yoz.ft8cn.log.ApplicationLogManager logManager = 
                    new com.bg7yoz.ft8cn.log.ApplicationLogManager(context);
                String message = String.format("%s call to %s: %s - %s", service, endpoint, status, details);
                logManager.writeLog(com.bg7yoz.ft8cn.log.ApplicationLogManager.LogType.EXTERNAL_CALLS, message);
            }
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    public interface AfterSyncTime {
        /**
         * Called when time sync succeeds
         * @param secTime Time difference in milliseconds
         * @param method Sync method used ("GPS" or "NTP")
         * @param server Server used (GPS: "GPS", NTP: server address)
         */
        void doAfterSyncTimer(int secTime, String method, String server);

        /**
         * Called when time sync fails
         * @param method Sync method that failed ("GPS" or "NTP")
         * @param server Server that failed (GPS: "GPS", NTP: server address)
         * @param reason Failure reason
         */
        void syncFailed(String method, String server, String reason);
        
        /**
         * Called when GPS fails and we're falling back to NTP
         * @param reason Reason GPS failed
         */
        void gpsFailedFallingBackToNTP(String reason);
    }
}
