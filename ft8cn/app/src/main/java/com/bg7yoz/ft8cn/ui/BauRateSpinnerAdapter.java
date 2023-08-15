package com.bg7yoz.ft8cn.ui;
/**
 * 波特率列表界面
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.R;


public class BauRateSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final int[] bauRates= {4800,9600,14400,19200,38400,43000,56000,57600,115200};
    public BauRateSpinnerAdapter(Context context) {
        mContext=context;
    }

    @Override
    public int getCount() {
        return bauRates.length;
    }

    @Override
    public Object getItem(int i) {
        return bauRates[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.bau_rate_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.bauRateItemTextView);
            textView.setText(String.valueOf(bauRates[i]));
        }
        return view;
    }
    public int getPosition(int i){
        for (int j = 0; j < bauRates.length; j++) {
            if (bauRates[j]==i){
                return j;
            }
        }
        return 2;
    }
    public int getValue(int position){
        return bauRates[position];
    }
}
