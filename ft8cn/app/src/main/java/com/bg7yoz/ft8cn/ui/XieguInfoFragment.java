package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.X6100Connector;
import com.bg7yoz.ft8cn.databinding.FragmentXieguInfoBinding;
import com.bg7yoz.ft8cn.x6100.X6100Meters;
import com.bg7yoz.ft8cn.x6100.X6100Radio;


public class XieguInfoFragment extends Fragment {
    private static final String TAG ="XieguInfoFragment";
    private MainViewModel mainViewModel;
    private X6100Connector connector;
    private FragmentXieguInfoBinding binding;
    private X6100Radio xieguRadio;
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener=null;



    public XieguInfoFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = MainViewModel.getInstance(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentXieguInfoBinding.inflate(getLayoutInflater());
        if (mainViewModel.baseRig != null){
            connector = (X6100Connector) mainViewModel.baseRig.getConnector();
            xieguRadio =connector.getXieguRadio();
            binding.xieguInfoTextView.setText(xieguRadio.getModelName());

            //ping 值
            xieguRadio.mutablePing.observe(getViewLifecycleOwner(), new Observer<Long>() {
                @Override
                public void onChanged(Long aLong) {
                    binding.xieguPingValueTextView.setText(
                            String.format(GeneralVariables.getStringFromResource(R.string.xiegu_ping_value)
                                    ,aLong));
                }
            });
            //丢包数量
            xieguRadio.mutableLossPackets.observe(getViewLifecycleOwner(), new Observer<Integer>() {
                @Override
                public void onChanged(Integer integer) {
                    binding.xieguLossValueTextView.setText(String.format(
                            GeneralVariables.getStringFromResource(R.string.x6100_packet_lost)
                            ,integer));
                }
            });
            mainViewModel.baseRig.mutableFrequency.observe(getViewLifecycleOwner(), new Observer<Long>() {
                @Override
                public void onChanged(Long aLong) {
                    binding.xieguFreqValueTextView.setText(String.format(
                            GeneralVariables.getStringFromResource(R.string.xiegu_band_str)
                            ,GeneralVariables.getBandString()));
                }
            });
            xieguRadio.mutableMeters.observe(getViewLifecycleOwner(), new Observer<X6100Meters>() {
                @Override
                public void onChanged(X6100Meters x6100Meters) {
                    binding.xieguMetersValueTextView.setText(x6100Meters.toString());
                    binding.xieguSMeterRulerView.setValue(x6100Meters.sMeter);
                    binding.xieguSwrMeterRulerView.setValue(x6100Meters.swr);
                    binding.xieguPowerMeterRulerView.setValue(x6100Meters.power);
                    binding.xieguAlcMeterRulerView.setValue(x6100Meters.alc);
                    binding.xieguVoltMeterRulerView.setValue(x6100Meters.volt);
                }
            });

        }
        binding.xieguSMeterRulerView.initVal(-130f, -30f, 10f, 9, 3);
        binding.xieguSMeterRulerView.initLabels("S.Po", "dBm"
                , new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
                , new String[]{"20", "40", ""});
        binding.xieguSwrMeterRulerView.initVal(1f, 3f, 8f, 4, 4);
        binding.xieguSwrMeterRulerView.initLabels("SWR", ""
                , new String[]{"1", "1.5", "2", "2.5", "3"}
                , new String[]{"", "", "", "∞"});

        binding.xieguAlcMeterRulerView.initVal(0f, 100f, 100f, 6, 4);
        binding.xieguAlcMeterRulerView.initLabels("ALC", ""
                , new String[]{"0", "10", "20","30","40","50","60"}
                , new String[]{"70", "80", "90","100"});

        binding.xieguPowerMeterRulerView.initVal(-0f, 5f, 10f, 5, 5);
        binding.xieguPowerMeterRulerView.initLabels("PWR", "W"
                , new String[]{"0", "1", "2", "3", "4", "5"}
                , new String[]{"6", "7", "8", "9", "10"});
        binding.xieguVoltMeterRulerView.initVal(-0f, 14f, 16f, 8, 2);
        binding.xieguVoltMeterRulerView.initLabels("Volt", "V"
                , new String[]{"0", "2", "4", "6", "8", "10","12","13","14"}
                , new String[]{ "15", "16"});

        binding.xieguMaxPwrProgress.setValueColor(getContext().getColor(R.color.power_progress_value));
        binding.xieguMaxPwrProgress.setRadarColor(getContext().getColor(R.color.power_progress_radar_value));
        binding.xieguMaxPwrProgress.setAlarmValue(0.99f);


        connector.mutableMaxTxPower.observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                binding.xieguMaxPwrProgress.setPercent( aFloat.floatValue() / 10f);
                binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener(null);
                binding.xiegumaxPowerSeekBar.setProgress(Math.round(aFloat.floatValue()*10));
                binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
                binding.xieguMaxTxPowertextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), Math.round(aFloat.floatValue())));
            }
        });



        binding.xieguSMeterRulerView.setValue(-60f);
        binding.xieguSwrMeterRulerView.setValue(1.1f);
        binding.xieguAlcMeterRulerView.setValue(30f);
        binding.xieguPowerMeterRulerView.setValue(8f);
        binding.xieguVoltMeterRulerView.setValue(12.5f);



        binding.xieguAtuOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                xieguRadio.commandAtuOn();
            }
        });
        binding.xieguAtuOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                xieguRadio.commandAtuOff();
            }
        });
        binding.xieguStartAtuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                xieguRadio.commandAtuStart();
            }
        });

        onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.xieguMaxPwrProgress.setPercent(i * 1.0f / 100);

                connector.setMaxTXPower(i/10);
                binding.xieguMaxTxPowertextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), i/10));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener( onSeekBarChangeListener);



        return binding.getRoot();
    }
}