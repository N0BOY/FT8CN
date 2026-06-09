package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.X6100Connector;
import com.bg7yoz.ft8cn.databinding.FragmentXieguInfoBinding;
import com.bg7yoz.ft8cn.x6100.X6100Meters;
import com.bg7yoz.ft8cn.x6100.X6100Radio;


public class XieguInfoFragment extends Fragment {
    private static final String TAG = "XieguInfoFragment";
    private MainViewModel mainViewModel;
    private X6100Connector connector;
    private FragmentXieguInfoBinding binding;
    private X6100Radio xieguRadio;
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = null;

    private final FlexMeterRulerView.OnSetValue onSetSMeterLabel = new FlexMeterRulerView.OnSetValue() {
        @SuppressLint("DefaultLocale")
        @Override
        public String setLabel(float val) {
            return String.format("%.0fdBm", X6100Meters.getMeter_dBm(val));
        }
    };


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
        binding = FragmentXieguInfoBinding.inflate(getLayoutInflater());
        if (mainViewModel.baseRig != null) {
            connector = (X6100Connector) mainViewModel.baseRig.getConnector();
            xieguRadio = connector.getXieguRadio();
            binding.xieguInfoTextView.setText(xieguRadio.getModelName());

            //ping 值
            xieguRadio.mutablePing.observe(getViewLifecycleOwner(), new Observer<Long>() {
                @Override
                public void onChanged(Long aLong) {
                    binding.xieguPingValueTextView.setText(
                            String.format(GeneralVariables.getStringFromResource(R.string.xiegu_ping_value)
                                    , aLong));
                }
            });
            //丢包数量
            xieguRadio.mutableLossPackets.observe(getViewLifecycleOwner(), new Observer<Integer>() {
                @Override
                public void onChanged(Integer integer) {
                    binding.xieguLossValueTextView.setText(String.format(
                            GeneralVariables.getStringFromResource(R.string.x6100_packet_lost)
                            , integer));
                }
            });
            mainViewModel.baseRig.mutableFrequency.observe(getViewLifecycleOwner(), new Observer<Long>() {
                @Override
                public void onChanged(Long aLong) {
                    binding.xieguFreqValueTextView.setText(String.format(
                            GeneralVariables.getStringFromResource(R.string.xiegu_band_str)
                            , GeneralVariables.getBandString()));
                }
            });
            xieguRadio.mutableMeters.observe(getViewLifecycleOwner(), new Observer<X6100Meters>() {
                @Override
                public void onChanged(X6100Meters x6100Meters) {
                    binding.xieguMetersValueTextView.setText(x6100Meters.toString());
                    binding.xieguSMeterRulerView.setValue(x6100Meters.sMeter, onSetSMeterLabel);
                    binding.xieguSwrMeterRulerView.setValue(x6100Meters.swr, null);
                    binding.xieguPowerMeterRulerView.setValue(x6100Meters.power, null);
                    binding.xieguAlcMeterRulerView.setValue(x6100Meters.alc, null);
                    binding.xieguVoltMeterRulerView.setValue(x6100Meters.volt, null);
                }
            });

        }
        if (connector == null || xieguRadio == null) {
            return binding.getRoot();
        }
        //binding.xieguSMeterRulerView.initVal(-150f, -75f, 0f, 9, 3);
        binding.xieguSMeterRulerView.initVal(0f, 120f, 242f, 9, 3);
        binding.xieguSMeterRulerView.initLabels("S.Po", "dBm"
                , new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
                , new String[]{"+20", "+40", "+60"});
        //6100的swr,分为4段，1～1.5，1.5～2.0，2.0～3.0，3.0～无穷大（实际值是25.5）
        binding.xieguSwrMeterRulerView.initVal(1f, 3f, 25.5f, 4, 4);
        binding.xieguSwrMeterRulerView.initLabels("SWR", ""
                , new String[]{"1", "1.5", "2", "2.5", "3"}
                , new String[]{"", "", "", "∞"});

        //ALC值的原始值（0～255）在127±50都是最佳线性度范围，转换到0～120的线性范围就是36.17到83.19
        binding.xieguAlcMeterRulerView.initVal(0f, 100f, 120f, 8, 2);
        binding.xieguAlcMeterRulerView.initLabels("ALC", ""
                , new String[]{"0", "10", "20", "30", "40", "50", "60","70","80"}
                , new String[]{"100", "120"});

        binding.xieguPowerMeterRulerView.initVal(-0f, 5f, 10f, 5, 5);
        binding.xieguPowerMeterRulerView.initLabels("PWR", "W"
                , new String[]{"0", "1", "2", "3", "4", "5"}
                , new String[]{"6", "7", "8", "9", "10"});
        binding.xieguVoltMeterRulerView.initVal(0f, 13.8f, 16f, 6, 2);
        binding.xieguVoltMeterRulerView.initLabels("Volt", "V"
                , new String[]{"0", "2", "4", "6", "8", "10", "12"}
                , new String[]{"14", "16"});

        binding.xieguMaxPwrProgress.setValueColor(getContext().getColor(R.color.power_progress_value));
        binding.xieguMaxPwrProgress.setRadarColor(getContext().getColor(R.color.power_progress_radar_value));
        binding.xieguMaxPwrProgress.setAlarmValue(0.99f);


        connector.mutableMaxTxPower.observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                binding.xieguMaxPwrProgress.setPercent(aFloat / 10f);
                binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener(null);
                binding.xiegumaxPowerSeekBar.setProgress(Math.round(aFloat * 10));
                binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
                binding.xieguMaxTxPowertextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), Math.round(aFloat)));
            }
        });


        binding.xieguSMeterRulerView.setValue(0f, onSetSMeterLabel);
        binding.xieguSwrMeterRulerView.setValue(1.1f, null);
        binding.xieguAlcMeterRulerView.setValue(30f, null);
        binding.xieguPowerMeterRulerView.setValue(8f, null);
        binding.xieguVoltMeterRulerView.setValue(12.5f, null);


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

                connector.setMaxTXPower(i / 10);
                binding.xieguMaxTxPowertextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), i / 10));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        binding.xiegumaxPowerSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);


        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
