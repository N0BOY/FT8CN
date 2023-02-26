package com.bg7yoz.ft8cn.ft8transmit;

import com.bg7yoz.ft8cn.log.QSLRecord;

public interface OnTransmitSuccess {
    void doAfterTransmit(QSLRecord qslRecord);
}
