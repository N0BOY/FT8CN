package com.bg7yoz.ft8cn.ui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.util.ArrayList;
import java.util.List;

public class LaunchSupervisionSpinnerAdapter extends BaseAdapter {
    private final List<Integer> timeOutList=new ArrayList<>();
    private final Context mContext;

    public LaunchSupervisionSpinnerAdapter(Context context) {
        mContext=context;
        timeOutList.add(0);
        for (int i = 1; i <= 10; i++) {
            timeOutList.add(i*10-5);
        }
    }
    public static int getTimeOut(int index){
        if (index==0) return 0;
       return  ((index+1) * 10-5) * 60 * 1000;
    }

    @Override
    public int getCount() {
        return timeOutList.size();
    }

    @Override
    public Object getItem(int i) {
        return timeOutList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"DefaultLocale", "ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.launch_supervision_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.timeOutTextView);
            if (i==0){
                textView.setText(
                        GeneralVariables.getStringFromResource(R.string.launch_supervision_ignore));
            }else {
                textView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.minutes), timeOutList.get(i)));
            }
        }
        return view;
    }

    public int getPosition(int timeOut){
        if (timeOut==0){
            return 0;
        }else if (timeOut<5*60*1000) {
            return 1;
        }else {
            return ((timeOut-5*60*1000)/60/1000/10);
        }

    }
}
