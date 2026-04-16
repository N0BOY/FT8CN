package com.bg7yoz.ft8cn.rigs;

import com.bg7yoz.ft8cn.GeneralVariables;

/**
 * FT-710 专用机型分支。
 * 目前主要与 FTDX10 共用大部分 CAT 行为，但会显式关闭后台轮询，
 * 并且不主动改写电台模式，把模式保持权交给用户和 USB 音频链路。
 */
public class FT710Rig extends YaesuDX10Rig {
    private static final String TAG = "FT710Rig";

    @Override
    protected boolean shouldEnableBackgroundPolling() {
        return false;
    }

    @Override
    public void setUsbModeToRig() {
        GeneralVariables.debugLog(TAG, "mode switch skipped for FT-710; preserve current rig mode");
    }

    @Override
    public String getName() {
        return "YAESU FT-710";
    }
}
