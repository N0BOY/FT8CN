package com.bg7yoz.ft8cn.bluetooth;
/**
 * 蓝牙状态广播类。连接、断开、变化
 * @writer bg7yoz
 * @date 2022-07-22
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

public class BluetoothStateBroadcastReceive extends BroadcastReceiver {
    private static final String TAG="BluetoothStateBroadcastReceive";
    private Context context;
    private MainViewModel mainViewModel;

    public BluetoothStateBroadcastReceive(Context context, MainViewModel mainViewModel) {
        this.context = context;
        this.mainViewModel = mainViewModel;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context=context;
        String action = intent.getAction();

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        int headset=-1;
        int a2dp=-1;
        if (blueAdapter!=null) {
            headset = blueAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
            a2dp = blueAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        }
        switch (action) {
            case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
            case BluetoothAdapter.EXTRA_CONNECTION_STATE:
            case BluetoothAdapter.EXTRA_STATE:
                if(headset == BluetoothProfile.STATE_CONNECTED ||a2dp==BluetoothProfile.STATE_CONNECTED){
                //if(headset == BluetoothProfile.STATE_CONNECTED){
                //if(a2dp==BluetoothProfile.STATE_CONNECTED){
                    mainViewModel.setBlueToothOn();
                }else {
                    mainViewModel.setBlueToothOff();
                }
                break;

            case BluetoothDevice.ACTION_ACL_CONNECTED:
                if (device!=null) {
                    ToastMessage.show(String.format(
                            GeneralVariables.getStringFromResource(R.string.bluetooth_is_connected)
                            ,device.getName()));
                }
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                if (device!=null) {
                    ToastMessage.show(String.format(
                            GeneralVariables.getStringFromResource(R.string.bluetooth_is_diconnected)
                            ,device.getName()));
                }
                break;

            case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                ToastMessage.show(GeneralVariables.getStringFromResource(R.string.sound_source_switched));
                break;


            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.bluetooth_turn_off));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.bluetooth_turn_on));
                        break;
                }
                break;

        }
    }

//    static final int PROFILE_HEADSET = 0;
//    static final int PROFILE_A2DP  = 1;
//    static final int PROFILE_OPP  = 2;
//    static final int PROFILE_HID = 3;
//    static final int PROFILE_PANU  = 4;
//    static final int PROFILE_NAP  = 5;
//    static final int PROFILE_A2DP_SINK  = 6;
//
//    private boolean checkBluetoothClass(BluetoothClass bluetoothClass,int proFile){
//        if (proFile==PROFILE_A2DP){
//            bluetoothClass.hasService(BluetoothClass.Service.RENDER);
//            return true;
//        }
//    }
}
