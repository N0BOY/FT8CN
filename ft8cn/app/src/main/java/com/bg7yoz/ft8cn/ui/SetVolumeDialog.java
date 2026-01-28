package com.bg7yoz.ft8cn.ui;
/**
 * 设置信号输出强度的对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;

public class SetVolumeDialog extends Dialog {
    private static final String TAG = "SetVolumeDialog";
    private TextView volumeValueMessage;
    private SeekBar volumeSeekBar;
    private final MainViewModel mainViewModel;
    private VolumeProgress volumeProgress;
    private TextView micInputGainValueMessage;
    private SeekBar micInputGainSeekBar;
    private VolumeProgress micInputGainProgress;

    public SetVolumeDialog(@NonNull Context context, MainViewModel mainViewModel) {
        super(context);
        this.mainViewModel = mainViewModel;
    }


    @SuppressLint({"NotifyDataSetChanged", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_volume_dialog);
        volumeValueMessage = (TextView) findViewById(R.id.volumeValueMessage);
        volumeSeekBar = (SeekBar) findViewById(R.id.volumeSeekBar);
        volumeProgress=(VolumeProgress) findViewById(R.id.volumeProgress);
        volumeProgress.setAlarmValue(1.1f);
        volumeProgress.setValueColor(getContext().getColor(R.color.volume_progress_value));//白色
        setVolumeText(GeneralVariables.volumePercent);
        volumeSeekBar.setProgress((int) (GeneralVariables.volumePercent*100));
        
        // Mic input gain controls
        micInputGainValueMessage = (TextView) findViewById(R.id.micInputGainValueMessage);
        micInputGainSeekBar = (SeekBar) findViewById(R.id.micInputGainSeekBar);
        micInputGainProgress = (VolumeProgress) findViewById(R.id.micInputGainProgress);
        micInputGainProgress.setAlarmValue(1.1f);
        micInputGainProgress.setValueColor(getContext().getColor(R.color.volume_progress_value));//白色
        setMicInputGainText(GeneralVariables.micInputGain);
        micInputGainSeekBar.setProgress((int) (GeneralVariables.micInputGain * 100));

        GeneralVariables.mutableVolumePercent.observeForever(new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                setVolumeText(aFloat);
            }
        });
        
        GeneralVariables.mutableMicInputGain.observeForever(new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                setMicInputGainText(aFloat);
            }
        });


        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                GeneralVariables.volumePercent=i/100f;
                GeneralVariables.mutableVolumePercent.postValue(i/100f);
                mainViewModel.databaseOpr.writeConfig("volumeValue",String.valueOf(i),null);
                if (mainViewModel.baseRig!=null){
                    if (mainViewModel.baseRig.getConnector()!=null) {
                        mainViewModel.baseRig.getConnector().setRFVolume(i);
                        Log.e(TAG,String.format("set volume:%d",i));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        
        // Mic input gain seekbar listener
        micInputGainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                GeneralVariables.micInputGain = i / 100f;
                GeneralVariables.mutableMicInputGain.postValue(i / 100f);
                mainViewModel.databaseOpr.writeConfig("micInputGain", String.valueOf(i), null);
                Log.d(TAG, String.format("set mic input gain: %d%%", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void setVolumeText(float vol){
        volumeValueMessage.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.volume_percent)
                , vol*100f));
        volumeProgress.setPercent(vol);

    }
    
    private void setMicInputGainText(float gain){
        micInputGainValueMessage.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.mic_input_gain_percent)
                , gain*100f));
        // Normalize gain for display (0.25 to 2.0 maps to 0.0 to 1.0 for progress bar)
        // Display range: 25% to 200%, so normalize: (gain - 0.25) / (2.0 - 0.25)
        float normalizedGain = (gain - 0.25f) / 1.75f;
        if (normalizedGain < 0.0f) normalizedGain = 0.0f;
        if (normalizedGain > 1.0f) normalizedGain = 1.0f;
        micInputGainProgress.setPercent(normalizedGain);
    }

    /**
     * 把配置信息写到数据库
     *
     * @param Value 值
     */
    private void writeConfig(String Value) {
        mainViewModel.databaseOpr.writeConfig("volumeValue", Value, null);
    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //设置对话框的大小，以百分比0.6
        int height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        if (width > height) {
            params.width = (int) (width * 0.7);
            //params.height = (int) (height * 0.6);
        } else {
            params.width = (int) (width * 0.95);
            //params.height = (int) (height * 0.5);
        }
        getWindow().setAttributes(params);
    }


}
