package com.bg7yoz.ft8cn.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentAdvancedSettingsBinding;
import com.bg7yoz.ft8cn.timer.UtcTimer;

/**
 * Advanced settings tab: Audio + Time + Decode + Operating
 * Contains settings for fine-tuning and operating preferences
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
public class AdvancedSettingsFragment extends Fragment {

    private FragmentAdvancedSettingsBinding binding;
    private MainViewModel mainViewModel;
    private UtcOffsetSpinnerAdapter utcOffsetSpinnerAdapter;
    private NtpServerSpinnerAdapter ntpServerSpinnerAdapter;
    private LaunchSupervisionSpinnerAdapter launchSupervisionSpinnerAdapter;
    private NoReplyLimitSpinnerAdapter noReplyLimitSpinnerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentAdvancedSettingsBinding.inflate(inflater, container, false);
        mainViewModel = MainViewModel.getInstance(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeAdapters();
        initializeControls();
        setupListeners();
    }

    private void initializeAdapters() {
        utcOffsetSpinnerAdapter = new UtcOffsetSpinnerAdapter(requireContext());
        ntpServerSpinnerAdapter = new NtpServerSpinnerAdapter(requireContext());
        launchSupervisionSpinnerAdapter = new LaunchSupervisionSpinnerAdapter(requireContext());
        noReplyLimitSpinnerAdapter = new NoReplyLimitSpinnerAdapter(requireContext());

        binding.utcOffsetSpinner.setAdapter(utcOffsetSpinnerAdapter);
        binding.ntpServerSpinner.setAdapter(ntpServerSpinnerAdapter);
        binding.launchSupervisionSpinner.setAdapter(launchSupervisionSpinnerAdapter);
        binding.noReplyLimitSpinner.setAdapter(noReplyLimitSpinnerAdapter);
    }

    private void initializeControls() {
        // Audio Configuration
        if (GeneralVariables.audioOutput32Bit) {
            binding.audioBit32Radio.setChecked(true);
        } else {
            binding.audioBit16Radio.setChecked(true);
        }

        switch (GeneralVariables.audioSampleRate) {
            case 12000:
                binding.audioRate12kRadio.setChecked(true);
                break;
            case 24000:
                binding.audioRate24kRadio.setChecked(true);
                break;
            case 48000:
            default:
                binding.audioRate48kRadio.setChecked(true);
                break;
        }

        // Time Synchronization - UtcTimer.delay is in 100ths of seconds
        int utcOffsetIndex = (UtcTimer.delay / 100 + 75) / 5;
        if (utcOffsetIndex >= 0 && utcOffsetIndex < 30) {
            binding.utcOffsetSpinner.setSelection(utcOffsetIndex);
        }

        // NTP Server
        int ntpPosition = ntpServerSpinnerAdapter.findServerPosition(GeneralVariables.ntpServer);
        binding.ntpServerSpinner.setSelection(ntpPosition);
        if (ntpPosition == ntpServerSpinnerAdapter.getCount() - 1) {
            binding.customNtpServerEdit.setVisibility(View.VISIBLE);
            binding.customNtpServerEdit.setText(GeneralVariables.ntpServer);
        }

        // Decode Settings
        if (GeneralVariables.deepDecodeMode) {
            binding.decodeModeDeepRadio.setChecked(true);
        } else {
            binding.decodeModeFastRadio.setChecked(true);
        }

        binding.decodeOverrunSwitch.setChecked(GeneralVariables.decode_overrun_toast);

        if (GeneralVariables.simpleCallItemMode) {
            binding.messageModeSimpleRadio.setChecked(true);
        } else {
            binding.messageModeStandardRadio.setChecked(true);
        }

        // Operating Behavior
        binding.followCQSwitch.setChecked(GeneralVariables.autoFollowCQ);
        binding.callingAddsToFollowSwitch.setChecked(GeneralVariables.callingAddsToFollowList);
        binding.autoCallFollowSwitch.setChecked(GeneralVariables.autoCallFollow);
        binding.skipMyGridSwitch.setChecked(GeneralVariables.skip_my_grid_when_responding);

        binding.launchSupervisionSpinner.setSelection(
                launchSupervisionSpinnerAdapter.getPosition(GeneralVariables.launchSupervision));
        binding.noReplyLimitSpinner.setSelection(GeneralVariables.noReplyLimit);
        binding.excludeCallsignsEdit.setText(GeneralVariables.getExcludeCallsigns());

        // Logging
        binding.saveSWLSwitch.setChecked(GeneralVariables.saveSWLMessage);
        binding.saveSWLQSOSwitch.setChecked(GeneralVariables.saveSWL_QSO);

        // Monitoring
        binding.swrSwitch.setChecked(GeneralVariables.swr_switch_on);
        binding.alcSwitch.setChecked(GeneralVariables.alc_switch_on);
    }

    private void setupListeners() {
        // Audio Bit Depth
        binding.audioBitDepthRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            GeneralVariables.audioOutput32Bit = (checkedId == R.id.audioBit32Radio);
            writeConfig("audioBits", GeneralVariables.audioOutput32Bit ? "1" : "0");
        });

        // Audio Sample Rate
        binding.audioSampleRateRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int rate = 48000;
            if (checkedId == R.id.audioRate12kRadio) {
                rate = 12000;
            } else if (checkedId == R.id.audioRate24kRadio) {
                rate = 24000;
            } else if (checkedId == R.id.audioRate48kRadio) {
                rate = 48000;
            }
            GeneralVariables.audioSampleRate = rate;
            writeConfig("audioRate", String.valueOf(rate));
        });

        // UTC Offset
        binding.utcOffsetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                UtcTimer.delay = position * 500 - 7500;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // NTP Server Spinner
        binding.ntpServerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String server = ntpServerSpinnerAdapter.getServerAddress(position);
                if (server.isEmpty()) {
                    binding.customNtpServerEdit.setVisibility(View.VISIBLE);
                } else {
                    binding.customNtpServerEdit.setVisibility(View.GONE);
                    GeneralVariables.ntpServer = server;
                    writeConfig("ntpServer", server);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Custom NTP Server
        binding.customNtpServerEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (binding.customNtpServerEdit.getVisibility() == View.VISIBLE) {
                    GeneralVariables.ntpServer = s.toString();
                    writeConfig("ntpServer", s.toString());
                }
            }
        });

        // Sync Time Button
        binding.syncTimeButton.setOnClickListener(v -> {
            ToastMessage.show(getString(R.string.sync_time_now));
        });

        // Decode Mode
        binding.decodeModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            GeneralVariables.deepDecodeMode = (checkedId == R.id.decodeModeDeepRadio);
            writeConfig("deepMode", GeneralVariables.deepDecodeMode ? "1" : "0");
        });

        // Decode Overrun Alert
        binding.decodeOverrunSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.decode_overrun_toast = isChecked;
            writeConfig("decodeOverrunToast", isChecked ? "1" : "0");
        });

        // Message Mode
        binding.messageModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            GeneralVariables.simpleCallItemMode = (checkedId == R.id.messageModeSimpleRadio);
            writeConfig("msgMode", GeneralVariables.simpleCallItemMode ? "1" : "0");
        });

        // Auto Follow CQ
        binding.followCQSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.autoFollowCQ = isChecked;
            writeConfig("autoFollowCQ", isChecked ? "1" : "0");
        });

        // Calling Adds to Follow List
        binding.callingAddsToFollowSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.callingAddsToFollowList = isChecked;
            writeConfig("callingAddsToFollowList", isChecked ? "1" : "0");
        });

        // Auto Call Follow
        binding.autoCallFollowSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.autoCallFollow = isChecked;
            writeConfig("autoCallFollow", isChecked ? "1" : "0");
        });

        // Skip My Grid
        binding.skipMyGridSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.skip_my_grid_when_responding = isChecked;
            writeConfig("skipMyGridWhenResponding", isChecked ? "1" : "0");
        });

        // Launch Supervision
        binding.launchSupervisionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.launchSupervision = LaunchSupervisionSpinnerAdapter.getTimeOut(position);
                writeConfig("launchSupervision", String.valueOf(GeneralVariables.launchSupervision));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // No Reply Limit
        binding.noReplyLimitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.noReplyLimit = position;
                writeConfig("noReplyLimit", String.valueOf(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Exclude Callsigns
        binding.excludeCallsignsEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.addExcludedCallsigns(s.toString());
                writeConfig("excludedCallsigns", GeneralVariables.getExcludeCallsigns());
            }
        });

        // Save SWL Messages
        binding.saveSWLSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.saveSWLMessage = isChecked;
            writeConfig("saveSWL", isChecked ? "1" : "0");
        });

        // Save SWL QSOs
        binding.saveSWLQSOSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.saveSWL_QSO = isChecked;
            writeConfig("saveSWLQSO", isChecked ? "1" : "0");
        });

        // SWR Switch
        binding.swrSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.swr_switch_on = isChecked;
            writeConfig("swrSwitch", isChecked ? "1" : "0");
        });

        // ALC Switch
        binding.alcSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.alc_switch_on = isChecked;
            writeConfig("alcSwitch", isChecked ? "1" : "0");
        });

        // Help buttons
        binding.audioBitDepthImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "audio_output_help.txt", true);
        });

        binding.utcOffsetImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "timeoffset.txt", true);
        });

        binding.decodeModeImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "decode_help.txt", true);
        });

        binding.messageModeImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "messageMode.txt", true);
        });

        binding.followCQImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "auto_follow_help.txt", true);
        });

        binding.callingAddsToFollowImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "calling_adds_to_follow_list_help.txt", true);
        });

        binding.skipMyGridImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "skip_my_grid_when_responding_help.txt", true);
        });

        binding.launchSupervisionImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "launch_supervision_help.txt", true);
        });

        binding.noReplyLimitImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "no_response_help.txt", true);
        });

        binding.excludeCallsignsImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "excludeCallsign.txt", true);
        });
    }

    private void writeConfig(String key, String value) {
        if (mainViewModel != null && mainViewModel.databaseOpr != null) {
            mainViewModel.databaseOpr.writeConfig(key, value, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
