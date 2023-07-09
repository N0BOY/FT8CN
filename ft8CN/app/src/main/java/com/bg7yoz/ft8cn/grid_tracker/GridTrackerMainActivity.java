package com.bg7yoz.ft8cn.grid_tracker;
/**
 * 网格追踪的主窗口。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.database.OnAfterQueryConfig;
import com.bg7yoz.ft8cn.databinding.ActivityGridTrackerMainBinding;
import com.bg7yoz.ft8cn.floatview.FloatView;
import com.bg7yoz.ft8cn.floatview.FloatViewButton;
import com.bg7yoz.ft8cn.ft8transmit.TransmitCallsign;
import com.bg7yoz.ft8cn.log.OnQueryQSLRecordCallsign;
import com.bg7yoz.ft8cn.log.QSLRecordStr;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.CallingListAdapter;
import com.bg7yoz.ft8cn.ui.FreqDialog;
import com.bg7yoz.ft8cn.ui.SetVolumeDialog;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GridTrackerMainActivity extends AppCompatActivity {
    private static final String TAG = "GridTrackerMainActivity";
    private static final String DataConfigShowMode = "tracker_show_mode";
    private static final String DataConfigShowQsx = "tracker_show_qsx";
    private static final String DataConfigShowCQ = "tracker_show_cq";

    private MainViewModel mainViewModel;
    private ActivityGridTrackerMainBinding binding;
    private FloatView floatView;
    private FloatViewButton transButton;
    private GridOsmMapView gridOsmMapView;

    private RecyclerView callMessagesRecyclerView;
    private CallingListAdapter callingListAdapter;
    private boolean messageListIsClose = false;
    private boolean configBarIsClose = false;
    private QSLRecordStr qlsRecorder = null;//用于历史显示消息
    private MutableLiveData<ArrayList<QSLRecordStr>> qslRecordList = new MutableLiveData<>();


    @SuppressLint("NotifyDataSetChanged")
    protected void doAfterCreate() {
        //设置消息列表
        callingListAdapter.notifyDataSetChanged();
        callMessagesRecyclerView.scrollToPosition(callingListAdapter.getItemCount() - 1);

        setTipsRadioGroupClickerListener();//显示模式Group radio动作
        setShowTipsSwitchClickerListener();//显示提示开关动作
        readConfig();

        //读取调用本activity的参数，如果不为空，说明要画参数中的消息
        //画在日志界面中被选择的消息
        Intent intentGet = getIntent();
        qlsRecorder = (QSLRecordStr) intentGet.getSerializableExtra("qslList");
        if (qlsRecorder != null) {
            GridOsmMapView.GridPolyLine line = drawMessage(qlsRecorder);//在地图上画每一个消息
            if (line != null) {
                line.showInfoWindow();
            }
        }
        //画日志界面查询出的全部消息
        String queryKey = intentGet.getStringExtra("qslAll");
        int queryFilter = intentGet.getIntExtra("queryFilter", 0);
        if (queryKey != null) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.tracker_query_qso_info));
            mainViewModel.databaseOpr.getQSLRecordByCallsign(true, 0, queryKey, queryFilter
                    , new OnQueryQSLRecordCallsign() {
                        @Override
                        public void afterQuery(ArrayList<QSLRecordStr> records) {
                            qslRecordList.postValue(records);
                        }
                    });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //禁止休眠
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                , WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //设置深色模式
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        //setContentView(R.layout.activity_grid_tracker_main);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//设定为横屏
        mainViewModel = MainViewModel.getInstance(this);
        binding = ActivityGridTrackerMainBinding.inflate(getLayoutInflater());

        gridOsmMapView = new GridOsmMapView(getBaseContext(), binding.osmMap, mainViewModel);


        callMessagesRecyclerView = binding.callMessagesRecyclerView;
        callingListAdapter = new CallingListAdapter(this, mainViewModel
                , mainViewModel.ft8Messages, CallingListAdapter.ShowMode.TRACKER);
        callMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        callMessagesRecyclerView.setAdapter(callingListAdapter);


        callingListAdapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) view.getTag();
                if (position == -1) {
                    return;
                }
                if (position > mainViewModel.ft8Messages.size() - 1) {
                    return;
                }
                Ft8Message msg = mainViewModel.ft8Messages.get(position);

                if (msg.checkIsCQ()) {
                    GridOsmMapView.GridMarker marker = gridOsmMapView.getMarker(msg);
                    if (marker == null) marker = gridOsmMapView.addGridMarker(
                            msg.getMaidenheadGrid(mainViewModel.databaseOpr), msg);
                    if (marker != null) {
                        gridOsmMapView.hideInfoWindows();
                        gridOsmMapView.gotoCqGrid(marker, true);
                        marker.showInfoWindow();
                    }
                } else {
                    gridOsmMapView.hideInfoWindows();
                    gridOsmMapView.clearSelectedLines();
                    GridOsmMapView.GridPolyLine line;
                    line = gridOsmMapView.drawLine(msg, mainViewModel.databaseOpr);
                    if (line != null) {
                        gridOsmMapView.zoomToLineBound(line);
                        line.showInfoWindow();
                        closeMessages();
                    }
                }
            }
        });
        //设置消息列表滑动，用于快速呼叫
        initRecyclerViewAction();

        //观察解码数量
        mainViewModel.mutable_Decoded_Counter.observe(this, new Observer<Integer>() {
            @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
            @Override
            public void onChanged(Integer integer) {
//                callingListAdapter.notifyDataSetChanged();
                //当列表下部稍微多出一些，自动上移
//                if (callMessagesRecyclerView.computeVerticalScrollRange()
//                        - callMessagesRecyclerView.computeVerticalScrollExtent()
//                        - callMessagesRecyclerView.computeVerticalScrollOffset() < 500) {
//                    callMessagesRecyclerView.scrollToPosition(callingListAdapter.getItemCount() - 1);
//                }
//                if (mainViewModel.currentMessages != null) {
//
//                    ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
//                                    R.string.tracker_decoded_new)
//                            , mainViewModel.currentDecodeCount)
//                            + " " + String.format(
//                            getString(R.string.decoding_takes_milliseconds)
//                            , mainViewModel.ft8SignalListener.decodeTimeSec.getValue()));
//                    //画电台之间的连线
//                    //对CQ的电台打点
//                    gridOsmMapView.clearLines();
//                    gridOsmMapView.clearMarkers();
//                    for (Ft8Message msg : mainViewModel.currentMessages) {
//                        drawMessage(msg);//在地图上画每一个消息
//                    }
//                    gridOsmMapView.showInfoWindows();
//                }
            }
        });
        mainViewModel.mutableIsDecoding.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    gridOsmMapView.clearLines();
                    gridOsmMapView.clearMarkers();
                }
            }
        });
        mainViewModel.mutableFt8MessageList.observe(this, new Observer<ArrayList<Ft8Message>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChanged(ArrayList<Ft8Message> messages) {
                if (mainViewModel.currentMessages == null) return;
                ArrayList<Ft8Message> tempMsg = new ArrayList<>(mainViewModel.currentMessages);
                callingListAdapter.notifyDataSetChanged();
                if (callMessagesRecyclerView.computeVerticalScrollRange()
                        - callMessagesRecyclerView.computeVerticalScrollExtent()
                        - callMessagesRecyclerView.computeVerticalScrollOffset() < 500) {
                    callMessagesRecyclerView.scrollToPosition(callingListAdapter.getItemCount() - 1);
                }

                binding.gridMessageTextView.setText(String.format("%s %s"
                        , String.format(GeneralVariables.getStringFromResource(
                                        R.string.tracker_decoded_new)
                                , mainViewModel.currentDecodeCount), String.format(
                                getString(R.string.decoding_takes_milliseconds)
                                , mainViewModel.ft8SignalListener.decodeTimeSec.getValue())));

                //画电台之间的连线
                //对CQ的电台打点
                for (Ft8Message msg : tempMsg) {
                    drawMessage(msg);//在地图上画每一个消息
                }
                gridOsmMapView.showInfoWindows();
                //}
            }
        });


        //观察DEBUG信息
        GeneralVariables.mutableDebugMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (s.length() > 1) {
                    binding.trackerDebugLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.trackerDebugLayout.setVisibility(View.GONE);
                }
                binding.debugMessageTextView.setText(s);
            }
        });
        //设置发射消息框的动画
        binding.transmittingMessageTextView.setAnimation(AnimationUtils.loadAnimation(this
                , R.anim.view_blink));
        //观察发射的状态
        mainViewModel.ft8TransmitSignal.mutableIsTransmitting.observe(this,
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        if (aBoolean) {
                            binding.transmittingLayout.setVisibility(View.VISIBLE);
                        } else {
                            binding.transmittingLayout.setVisibility(View.GONE);
                        }
                    }
                });

        //观察发射内容的变化
        mainViewModel.ft8TransmitSignal.mutableTransmittingMessage.observe(this,
                new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        binding.transmittingMessageTextView.setText(s);
                    }
                });


        //观察时钟的变化，显示进度条
        mainViewModel.timerSec.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                if (mainViewModel.ft8TransmitSignal.sequential == UtcTimer.getNowSequential()
                        && mainViewModel.ft8TransmitSignal.isActivated()) {
                    binding.utcProgressBar.setBackgroundColor(getColor(R.color.calling_list_isMyCall_color));
                } else {
                    binding.utcProgressBar.setBackgroundColor(getColor(R.color.progresss_bar_back_color));
                }
                binding.utcProgressBar.setProgress((int) ((aLong / 1000) % 15));
            }
        });

        //添加浮动按钮
        InitFloatView();


        //gridOsmMapView.initMap(GeneralVariables.getMyMaidenhead4Grid(), true);

        //把呼号与网格对应关系中的网格提取出来
        for (Map.Entry<String, String> entry : GeneralVariables.callsignAndGrids.entrySet()) {
            gridOsmMapView.upgradeGridMode(entry.getValue(), GridOsmMapView.GridMode.QSX);
        }

        //观察呼号与网格的对应关系表的变化，如果有新增的，就添加
        GeneralVariables.mutableNewGrid.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (!gridOsmMapView.upgradeGridMode(s, GridOsmMapView.GridMode.QSX)) {
                    ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                            R.string.grid_tracker_new_grid), s));
                }
            }
        });

        //获取曾经通联过的网格
        mainViewModel.databaseOpr.getQsoGridQuery(new DatabaseOpr.OnGetQsoGrids() {
            //ConcurrentHashMap
            @Override
            public void onAfterQuery(HashMap<String, Boolean> grids) {
                for (Map.Entry<String, Boolean> entry : grids.entrySet()) {
                    gridOsmMapView.upgradeGridMode(entry.getKey()
                            , entry.getValue() ? GridOsmMapView.GridMode.QSL : GridOsmMapView.GridMode.QSO);
                }
            }
        });

        //关闭消息按钮
        binding.closeMessageImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeMessages();
            }
        });
        //打开消息按钮
        binding.openMessagesImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMessage();
            }
        });
        gridOsmMapView.initMap(GeneralVariables.getMyMaidenhead4Grid(), true);

        qslRecordList.observe(this, new Observer<ArrayList<QSLRecordStr>>() {
            @Override
            public void onChanged(ArrayList<QSLRecordStr> qslRecordStrs) {
                for (QSLRecordStr record : qslRecordStrs) {
                    drawMessage(record);//在地图上画每一个消息
                }
                gridOsmMapView.mapUpdate();
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                closeMessages();
                closeConfigBar();
                doAfterCreate();
            }
        }, 1000);

        setContentView(binding.getRoot());
    }

    /**
     * 在地图上画消息，包括收发消息和CQ消息
     *
     * @param msg 消息
     */
    @SuppressLint("DefaultLocale")
    private void drawMessage(Ft8Message msg) {
        gridOsmMapView.upgradeGridInfo(
                msg.getMaidenheadGrid(mainViewModel.databaseOpr), msg.getMessageText()
                , String.format("%d dBm , %.1f ms", msg.snr, msg.time_sec));
        gridOsmMapView.drawLine(msg, mainViewModel.databaseOpr);
        if (msg.checkIsCQ()) {
            gridOsmMapView.addGridMarker(
                    msg.getMaidenheadGrid(mainViewModel.databaseOpr)
                    , msg);
        }
    }

    private GridOsmMapView.GridPolyLine drawMessage(QSLRecordStr recordStr) {
        gridOsmMapView.gridMapView.post(new Runnable() {
            @Override
            public void run() {
                gridOsmMapView.upgradeGridInfo(recordStr);
                gridOsmMapView.drawLine(recordStr);
            }
        });

        return null;
    }

    private void setShowTipsSwitchClickerListener() {

        binding.trackerShowQsxSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gridOsmMapView.hideInfoWindows();
                if (binding.trackerShowQsxSwitch.isChecked()) {
                    binding.trackerShowQsxSwitch.setText(GeneralVariables.getStringFromResource(
                            R.string.tracker_show_qsx_tips));
                } else {
                    binding.trackerShowQsxSwitch.setText(GeneralVariables.getStringFromResource(
                            R.string.tracker_hide_qsx_tips));
                }
                gridOsmMapView.setShowQSX(binding.trackerShowQsxSwitch.isChecked());
                writeConfig(DataConfigShowQsx, binding.trackerShowQsxSwitch.isChecked() ? "1" : "0");
            }
        });

        binding.trackerShowCqSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gridOsmMapView.hideInfoWindows();
                if (binding.trackerShowCqSwitch.isChecked()) {
                    binding.trackerShowCqSwitch.setText(GeneralVariables.getStringFromResource(
                            R.string.tracker_show_cq_tips));
                } else {
                    binding.trackerShowCqSwitch.setText(GeneralVariables.getStringFromResource(
                            R.string.tracker_hide_cq_tips));
                }
                gridOsmMapView.setShowCQ(binding.trackerShowCqSwitch.isChecked());
                writeConfig(DataConfigShowCQ, binding.trackerShowCqSwitch.isChecked() ? "1" : "0");

            }
        });
    }

    /**
     * 设置显示模式动作
     */
    private void setTipsRadioGroupClickerListener() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.tipsAllRadioButton.isChecked()) {
                    gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.ALL);
                    writeConfig(DataConfigShowMode, "0");
                }
                if (binding.tipsNewRadioButton.isChecked()) {
                    gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.NEW);
                    writeConfig(DataConfigShowMode, "1");
                }
                if (binding.tipsNoneRadioButton.isChecked()) {
                    gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.NONE);
                    binding.trackerShowQsxSwitch.setVisibility(View.GONE);
                    binding.trackerShowCqSwitch.setVisibility(View.GONE);
                    writeConfig(DataConfigShowMode, "2");
                } else {
                    binding.trackerShowQsxSwitch.setVisibility(View.VISIBLE);
                    binding.trackerShowCqSwitch.setVisibility(View.VISIBLE);
                }
            }
        };
        binding.tipsAllRadioButton.setOnClickListener(listener);
        binding.tipsNewRadioButton.setOnClickListener(listener);
        binding.tipsNoneRadioButton.setOnClickListener(listener);

        View.OnClickListener barListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConfigBar();
            }
        };
        binding.closeTIpsImageView.setOnClickListener(barListener);
        binding.trackerInfoModeTextView.setOnClickListener(barListener);

    }

    private void openConfigBar() {
        if (!configBarIsClose) return;
        configBarIsClose = false;
        ObjectAnimator openConfigAnimator = ObjectAnimator.ofFloat(binding.trackerConfigLayout
                , "translationY", 0);
        //openConfigAnimator.setDuration(500);
        openConfigAnimator.setFloatValues(binding.trackerConfigLayout.getHeight() + 10, 0);
        openConfigAnimator.start();
    }

    private void closeConfigBar() {
        if (configBarIsClose) return;
        configBarIsClose = true;
        ObjectAnimator openConfigAnimator = ObjectAnimator.ofFloat(binding.trackerConfigLayout
                , "translationY", 0);
        //openConfigAnimator.setDuration(500);
        openConfigAnimator.setFloatValues(0, binding.trackerConfigLayout.getHeight() + 100);
        openConfigAnimator.start();
    }

    /**
     * 关闭消息栏
     */
    private void openMessage() {
        if (!messageListIsClose) return;
        messageListIsClose = false;
        ObjectAnimator openMessageAnimator = ObjectAnimator.ofFloat(binding.callingListConstraintLayout
                , "translationX", 0);
        //openMessageAnimator.setDuration(500);
        openMessageAnimator.setFloatValues(-binding.callingListConstraintLayout.getWidth() - 10, 0);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(openMessageAnimator);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                binding.openMessagesImageButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                binding.closeMessageImageButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorSet.start();
    }

    /**
     * 动画关闭消息栏
     */
    private void closeMessages() {
        if (messageListIsClose) return;
        messageListIsClose = true;
        ObjectAnimator closeMessageAnimator = ObjectAnimator.ofFloat(binding.callingListConstraintLayout
                , "translationX", 0);
        //closeMessageAnimator.setDuration(500);
        closeMessageAnimator.setFloatValues(0, -binding.callingListConstraintLayout.getWidth() - 10);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(closeMessageAnimator);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                binding.closeMessageImageButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                binding.openMessagesImageButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorSet.start();
    }


    /**
     * 添加浮动按钮
     */

    private void InitFloatView() {
        floatView = new FloatView(this, 32);

        binding.trackConstraint.addView(floatView);
        floatView.setButtonMargin(0);
        floatView.setFloatBoard(FloatView.FLOAT_BOARD.RIGHT);

        floatView.setButtonBackgroundResourceId(R.drawable.float_button_style);

        transButton = floatView.addButton(R.id.grid_tracker_trans, "grid_tracker_trans"
                , R.drawable.ic_baseline_cancel_schedule_send_off
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //如果
                        if (!mainViewModel.ft8TransmitSignal.isActivated()) {
                            mainViewModel.ft8TransmitSignal.restTransmitting();
                        }
                        mainViewModel.ft8TransmitSignal.setActivated(!mainViewModel.ft8TransmitSignal.isActivated());
                        GeneralVariables.resetLaunchSupervision();//复位自动监管
                    }
                });


        //动态添加按钮，建议使用静态的ID，静态ID在VALUES/FLOAT_BUTTON_IDS.XML中设置

        floatView.addButton(R.id.float_freq, "float_freq", R.drawable.ic_baseline_freq_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new FreqDialog(binding.trackConstraint.getContext(), mainViewModel).show();
                    }
                });

        floatView.addButton(R.id.set_volume, "set_volume", R.drawable.ic_baseline_volume_up_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new SetVolumeDialog(binding.trackConstraint.getContext(), mainViewModel).show();
                    }
                });
        floatView.addButton(R.id.grid_tracker_config, "grid_tracker_config"
                , R.drawable.ic_baseline_tracker_settings_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (configBarIsClose) {
                            openConfigBar();
                        } else {
                            closeConfigBar();
                        }
                    }
                });


        //显示当前目标呼号
        mainViewModel.ft8TransmitSignal.mutableToCallsign.observe(this, new Observer<TransmitCallsign>() {
            @Override
            public void onChanged(TransmitCallsign transmitCallsign) {
                binding.trackerTargetTextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.target_callsign)
                        , transmitCallsign.callsign));
            }
        });

        //观察发射状态按钮的变化
        Observer<Boolean> transmittingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (mainViewModel.ft8TransmitSignal.isActivated()) {
                    binding.trackerTargetTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.trackerTargetTextView.setVisibility(View.GONE);
                }

                if (mainViewModel.ft8TransmitSignal.isTransmitting()) {
                    transButton.setImageResource(R.drawable.ic_baseline_send_red_48);
                    transButton.setAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.view_blink));
                } else {
                    //录音对象也要处于启动状态才可以有发射的状态
                    if (mainViewModel.ft8TransmitSignal.isActivated() && mainViewModel.hamRecorder.isRunning()) {
                        transButton.setImageResource(R.drawable.ic_baseline_send_white_48);
                    } else {
                        transButton.setImageResource(R.drawable.ic_baseline_cancel_schedule_send_off);
                    }
                    transButton.setAnimation(null);
                }

            }
        };
        //显示发射状态
        mainViewModel.ft8TransmitSignal.mutableIsTransmitting.observe(this, transmittingObserver);
        mainViewModel.ft8TransmitSignal.mutableIsActivated.observe(this, transmittingObserver);

        floatView.initLocation();
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

    private void readConfig() {
        OnAfterQueryConfig queryConfig = new OnAfterQueryConfig() {
            @Override
            public void doOnBeforeQueryConfig(String KeyName) {

            }

            @Override
            public void doOnAfterQueryConfig(String KeyName, String Value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (KeyName.equalsIgnoreCase(DataConfigShowMode)) {
                            if (Value.equals("1")) {
                                gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.NEW);
                                binding.tipsNewRadioButton.setChecked(true);
                                binding.tipsNewRadioButton.callOnClick();
                            } else if (Value.equals("2")) {
                                gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.NONE);
                                binding.tipsNoneRadioButton.setChecked(true);
                                binding.tipsNoneRadioButton.callOnClick();
                            } else {
                                gridOsmMapView.setShowTipsMode(GridOsmMapView.ShowTipsMode.ALL);
                                binding.tipsAllRadioButton.setChecked(true);
                                binding.tipsAllRadioButton.callOnClick();
                            }
                            //tipsAllRadioButton
                        }
                        if (KeyName.equalsIgnoreCase(DataConfigShowQsx)) {
                            gridOsmMapView.setShowQSX(Value.equals("1"));
                            binding.trackerShowQsxSwitch.setChecked(Value.equals("1"));
                            binding.trackerShowQsxSwitch.callOnClick();
                        }
                        if (KeyName.equalsIgnoreCase(DataConfigShowCQ)) {
                            gridOsmMapView.setShowCQ(Value.equals("1"));
                            binding.trackerShowCqSwitch.setChecked(Value.equals("1"));
                            binding.trackerShowCqSwitch.callOnClick();
                        }
                    }
                });
            }
        };
        mainViewModel.databaseOpr.getConfigByKey(DataConfigShowMode, queryConfig);
        mainViewModel.databaseOpr.getConfigByKey(DataConfigShowQsx, queryConfig);
        mainViewModel.databaseOpr.getConfigByKey(DataConfigShowCQ, queryConfig);
    }

    /**
     * 马上对发起者呼叫
     *
     * @param message 消息
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    private void doCallNow(Ft8Message message) {
        mainViewModel.addFollowCallsign(message.getCallsignFrom());
        if (!mainViewModel.ft8TransmitSignal.isActivated()) {
            mainViewModel.ft8TransmitSignal.setActivated(true);
            GeneralVariables.transmitMessages.add(message);//把消息添加到关注列表中
        }
        //呼叫发启者
        mainViewModel.ft8TransmitSignal.setTransmit(message.getFromCallTransmitCallsign()
                , 1, message.extraInfo);
        mainViewModel.ft8TransmitSignal.transmitNow();

        GeneralVariables.resetLaunchSupervision();//复位自动监管
    }


    /**
     * 设置列表滑动动作
     */
    private void initRecyclerViewAction() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_DRAG
                , ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder
                    , @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            //@RequiresApi(api = Build.VERSION_CODES.N)
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.START) {
                    Ft8Message message = callingListAdapter.getMessageByViewHolder(viewHolder);
                    if (message != null) {
                        //呼叫的目标不能是自己
                        if (!message.getCallsignFrom().equals("<...>")
                                && !message.getCallsignFrom().equals(GeneralVariables.myCallsign)
                                && !(message.i3 == 0 && message.n3 == 0)) {
                            doCallNow(message);
                        }
                    }
                    callingListAdapter.notifyDataSetChanged();
                    //callingListAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                }
                if (direction == ItemTouchHelper.END) {//删除
                    callingListAdapter.deleteMessage(viewHolder.getAdapterPosition());
                    callingListAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                }
            }


            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                Ft8Message message = callingListAdapter.getMessageByViewHolder(viewHolder);
                //制作呼叫背景的图标显示
                final Drawable callIcon = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_baseline_send_red_48);
                final Drawable delIcon = ContextCompat.getDrawable(getBaseContext(), R.drawable.log_item_delete_icon);
                final Drawable background = new ColorDrawable(Color.LTGRAY);

                if (message == null) {
                    return;
                }
                if (message.getCallsignFrom().equals("<...>")) {//如果属于不能呼叫的消息，就不显示图标
                    return;
                }
                Drawable icon;
                if (dX > 0) {
                    icon = delIcon;
                } else {
                    icon = callIcon;
                }
                View itemView = viewHolder.itemView;
                int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                int iconLeft, iconRight, iconTop, iconBottom;
                int backTop, backBottom, backLeft, backRight;
                backTop = itemView.getTop();
                backBottom = itemView.getBottom();
                iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                iconBottom = iconTop + icon.getIntrinsicHeight();
                if (dX > 0) {
                    backLeft = itemView.getLeft();
                    backRight = itemView.getLeft() + (int) dX;
                    background.setBounds(backLeft, backTop, backRight, backBottom);
                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = iconLeft + icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else if (dX < 0) {
                    backRight = itemView.getRight();
                    backLeft = itemView.getRight() + (int) dX;
                    background.setBounds(backLeft, backTop, backRight, backBottom);
                    iconRight = itemView.getRight() - iconMargin;
                    iconLeft = iconRight - icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else {
                    background.setBounds(0, 0, 0, 0);
                    icon.setBounds(0, 0, 0, 0);
                }
                background.draw(c);
                icon.draw(c);

            }
        }).attachToRecyclerView(binding.callMessagesRecyclerView);
    }


    /**
     * 菜单选项
     *
     * @param item 菜单
     * @return 是否选择
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        //Ft8Message ft8Message = (Ft8Message) item.getActionView().getTag();
        int position = (int) item.getActionView().getTag();
        Ft8Message ft8Message = callingListAdapter.getMessageByPosition(position);
        if (ft8Message == null) return super.onContextItemSelected(item);

        GeneralVariables.resetLaunchSupervision();//复位自动监管
        switch (item.getItemId()) {
            case 1://时序与发送者相反！！！
                Log.d(TAG, "呼叫：" + ft8Message.getCallsignTo());
                if (!mainViewModel.ft8TransmitSignal.isActivated()) {
                    mainViewModel.ft8TransmitSignal.setActivated(true);
                }
                mainViewModel.ft8TransmitSignal.setTransmit(ft8Message.getToCallTransmitCallsign()
                        , 1, ft8Message.extraInfo);
                mainViewModel.ft8TransmitSignal.transmitNow();
                break;

            case 3:
                Log.d(TAG, "呼叫：" + ft8Message.getCallsignFrom());
                doCallNow(ft8Message);
                break;

            case 4://回复
                Log.d(TAG, "回复：" + ft8Message.getCallsignFrom());
                mainViewModel.addFollowCallsign(ft8Message.getCallsignFrom());
                if (!mainViewModel.ft8TransmitSignal.isActivated()) {
                    mainViewModel.ft8TransmitSignal.setActivated(true);
                    GeneralVariables.transmitMessages.add(ft8Message);//把消息添加到关注列表中
                }
                //呼叫发启者
                mainViewModel.ft8TransmitSignal.setTransmit(ft8Message.getFromCallTransmitCallsign()
                        , -1, ft8Message.extraInfo);
                mainViewModel.ft8TransmitSignal.transmitNow();
                break;


        }

        return super.onContextItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        binding.osmMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.osmMap.onPause();
    }
}