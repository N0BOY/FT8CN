package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.FAQActivity;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.OperationBand;
import com.bg7yoz.ft8cn.database.RigNameList;
import com.bg7yoz.ft8cn.databinding.FragmentConfigBinding;
import com.bg7yoz.ft8cn.ft8signal.FT8Package;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.InstructionSet;
import com.bg7yoz.ft8cn.timer.UtcTimer;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ConfigFragment extends Fragment {
    private static final String TAG = "ConfigFragment";
    private MainViewModel mainViewModel;
    private FragmentConfigBinding binding;
    private BandsSpinnerAdapter bandsSpinnerAdapter;
    private BauRateSpinnerAdapter bauRateSpinnerAdapter;
    private RigNameSpinnerAdapter rigNameSpinnerAdapter;
    private LaunchSupervisionSpinnerAdapter launchSupervisionSpinnerAdapter;
    private PttDelaySpinnerAdapter pttDelaySpinnerAdapter;
    private NoReplyLimitSpinnerAdapter noReplyLimitSpinnerAdapter;
    //private SerialPortSpinnerAdapter serialPortSpinnerAdapter;

    public ConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //我的网格位置
    private final TextWatcher onGridEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            //String s = "";
            StringBuilder s=new StringBuilder();
            for (int j = 0; j < binding.inputMyGridEdit.getText().length(); j++) {
                if (j < 2) {
                    //s = s + Character.toUpperCase(binding.inputMyGridEdit.getText().charAt(j));
                    s.append(Character.toUpperCase(binding.inputMyGridEdit.getText().charAt(j)));
                } else {
                    //s = s + Character.toLowerCase(binding.inputMyGridEdit.getText().charAt(j));
                    s.append(Character.toLowerCase(binding.inputMyGridEdit.getText().charAt(j)));
                }
            }
            writeConfig("grid", s.toString());
            GeneralVariables.setMyMaidenheadGrid(s.toString());
        }
    };
    //我的呼号
    private final TextWatcher onMyCallEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            writeConfig("callsign", editable.toString().toUpperCase().trim());
            String callsign = editable.toString().toUpperCase().trim();
            if (callsign.length() > 0) {
                Ft8Message.hashList.addHash(FT8Package.getHash22(callsign), callsign);
                Ft8Message.hashList.addHash(FT8Package.getHash12(callsign), callsign);
                Ft8Message.hashList.addHash(FT8Package.getHash10(callsign), callsign);
            }
            GeneralVariables.myCallsign = (editable.toString().toUpperCase().trim());
        }
    };
    //发射频率
    private final TextWatcher onFreqEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            setfreq(editable.toString());
        }
    };
    //发射延迟时间
    private final TextWatcher onTransDelayEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            int transDelay = 1000;
            if (editable.toString().matches("^\\d{1,4}$")) {
                transDelay = Integer.parseInt(editable.toString());
            }
            GeneralVariables.transmitDelay = transDelay;
            mainViewModel.ft8TransmitSignal.setTimer_sec(GeneralVariables.transmitDelay);
            writeConfig("transDelay", Integer.toString(transDelay));
        }
    };

    //排除的呼号前缀
    private final TextWatcher onExcludedCallsigns=new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            GeneralVariables.addExcludedCallsigns(editable.toString());
            writeConfig("excludedCallsigns", GeneralVariables.getExcludeCallsigns());
        }
    };

    //修饰符
    private final TextWatcher onModifierEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.toString().toUpperCase().trim().matches("[0-9]{3}|[A-Z]{1,4}")
                    ||editable.toString().trim().length()==0){
                binding.modifierEdit.setTextColor(requireContext().getColor(R.color.text_view_color));
                GeneralVariables.toModifier=editable.toString().toUpperCase().trim();
                writeConfig("toModifier", GeneralVariables.toModifier);
            }else{
                binding.modifierEdit.setTextColor(requireContext().getColor(R.color.text_view_error_color));
            }
        }
    };

    //CI-V地址
    private final TextWatcher onCIVAddressEditorChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.toString().length() < 2) {
                return;
            }
            String s = "0x" + editable.toString();
            if (s.matches("\\b0[xX][0-9a-fA-F]+\\b")) {//匹配十六进制
                String temp = editable.toString().substring(0, 2).toUpperCase();
                writeConfig("civ", temp);
                GeneralVariables.civAddress = Integer.parseInt(temp, 16);
                mainViewModel.setCivAddress();
            }
        }
    };


    @SuppressLint("DefaultLocale")
    private void setfreq(String sFreq) {
        float freq;
        try {
            freq = Float.parseFloat(sFreq);
            if (freq < 100) {
                freq = 100;
            }
            if (freq > 2900) {
                freq = 2900;
            }
        } catch (Exception e
        ) {
            freq = 1000;
        }


        writeConfig("freq", String.format("%.0f", freq));
        GeneralVariables.setBaseFrequency(freq);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainViewModel = MainViewModel.getInstance(this);
        binding = FragmentConfigBinding.inflate(inflater, container, false);

        //只对中国开方问题收集
//        if (GeneralVariables.isChina) {
//            binding.faqButton.setVisibility(View.VISIBLE);
//        } else {
//            binding.faqButton.setVisibility(View.GONE);
//        }


        //设置时间偏移
        setUtcTimeOffsetSpinner();

        //设置PTT延时
        setPttDelaySpinner();

        //设置操作频段
        setBandsSpinner();

        //设置波特率列表
        setBauRateSpinner();

        //设置电台名称，参数列表
        setRigNameSpinner();

        //设置控制模式 VOX CAT
        setControlMode();

        //设置连线的方式
        setConnectMode();

        //设置发射监管列表
        setLaunchSupervision();

        //设置帮助对话框
        setHelpDialog();

        //设置无回应次数中断
        setNoReplyLimitSpinner();

        //显示滚动箭头
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setScrollImageVisible();
            }
        }, 1000);
        binding.scrollView3.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                setScrollImageVisible();
            }
        });

        //FAQ按钮的onClick
        binding.faqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireContext(), FAQActivity.class);
                startActivity(intent);
            }
        });

        //梅登海德网格
        binding.inputMyGridEdit.removeTextChangedListener(onGridEditorChanged);
        binding.inputMyGridEdit.setText(GeneralVariables.getMyMaidenheadGrid());
        binding.inputMyGridEdit.addTextChangedListener(onGridEditorChanged);

        //我的呼号
        binding.inputMycallEdit.removeTextChangedListener(onMyCallEditorChanged);
        binding.inputMycallEdit.setText(GeneralVariables.myCallsign);
        binding.inputMycallEdit.addTextChangedListener(onMyCallEditorChanged);

        //修饰符
        binding.modifierEdit.removeTextChangedListener(onModifierEditorChanged);
        binding.modifierEdit.setText(GeneralVariables.toModifier);
        binding.modifierEdit.addTextChangedListener(onModifierEditorChanged);

        //发射频率
        binding.inputFreqEditor.removeTextChangedListener(onFreqEditorChanged);
        binding.inputFreqEditor.setText(GeneralVariables.getBaseFrequencyStr());
        binding.inputFreqEditor.addTextChangedListener(onFreqEditorChanged);



        //CIV地址
        binding.civAddressEdit.removeTextChangedListener(onCIVAddressEditorChanged);
        binding.civAddressEdit.setText(GeneralVariables.getCivAddressStr());
        binding.civAddressEdit.addTextChangedListener(onCIVAddressEditorChanged);

        //发射延迟
        binding.inputTransDelayEdit.removeTextChangedListener(onTransDelayEditorChanged);
        binding.inputTransDelayEdit.setText(GeneralVariables.getTransmitDelayStr());
        binding.inputTransDelayEdit.addTextChangedListener(onTransDelayEditorChanged);

        binding.excludedCallsignEdit.removeTextChangedListener(onExcludedCallsigns);
        binding.excludedCallsignEdit.setText(GeneralVariables.getExcludeCallsigns());
        binding.excludedCallsignEdit.addTextChangedListener(onExcludedCallsigns);


        //设置同频发射开关
        binding.synFrequencySwitch.setOnCheckedChangeListener(null);
        binding.synFrequencySwitch.setChecked(GeneralVariables.synFrequency);
        setSyncFreqText();//设置开关的文本
        binding.synFrequencySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (binding.synFrequencySwitch.isChecked()) {
                    mainViewModel.databaseOpr.writeConfig("synFreq", "1", null);
                } else {
                    mainViewModel.databaseOpr.writeConfig("synFreq", "0", null);
                    setfreq(binding.inputFreqEditor.getText().toString());
                }
                GeneralVariables.synFrequency = binding.synFrequencySwitch.isChecked();
                setSyncFreqText();
                binding.inputFreqEditor.setEnabled(!binding.synFrequencySwitch.isChecked());
            }
        });

        //设置PTT延迟
        binding.pttDelayOffsetSpinner.setOnItemSelectedListener(null);
        binding.pttDelayOffsetSpinner.setSelection(GeneralVariables.pttDelay / 10);
        binding.pttDelayOffsetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GeneralVariables.pttDelay = i * 10;
                writeConfig("pttDelay", String.valueOf(GeneralVariables.pttDelay));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        //获取操作的波段
        binding.operationBandSpinner.setOnItemSelectedListener(null);
        binding.operationBandSpinner.setSelection(GeneralVariables.bandListIndex);
        binding.operationBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GeneralVariables.bandListIndex = i;
                GeneralVariables.band = OperationBand.getBandFreq(i);//把当前的频段保存下来

                mainViewModel.databaseOpr.getAllQSLCallsigns();//通联成功的呼号读出来
                writeConfig("bandFreq", String.valueOf(GeneralVariables.band));
                if (GeneralVariables.controlMode == ControlMode.CAT//CAT、RTS、DTR模式下控制电台
                        || GeneralVariables.controlMode == ControlMode.RTS
                        || GeneralVariables.controlMode == ControlMode.DTR) {
                    //如果在CAT、RTS模式下，修改电台的频率
                    mainViewModel.setOperationBand();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        //获取电台型号
        binding.rigNameSpinner.setOnItemSelectedListener(null);
        binding.rigNameSpinner.setSelection(GeneralVariables.modelNo);
        new Handler().postDelayed(new Runnable() {//延迟2秒修改OnItemSelectedListener
            @Override
            public void run() {
                binding.rigNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        GeneralVariables.modelNo = i;
                        writeConfig("model", String.valueOf(i));
                        setAddrAndBauRate(rigNameSpinnerAdapter.getRigName(i));

                        //指令集
                        GeneralVariables.instructionSet = rigNameSpinnerAdapter.getRigName(i).instructionSet;
                        writeConfig("instruction", String.valueOf(GeneralVariables.instructionSet));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }
        }, 2000);


        //获取波特率
        binding.baudRateSpinner.setOnItemSelectedListener(null);
        binding.baudRateSpinner.setSelection(bauRateSpinnerAdapter.getPosition(
                GeneralVariables.baudRate));
        binding.baudRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GeneralVariables.baudRate = bauRateSpinnerAdapter.getValue(i);
                writeConfig("baudRate", String.valueOf(GeneralVariables.baudRate));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //设置发射监管
        binding.launchSupervisionSpinner.setOnItemSelectedListener(null);
        binding.launchSupervisionSpinner.setSelection(launchSupervisionSpinnerAdapter
                .getPosition(GeneralVariables.launchSupervision));
        binding.launchSupervisionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GeneralVariables.launchSupervision = LaunchSupervisionSpinnerAdapter.getTimeOut(i);
                writeConfig("launchSupervision", String.valueOf(GeneralVariables.launchSupervision));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //设置无回应中断
        binding.noResponseCountSpinner.setOnItemSelectedListener(null);
        binding.noResponseCountSpinner.setSelection(GeneralVariables.noReplyLimit);
        binding.noResponseCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                GeneralVariables.noReplyLimit = i;
                writeConfig("noReplyLimit", String.valueOf(GeneralVariables.noReplyLimit));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        //设置自动关注CQ
        binding.followCQSwitch.setOnCheckedChangeListener(null);
        binding.followCQSwitch.setChecked(GeneralVariables.autoFollowCQ);
        setAutoFollowCQText();
        binding.followCQSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                GeneralVariables.autoFollowCQ = binding.followCQSwitch.isChecked();
                if (binding.followCQSwitch.isChecked()) {
                    mainViewModel.databaseOpr.writeConfig("autoFollowCQ", "1", null);
                } else {
                    mainViewModel.databaseOpr.writeConfig("autoFollowCQ", "0", null);
                }
                setAutoFollowCQText();
            }
        });

        //设置自动呼叫关注的呼号
        binding.autoCallfollowSwitch.setOnCheckedChangeListener(null);
        binding.autoCallfollowSwitch.setChecked(GeneralVariables.autoCallFollow);
        setAutoCallFollow();
        binding.autoCallfollowSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                GeneralVariables.autoCallFollow = binding.autoCallfollowSwitch.isChecked();
                if (binding.autoCallfollowSwitch.isChecked()) {
                    mainViewModel.databaseOpr.writeConfig("autoCallFollow", "1", null);
                } else {
                    mainViewModel.databaseOpr.writeConfig("autoCallFollow", "0", null);
                }
                setAutoCallFollow();
            }
        });


        //获取梅登海德网格
        binding.configGetGridImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String grid = MaidenheadGrid.getMyMaidenheadGrid(getContext());
                if (!grid.equals("")) {
                    binding.inputMyGridEdit.setText(grid);
                }
            }
        });


        return binding.getRoot();
    }

    /**
     * 设置地址和波特率，指令集
     *
     * @param rigName 电台型号
     */
    private void setAddrAndBauRate(RigNameList.RigName rigName) {
        //mainViewModel.setCivAddress(rigName.address);
        GeneralVariables.civAddress = rigName.address;
        mainViewModel.setCivAddress();
        GeneralVariables.baudRate = rigName.bauRate;
        binding.civAddressEdit.setText(String.format("%X", rigName.address));
        binding.baudRateSpinner.setSelection(
                bauRateSpinnerAdapter.getPosition(rigName.bauRate));
    }


    /**
     * 设置同频发射开关的显示文本
     */
    private void setSyncFreqText() {
        if (binding.synFrequencySwitch.isChecked()) {
            binding.synFrequencySwitch.setText(getString(R.string.freq_syn));
        } else {
            binding.synFrequencySwitch.setText(getString(R.string.freq_asyn));
        }
    }

    /**
     * 设置自动关注CQ开关的文本
     */
    private void setAutoFollowCQText() {
        if (binding.followCQSwitch.isChecked()) {
            binding.followCQSwitch.setText(getString(R.string.auto_follow_cq));
        } else {
            binding.followCQSwitch.setText(getString(R.string.not_concerned_about_CQ));
        }
    }

    //设置自动呼叫关注的呼号
    private void setAutoCallFollow() {
        if (binding.autoCallfollowSwitch.isChecked()) {
            binding.autoCallfollowSwitch.setText(getString(R.string.automatic_call_following));
        } else {
            binding.autoCallfollowSwitch.setText(getString(R.string.do_not_call_the_following_callsign));
        }
    }

    /**
     * 设置UTC时间偏移的spinner
     */
    private void setUtcTimeOffsetSpinner() {
        UtcOffsetSpinnerAdapter adapter = new UtcOffsetSpinnerAdapter(requireContext());
        binding.utcTimeOffsetSpinner.setAdapter(adapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                binding.utcTimeOffsetSpinner.setSelection((UtcTimer.delay / 100 + 75) / 5);
            }
        });
        binding.utcTimeOffsetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                UtcTimer.delay = i * 500 - 7500;//设置延迟
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * 设置操作频段的spinner
     */
    private void setBandsSpinner() {
        GeneralVariables.mutableBandChange.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                binding.operationBandSpinner.setSelection(integer);
            }
        });


        bandsSpinnerAdapter = new BandsSpinnerAdapter(requireContext());
        binding.operationBandSpinner.setAdapter(bandsSpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bandsSpinnerAdapter.notifyDataSetChanged();
            }
        });

    }

    /**
     * 设置波特率列表
     */
    private void setBauRateSpinner() {
        bauRateSpinnerAdapter = new BauRateSpinnerAdapter(requireContext());
        binding.baudRateSpinner.setAdapter(bauRateSpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bauRateSpinnerAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 设置无回应次数中断
     */
    private void setNoReplyLimitSpinner() {
        noReplyLimitSpinnerAdapter = new NoReplyLimitSpinnerAdapter(requireContext());
        binding.noResponseCountSpinner.setAdapter(noReplyLimitSpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                noReplyLimitSpinnerAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 设置发射监管列表
     */
    private void setLaunchSupervision() {
        launchSupervisionSpinnerAdapter = new LaunchSupervisionSpinnerAdapter(requireContext());
        binding.launchSupervisionSpinner.setAdapter(launchSupervisionSpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                launchSupervisionSpinnerAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 设置电台名称，参数列表
     */
    private void setRigNameSpinner() {
        rigNameSpinnerAdapter = new RigNameSpinnerAdapter(requireContext());
        binding.rigNameSpinner.setAdapter(rigNameSpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rigNameSpinnerAdapter.notifyDataSetChanged();
            }
        });

    }

    /**
     * 设置PTT延时
     */
    private void setPttDelaySpinner() {
        pttDelaySpinnerAdapter = new PttDelaySpinnerAdapter(requireContext());
        binding.pttDelayOffsetSpinner.setAdapter(pttDelaySpinnerAdapter);
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pttDelaySpinnerAdapter.notifyDataSetChanged();
                binding.pttDelayOffsetSpinner.setSelection(GeneralVariables.pttDelay / 10);
            }
        });
    }


    /**
     * 设置控制模式VOX CAT
     */
    private void setControlMode() {
        //binding.controlModeRadioGroup.setOnCheckedChangeListener(null);
        binding.controlModeRadioGroup.clearCheck();

        switch (GeneralVariables.controlMode) {
            case ControlMode.CAT:
            case ConnectMode.NETWORK:
                binding.ctrCATradioButton.setChecked(true);
                break;
            case ControlMode.RTS:
                binding.ctrRTSradioButton.setChecked(true);
                break;
            case ControlMode.DTR:
                binding.ctrDTRradioButton.setChecked(true);
                break;
            default:
                binding.ctrVOXradioButton.setChecked(true);
        }


        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int buttonId = binding.controlModeRadioGroup.getCheckedRadioButtonId();

                if (buttonId == binding.ctrVOXradioButton.getId()) {
                    GeneralVariables.controlMode = ControlMode.VOX;
                } else if (buttonId == binding.ctrCATradioButton.getId()) {//CAT模式
                    GeneralVariables.controlMode = ControlMode.CAT;
                } else if (buttonId == binding.ctrRTSradioButton.getId()) {//RTS模式
                    GeneralVariables.controlMode = ControlMode.RTS;
                } else if (buttonId == binding.ctrDTRradioButton.getId()) {//RTS模式
                    GeneralVariables.controlMode = ControlMode.DTR;
                }
                mainViewModel.setControlMode();//通知一下电台控制模式改变
                //无论CAT还是RTS，CI-V指令还是有效的，都是串口
                if (GeneralVariables.controlMode == ControlMode.CAT
                        || GeneralVariables.controlMode == ControlMode.RTS
                        || GeneralVariables.controlMode == ControlMode.DTR) {
                    if (!mainViewModel.isRigConnected()) {
                        mainViewModel.getUsbDevice();
                    } else {
                        mainViewModel.setOperationBand();
                    }
                }
                writeConfig("ctrMode", String.valueOf(GeneralVariables.controlMode));
                setConnectMode();
            }
        };

        binding.ctrCATradioButton.setOnClickListener(listener);
        binding.ctrVOXradioButton.setOnClickListener(listener);
        binding.ctrRTSradioButton.setOnClickListener(listener);
        binding.ctrDTRradioButton.setOnClickListener(listener);
    }

    /**
     * 设置连线的方式，可以是USB，也可以是BLUE_TOOTH
     */
    private void setConnectMode() {
        if (GeneralVariables.controlMode == ControlMode.CAT
                //&& BluetoothConstants.checkBluetoothIsOpen()
            ) {
            //此处要改成VISIBLE
            binding.connectModeLayout.setVisibility(View.VISIBLE);
        } else {
            binding.connectModeLayout.setVisibility(View.GONE);
        }
        binding.connectModeRadioGroup.clearCheck();
        switch (GeneralVariables.connectMode) {
            case ConnectMode.USB_CABLE:
                binding.cableConnectRadioButton.setChecked(true);
                break;
            case ConnectMode.BLUE_TOOTH:
                binding.bluetoothConnectRadioButton.setChecked(true);
                break;
            case ConnectMode.NETWORK:
                binding.networkConnectRadioButton.setChecked(true);
                break;
        }
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int buttonId = binding.connectModeRadioGroup.getCheckedRadioButtonId();
                if (buttonId == binding.cableConnectRadioButton.getId()) {
                    GeneralVariables.connectMode = ConnectMode.USB_CABLE;
                } else if (buttonId == binding.bluetoothConnectRadioButton.getId()) {
                    GeneralVariables.connectMode = ConnectMode.BLUE_TOOTH;
                }else if (buttonId==binding.networkConnectRadioButton.getId()){
                    GeneralVariables.connectMode=ConnectMode.NETWORK;
                }
                //------显示蓝牙列表，并选择，然后建立蓝牙连接
                if (GeneralVariables.connectMode == ConnectMode.BLUE_TOOTH) {
                    //根据安卓12，要判断一下蓝牙权限：
                    new SelectBluetoothDialog(requireContext(), mainViewModel).show();
                }

                //-----显示网络上的电台，目前是flex电台，-------------------
                if (GeneralVariables.connectMode==ConnectMode.NETWORK){
                    //打开网络电台列表对话框
                    if (GeneralVariables.instructionSet== InstructionSet.FLEX_NETWORK) {
                        new SelectFlexRadioDialog(requireContext(), mainViewModel).show();
                    }else if(GeneralVariables.instructionSet== InstructionSet.ICOM) {
                        new LoginIcomRadioDialog(requireContext(), mainViewModel).show();
                    }else {
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.only_flex_supported));
                    }
                }

            }
        };
        binding.cableConnectRadioButton.setOnClickListener(listener);
        binding.bluetoothConnectRadioButton.setOnClickListener(listener);
        binding.networkConnectRadioButton.setOnClickListener(listener);
    }


    /**
     * 把配置信息写到数据库
     *
     * @param KeyName 关键词
     * @param Value   值
     */
    private void writeConfig(String KeyName, String Value) {
        mainViewModel.databaseOpr.writeConfig(KeyName, Value, null);
    }

    private void setHelpDialog() {
        //呼号帮助
        binding.callsignHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "callsign.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "callsign_en.txt", true).show();
                }
            }
        });
        //梅登海德网格的帮助
        binding.maidenGridImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "maidenhead.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "maidenhead_en.txt", true).show();
                }
            }
        });
        //发射频率的帮助
        binding.frequencyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "frequency.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "frequency_en.txt", true).show();
                }
            }
        });
        //延迟发射帮助
        binding.transDelayImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "transDelay.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "transDelay_en.txt", true).show();
                }
            }
        });
        //时间偏移帮助
        binding.timeOffsetImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "timeoffset.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "timeoffset_en.txt", true).show();
                }
            }
        });
        //PTT延时帮助
        binding.pttDelayImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "pttdelay.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "pttdelay_en.txt", true).show();
                }
            }
        });
        //设置ABOUT
        binding.aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new HelpDialog(requireContext(), requireActivity(), "readme.txt", true).show();
            }
        });
        //设置操作频段
        binding.operationHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "operationBand.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "operationBand_en.txt", true).show();
                }
            }
        });
        //设置操作模式
        binding.controlModeHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "controlMode.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "controlMode_en.txt", true).show();
                }
            }
        });
        //设置CI-V地址和波特率帮助
        binding.baudRateHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "civ_help.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "civ_help_en.txt", true).show();
                }
            }
        });
        //电台型号列表
        binding.rigNameHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "rig_model_help.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "rig_model_help_en.txt", true).show();
                }
            }
        });
        //发射监管
        binding.launchSupervisionImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "launch_supervision_help.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "launch_supervision_en.txt", true).show();
                }
            }
        });
        //无回应次数
        binding.noResponseCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "no_response_help.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "no_response_help_en.txt", true).show();
                }
            }
        });
        //自动呼叫
        binding.autoFollowCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "auto_follow_help.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "auto_follow_help_en.txt", true).show();
                }
            }
        });
        //连接模式
        binding.connectModeHelpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "connectMode.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "connectMode_en.txt", true).show();
                }
            }
        });
        binding.excludedHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "excludeCallsign.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "excludeCallsign_en.txt", true).show();
                }
            }
        });
        binding.clearCacheHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GeneralVariables.isChina) {
                    new HelpDialog(requireContext(), requireActivity(), "clear_cache_data.txt", true).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity(), "clear_cache_data_en.txt", true).show();
                }
            }
        });
        binding.clearFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ClearCacheDataDialog(requireContext(), requireActivity()
                        ,mainViewModel.databaseOpr
                        ,ClearCacheDataDialog.CACHE_MODE.FOLLOW_DATA).show();
            }
        });
        binding.clearLogCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ClearCacheDataDialog(requireContext(), requireActivity()
                        ,mainViewModel.databaseOpr
                        ,ClearCacheDataDialog.CACHE_MODE.CALL_LOG).show();
            }
        });
    }

    /**
     * 设置界面的上下滚动的图标
     */
    private void setScrollImageVisible() {

        if (binding.scrollView3.getScrollY() == 0) {
            binding.configScrollUpImageView.setVisibility(View.GONE);
        } else {
            binding.configScrollUpImageView.setVisibility(View.VISIBLE);
        }

        if (binding.scrollView3.getHeight() + binding.scrollView3.getScrollY()
                < binding.scrollLinearLayout.getMeasuredHeight()) {
            binding.configScrollDownImageView.setVisibility(View.VISIBLE);
        } else {
            binding.configScrollDownImageView.setVisibility(View.GONE);
        }
    }


}