package com.bg7yoz.ft8cn.ui;
/**
 * 快速切换频率的对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.OperationBand;

public class FreqDialog extends Dialog {
    private static final String TAG = "FreqDialog";

    private MainViewModel mainViewModel;
    private RecyclerView freqRecyclerView;
    private FreqAdapter freqAdapter;
    //private BandsSpinnerAdapter bandsSpinnerAdapter;


    public FreqDialog(Context  context, MainViewModel mainViewModel) {
        super(context, R.style.HelpDialog);
        this.mainViewModel=mainViewModel;

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freq_dialog_layout);
        freqRecyclerView=(RecyclerView) findViewById(R.id.freqDialogRecyclerView);
        freqRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        freqAdapter = new FreqAdapter();
        freqRecyclerView.setAdapter(freqAdapter);

        freqRecyclerView.scrollToPosition(OperationBand.getIndexByFreq(GeneralVariables.band));
//
//        View.OnClickListener onClickListener=new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FreqDialog.this.dismiss();
//            }
//        };

    }


    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //设置对话框的大小，以百分比0.6
        int height=getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width=getWindow().getWindowManager().getDefaultDisplay().getWidth();
        params.height = (int) (height * 0.6);
        if (width>height) {
            params.width = (int) (width * 0.5);
            params.height = (int) (height * 0.6);
        }else {
            params.width= (int) (width * 0.6);
            params.height = (int) (height * 0.5);
        }
        getWindow().setAttributes(params);

    }

   public class FreqAdapter  extends RecyclerView.Adapter<FreqAdapter.FreqHolder>{


       @NonNull
       @Override
       public FreqHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
           LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
           View view = layoutInflater.inflate(R.layout.operation_band_dialog_item, parent, false);
           final FreqHolder freqHolder=new FreqHolder(view);
           return freqHolder;
       }

       @Override
       public void onBindViewHolder(@NonNull FreqHolder holder, int position) {
           holder.band=OperationBand.getBandFreq(position);
           //holder.index=position;
           holder.operationDialogBandItemTextView.setText(OperationBand.getBandInfo(position));
           if (holder.band==GeneralVariables.band){
               holder.operationDialogBandConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_3_style);
           }else {
               holder.operationDialogBandConstraintLayout.setBackgroundResource(R.drawable.calling_list_cell_style);
           }
           holder.operationDialogBandConstraintLayout.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   GeneralVariables.bandListIndex = OperationBand.getIndexByFreq(holder.band);
                   GeneralVariables.band = holder.band;

                   mainViewModel.databaseOpr.getAllQSLCallsigns();//通联成功的呼号读出来
                   mainViewModel.databaseOpr.writeConfig("bandFreq"
                           , String.valueOf(GeneralVariables.band)
                           , null);
                   if (GeneralVariables.controlMode == ControlMode.CAT//CAT、RTS、DTR模式下控制电台
                           || GeneralVariables.controlMode == ControlMode.RTS
                           || GeneralVariables.controlMode == ControlMode.DTR) {
                       //如果在CAT、RTS模式下，修改电台的频率
                       mainViewModel.setOperationBand();
                   }
                   dismiss();
               }
           });


           //OperationBand.bandList.get(i)
       }

       @Override
       public int getItemCount() {
           return OperationBand.bandList.size();
       }

       class  FreqHolder extends RecyclerView.ViewHolder{
            long band;

            TextView operationDialogBandItemTextView;
            ConstraintLayout operationDialogBandConstraintLayout;
            public FreqHolder(@NonNull View itemView) {
                super(itemView);
                operationDialogBandItemTextView=itemView.findViewById(R.id.operationDialogBandItemTextView);
                operationDialogBandConstraintLayout=itemView.findViewById(R.id.operationDialogBandConstraintLayout);
            }
        }
   }


}
