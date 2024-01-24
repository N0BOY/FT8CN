package com.bg7yoz.ft8cn.ui;
/**
 * 串口校验位列表界面
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

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;


public class SerialParityBitsSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final int[] parityBits= {0,1,2,3,4};
    private final String[] parityStr={GeneralVariables.getStringFromResource(R.string.serial_parity_none)
            ,GeneralVariables.getStringFromResource(R.string.serial_parity_odd)
            ,GeneralVariables.getStringFromResource(R.string.serial_parity_even)
            ,GeneralVariables.getStringFromResource(R.string.serial_parity_mark)
            ,GeneralVariables.getStringFromResource(R.string.serial_parity_space)
        };
    public SerialParityBitsSpinnerAdapter(Context context) {
        mContext=context;
    }

    @Override
    public int getCount() {
        return parityBits.length;
    }

    @Override
    public Object getItem(int i) {
        return parityBits[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.serial_parity_bits_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.serialParityBitsItemTextView);
            //textView.setText(String.valueOf(parityBits[i]));
            textView.setText(parityStr[i]);
        }
        return view;
    }
    public int getPosition(int i){
        for (int j = 0; j < parityBits.length; j++) {
            if (parityBits[j]==i){
                return j;
            }
        }
        return 2;
    }
    public int getValue(int position){
        return parityBits[position];
    }
}
