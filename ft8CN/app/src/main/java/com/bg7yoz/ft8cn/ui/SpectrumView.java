package com.bg7yoz.ft8cn.ui;
/**
 * 包含瀑布图、频率柱状图、标尺的自定义控件。
 * @author BGY70Z
 * @date 2023-03-20
 */

import static android.view.MotionEvent.ACTION_UP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.timer.UtcTimer;

public class SpectrumView extends ConstraintLayout {
    private MainViewModel mainViewModel;
    private ColumnarView columnarView;
    private Switch controlDeNoiseSwitch;
    private Switch controlShowMessageSwitch;
    private WaterfallView waterfallView;
    private RulerFrequencyView rulerFrequencyView;
    private Fragment fragment;


    private int frequencyLineTimeOut = 0;//画频率线的时间量

    static {
        System.loadLibrary("ft8cn");
    }



    public SpectrumView(@NonNull Context context) {
        super(context);
    }

    public SpectrumView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View view = (View) View.inflate(context, R.layout.spectrum_layout,this);
    }


    @SuppressLint("ClickableViewAccessibility")
    public void run(MainViewModel mainViewModel , Fragment fragment){
        this.mainViewModel = MainViewModel.getInstance(null);
        this.fragment=fragment;
        columnarView=findViewById(R.id.controlColumnarView);
        controlDeNoiseSwitch=findViewById(R.id.controlDeNoiseSwitch);
        waterfallView=findViewById(R.id.controlWaterfallView);
        rulerFrequencyView=findViewById(R.id.controlRulerFrequencyView);
        controlShowMessageSwitch=findViewById(R.id.controlShowMessageSwitch);


        setDeNoiseSwitchState();
        setMarkMessageSwitchState();

        rulerFrequencyView.setFreq(Math.round(GeneralVariables.getBaseFrequency()));
        mainViewModel.currentMessages=null;




        //原始频谱开关
        controlDeNoiseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.deNoise = b;
                setDeNoiseSwitchState();
                mainViewModel.currentMessages=null;
            }
        });
        //标记消息开关
        controlShowMessageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.markMessage = b;
                setMarkMessageSwitchState();
            }
        });

        //当声音变化，画频谱
        mainViewModel.spectrumListener.mutableDataBuffer.observe(fragment.getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] ints) {
                drawSpectrum(ints);
            }
        });


        //观察解码的变化
        mainViewModel.mutableIsDecoding.observe(fragment.getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                waterfallView.setDrawMessage(!aBoolean);//false说明解码完毕
            }
        });

        //触摸频谱时的动作
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                frequencyLineTimeOut = 60;//显示频率线的时长：60*0.16

                waterfallView.setTouch_x(Math.round(motionEvent.getX()));
                columnarView.setTouch_x(Math.round(motionEvent.getX()));



                if (!mainViewModel.ft8TransmitSignal.isSynFrequency()
                        && (waterfallView.getFreq_hz() > 0)
                        && (motionEvent.getAction() == ACTION_UP)
                ) {//如果时异频发射
                    mainViewModel.databaseOpr.writeConfig("freq",
                            String.valueOf(waterfallView.getFreq_hz()),
                            null);
                    mainViewModel.ft8TransmitSignal.setBaseFrequency(
                            (float) waterfallView.getFreq_hz());


                    rulerFrequencyView.setFreq(waterfallView.getFreq_hz());

                    fragment.requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastMessage.show(String.format(
                                    GeneralVariables.getStringFromResource(R.string.sound_frequency_is_set_to)
                                    , waterfallView.getFreq_hz()),true);
                        }
                    });
                }
                return false;
            }
        };

        waterfallView.setOnTouchListener(touchListener);
        columnarView.setOnTouchListener(touchListener);


    }
    private void setDeNoiseSwitchState() {
        if (mainViewModel==null) return;
        controlDeNoiseSwitch.setChecked(mainViewModel.deNoise);
        if (mainViewModel.deNoise) {
            controlDeNoiseSwitch.setText(GeneralVariables.getStringFromResource(R.string.de_noise));
        } else {
            controlDeNoiseSwitch.setText(GeneralVariables.getStringFromResource(R.string.raw_spectrum_data));
        }
    }
    private void setMarkMessageSwitchState(){
        if (mainViewModel.markMessage) {
            controlShowMessageSwitch.setText(GeneralVariables.getStringFromResource(R.string.markMessage));
        } else {
            controlShowMessageSwitch.setText(GeneralVariables.getStringFromResource(R.string.unMarkMessage));
        }
    }




    public void drawSpectrum(float[] buffer) {
        if (buffer.length <= 0) {
            return;
        }
        int[] fft = new int[buffer.length / 2];
        if (mainViewModel.deNoise) {
            getFFTDataFloat(buffer, fft);
        } else {
            getFFTDataRawFloat(buffer, fft);
        }
        frequencyLineTimeOut--;
        if (frequencyLineTimeOut < 0) {
            frequencyLineTimeOut = 0;
        }
        //达到显示的时长，就取取消掉频率线
        if (frequencyLineTimeOut == 0) {
            waterfallView.setTouch_x(-1);
            columnarView.setTouch_x(-1);
        }
        columnarView.setWaveData(fft);
        if (mainViewModel.markMessage) {//是否标记消息
            waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), mainViewModel.currentMessages);
        } else {
            waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), null);
        }
    }


    public native void getFFTData(int[] data, int fftData[]);
    public native void getFFTDataFloat(float[] data, int fftData[]);

    public native void getFFTDataRaw(int[] data, int fftData[]);
    public native void getFFTDataRawFloat(float[] data, int fftData[]);


}
