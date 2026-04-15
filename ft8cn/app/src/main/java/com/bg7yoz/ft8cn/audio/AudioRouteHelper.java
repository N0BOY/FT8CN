package com.bg7yoz.ft8cn.audio;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.rigs.InstructionSet;
import com.bg7yoz.ft8cn.ui.ToastMessage;

public final class AudioRouteHelper {
    private static final String TAG = "AudioRouteHelper";

    private AudioRouteHelper() {
    }

    private static boolean shouldHandleFt710Audio() {
        return GeneralVariables.instructionSet == InstructionSet.YAESU_FT710;
    }

    public static void publishDeviceReport(String reason) {
        if (!shouldHandleFt710Audio()) {
            return;
        }
        Context context = GeneralVariables.getMainContext();
        if (context == null) {
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        publishReport(audioManager, reason, findPreferredInputDevice(audioManager),
                findPreferredOutputDevice(audioManager), null, null);
    }

    public static boolean bindTrackToPreferredOutput(AudioTrack audioTrack, String reason) {
        if (!shouldHandleFt710Audio()) {
            return false;
        }
        Context context = GeneralVariables.getMainContext();
        if (context == null || audioTrack == null) {
            return false;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return false;
        }

        AudioDeviceInfo preferredOutput = findPreferredOutputDevice(audioManager);
        boolean bound = false;
        if (preferredOutput != null) {
            try {
                bound = audioTrack.setPreferredDevice(preferredOutput);
            } catch (Exception e) {
                Log.e(TAG, "bindTrackToPreferredOutput: " + e.getMessage());
            }
        }

        publishReport(audioManager, reason, findPreferredInputDevice(audioManager),
                preferredOutput, audioTrack, bound);
        return bound;
    }

    private static void publishReport(AudioManager audioManager, String reason,
                                      AudioDeviceInfo preferredInput,
                                      AudioDeviceInfo preferredOutput,
                                      AudioTrack audioTrack, Boolean bindResult) {
        StringBuilder report = new StringBuilder();
        report.append("reason=").append(reason).append('\n');
        report.append("connectMode=")
                .append(ConnectMode.getModeStr(GeneralVariables.connectMode))
                .append(", controlMode=")
                .append(ControlMode.getControlModeStr(GeneralVariables.controlMode))
                .append('\n');
        report.append("audioManager mode=").append(audioManager.getMode())
                .append(", sco=").append(audioManager.isBluetoothScoOn())
                .append(", speaker=").append(audioManager.isSpeakerphoneOn())
                .append('\n');
        if (audioTrack != null) {
            report.append("track state=").append(audioTrack.getState())
                    .append(", playState=").append(audioTrack.getPlayState());
            if (bindResult != null) {
                report.append(", preferredBound=").append(bindResult);
            }
            report.append('\n');
        }
        report.append("preferredInput=").append(describeDevice(preferredInput)).append('\n');
        report.append("preferredOutput=").append(describeDevice(preferredOutput)).append('\n');
        report.append("inputs=")
                .append(describeDevices(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)))
                .append('\n');
        report.append("outputs=")
                .append(describeDevices(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)));

        String result = report.toString();
        GeneralVariables.setAudioRouteReport(result);
        Log.d(TAG, result);
        GeneralVariables.debugLog(TAG, "route " + reason
                + " IN=" + describeDevice(preferredInput)
                + " OUT=" + describeDevice(preferredOutput)
                + (bindResult == null ? "" : " bind=" + bindResult));

        if (GeneralVariables.connectMode == ConnectMode.USB_CABLE) {
            ToastMessage.show(buildShortSummary(reason, preferredInput, preferredOutput), true);
        }
    }

    private static String buildShortSummary(String reason, AudioDeviceInfo preferredInput,
                                            AudioDeviceInfo preferredOutput) {
        return "Audio route [" + reason + "]\nIN: " + describeDevice(preferredInput)
                + "\nOUT: " + describeDevice(preferredOutput);
    }

    private static AudioDeviceInfo findPreferredInputDevice(AudioManager audioManager) {
        AudioDeviceInfo fallback = null;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (!device.isSource()) {
                continue;
            }
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_USB_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE
                    || type == AudioDeviceInfo.TYPE_USB_ACCESSORY) {
                return device;
            }
            if (fallback == null) {
                fallback = device;
            }
        }
        return fallback;
    }

    private static AudioDeviceInfo findPreferredOutputDevice(AudioManager audioManager) {
        AudioDeviceInfo wiredFallback = null;
        AudioDeviceInfo anyFallback = null;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (!device.isSink()) {
                continue;
            }
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_USB_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE
                    || type == AudioDeviceInfo.TYPE_USB_ACCESSORY) {
                return device;
            }
            if (wiredFallback == null
                    && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_LINE_ANALOG
                    || type == AudioDeviceInfo.TYPE_LINE_DIGITAL)) {
                wiredFallback = device;
            }
            if (anyFallback == null) {
                anyFallback = device;
            }
        }
        return wiredFallback != null ? wiredFallback : anyFallback;
    }

    private static String describeDevices(AudioDeviceInfo[] devices) {
        if (devices == null || devices.length == 0) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < devices.length; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(describeDevice(devices[i]));
        }
        return builder.toString();
    }

    private static String describeDevice(AudioDeviceInfo device) {
        if (device == null) {
            return "-";
        }
        CharSequence name = device.getProductName();
        String productName = name == null ? "" : name.toString().trim();
        if (productName.isEmpty()) {
            productName = "unknown";
        }
        return "#" + device.getId() + ":" + typeToString(device.getType()) + "(" + productName + ")";
    }

    private static String typeToString(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "SPEAKER";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "WIRED_HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "WIRED_HEADPHONES";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "LINE_ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "LINE_DIGITAL";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "BT_SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "BT_A2DP";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB_DEVICE";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB_ACCESSORY";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB_HEADSET";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "HDMI_ARC";
            case AudioDeviceInfo.TYPE_HDMI_EARC:
                return "HDMI_EARC";
            case AudioDeviceInfo.TYPE_BLE_HEADSET:
                return "BLE_HEADSET";
            case AudioDeviceInfo.TYPE_BLE_SPEAKER:
                return "BLE_SPEAKER";
            default:
                return "TYPE_" + type;
        }
    }
}
