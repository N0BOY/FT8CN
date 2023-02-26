package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import com.bg7yoz.ft8cn.GeneralVariables;

import java.util.ArrayList;

public class ToastMessage {
    private static final String TAG="ToastMessage";
    //private static Activity activity;
    private static ToastMessage toastMessage=null;
    private static final ArrayList<String> debugList=new ArrayList<>();
    public static ToastMessage getInstance(){
        if (toastMessage==null){
            toastMessage=new ToastMessage();
        }
        return toastMessage;
    }

    //public ToastMessage(Activity activity) {
    //    this.activity = activity;
    //}
    public ToastMessage() {
        //this.activity = activity;
    }

    public static void show(String message){
        addDebugInfo(message);
    }
    public static synchronized void show(String message,boolean clearMessage){
        if (clearMessage) {
            debugList.clear();
        }
        show(message);
    }
    @SuppressLint("DefaultLocale")
    private static synchronized void addDebugInfo(String s){
        if (debugList.size()>5){
        //if (debugList.size()>20){
            debugList.remove(0);
        }
        final String info=s;
        debugList.add(info);
        GeneralVariables.mutableDebugMessage.postValue(getDebugMessage());

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <debugList.size() ; i++) {
                    if (debugList.get(i).equals(info)){
                        debugList.remove(i);
                        GeneralVariables.mutableDebugMessage.postValue(getDebugMessage());
                        break;
                    }
                }
            }
        //},10000);
        },5000);


    }
    private static synchronized String getDebugMessage(){
        StringBuilder builder=new StringBuilder();
        for (int i = 0; i <debugList.size() ; i++) {
            if (i==debugList.size()-1){
                builder.append(debugList.get(i));
            }else {
                builder.append(debugList.get(i)+"\n");
            }
        }
        return builder.toString();
    }

}
