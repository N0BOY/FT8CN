package com.bg7yoz.ft8cn;
/**
 * 呼号的哈希码列表。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import java.util.HashMap;

public class MessageHashMap extends HashMap<Long,String> {
    private static final String TAG = "MessageHashMap";

    /**
     * 添加呼号和哈希码到列表
     *
     * @param hashCode 哈希码
     * @param callsign 呼号
     * @return false说明已经存在了
     */
    public synchronized void addHash(long hashCode, String callsign) {
        //if (callsign.length()<2){return;}
        //if (){return;}
        if (callsign.equals("CQ")||callsign.equals("QRZ")||callsign.equals("DE")){
            return;
        }
        if (hashCode == 0 || checkHash(hashCode)|| callsign.charAt(0) == '<') {
            return;
        }
        Log.d(TAG, String.format("addHash: callsign:%s ,hash:%x",callsign,hashCode ));
        put(hashCode,callsign);
    }

    //检查是否存在这个hash码
    public boolean checkHash(long hashCode) {
       return get(hashCode)!=null;
//        for (HashStruct hash : this) {
//            if (hash.hashCode == hashCode) {
//                return true;
//            }
//        }
//        return false;
    }

    //通过哈希码查呼号
    public synchronized String getCallsign(long[] hashCode) {
        for (long l : hashCode) {
            if (checkHash(l)) {
                return String.format("<%s>", get(l));
            }
        }
        return "<...>";
    }
}
