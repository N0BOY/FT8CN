package com.bg7yoz.ft8cn.ui;
/**
 * 串口停止位列表界面
 * @author BGY70Z
 * @date 2024-01-03
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.R;


public class SerialStopBitsSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final int[] stopBits= {1,3,2};
    private final String[] stopBitsStr= {"1","1.5","2"};
    public SerialStopBitsSpinnerAdapter(Context context) {
        mContext=context;
    }

    @Override
    public int getCount() {
        return stopBits.length;
    }

    @Override
    public Object getItem(int i) {
        return stopBits[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.serial_stop_bits_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.serialStopBitsItemTextView);
            textView.setText(stopBitsStr[i]);
        }
        return view;
    }
    public int getPosition(int i){
        for (int j = 0; j < stopBits.length; j++) {
            if (stopBits[j]==i){
                return j;
            }
        }
        return 0;
    }
    public int getValue(int position){
        return stopBits[position];
    }
}
