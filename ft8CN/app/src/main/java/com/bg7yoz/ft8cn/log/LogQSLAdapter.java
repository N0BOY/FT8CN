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

import java.util.ArrayList;

public class LogQSLAdapter extends RecyclerView.Adapter<LogQSLAdapter.LogQSLItemHolder> {
    private ArrayList<QSLRecordStr> records=new ArrayList<>();
    private final MainViewModel mainViewModel;
    private final Context context;

    public LogQSLAdapter(Context context,MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.context=context;
    }

    @NonNull
    @Override
    public LogQSLItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.log_qsl_holder_item, parent, false);
        return new LogQSLItemHolder(view);
    }


    @SuppressLint("NotifyDataSetChanged")
    public void setQSLList(ArrayList<QSLRecordStr> list){
        records=list;
        notifyDataSetChanged();
    }

    /**
     * 删除日志
     * @param position 在列表中的位置
     */
    public void deleteRecord(int position){
        mainViewModel.databaseOpr.deleteQSLByID(records.get(position).id);
        records.remove(position);
    }

    public QSLRecordStr getRecord(int position){
        return records.get(position);
    }
    /**
     * 修改手工确认项
     * @param position 列表位置
     * @param b 状态
     */
    public void setRecordIsQSL(int position,boolean b){
        records.get(position).isQSL=b;
        mainViewModel.databaseOpr.setQSLTableIsQSL(b,records.get(position).id);

    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull LogQSLItemHolder holder, int position) {
        holder.record=records.get(position);

        if ((position%2)==0){
            holder.logQSLHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_0_style);
        }else {
            holder.logQSLHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_1_style);
        }
        holder.logQSLCallsignTextView.setText(holder.record.getCall());
        if (!holder.record.getGridsquare().equals("")) {
            holder.logQSOGridTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_grid)
                    , holder.record.getGridsquare()));
        }else {
            holder.logQSOGridTextView.setText("");
        }
        holder.logQSLMyCallsignTextView.setText(holder.record.getStation_callsign());
        if (!holder.record.getMy_gridsquare().equals("")) {
            holder.logQSLMyGridTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_grid)
                    , holder.record.getMy_gridsquare()));
        }else {
            holder.logQSLMyGridTextView.setText("");
        }
        holder.logQSOStartTimeTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_start_time)
                , holder.record.getTime_on()));
        holder.logQSOEndTimeTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_end_time)
                ,holder.record.getTime_off()));
        holder.logQSOReceiveTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_rcvd)
                ,holder.record.getRst_rcvd()));
        holder.logQSOSendTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_sent)
                ,holder.record.getRst_sent()));
        holder.logQSLBandTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_band)
                ,holder.record.getBand()));
        holder.logQSLFreqTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_freq)
                ,holder.record.getFreq()));
        holder.logQSOModeTextView.setText(String.format(GeneralVariables.getStringFromResource(R.string.qsl_mode)
                ,holder.record.getMode()));
        holder.logQSOcCommentTextView.setText(holder.record.getComment());

        if (holder.record.isLotW_QSL){
            holder.logIsQSLTextView.setText(GeneralVariables.getStringFromResource(R.string.qsl_lotw_confirmation));
            holder.logIsQSLTextView.setTextColor(context.getResources().getColor(
                    R.color.is_qsl_text_color));
        }else if(holder.record.isQSL){
            holder.logIsQSLTextView.setText(GeneralVariables.getStringFromResource(R.string.qsl_manual_confirmation));
            holder.logIsQSLTextView.setTextColor(context.getResources().getColor(
                    R.color.is_qsl_text_color));

        }else
        {
            holder.logIsQSLTextView.setText(GeneralVariables.getStringFromResource(R.string.qsl_unconfirmed));
            holder.logIsQSLTextView.setTextColor(context.getResources().getColor(
                    R.color.is_not_qsl_text_color));
        }

        //查呼号的位置
        if (holder.record.where==null){
            setQueryHolderCallsign(holder);
        }else if (holder.record.where.equals("")){
            setQueryHolderCallsign(holder);
        }else {
            holder.logQSLWhereTextView.setText(holder.record.where);
        }
    }

    //查呼号的归属地
    private void setQueryHolderCallsign(@NonNull LogQSLAdapter.LogQSLItemHolder holder) {
        GeneralVariables.callsignDatabase.getCallsignInformation(holder.record.getCall()
                , new OnAfterQueryCallsignLocation() {
                    @Override
                    public void doOnAfterQueryCallsignLocation(CallsignInfo callsignInfo) {
                        holder.logQSLWhereTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (GeneralVariables.isChina) {
                                    holder.logQSLWhereTextView.setText(callsignInfo.CountryNameCN);
                                    holder.record.where = callsignInfo.CountryNameCN;
                                }else {
                                    holder.logQSLWhereTextView.setText(callsignInfo.CountryNameEn);
                                    holder.record.where = callsignInfo.CountryNameEn;
                                }
                            }
                        });

                    }
                });
    }



    @Override
    public int getItemCount() {
        return records.size();
    }

    static class LogQSLItemHolder extends RecyclerView.ViewHolder {
        QSLRecordStr record;
        ConstraintLayout logQSLHolderConstraintLayout;
        TextView logQSLCallsignTextView,logQSOGridTextView,logQSOStartTimeTextView
                ,logQSOEndTimeTextView,logQSOReceiveTextView,logQSOSendTextView
                ,logQSLBandTextView,logQSLFreqTextView,logQSOModeTextView
                ,logQSOcCommentTextView,logQSLMyCallsignTextView
                ,logQSLMyGridTextView,logQSLWhereTextView,logIsQSLTextView;
        public LogQSLItemHolder(@NonNull View itemView) {
            super(itemView);
            logQSLHolderConstraintLayout=itemView.findViewById(R.id.logQSLHolderConstraintLayout) ;
            logQSLCallsignTextView=itemView.findViewById(R.id.logQSLCallsignTextView) ;
            logQSOGridTextView=itemView.findViewById(R.id.logQSOGridTextView) ;
            logQSOStartTimeTextView=itemView.findViewById(R.id.logQSOStartTimeTextView) ;
            logQSOEndTimeTextView=itemView.findViewById(R.id.logQSOEndTimeTextView) ;
            logQSOReceiveTextView=itemView.findViewById(R.id.logQSOReceiveTextView) ;
            logQSOSendTextView=itemView.findViewById(R.id.logQSOSendTextView) ;
            logQSLBandTextView=itemView.findViewById(R.id.logQSLBandTextView) ;
            logQSLFreqTextView=itemView.findViewById(R.id.logQSLFreqTextView) ;
            logQSOModeTextView=itemView.findViewById(R.id.logQSOModeTextView) ;
            logQSOcCommentTextView=itemView.findViewById(R.id.logQSOcCommentTextView) ;
            logQSLMyCallsignTextView=itemView.findViewById(R.id.logQSLMyCallsignTextView) ;
            logQSLMyGridTextView=itemView.findViewById(R.id.logQSLMyGridTextView) ;
            logQSLWhereTextView=itemView.findViewById(R.id.logQSLWhereTextView) ;
            logIsQSLTextView=itemView.findViewById(R.id.logIsQSLTextView) ;

            itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view
                        , ContextMenu.ContextMenuInfo contextMenuInfo) {
                    view.setTag(getAdapterPosition());
                    //添加菜单的参数i1:组，i2:id值，i3:显示顺序
                    if (record.isQSL){
                        contextMenu.add(0,0,0
                                ,String.format(GeneralVariables.getStringFromResource(R.string.qsl_cancel_confirmation)
                                        ,record.getCall())).setActionView(view);
                    }else {
                        contextMenu.add(0,1,0
                                ,String.format(GeneralVariables.getStringFromResource(R.string.qsl_manual_confirmation_s)
                                        ,record.getCall())).setActionView(view);
                    }
                    contextMenu.add(0,2,0
                            ,String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                    ,record.getCall())).setActionView(view);
                }
            });
        }
    }
}
