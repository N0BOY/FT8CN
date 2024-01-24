package com.bg7yoz.ft8cn.ui;
/**
 * 电台型号列表。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.RigNameList;

public class RigNameSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final RigNameList rigNameList;
    public RigNameSpinnerAdapter(Context context) {
        rigNameList=RigNameList.getInstance(context);
        mContext=context;
    }

    public RigNameList.RigName getRigName(int index){
        return rigNameList.getRigNameByIndex(index);
    }
    @Override
    public int getCount() {
        return rigNameList.rigList.size();
    }

    @Override
    public Object getItem(int i) {
        return rigNameList.rigList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams", "UseCompatLoadingForDrawables"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater=LayoutInflater.from(mContext);
        view=_LayoutInflater.inflate(R.layout.rig_name_spinner_item, null);
        if (view!=null){
            TextView textView=view.findViewById(R.id.rigNameItemTextView);
            if (rigNameList.getRigNameInfo(i).startsWith("#")) {
                view.setVisibility(View.GONE);
            }
            ImageView imageView=view.findViewById(R.id.rigLogoImageView);
            if (rigNameList.getRigNameInfo(i).toUpperCase().contains("GUOHE")){
                imageView.setImageDrawable(mContext.getDrawable(R.drawable.guohe_logo));
                imageView.setVisibility(View.VISIBLE);
            }else if (rigNameList.getRigNameInfo(i).toUpperCase().contains("XIEGU")){
                imageView.setImageDrawable(mContext.getDrawable(R.drawable.xiegulogo));
                imageView.setVisibility(View.VISIBLE);
            }else  {
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
            }
            textView.setText(rigNameList.getRigNameInfo(i));
        }
        return view;
    }
}
