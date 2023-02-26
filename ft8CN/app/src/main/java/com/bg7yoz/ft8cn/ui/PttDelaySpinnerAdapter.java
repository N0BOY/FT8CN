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

public class PttDelaySpinnerAdapter extends BaseAdapter {
    private final List<Integer> delayTime=new ArrayList<>();
    private final Context mContext;

    public PttDelaySpinnerAdapter(Context context) {
        mContext=context;
        for (int i = 0; i < 20; i++) {
            delayTime.add(i*10);
        }
    }

    @Override
    public int getCount() {
        return delayTime.size();
    }

    @Override
    public Object getItem(int i) {
        return delayTime.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"DefaultLocale", "ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.ptt_delay_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.pttDelayItemTextView);
            textView.setText(String.format(
                    GeneralVariables.getStringFromResource(R.string.milliseconds),delayTime.get(i)));
        }
        return view;
    }
}
