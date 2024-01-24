package com.bg7yoz.ft8cn.ui;
/**
 * 串口数据位列表界面
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


public class SerialDataBitsSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final int[] dataBits= {8,7,6,5};
    public SerialDataBitsSpinnerAdapter(Context context) {
        mContext=context;
    }

    @Override
    public int getCount() {
        return dataBits.length;
    }

    @Override
    public Object getItem(int i) {
        return dataBits[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.serial_data_bits_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.serialDataBitsItemTextView);
            textView.setText(String.valueOf(dataBits[i]));
        }
        return view;
    }
    public int getPosition(int i){
        for (int j = 0; j < dataBits.length; j++) {
            if (dataBits[j]==i){
                return j;
            }
        }
        return 0;
    }
    public int getValue(int position){
        return dataBits[position];
    }
}
