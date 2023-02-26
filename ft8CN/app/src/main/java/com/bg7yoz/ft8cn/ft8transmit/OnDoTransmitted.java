package com.bg7yoz.ft8cn.ft8transmit;

import com.bg7yoz.ft8cn.Ft8Message;

public interface OnDoTransmitted {
    void onBeforeTransmit(Ft8Message message,int functionOder);
    void onAfterTransmit(Ft8Message message, int functionOder);
    void onAfterGenerate(short[] data);
}
