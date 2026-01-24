package com.bg7yoz.ft8cn.ft8listener;

import com.bg7yoz.ft8cn.Ft8Message;

import java.util.List;

final class DecodeDuplicateFilter {
    private DecodeDuplicateFilter() {
    }

    static boolean isDuplicate(List<Ft8Message> messages, Ft8Message candidate) {
        for (Ft8Message msg : messages) {
            if (!msg.getMessageText().equals(candidate.getMessageText())) {
                continue;
            }
            if (!sameCallsigns(msg, candidate)) {
                continue;
            }
            if (Math.abs(msg.freq_hz - candidate.freq_hz) > 3.0f) {
                continue;
            }
            if (Math.abs(msg.time_sec - candidate.time_sec) > 0.2f) {
                continue;
            }
            if (msg.snr < candidate.snr) {
                msg.snr = candidate.snr;
            }
            return true;
        }
        return false;
    }

    private static boolean sameCallsigns(Ft8Message left, Ft8Message right) {
        return sameString(left.callsignFrom, right.callsignFrom)
                && sameString(left.callsignTo, right.callsignTo);
    }

    private static boolean sameString(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
