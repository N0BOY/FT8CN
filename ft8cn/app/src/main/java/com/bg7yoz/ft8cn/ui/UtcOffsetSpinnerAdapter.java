package com.bg7yoz.ft8cn.ui;
/**
 * 时间偏移列表。
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

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.util.ArrayList;
import java.util.List;

public class UtcOffsetSpinnerAdapter extends BaseAdapter {
    private List<Integer> offsetTime=new ArrayList<>();
    private Context mContext;

    public UtcOffsetSpinnerAdapter(Context context) {
        mContext=context;
        for (int i = 0; i < 30; i++) {
            offsetTime.add(i*5-75);
        }
    }

    @Override
    public int getCount() {
        return offsetTime.size();
    }

    @Override
    public Object getItem(int i) {
        return offsetTime.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"DefaultLocale", "ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.utc_time_offset_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.serialPortItemTextView);
            textView.setText(String.format(GeneralVariables.getStringFromResource(R.string.offset_time_sec)
                    ,(offsetTime.get(i) /10f)));
        }
        return view;
    }
}
