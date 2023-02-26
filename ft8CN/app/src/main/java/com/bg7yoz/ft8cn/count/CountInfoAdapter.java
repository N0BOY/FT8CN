package com.bg7yoz.ft8cn.count;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class CountInfoAdapter extends RecyclerView.Adapter<CountInfoAdapter.CountInfoItemHolder>{
    private static final String TAG="CountInfoAdapter";
    private final ArrayList<CountDbOpr.CountInfo> countInfoList;
    private final Context context;

    public CountInfoAdapter(Context context,ArrayList<CountDbOpr.CountInfo> countInfoList) {
        this.countInfoList = countInfoList;
        this.context=context;
    }

    @NonNull
    @Override
    public CountInfoItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view ;
        switch (viewType){
            case CountDbOpr.ChartPie:
                view= layoutInflater.inflate(R.layout.count_info_pie_item, parent, false);
                break;
            case CountDbOpr.ChartBar:
                view= layoutInflater.inflate(R.layout.count_info_bar_item, parent, false);
                break;
            default:
                view= layoutInflater.inflate(R.layout.count_info_none_item, parent, false);
                break;
        }

        return new CountInfoItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CountInfoItemHolder holder, int position) {
        holder.countInfo=countInfoList.get(position);
        holder.countInfoTextView.setText(holder.countInfo.info);
        holder.countTitleTextView.setText(holder.countInfo.title);
        switch (holder.countInfo.chartType){
            case Pie:
                drawPie(holder);
                break;
            case Bar:
                drawBar(holder);
                break;
            default:
                addDetail(holder);
        }

    }

    private void addDetail(@NonNull CountInfoItemHolder holder){
        if (holder.detailLayout==null) return;
        holder.detailLayout.removeAllViews();
        for (int i = 0; i <holder.countInfo.values.size() ; i++) {
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                    , LinearLayout.LayoutParams.WRAP_CONTENT);
            View view=LayoutInflater.from(context).inflate(R.layout.count_detail_item,null);
            view.setLayoutParams(lp);
            if (i%2==0) {
                view.setBackgroundResource(R.drawable.count_detail_item_0_style);
            }else {
                view.setBackgroundResource(R.drawable.count_detail_item_1_style);
            }

            TextView tv1=view.findViewById(R.id.countDetailTextView);
            tv1.setText(holder.countInfo.values.get(i).name);
            TextView tv2=view.findViewById(R.id.countDetailValueTextView);
            tv2.setText(String.valueOf(holder.countInfo.values.get(i).value));

            holder.detailLayout.addView(view);

        }
    }

    private void drawBar(@NonNull CountInfoItemHolder holder){
        addDetail(holder);

        BarChart barChart;
        barChart=(BarChart) holder.countChart;
        barChart.getLegend().setEnabled(false);//不显示图例
        barChart.getXAxis().setDrawLabels(true);//显示X轴标签
        barChart.getXAxis().setDrawGridLines(false);//不显示X网格线
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.TOP);
        barChart.getXAxis().setTextColor(context.getResources().getColor(R.color.text_view_color));
        barChart.getLegend().setTextColor(context.getResources().getColor(R.color.text_view_color));
        barChart.getAxisLeft().setTextColor(context.getResources().getColor(R.color.text_view_color));



        List<BarEntry> barEntries=new ArrayList<>();
        for (int i = 0; i <holder.countInfo.values.size() ; i++) {
            BarEntry barEntry = new BarEntry((float) (i)
                    , (float) holder.countInfo.values.get(i).value);
            barEntries.add(barEntry);
        }

        BarDataSet dataSet = new BarDataSet(barEntries, null);
        dataSet.setColors(new int[]{R.color.char_bar_1  },context);

        BarData barData = new BarData(dataSet);
        barData.setValueTextColor(context.getResources().getColor(R.color.text_view_color));
        barChart.setData(barData);
        //显示标签信息
        IndexAxisValueFormatter formatter=new IndexAxisValueFormatter(){
            @Override
            public String getFormattedValue(float value) {
                if (holder.countInfo.values.size()>(int) value) {
                    return holder.countInfo.values.get((int) value).name;
                }else {
                    return "";
                }
            }
        };
        barChart.getXAxis().setValueFormatter(formatter);

        barChart.setDescription(null);

        barChart.animateY(500);
        barChart.invalidate();

    }

    private void drawPie(@NonNull CountInfoItemHolder holder){
        PieChart countPieChart;
        countPieChart=(PieChart) holder.countChart;
        countPieChart.getLegend().setTextColor(context.getResources().getColor(R.color.text_view_color));

        List<PieEntry> pieEntries=new ArrayList<>();
        for (int i = 0; i <holder.countInfo.values.size() ; i++) {
            pieEntries.add(new PieEntry(holder.countInfo.values.get(i).value,holder.countInfo.values.get(i).name));
        }
        PieDataSet dataSet=new PieDataSet(pieEntries,null);
        dataSet.setColors(new int[]{R.color.char_bar_1
                ,R.color.char_bar_2
                ,R.color.char_bar_3
                ,R.color.char_bar_4
                ,R.color.char_bar_5
                ,R.color.char_bar_6
                ,R.color.char_bar_7
                ,R.color.char_bar_8
                ,R.color.char_bar_9
                ,R.color.char_bar_10
                ,R.color.char_bar_11
                ,R.color.char_bar_12
                ,R.color.char_bar_13
                ,R.color.char_bar_14
                ,R.color.char_bar_15
                ,R.color.char_bar_16
            },context);
        PieData pieData=new PieData(dataSet);
        pieData.setValueTextColor(context.getResources().getColor(R.color.text_view_color));

        countPieChart.setData(pieData);
        Description description = new Description();
        description.setText(holder.countInfo.title);
        countPieChart.setDescription(null);

        countPieChart.animateY(500);

        countPieChart.invalidate();
    }

    @Override
    public int getItemCount() {
        return countInfoList.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (countInfoList.get(position).chartType){
            case Bar:return CountDbOpr.ChartBar;
            case Pie:return CountDbOpr.ChartPie;
            case Line:return CountDbOpr.ChartLine;
            case None:return CountDbOpr.ChartNone;
        }
        return super.getItemViewType(position);
    }

    static class CountInfoItemHolder extends RecyclerView.ViewHolder{
        TextView countInfoTextView,countTitleTextView;
        CountDbOpr.CountInfo countInfo;
        Object countChart;
        LinearLayout detailLayout;
        ImageButton countDownImageButton,countUpImageButton;

        public CountInfoItemHolder(@NonNull View itemView) {
            super(itemView);
            countInfoTextView = itemView.findViewById(R.id.countInfoTextView);
            countTitleTextView = itemView.findViewById(R.id.countTitleTextView);
            countDownImageButton = itemView.findViewById(R.id.countDownImageButton);
            countUpImageButton = itemView.findViewById(R.id.countUpImageButton);
            detailLayout = itemView.findViewById(R.id.countDetailLayout);
            countChart = itemView.findViewById(R.id.countChart);

            if (countUpImageButton!=null) {
                countUpImageButton.setVisibility(View.GONE);
                countUpImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (detailLayout!=null){
                            detailLayout.setVisibility(View.GONE);
                            countUpImageButton.setVisibility(View.GONE);
                            countDownImageButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            if (countDownImageButton!=null){
                countDownImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (detailLayout!=null){
                            detailLayout.setVisibility(View.VISIBLE);
                            countDownImageButton.setVisibility(View.GONE);
                            countUpImageButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            if (detailLayout!=null){
                detailLayout.setVisibility(View.GONE);
                countDownImageButton.setVisibility(View.VISIBLE);
            }

        }
    }
}
