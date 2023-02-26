package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ft8transmit.FunctionOfTransmit;

public class FunctionOrderSpinnerAdapter extends BaseAdapter {
    private Context mContext;
    private MainViewModel mainViewModel;

    public FunctionOrderSpinnerAdapter(Context context, MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mainViewModel.ft8TransmitSignal.functionList.size();
    }

    @Override
    public Object getItem(int i) {
        return mainViewModel.ft8TransmitSignal.functionList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater _LayoutInflater = LayoutInflater.from(mContext);
        FunctionOfTransmit function;
        function = mainViewModel.ft8TransmitSignal.functionList.get(i);

        view = _LayoutInflater.inflate(R.layout.function_order_spinner_item, null);
        if (view != null) {
            TextView messageTextView = (TextView) view.findViewById(R.id.functionOrderItemTextView);
            messageTextView.setText(function.getFunctionMessage());
            TextView numTextView = (TextView) view.findViewById(R.id.functionNumItemTextView);
            numTextView.setText(String.valueOf(function.getFunctionOrder()));

//            ImageView completedImageView = (ImageView) view.findViewById(R.id.functionCompletedImageView);

            ImageView currentImageView=(ImageView) view.findViewById(R.id.currentOrderImageView);
            if (function.isCurrentOrder()){
                currentImageView.setVisibility(View.VISIBLE);
            }else {
                currentImageView.setVisibility(View.INVISIBLE);
            }

//            if (function.isCompleted()) {
//                completedImageView.setVisibility(View.VISIBLE);
//            } else {
//                completedImageView.setVisibility(View.GONE);
//            }

        }
        return view;
    }

}
