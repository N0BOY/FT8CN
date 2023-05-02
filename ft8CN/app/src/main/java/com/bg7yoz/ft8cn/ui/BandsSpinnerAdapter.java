package com.bg7yoz.ft8cn.ui;
/**
 * 频段列表界面
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
import com.bg7yoz.ft8cn.database.OperationBand;

public class BandsSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    //private final OperationBand operationBand;
    public BandsSpinnerAdapter(Context context) {
        //operationBand=
                OperationBand.getInstance(context);
        mContext=context;
    }

    @Override
    public int getCount() {
        return OperationBand.bandList.size();
    }

    @Override
    public Object getItem(int i) {
        return OperationBand.bandList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.operation_band_spinner_item, null);
        if (view!=null){
            TextView textView=view.findViewById(R.id.operationBandItemTextView);
            textView.setText(OperationBand.getBandInfo(i));
        }
        return view;
    }
}
