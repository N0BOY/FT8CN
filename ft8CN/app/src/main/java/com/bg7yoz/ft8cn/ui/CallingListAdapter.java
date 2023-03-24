package com.bg7yoz.ft8cn.ui;
/**
 * 消息列表Adapter。使用此Adapter有解码界面、呼叫界面、网格追踪界面。
 * 不同周期背景不同。为了区分，共有4种背景颜色。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.ArrayList;

public class CallingListAdapter extends RecyclerView.Adapter<CallingListAdapter.CallingListItemHolder> {
    public enum ShowMode{CALLING_LIST,MY_CALLING,TRACKER}
    private static final String TAG = "CallingListAdapter";
    private final MainViewModel mainViewModel;
    private final ArrayList<Ft8Message> ft8MessageArrayList;
    private final Context context;

    //private boolean isCallingList = true;
    private final ShowMode showMode;
    private View.OnClickListener onItemClickListener;

    private final View.OnCreateContextMenuListener menuListener=new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

            //view.setTag(ft8Message);//把消息对象传递给上一级界面
            int postion= (int) view.getTag();
            if (postion==-1) return;
            if (postion>ft8MessageArrayList.size()-1) return;
            Ft8Message ft8Message=ft8MessageArrayList.get(postion);

            //添加菜单的参数i1:组，i2:id值，i3:显示顺序
            if (!ft8Message.getCallsignTo().contains("...")//目标不能是自己
                    && !ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
                    && !(ft8Message.i3==0&&ft8Message.n3==0)) {
                if (!ft8Message.checkIsCQ()) {
                    if (showMode==ShowMode.CALLING_LIST) {//在消息列表中就可以显示这个菜单了
                        contextMenu.add(0, 0, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.tracking_receiver)
                                        , ft8Message.getCallsignTo(), ft8Message.toWhere))
                                .setActionView(view);
                    }
                    if (!mainViewModel.ft8TransmitSignal.isSynFrequency()) {//如果同频率的话，会与发送者同频，会影响发送者！！！
                        contextMenu.add(0, 1, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.calling_receiver)
                                        , ft8Message.getCallsignTo(), ft8Message.toWhere))
                                .setActionView(view);
                    }
                    //说明是对我呼叫，加上回复菜单
                    if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)) {
                        contextMenu.add(0, 4, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.reply_to)
                                        , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                                .setActionView(view);

                    }
                    if (showMode!=ShowMode.TRACKER) {
                        contextMenu.add(0, 5, 0
                                , String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                        , ft8Message.getCallsignTo())).setActionView(view);
                    }

                }
            }

            if (!ft8Message.getCallsignFrom().contains("...")
                    && !ft8Message.getCallsignFrom().equals(GeneralVariables.myCallsign)
                    && !(ft8Message.i3==0&&ft8Message.n3==0)) {
                if (showMode==ShowMode.CALLING_LIST) {//在消息列表中就可以显示这个菜单了
                    contextMenu.add(1, 2, 0, String.format(
                                    GeneralVariables.getStringFromResource(R.string.tracking)
                                    , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                            .setActionView(view);
                }
                contextMenu.add(1, 3, 0, String.format(
                                GeneralVariables.getStringFromResource(R.string.calling)
                                , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                        .setActionView(view);
                if (showMode!=ShowMode.TRACKER) {
                    contextMenu.add(1, 6, 0
                            , String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                    , ft8Message.getCallsignFrom())).setActionView(view);
                }
            }

        }
    };



    public CallingListAdapter(Context context, MainViewModel mainViewModel
            , ArrayList<Ft8Message> messages, ShowMode showMode) {
        this.mainViewModel = mainViewModel;
        this.context = context;
        //this.isCallingList = isCallingList;
        this.showMode=showMode;
        ft8MessageArrayList = messages;
    }

    @NonNull
    @Override
    public CallingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.call_list_holder_item, parent, false);
        return new CallingListItemHolder(view,onItemClickListener,menuListener);
    }

    /**
     * 删除消息
     *
     * @param position 在列表中的位置
     */
    public void deleteMessage(int position) {
        if (position >= 0) {
            ft8MessageArrayList.remove(position);
        }
    }

    public Ft8Message getMessageByPosition(int position){
        if (ft8MessageArrayList==null) return null;
        if (position<0) return null;
        if (position>ft8MessageArrayList.size()-1) return null;
        return ft8MessageArrayList.get(position);
    }

    /**
     * 通过holder获取消息
     *
     * @param holder holder
     * @return ft8message
     */
    public Ft8Message getMessageByViewHolder(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() == -1) {
            return null;
        }
        return ft8MessageArrayList.get(holder.getAdapterPosition());
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull CallingListItemHolder holder, int position) {
        holder.callListHolderConstraintLayout.setTag(position);//设置layout的tag，为了识别消息的定位
        holder.ft8Message = ft8MessageArrayList.get(position);
        holder.showMode = showMode;//确定是消息列表还是关注消息的列表
        holder.isSyncFreq = mainViewModel.ft8TransmitSignal.isSynFrequency();//如果同频发射，就不显示呼叫接收者

        holder.callingUtcTextView.setText(UtcTimer.getTimeHHMMSS(holder.ft8Message.utcTime));
        //时序，包括颜色,
        holder.callingListSequenceTextView.setText(holder.ft8Message.getSequence() == 0 ? "0" : "1");

        if (showMode==ShowMode.MY_CALLING) {//在呼叫界面
            holder.callingListSequenceTextView.setTextColor(context.getColor(R.color.follow_call_text_color));
        }

        //根据1分钟内的4个时序区分颜色
        switch (holder.ft8Message.getSequence4()) {
            case 0:
                holder.callListHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_0_style);
                break;
            case 1:
                holder.callListHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_1_style);
                break;
            case 2:
                holder.callListHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_2_style);
                break;
            case 3:
                holder.callListHolderConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_3_style);
                break;
        }

        holder.callingListIdBTextView.setText(holder.ft8Message.getdB());
        //时间偏移，如果超过1.0秒，-0.05秒，红色提示
        holder.callListDtTextView.setText(holder.ft8Message.getDt());
        if (holder.ft8Message.time_sec > 1.0f || holder.ft8Message.time_sec < -0.05) {
            holder.callListDtTextView.setTextColor(context.getResources().getColor(
                    R.color.message_in_my_call_text_color));
        } else {
            holder.callListDtTextView.setTextColor(context.getResources().getColor(
                    R.color.text_view_color));
        }


        holder.callingListFreqTextView.setText(holder.ft8Message.getFreq_hz());

        //查是不是通联过的呼号，获取是否存在holder.otherBandIsQso中
        setQueryHolderQSL_Callsign(holder);

        //是否有与我呼号有关的消息
        if (holder.ft8Message.inMyCall()) {
            holder.callListMessageTextView.setTextColor(context.getResources().getColor(
                    R.color.message_in_my_call_text_color));
        } else if (holder.otherBandIsQso) {
            //设置在别的波段通联过的消息颜色
            holder.callListMessageTextView.setTextColor(context.getResources().getColor(
                    R.color.fromcall_is_qso_text_color));
        } else {
            holder.callListMessageTextView.setTextColor(context.getResources().getColor(
                    R.color.message_text_color));
        }


        holder.callListMessageTextView.setText(holder.ft8Message.getMessageText());

        //载波频率
        holder.bandItemTextView.setText(BaseRigOperation.getFrequencyStr(holder.ft8Message.band));
        //计算距离
        holder.callingListDistTextView.setText(MaidenheadGrid.getDistStr(
                GeneralVariables.getMyMaidenheadGrid()
                , holder.ft8Message.getMaidenheadGrid(mainViewModel.databaseOpr)));
        holder.callingListCallsignToTextView.setText("");//被呼叫者
        holder.callingListCallsignFromTextView.setText("");//呼叫者

        //消息类型
        holder.callingListCommandIInfoTextView.setText(holder.ft8Message.getCommandInfo());
        if (holder.ft8Message.i3 == 1 || holder.ft8Message.i3 == 2) {
            holder.callingListCommandIInfoTextView.setTextColor(context.getResources().getColor(
                    R.color.text_view_color));
        } else {
            holder.callingListCommandIInfoTextView.setTextColor(context.getResources().getColor(
                    R.color.message_in_my_call_text_color));
        }

        //设置是否CQ的颜色
        if (holder.ft8Message.checkIsCQ()) {
            holder.callListMessageTextView.setBackgroundResource(R.color.textview_cq_color);
            holder.ft8Message.toWhere = "";
        } else {
            holder.callListMessageTextView.setBackgroundResource(R.color.textview_none_color);
        }


        if (holder.ft8Message.fromWhere != null) {
            holder.callingListCallsignFromTextView.setText(holder.ft8Message.fromWhere);
        } else {
            holder.callingListCallsignFromTextView.setText("");
        }

        if (holder.ft8Message.toWhere != null) {
            holder.callingListCallsignToTextView.setText(holder.ft8Message.toWhere);
        } else {
            holder.callingListCallsignToTextView.setText("");
        }

        //给没有通联过的分区打标记
        setToDxcc(holder);
        setFromDxcc(holder);


        //查询呼号归属地，为防止占用太多运算资源，当from为空是再做查询的工作
//        if (holder.ft8Message.fromWhere == null) {
//            setQueryHolderCallsign(holder);//查询呼号归属地
//        }

        if (holder.ft8Message.freq_hz <= 0.01f) {//这是发射
            //holder.callingListSequenceTextView.setVisibility(View.GONE);
            holder.callingListIdBTextView.setVisibility(View.GONE);
            holder.callListDtTextView.setVisibility(View.GONE);
            holder.callingListFreqTextView.setText("TX");
            holder.bandItemTextView.setVisibility(View.GONE);
            holder.callingListDistTextView.setVisibility(View.GONE);
            holder.callingListCommandIInfoTextView.setVisibility(View.GONE);
            holder.callingUtcTextView.setVisibility(View.GONE);
            holder.callingListCallsignToTextView.setVisibility(View.GONE);
            holder.callingListCallsignFromTextView.setVisibility(View.GONE);
            holder.dxccToImageView.setVisibility(View.GONE);
            holder.ituToImageView.setVisibility(View.GONE);
            holder.cqToImageView.setVisibility(View.GONE);
            holder.dxccFromImageView.setVisibility(View.GONE);
            holder.ituFromImageView.setVisibility(View.GONE);
            holder.cqFromImageView.setVisibility(View.GONE);
        } else {
            //holder.callingListSequenceTextView.setVisibility(View.VISIBLE);
            holder.callingListIdBTextView.setVisibility(View.VISIBLE);
            holder.callListDtTextView.setVisibility(View.VISIBLE);
            holder.bandItemTextView.setVisibility(View.VISIBLE);
            holder.callingListDistTextView.setVisibility(View.VISIBLE);
            holder.callingListCommandIInfoTextView.setVisibility(View.VISIBLE);
            holder.callingUtcTextView.setVisibility(View.VISIBLE);
            holder.callingListCallsignToTextView.setVisibility(View.VISIBLE);
            holder.callingListCallsignFromTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setFromDxcc(@NonNull CallingListItemHolder holder) {

        if (holder.ft8Message.fromDxcc && holder.ft8Message.freq_hz > 0.01f) {
            holder.dxccFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.dxccFromImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.fromCq && holder.ft8Message.freq_hz > 0.01f) {
            holder.cqFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.cqFromImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.fromItu && holder.ft8Message.freq_hz > 0.01f) {
            holder.ituFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.ituFromImageView.setVisibility(View.GONE);
        }
    }

    private void setToDxcc(@NonNull CallingListItemHolder holder) {
        if (holder.ft8Message.toDxcc && holder.ft8Message.freq_hz > 0.01f) {
            holder.dxccToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.dxccToImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.toCq && holder.ft8Message.freq_hz > 0.01f) {
            holder.cqToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.cqToImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.toItu && holder.ft8Message.freq_hz > 0.01f) {
            holder.ituToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.ituToImageView.setVisibility(View.GONE);
        }
    }

    //检查是不是通联过的呼号
    private void setQueryHolderQSL_Callsign(@NonNull CallingListItemHolder holder) {
        //查是不是在本波段内通联成功过的呼号
        if (GeneralVariables.checkQSLCallsign(holder.ft8Message.getCallsignFrom())) {//如果在数据库中，划线
            holder.callListMessageTextView.setPaintFlags(
                    holder.callListMessageTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {//如果不在数据库中，去掉划线
            holder.callListMessageTextView.setPaintFlags(
                    holder.callListMessageTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        holder.otherBandIsQso = GeneralVariables.checkQSLCallsign_OtherBand(holder.ft8Message.getCallsignFrom());
    }

    @Override
    public int getItemCount() {
        return ft8MessageArrayList.size();
    }

    public void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    static class CallingListItemHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "CallingListItemHolder";
        ConstraintLayout callListHolderConstraintLayout;
        TextView callingListIdBTextView, callListDtTextView, callingListFreqTextView,
                callListMessageTextView, callingListDistTextView, callingListSequenceTextView,
                callingListCallsignFromTextView, callingListCallsignToTextView, callingListCommandIInfoTextView,
                bandItemTextView, callingUtcTextView;
        ImageView dxccToImageView, ituToImageView, cqToImageView, dxccFromImageView, ituFromImageView, cqFromImageView;
        public Ft8Message ft8Message;
        //boolean showFollow;
        ShowMode showMode;
        boolean isSyncFreq;
        boolean otherBandIsQso = false;


        public CallingListItemHolder(@NonNull View itemView, View.OnClickListener listener
                    ,View.OnCreateContextMenuListener menuListener) {
            super(itemView);
            callListHolderConstraintLayout = itemView.findViewById(R.id.callListHolderConstraintLayout);
            callingListIdBTextView = itemView.findViewById(R.id.callingListIdBTextView);
            callListDtTextView = itemView.findViewById(R.id.callListDtTextView);
            callingListFreqTextView = itemView.findViewById(R.id.callingListFreqTextView);
            callListMessageTextView = itemView.findViewById(R.id.callListMessageTextView);
            callingListDistTextView = itemView.findViewById(R.id.callingListDistTextView);
            callingListSequenceTextView = itemView.findViewById(R.id.callingListSequenceTextView);
            callingListCallsignFromTextView = itemView.findViewById(R.id.callingListCallsignFromTextView);
            callingListCallsignToTextView = itemView.findViewById(R.id.callToItemTextView);
            callingListCommandIInfoTextView = itemView.findViewById(R.id.callingListCommandIInfoTextView);
            bandItemTextView = itemView.findViewById(R.id.bandItemTextView);
            callingUtcTextView = itemView.findViewById(R.id.callingUtcTextView);

            dxccToImageView = itemView.findViewById(R.id.dxccToImageView);
            ituToImageView = itemView.findViewById(R.id.ituToImageView);
            cqToImageView = itemView.findViewById(R.id.cqToImageView);
            dxccFromImageView = itemView.findViewById(R.id.dxccFromImageView);
            ituFromImageView = itemView.findViewById(R.id.ituFromImageView);
            cqFromImageView = itemView.findViewById(R.id.cqFromImageView);

            dxccToImageView.setVisibility(View.GONE);
            ituToImageView.setVisibility(View.GONE);
            cqToImageView.setVisibility(View.GONE);
            dxccFromImageView.setVisibility(View.GONE);
            ituFromImageView.setVisibility(View.GONE);
            cqFromImageView.setVisibility(View.GONE);
            itemView.setTag(-1);
            itemView.setOnClickListener(listener);
            itemView.setOnCreateContextMenuListener(menuListener);

        }


    }
}
