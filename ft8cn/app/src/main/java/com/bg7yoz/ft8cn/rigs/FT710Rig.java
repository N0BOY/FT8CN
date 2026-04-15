package com.bg7yoz.ft8cn.rigs;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

public class FT710Rig extends YaesuDX10Rig {
    private static final String TAG = "FT710Rig";
    private static final long DATA_MODE_SETTLE_DELAY_MS = 180L;

    @Override
    public void setUsbModeToRig() {
        if (getConnector() == null) {
            return;
        }
        Log.d(TAG, "setUsbModeToRig: switch FT-710 via RTTY-U -> DATA-U");
        GeneralVariables.debugLog(TAG, "mode switch request: RTTY-U -> DATA-U");
        getConnector().sendData(Yaesu3RigConstant.setOperationRTTY_U_Mode());
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getConnector() == null || !isConnected()) {
                    GeneralVariables.debugLog(TAG, "mode switch aborted before DATA-U");
                    return;
                }
                getConnector().sendData(Yaesu3RigConstant.setOperationDATA_U_Mode());
                GeneralVariables.debugLog(TAG, "mode switch settled to DATA-U");
            }
        }, DATA_MODE_SETTLE_DELAY_MS);
    }

    @Override
    public String getName() {
        return "YAESU FT-710";
    }
}
