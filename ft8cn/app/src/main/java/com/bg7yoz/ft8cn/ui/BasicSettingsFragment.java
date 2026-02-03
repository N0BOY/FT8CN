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
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.databinding.FragmentBasicSettingsBinding;

/**
 * Basic settings tab: Station + Radio + Transmit
 * Organized version with clean layout
 *
 * @author BGY70Z
 * @date 2026-02-02
 */
public class BasicSettingsFragment extends Fragment {

    private FragmentBasicSettingsBinding binding;
    private MainViewModel mainViewModel;
    private BandsSpinnerAdapter bandsSpinnerAdapter;
    private BauRateSpinnerAdapter bauRateSpinnerAdapter;
    private SerialDataBitsSpinnerAdapter dataBitsSpinnerAdapter;
    private SerialStopBitsSpinnerAdapter stopBitsSpinnerAdapter;
    private SerialParityBitsSpinnerAdapter parityBitsSpinnerAdapter;
    private RigModelSpinnerAdapter rigModelSpinnerAdapter;
    private PttDelaySpinnerAdapter pttDelaySpinnerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentBasicSettingsBinding.inflate(inflater, container, false);
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
        // Initialize spinners
        bandsSpinnerAdapter = new BandsSpinnerAdapter(requireContext());
        bauRateSpinnerAdapter = new BauRateSpinnerAdapter(requireContext());
        dataBitsSpinnerAdapter = new SerialDataBitsSpinnerAdapter(requireContext());
        stopBitsSpinnerAdapter = new SerialStopBitsSpinnerAdapter(requireContext());
        parityBitsSpinnerAdapter = new SerialParityBitsSpinnerAdapter(requireContext());
        rigModelSpinnerAdapter = new RigModelSpinnerAdapter(requireContext());
        pttDelaySpinnerAdapter = new PttDelaySpinnerAdapter(requireContext());

        binding.operationBandSpinner.setAdapter(bandsSpinnerAdapter);
        binding.baudRateSpinner.setAdapter(bauRateSpinnerAdapter);
        binding.dataBitsSpinner.setAdapter(dataBitsSpinnerAdapter);
        binding.stopBitsSpinner.setAdapter(stopBitsSpinnerAdapter);
        binding.parityBitsSpinner.setAdapter(parityBitsSpinnerAdapter);
        binding.rigNameSpinner.setAdapter(rigModelSpinnerAdapter);
        binding.pttDelayOffsetSpinner.setAdapter(pttDelaySpinnerAdapter);
    }

    private void initializeControls() {
        // Station settings
        binding.inputMyGridEdit.setText(GeneralVariables.getMyMaidenheadGrid());
        binding.inputMycallEdit.setText(GeneralVariables.myCallsign);
        binding.modifierEdit.setText(GeneralVariables.toModifier);
        binding.parkNumberEdit.setText(GeneralVariables.parkNumber != null ? GeneralVariables.parkNumber : "");

        // Radio settings
        binding.operationBandSpinner.setSelection(GeneralVariables.bandListIndex);
        int modelPosition = rigModelSpinnerAdapter.getPositionForFullIndex(GeneralVariables.modelNo);
        binding.rigNameSpinner.setSelection(modelPosition);

        // Control mode - these are static int constants
        if (GeneralVariables.controlMode == ControlMode.VOX) {
            binding.controlModeVoxRadio.setChecked(true);
        } else if (GeneralVariables.controlMode == ControlMode.CAT) {
            binding.controlModeCatRadio.setChecked(true);
        } else if (GeneralVariables.controlMode == ControlMode.RTS) {
            binding.controlModeRtsRadio.setChecked(true);
        } else if (GeneralVariables.controlMode == ControlMode.DTR) {
            binding.controlModeDtrRadio.setChecked(true);
        }

        // Connection mode - these are static int constants
        if (GeneralVariables.connectMode == ConnectMode.USB_CABLE) {
            binding.connectModeSerialRadio.setChecked(true);
            binding.serialSettingsLayout.setVisibility(View.VISIBLE);
        } else if (GeneralVariables.connectMode == ConnectMode.BLUE_TOOTH) {
            binding.connectModeBluetoothRadio.setChecked(true);
            binding.serialSettingsLayout.setVisibility(View.GONE);
        } else if (GeneralVariables.connectMode == ConnectMode.NETWORK) {
            binding.connectModeWifiRadio.setChecked(true);
            binding.serialSettingsLayout.setVisibility(View.GONE);
        }

        // Serial settings
        binding.civAddressEdit.setText(GeneralVariables.getCivAddressStr());
        binding.baudRateSpinner.setSelection(bauRateSpinnerAdapter.getPosition(GeneralVariables.baudRate));
        binding.dataBitsSpinner.setSelection(dataBitsSpinnerAdapter.getPosition(GeneralVariables.serialDataBits));
        binding.stopBitsSpinner.setSelection(stopBitsSpinnerAdapter.getPosition(GeneralVariables.serialStopBits));
        binding.parityBitsSpinner.setSelection(parityBitsSpinnerAdapter.getPosition(GeneralVariables.serialParity));

        // Transmit settings
        binding.inputFreqEditor.setText(GeneralVariables.getBaseFrequencyStr());
        binding.synFrequencySwitch.setChecked(GeneralVariables.synFrequency);
        binding.inputFreqEditor.setEnabled(!GeneralVariables.synFrequency);

        binding.fakeItSplitSwitch.setChecked(GeneralVariables.fakeItSplit);
        binding.fakeItSplitSwitch.setEnabled(GeneralVariables.controlMode != ControlMode.VOX);

        // Transmit offset
        int absOffset = Math.abs(GeneralVariables.transmitOffsetHz);
        binding.transmitOffsetEdit.setText(String.valueOf(absOffset));
        if (GeneralVariables.transmitOffsetHz >= 0) {
            binding.transmitOffsetPositiveRadio.setChecked(true);
        } else {
            binding.transmitOffsetNegativeRadio.setChecked(true);
        }

        binding.inputTransDelayEdit.setText(GeneralVariables.getTransmitDelayStr());
        binding.pttDelayOffsetSpinner.setSelection(GeneralVariables.pttDelay / 10);
    }

    private void setupListeners() {
        // Grid editor
        binding.inputMyGridEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                StringBuilder grid = new StringBuilder();
                for (int j = 0; j < s.length(); j++) {
                    if (j < 2 || (j >= 4 && j < 6)) {
                        grid.append(Character.toUpperCase(s.charAt(j)));
                    } else {
                        grid.append(Character.toLowerCase(s.charAt(j)));
                    }
                }
                if (!grid.toString().equals(s.toString())) {
                    binding.inputMyGridEdit.setText(grid.toString());
                    binding.inputMyGridEdit.setSelection(grid.length());
                }
                GeneralVariables.setMyMaidenheadGrid(grid.toString());
                writeConfig("myMaidenheadGrid", grid.toString());
            }
        });

        // Callsign editor
        binding.inputMycallEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.myCallsign = s.toString().toUpperCase();
                writeConfig("myCallsign", s.toString().toUpperCase());
            }
        });

        // Modifier editor
        binding.modifierEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.toModifier = s.toString().toUpperCase();
                writeConfig("toModifier", s.toString().toUpperCase());
            }
        });

        // Park number editor
        binding.parkNumberEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                GeneralVariables.parkNumber = s.toString();
                writeConfig("parkNumber", s.toString());
            }
        });

        // Frequency editor
        binding.inputFreqEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float freq = Float.parseFloat(s.toString());
                    GeneralVariables.setBaseFrequency(freq);
                    writeConfig("baseFrequency", s.toString());
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            }
        });

        // Sync frequency switch
        binding.synFrequencySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.synFrequency = isChecked;
            binding.inputFreqEditor.setEnabled(!isChecked);
            writeConfig("synFrequency", isChecked ? "1" : "0");
        });

        // Fake It Split switch
        binding.fakeItSplitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GeneralVariables.fakeItSplit = isChecked;
            writeConfig("fakeItSplit", isChecked ? "1" : "0");
        });

        // Transmit offset editors
        binding.transmitOffsetEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateTransmitOffset();
            }
        });

        binding.transmitOffsetSignRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateTransmitOffset();
        });

        // Control mode radio group
        binding.controlRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode = ControlMode.VOX;
            if (checkedId == R.id.controlModeVoxRadio) {
                newMode = ControlMode.VOX;
            } else if (checkedId == R.id.controlModeCatRadio) {
                newMode = ControlMode.CAT;
            } else if (checkedId == R.id.controlModeRtsRadio) {
                newMode = ControlMode.RTS;
            } else if (checkedId == R.id.controlModeDtrRadio) {
                newMode = ControlMode.DTR;
            }
            GeneralVariables.controlMode = newMode;
            binding.fakeItSplitSwitch.setEnabled(newMode != ControlMode.VOX);
            writeConfig("controlMode", String.valueOf(newMode));
        });

        // Connection mode radio group
        binding.connectRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode = ConnectMode.USB_CABLE;
            if (checkedId == R.id.connectModeSerialRadio) {
                newMode = ConnectMode.USB_CABLE;
                binding.serialSettingsLayout.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.connectModeBluetoothRadio) {
                newMode = ConnectMode.BLUE_TOOTH;
                binding.serialSettingsLayout.setVisibility(View.GONE);
            } else if (checkedId == R.id.connectModeWifiRadio) {
                newMode = ConnectMode.NETWORK;
                binding.serialSettingsLayout.setVisibility(View.GONE);
            }
            GeneralVariables.connectMode = newMode;
            writeConfig("connectMode", String.valueOf(newMode));
        });

        // CI-V Address editor
        binding.civAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    GeneralVariables.civAddress = Integer.parseInt(s.toString(), 16);
                    writeConfig("civAddress", s.toString());
                } catch (NumberFormatException e) {
                    // Invalid hex input, ignore
                }
            }
        });

        // TX Delay editor
        binding.inputTransDelayEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int delay = Integer.parseInt(s.toString());
                    GeneralVariables.transmitDelay = delay;
                    writeConfig("transmitDelay", s.toString());
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            }
        });

        // Spinners
        binding.operationBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.bandListIndex = position;
                GeneralVariables.band = ((com.bg7yoz.ft8cn.database.OperationBand.Band) bandsSpinnerAdapter.getItem(position)).band;
                writeConfig("operationBand", String.valueOf(GeneralVariables.band));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.rigNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int fullListIndex = rigModelSpinnerAdapter.getFullListIndex(position);
                GeneralVariables.modelNo = fullListIndex;
                com.bg7yoz.ft8cn.database.RigNameList.RigName rigName = rigModelSpinnerAdapter.getRigName(position);
                if (rigName != null) {
                    GeneralVariables.instructionSet = rigName.instructionSet;
                    writeConfig("instruction", String.valueOf(GeneralVariables.instructionSet));
                    writeConfig("modelNo", String.valueOf(GeneralVariables.modelNo));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.baudRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.baudRate = (Integer) bauRateSpinnerAdapter.getItem(position);
                writeConfig("baudRate", String.valueOf(GeneralVariables.baudRate));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.dataBitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.serialDataBits = (Integer) dataBitsSpinnerAdapter.getItem(position);
                writeConfig("serialDataBits", String.valueOf(GeneralVariables.serialDataBits));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.stopBitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.serialStopBits = (Integer) stopBitsSpinnerAdapter.getItem(position);
                writeConfig("serialStopBits", String.valueOf(GeneralVariables.serialStopBits));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.parityBitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.serialParity = (Integer) parityBitsSpinnerAdapter.getItem(position);
                writeConfig("serialParity", String.valueOf(GeneralVariables.serialParity));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.pttDelayOffsetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GeneralVariables.pttDelay = position * 10;
                writeConfig("pttDelay", String.valueOf(GeneralVariables.pttDelay));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Help buttons
        binding.maidenGridImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "maidenhead.txt", true);
        });

        binding.rigModelImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "rig_model_help.txt", true);
        });

        binding.operationBandImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "operationBand.txt", true);
        });

        binding.controlModeImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "controlMode.txt", true);
        });

        binding.connectionModeImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "connectMode.txt", true);
        });

        binding.civAddressImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "civ_help.txt", true);
        });

        binding.serialImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "serial_help.txt", true);
        });

        binding.frequencyImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "frequency.txt", true);
        });

        binding.synFrequencyImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "frequency.txt", true);
        });

        binding.fakeItSplitImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "fake_it_split.txt", true);
        });

        binding.txDelayImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "transDelay.txt", true);
        });

        binding.pttDelayImageButton.setOnClickListener(v -> {
            new HelpDialog(requireContext(), requireActivity(), "pttdelay.txt", true);
        });
    }

    private void updateTransmitOffset() {
        String offsetText = binding.transmitOffsetEdit.getText().toString();
        if (offsetText.isEmpty()) {
            return;
        }
        try {
            int offset = Integer.parseInt(offsetText);
            boolean isPositive = binding.transmitOffsetPositiveRadio.isChecked();
            GeneralVariables.transmitOffsetHz = isPositive ? offset : -offset;
            writeConfig("transmitOffsetHz", String.valueOf(GeneralVariables.transmitOffsetHz));
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
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
