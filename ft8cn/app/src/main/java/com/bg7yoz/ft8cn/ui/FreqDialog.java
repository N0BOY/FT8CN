package com.bg7yoz.ft8cn.ui;
/**
 * 快速切换频率的对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.app.Dialog;
import android.text.InputType;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.OperationBand;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;

public class FreqDialog extends Dialog {
    private static final String TAG = "FreqDialog";

    private MainViewModel mainViewModel;
    private RecyclerView freqRecyclerView;
    private FreqAdapter freqAdapter;
    //private BandsSpinnerAdapter bandsSpinnerAdapter;
    private Button manualFreqButton;


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

        manualFreqButton = findViewById(R.id.manualFreqButton);
        manualFreqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showManualFrequencyDialog();
            }
        });
//
//        View.OnClickListener onClickListener=new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FreqDialog.this.dismiss();
//            }
//        };

    }

    private void showManualFrequencyDialog() {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.manual_frequency_hint);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.manual_frequency)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    Long hz = parseFrequency(raw);
                    if (hz == null) {
                        return;
                    }
                    applyManualFrequency(hz);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditFrequencyDialog(int position, long currentHz) {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.manual_frequency_hint);
        input.setText(String.valueOf(currentHz / 1000000d));

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.edit_frequency)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    Long hz = parseFrequency(raw);
                    if (hz == null) {
                        return;
                    }
                    applyEditedFrequency(position, hz);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private Long parseFrequency(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            Toast.makeText(getContext(), R.string.frequency_required, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            long hz = (value < 1000000d) ? Math.round(value * 1000000d) : Math.round(value);
            if (hz <= 0) {
                Toast.makeText(getContext(), R.string.invalid_frequency, Toast.LENGTH_SHORT).show();
                return null;
            }
            return hz;
        } catch (NumberFormatException ex) {
            Toast.makeText(getContext(), R.string.invalid_frequency, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void applyManualFrequency(long hz) {
        GeneralVariables.bandListIndex = OperationBand.getIndexByFreq(hz);
        GeneralVariables.band = hz;

        mainViewModel.databaseOpr.getAllQSLCallsigns();
        mainViewModel.databaseOpr.writeConfig("bandFreq", String.valueOf(GeneralVariables.band), null);
        if (GeneralVariables.controlMode == ControlMode.CAT
                || GeneralVariables.controlMode == ControlMode.RTS
                || GeneralVariables.controlMode == ControlMode.DTR) {
            mainViewModel.setOperationBand();
        }
        freqAdapter.notifyDataSetChanged();
        freqRecyclerView.scrollToPosition(GeneralVariables.bandListIndex);
        dismiss();
    }

    private void applyEditedFrequency(int position, long hz) {
        if (position < 0 || position >= OperationBand.bandList.size()) {
            return;
        }
        OperationBand.Band band = OperationBand.bandList.get(position);
        band.band = hz;
        band.waveLength = BaseRigOperation.getMeterFromFreq(hz);

        if (GeneralVariables.bandListIndex == position || GeneralVariables.band == band.band) {
            GeneralVariables.bandListIndex = position;
            GeneralVariables.band = hz;
            mainViewModel.databaseOpr.writeConfig("bandFreq", String.valueOf(GeneralVariables.band), null);
            if (GeneralVariables.controlMode == ControlMode.CAT
                    || GeneralVariables.controlMode == ControlMode.RTS
                    || GeneralVariables.controlMode == ControlMode.DTR) {
                mainViewModel.setOperationBand();
            }
        }
        freqAdapter.notifyDataSetChanged();
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

            holder.operationDialogBandConstraintLayout.setOnLongClickListener(view -> {
                if (position < 0 || position >= OperationBand.bandList.size()) {
                    return true;
                }
                OperationBand.Band band = OperationBand.bandList.get(position);
                if (band.marked) {
                    Toast.makeText(getContext(), R.string.default_band_locked, Toast.LENGTH_SHORT).show();
                    return true;
                }

                CharSequence[] options = new CharSequence[]{
                        getContext().getString(R.string.edit_frequency),
                        getContext().getString(R.string.delete_frequency)
                };
                new AlertDialog.Builder(getContext())
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                showEditFrequencyDialog(position, band.band);
                            } else {
                                confirmDeleteFrequency(position);
                            }
                        })
                        .show();
                return true;
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

    private void confirmDeleteFrequency(int position) {
        if (position < 0 || position >= OperationBand.bandList.size()) {
            return;
        }
        new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(R.string.delete_frequency)
                .setMessage(R.string.confirm_delete_frequency)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteFrequency(position))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteFrequency(int position) {
        if (OperationBand.bandList.size() <= 1) {
            Toast.makeText(getContext(), R.string.delete_frequency_minimum, Toast.LENGTH_SHORT).show();
            return;
        }
        OperationBand.bandList.remove(position);
        if (GeneralVariables.bandListIndex == position) {
            GeneralVariables.bandListIndex = 0;
            GeneralVariables.band = OperationBand.bandList.get(0).band;
            mainViewModel.databaseOpr.writeConfig("bandFreq", String.valueOf(GeneralVariables.band), null);
            if (GeneralVariables.controlMode == ControlMode.CAT
                    || GeneralVariables.controlMode == ControlMode.RTS
                    || GeneralVariables.controlMode == ControlMode.DTR) {
                mainViewModel.setOperationBand();
            }
        } else if (GeneralVariables.bandListIndex > position) {
            GeneralVariables.bandListIndex -= 1;
        }
        freqAdapter.notifyDataSetChanged();
    }


}
