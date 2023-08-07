package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 发射的回调
 * @author BGY70Z
 * @date 2023-03-20
 */

import com.bg7yoz.ft8cn.Ft8Message;

public interface OnDoTransmitted {
    void onBeforeTransmit(Ft8Message message,int functionOder);
    void onAfterTransmit(Ft8Message message, int functionOder);
    void onTransmitByWifi(Ft8Message message);
    boolean supportTransmitOverCAT();
    void onTransmitOverCAT(Ft8Message message);
}
