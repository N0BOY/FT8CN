package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 发射结束后的回调
 * @author BGY70Z
 * @date 2023-03-20
 */

import com.bg7yoz.ft8cn.log.QSLRecord;

public interface OnTransmitSuccess {
    void doAfterTransmit(QSLRecord qslRecord);
}
