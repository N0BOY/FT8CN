package com.bg7yoz.ft8cn.ui;
/**
 * flexRadio的仪表界面。
 * @author BGY70Z
 * @date 2023-03-20
 */

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
import com.bg7yoz.ft8cn.connector.FlexConnector;
import com.bg7yoz.ft8cn.databinding.FragmentFlexRadioInfoBinding;
import com.bg7yoz.ft8cn.flex.FlexMeterList;


public class FlexRadioInfoFragment extends Fragment {
    private static final String TAG = "FlexRadioInfoFragment";
    private MainViewModel mainViewModel;
    private FlexConnector connector;
    private FragmentFlexRadioInfoBinding binding;

    public FlexRadioInfoFragment() {
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
        binding = FragmentFlexRadioInfoBinding.inflate(getLayoutInflater());
        if (mainViewModel.baseRig != null) {
            connector = (FlexConnector) mainViewModel.baseRig.getConnector();
            connector.subAllMeters();
            connector.mutableMeterList.observe(this, new Observer<FlexMeterList>() {
                @Override
                public void onChanged(FlexMeterList meters) {
                    //binding.flexInfoTextView.setText(String.format("%f",meters.alcVal));
                    binding.sMeterRulerView.setValue(meters.sMeterVal);
                    binding.alcMeterRulerView.setValue(meters.alcVal);
                    binding.swrMeterRulerView.setValue(meters.swrVal);
                    binding.powerMeterRulerView.setValue(meters.pwrVal);
                    binding.tempMeterRulerView.setValue(meters.tempCVal);

                    binding.flexInfoTextView.setText(meters.getMeters());
                }
            });
        }


        binding.sMeterRulerView.initVal(-150f, -72f, 10f, 9, 3);
        binding.sMeterRulerView.initLabels("S.Po", "dBm"
                , new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
                , new String[]{"20", "40", ""});

        binding.swrMeterRulerView.initVal(1f, 3f, 20f, 4, 4);
        binding.swrMeterRulerView.initLabels("SWR", ""
                , new String[]{"1", "1.5", "2", "2.5", "3"}
                , new String[]{"", "", "", "∞"});

        binding.alcMeterRulerView.initVal(-150f, 0f, 20f, 3, 3);
        binding.alcMeterRulerView.initLabels("ALC", "dBm"
                , new String[]{"", "", ""}
                , new String[]{"", "", ""});

        binding.powerMeterRulerView.initVal(-0f, 50f, 100f, 5, 5);
        binding.powerMeterRulerView.initLabels("PWR", "W"
                , new String[]{"0", "10", "20", "30", "40", "50"}
                , new String[]{"60", "70", "80", "90", "100"});

        binding.tempMeterRulerView.initVal(-0f, 80f, 100f, 8, 2);
        binding.tempMeterRulerView.initLabels("Temp", "°c"
                , new String[]{"0", "10", "20", "30", "40", "50", "60", "70", "80"}
                , new String[]{"90", "100"});


        binding.maxPowerProgress.setValueColor(getContext().getColor(R.color.power_progress_value));
        binding.tunePowerProgress.setValueColor(getContext().getColor(R.color.power_progress_value));
        binding.maxPowerProgress.setRadarColor(getContext().getColor(R.color.power_progress_radar_value));
        binding.tunePowerProgress.setRadarColor(getContext().getColor(R.color.power_progress_radar_value));

        binding.maxPowerProgress.setAlarmValue(0.51f);


        binding.maxPowerProgress.setPercent(((float) connector.maxRfPower) / 100f);
        binding.maxPowerSeekBar.setProgress(connector.maxRfPower);
        binding.maxTxPowerTextView.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), connector.maxRfPower));

        binding.tunePowerProgress.setPercent(((float) connector.maxTunePower) / 100f);
        binding.tunePowerSeekBar.setProgress(connector.maxTunePower);
        binding.tunePowerProgress.setAlarmValue((connector.maxTunePower / 100f) + 0.01f);
        binding.tunePowerTextView.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.flex_tune_power), connector.maxTunePower));

        binding.maxPowerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.maxPowerProgress.setPercent(i * 1.0f / 100);
                connector.maxRfPower = i;
                connector.setMaxRfPower(i);
                binding.maxTxPowerTextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_max_tx_power), i));

                binding.tunePowerProgress.setAlarmValue((i / 100f) + 0.02f);
                setTuneProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        binding.tunePowerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                connector.maxTunePower=i;
                binding.tunePowerProgress.setPercent(seekBar.getProgress() * 1.0f / 100);
                binding.tunePowerTextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.flex_tune_power)
                        , connector.maxTunePower));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setTuneProgress();
            }
        });


        binding.autStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.startATU();
            }
        });
        binding.pttOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.tuneOnOff(true);
            }
        });
        binding.pttOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connector.tuneOnOff(false);
            }
        });

        return binding.getRoot();
    }

    private void setTuneProgress() {
        //binding.tunePowerSeekBar.setProgress(connector.maxRfPower);
        if (connector.maxTunePower>connector.maxRfPower) {
            connector.maxTunePower =connector.maxRfPower;
        }
        binding.tunePowerTextView.setText(String.format(
                GeneralVariables.getStringFromResource(R.string.flex_tune_power)
                , connector.maxTunePower));
        binding.tunePowerProgress.setPercent(connector.maxTunePower * 1.0f / 100);

        binding.tunePowerSeekBar.setProgress(connector.maxTunePower);
        connector.setMaxTunePower(connector.maxTunePower);

        mainViewModel.databaseOpr.writeConfig("flexMaxRfPower"
                ,String.valueOf(connector.maxRfPower),null);
        mainViewModel.databaseOpr.writeConfig("flexMaxTunePower"
                ,String.valueOf(connector.maxTunePower),null);
    }

}