package com.bg7yoz.ft8cn.database;

/**
 * 查询呼号的回调
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnGetCallsign {
    void  doOnAfterGetCallSign(boolean exists);
}
