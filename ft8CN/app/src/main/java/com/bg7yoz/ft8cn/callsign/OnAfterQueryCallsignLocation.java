package com.bg7yoz.ft8cn.callsign;

/**
 * 用于查询呼号归属地的回调接口，因为数据库操作采用异步方式
 *
 * @author BG7YOZ
 * @date 2023-03-20
 *
 */
public interface OnAfterQueryCallsignLocation {
    void doOnAfterQueryCallsignLocation(CallsignInfo callsignInfo);
}
