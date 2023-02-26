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

public class NoReplyLimitSpinnerAdapter extends BaseAdapter {
    private final List<Integer> noReplyCount=new ArrayList<>();
    private final Context mContext;

    public NoReplyLimitSpinnerAdapter(Context context) {
        mContext=context;
        for (int i = 0; i <= 30; i++) {
            noReplyCount.add(i);
        }
    }

    @Override
    public int getCount() {
        return noReplyCount.size();
    }

    @Override
    public Object getItem(int i) {
        return noReplyCount.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"DefaultLocale", "ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.no_reply_limit_spinner_item, null);
        if (view!=null){
            TextView textView=(TextView)view.findViewById(R.id.noReplyLimitCountItemTextView);
            if (i==0){
                textView.setText(GeneralVariables.getStringFromResource(R.string.ignore));
            }
            else {
                textView.setText(String.format(GeneralVariables.getStringFromResource(R.string.times), i));
            }
        }
        return view;
    }
}
