package com.bg7yoz.ft8cn.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.callsign.CallsignInfo;
import com.bg7yoz.ft8cn.callsign.OnAfterQueryCallsignLocation;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;

import java.util.ArrayList;

public class LogCallsignAdapter extends RecyclerView.Adapter<LogCallsignAdapter.LogCallsignItemHolder> {
    private ArrayList<QSLCallsignRecord> callsignRecords=new ArrayList<>();
    private final MainViewModel mainViewModel;
    private final Context context;

    public LogCallsignAdapter(Context context,MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.context=context;
    }

    @NonNull
    @Override
    public LogCallsignItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.log_callsign_holder_item, parent, false);
        return new LogCallsignItemHolder(view);
    }




    /**
     * 获取记录
     * @param position 位置
     * @return 记录
     */
    public QSLCallsignRecord getRecord(int position){
        return callsignRecords.get(position);
    }
    /**
     * 返回查询结题
     * @param records 记录
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setQSLCallsignList(ArrayList<QSLCallsignRecord> records){
        callsignRecords=records;
        notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull LogCallsignItemHolder holder, int position) {
        if ((position%2)==0){
            holder.logCallSignQSLHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_0_style);
        }else {
            holder.logCallSignQSLHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_1_style);
        }
        holder.record=callsignRecords.get(position);


        if (holder.record.isQSL||holder.record.isLotW_QSL){
            holder.callsignQSOIsQSLextView.setText(GeneralVariables.getStringFromResource(R.string.confirmed));
            holder.callsignQSOIsQSLextView.setTextColor(context.getResources().getColor(
                    R.color.is_qsl_text_color));
        }else {
            holder.callsignQSOIsQSLextView.setText(GeneralVariables.getStringFromResource(R.string.unconfirmed));
            holder.callsignQSOIsQSLextView.setTextColor(context.getResources().getColor(
                    R.color.is_not_qsl_text_color));
        }
        if (holder.record.isLotW_QSL){
            holder.isQSLModeDistTextView.setText(GeneralVariables.getStringFromResource(R.string.lotw_confirmation));
        }else if (holder.record.isQSL){
            holder.isQSLModeDistTextView.setText(GeneralVariables.getStringFromResource(R.string.manual_confirmation));
        }else {
            holder.isQSLModeDistTextView.setText("");
        }

        holder.callsignLogTextView.setText(holder.record.getCallsign());

        holder.callsignQSOLastTimeTextView.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.log_last_time)
                ,holder.record.getLastTime()));
        if (holder.record.getGrid().length()>0) {
            holder.callsignQSOGridTextView.setText(String.format(
                    GeneralVariables.getStringFromResource(R.string.log_grid)
                    , holder.record.getGrid()));
        }else {
            holder.callsignQSOGridTextView.setText("");
        }

        holder.callsignQSLBandTextView.setText(holder.record.getBand());
        holder.callsignQSOModeTextView.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.log_mode)
                ,holder.record.getMode()));
        //计算距离
        holder.callsignQSLDistTextView.setText(MaidenheadGrid.getDistStr(
                GeneralVariables.getMyMaidenheadGrid()
                , holder.record.getGrid()));

        if (holder.record.where==null){
            setQueryHolderCallsign(holder);
        }else {holder.callsignQSOWhereTextView.setText(holder.record.where);}
        holder.callsignDxccZoneTextView.setText(holder.record.dxccStr);
    }


    //查呼号的归属地
    private void setQueryHolderCallsign(@NonNull LogCallsignAdapter.LogCallsignItemHolder holder) {
        GeneralVariables.callsignDatabase.getCallsignInformation(holder.record.getCallsign()
                , new OnAfterQueryCallsignLocation() {
                    @Override
                    public void doOnAfterQueryCallsignLocation(CallsignInfo callsignInfo) {
                        holder.callsignQSOWhereTextView.post(new Runnable() {
                            @SuppressLint("DefaultLocale")
                            @Override
                            public void run() {
                                if (GeneralVariables.isChina) {
                                    holder.callsignQSOWhereTextView.setText(callsignInfo.CountryNameCN);
                                    holder.record.where = callsignInfo.CountryNameCN;
                                }else {
                                    holder.callsignQSOWhereTextView.setText(callsignInfo.CountryNameEn);
                                    holder.record.where = callsignInfo.CountryNameEn;
                                }
                                holder.record.dxccStr=String.format("DXCC : %s, ITU : %d, CQZONE : %d"
                                        ,callsignInfo.DXCC,callsignInfo.ITUZone,callsignInfo.CQZone);
                                holder.callsignDxccZoneTextView.setText(holder.record.dxccStr);
                            }
                        });
                    }
                });
    }



    @Override
    public int getItemCount() {
        return callsignRecords.size();
    }

    static class LogCallsignItemHolder extends  RecyclerView.ViewHolder{
        QSLCallsignRecord record;
        ConstraintLayout logCallSignQSLHolderConstraintLayout;
        TextView callsignLogTextView,callsignQSOLastTimeTextView,callsignQSLBandTextView
                ,callsignQSOGridTextView,callsignQSOModeTextView,callsignQSLDistTextView
                ,callsignQSOWhereTextView,callsignQSOIsQSLextView
                ,isQSLModeDistTextView,callsignDxccZoneTextView;
        public LogCallsignItemHolder(@NonNull View itemView) {
            super(itemView);
            logCallSignQSLHolderConstraintLayout=itemView.findViewById(R.id.logCallSignQSLHolderConstraintLayout);
            callsignLogTextView=itemView.findViewById(R.id.callsignLogTextView);
            callsignQSOLastTimeTextView=itemView.findViewById(R.id.callsignQSOLastTimeTextView);
            callsignQSLBandTextView=itemView.findViewById(R.id.callsignQSLBandTextView);
            callsignQSOGridTextView=itemView.findViewById(R.id.callsignQSOGridTextView);
            callsignQSOModeTextView=itemView.findViewById(R.id.callsignQSOModeTextView);
            callsignQSLDistTextView=itemView.findViewById(R.id.callsignQSLDistTextView);
            callsignQSOWhereTextView=itemView.findViewById(R.id.callsignQSOWhereTextView);
            callsignQSOIsQSLextView=itemView.findViewById(R.id.callsignQSOIsQSLextView);
            isQSLModeDistTextView=itemView.findViewById(R.id.isQSLModeDistTextView);
            callsignDxccZoneTextView=itemView.findViewById(R.id.callsignDxccZoneTextView);

            itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view
                        , ContextMenu.ContextMenuInfo contextMenuInfo) {
                    view.setTag(getAdapterPosition());
                    contextMenu.add(0,2,0
                            ,String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                    ,record.getCallsign())).setActionView(view);
                }
            });
        }
    }
}
