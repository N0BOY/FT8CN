package com.bg7yoz.ft8cn.ui;
/**
 * 通联纪录的主界面。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.log.ShareLogs;
import com.bg7yoz.ft8cn.databinding.FragmentLogBinding;
import com.bg7yoz.ft8cn.grid_tracker.GridTrackerMainActivity;
import com.bg7yoz.ft8cn.html.LogHttpServer;
import com.bg7yoz.ft8cn.log.LogCallsignAdapter;
import com.bg7yoz.ft8cn.log.LogQSLAdapter;
import com.bg7yoz.ft8cn.log.OnQueryQSLCallsign;
import com.bg7yoz.ft8cn.log.OnQueryQSLRecordCallsign;
import com.bg7yoz.ft8cn.log.QSLCallsignRecord;
import com.bg7yoz.ft8cn.log.QSLRecordStr;
import com.bg7yoz.ft8cn.log.OnShareLogEvents;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.log.ThirdPartyService;
import com.bg7yoz.ft8cn.ui.ToastMessage;
import com.bg7yoz.ft8cn.ui.ActionsSpinnerAdapter;

import java.util.HashMap;

import java.io.File;
import java.util.ArrayList;


public class LogFragment extends Fragment {
    private static final String TAG = "LogFragment";
    private FragmentLogBinding binding;
    private MainViewModel mainViewModel;

    private LogCallsignAdapter logCallsignAdapter;
    private LogQSLAdapter logQSLAdapter;
    private boolean loading = false;//防止滑动触发多次查询
    private int lastItemPosition;
    private ShareLogsProgressDialog dialog = null;//生成共享log的对话框


    public LogFragment() {
        // Required empty public constructor
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = MainViewModel.getInstance(this);

    }

    @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(getLayoutInflater());

        logCallsignAdapter = new LogCallsignAdapter(requireContext(), mainViewModel);
        logQSLAdapter = new LogQSLAdapter(requireContext(), mainViewModel);
        binding.logRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        setShowStyle();//设置显模式


        initRecyclerViewAction();//设置列表滑动动作

        // Set up Actions spinner
        ActionsSpinnerAdapter actionsAdapter = new ActionsSpinnerAdapter(requireContext());
        binding.actionsSpinner.setAdapter(actionsAdapter);
        // Set initial selection to prompt (position 0)
        binding.actionsSpinner.setSelection(0, false);
        
        // Handle action selection - execute immediately
        // Track if this is the initial selection to avoid executing on spinner creation
        final boolean[] isInitialSelection = {true};
        binding.actionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip the initial selection (when spinner is first created)
                if (isInitialSelection[0]) {
                    isInitialSelection[0] = false;
                    return;
                }
                
                // Skip if "Actions" prompt is selected (position 0)
                if (position == 0) {
                    return;
                }
                
                // Get and execute the action
                try {
                    ActionsSpinnerAdapter.Action action = actionsAdapter.getAction(position);
                    if (action != null) {
                        executeAction(action);
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error handling action selection: " + e.getMessage(), e);
                }
                
                // Reset spinner to first position (prompt) so same action can be selected again
                // Use post to avoid interfering with the selection event
                binding.actionsSpinner.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            binding.actionsSpinner.setSelection(0, false);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error resetting spinner: " + e.getMessage(), e);
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        binding.inputMycallEdit.setText(mainViewModel.queryKey);
        queryByCallsign(mainViewModel.queryKey, 0);
        
        // Initialize filter indicator
        updateFilterIndicator();

        mainViewModel.mutableQueryCommentFilter.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                queryByCallsign(mainViewModel.queryKey, 0);
                updateFilterIndicator();
            }
        });

        mainViewModel.mutableQueryQRZFilter.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                queryByCallsign(mainViewModel.queryKey, 0);
                updateFilterIndicator();
            }
        });

        mainViewModel.mutableQueryStartDate.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                queryByCallsign(mainViewModel.queryKey, 0);
                updateFilterIndicator();
            }
        });

        mainViewModel.mutableQueryEndDate.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                queryByCallsign(mainViewModel.queryKey, 0);
                updateFilterIndicator();
            }
        });

        //输入条件监听
        binding.inputMycallEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mainViewModel.queryKey = editable.toString();
                queryByCallsign(mainViewModel.queryKey, 0);
            }
        });

        //判断生成共享log文件的工作线程还在，如果在，就显示对话框
        if (Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue())) {
            showShareDialog();
        }

        return binding.getRoot();
    }


    /**
     * 显示生成log的对话框
     */
    private void showShareDialog() {
        mainViewModel.mutableShareRunning.setValue(true);
        dialog = new ShareLogsProgressDialog(
                binding.getRoot().getContext()
                , mainViewModel,false);

        dialog.show();
        mainViewModel.mutableSharePosition.postValue(0);
        mainViewModel.mutableShareInfo.postValue("");
        mainViewModel.mutableShareCount.postValue(0);
    }


    /**
     * 创建共享日志的数据文件
     */
    private void buildShareLogs() {

        //先显示生成log的对话框
        showShareDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {

                File adiFile = GeneralVariables.writeToTempFile(requireContext()
                        , "FT8CN"
                        , ".txt"
                        , "");


                new ShareLogs().doShareLogs(requireContext(), adiFile
                        , GeneralVariables.getStringFromResource(R.string.share_logs)
                        , mainViewModel.databaseOpr.getDb()
                        , mainViewModel.queryKey
                        , mainViewModel.queryQRZFilter
                        , mainViewModel.queryCommentFilter
                        , mainViewModel.queryStartDate
                        , mainViewModel.queryEndDate
                        , adiFile
                        , false
                        , new OnShareLogEvents() {
                            @Override
                            public void onPreparing(String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                            }

                            @Override
                            public void onShareStart(int count, String info) {
                                mainViewModel.mutableSharePosition.postValue(0);
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareRunning.postValue(true);
                                mainViewModel.mutableShareCount.postValue(count);
                            }

                            @Override
                            public boolean onShareProgress(int count, int position, String info) {
                                mainViewModel.mutableSharePosition.postValue(position);
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareCount.postValue(count);
                                return Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue());
                            }

                            @Override
                            public void afterGet(int count, String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareRunning.postValue(false);
                            }

                            @Override
                            public void onShareFailed(String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                            }
                        });
            }
        }).start();
    }

    /**
     * 弹出菜单选项
     *
     * @param item item
     * @return item
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int position = (Integer) item.getActionView().getTag();
        if (!mainViewModel.logListShowCallsign) {
            switch (item.getItemId()) {
                case 2:
                    showQrzFragment(logQSLAdapter.getRecord(position).getCall());
                    break;
                case 3:
                    Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
                    intent.putExtra("qslList", logQSLAdapter.getRecord(position));
                    startActivity(intent);
                    break;

            }
        } else {
            if (item.getItemId() == 2) {
                showQrzFragment(logCallsignAdapter.getRecord(position).getCallsign());
            }
        }

        return super.onContextItemSelected(item);
    }

//    private boolean itemIsOnScreen(View view) {
//        if (view != null) {
//            int width = view.getWidth();
//            int height = view.getHeight();
//            Rect rect = new Rect(0, 0, width, height);
//            return view.getLocalVisibleRect(rect);
//        }
//        return false;
//    }

    private void loadQueryData() {
        if ((!loading)) {
            if (mainViewModel.logListShowCallsign) {
                queryByCallsign(mainViewModel.queryKey, logCallsignAdapter.getItemCount());
            } else {
                queryByCallsign(mainViewModel.queryKey, logQSLAdapter.getItemCount());
            }
        }
    }

    /**
     * 设置列表滑动动作
     */
    private void initRecyclerViewAction() {

        binding.logRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int itemCount;
                if (mainViewModel.logListShowCallsign) {
                    itemCount = logCallsignAdapter.getItemCount();
                } else {
                    itemCount = logQSLAdapter.getItemCount();
                }
                if (newState == SCROLL_STATE_IDLE &&
                        lastItemPosition == itemCount) {
                    loadQueryData();

                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager manager = (LinearLayoutManager) layoutManager;
                    int firstVisibleItem = manager.findFirstVisibleItemPosition();
                    int l = manager.findLastCompletelyVisibleItemPosition();
                    lastItemPosition = firstVisibleItem + (l - firstVisibleItem) + 1;
                }
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_DRAG
                , ItemTouchHelper.END | ItemTouchHelper.START) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder
                    , @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.END) {
                    //做一个是否删除确认对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setIcon(null);
                    builder.setTitle(GeneralVariables.getStringFromResource(R.string.delete_confirmation));
                    builder.setMessage(GeneralVariables.getStringFromResource(R.string.are_you_sure_delete));
                    builder.setPositiveButton(GeneralVariables.getStringFromResource(R.string.ok_confirmed)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    logQSLAdapter.deleteRecord(viewHolder.getAdapterPosition());//删除日志
                                    logQSLAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                                }
                            });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            logQSLAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton(GeneralVariables.getStringFromResource(R.string.cancel)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    logQSLAdapter.notifyDataSetChanged();
                                }
                            }).show();

                }

                // Swipe left action removed - no longer toggling QSL confirmation
            }

            //判断列表格式，呼号列表
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder) {
                int swipeFlag;
                if (mainViewModel.logListShowCallsign) {
                    swipeFlag = 0;
                } else {
                    // Only allow swipe right (END) for delete, no more swipe left (START) for QSL toggle
                    swipeFlag = ItemTouchHelper.END;
                }
                return makeMovementFlags(0, swipeFlag);
            }

            //制作删除背景的图标显示
            final Drawable delIcon = ContextCompat.getDrawable(requireActivity()
                    , R.drawable.log_item_delete_icon);
            final Drawable background = new ColorDrawable(Color.LTGRAY);

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY
                    , int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                // Only show delete icon when swiping right (no more QSL toggle on left swipe)
                if (dX > 0) {
                    Drawable icon = delIcon;
                    View itemView = viewHolder.itemView;
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconLeft, iconRight, iconTop, iconBottom;
                    int backTop, backBottom, backLeft, backRight;
                    backTop = itemView.getTop();
                    backBottom = itemView.getBottom();
                    iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    iconBottom = iconTop + icon.getIntrinsicHeight();
                    backLeft = itemView.getLeft();
                    backRight = itemView.getLeft() + (int) dX;
                    background.setBounds(backLeft, backTop, backRight, backBottom);
                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = iconLeft + icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    background.draw(c);
                    icon.draw(c);
                }
            }
        }).attachToRecyclerView(binding.logRecyclerView);
    }


    /**
     * 设置显示模式。通联的呼号和日志两种表现方式
     */
    @SuppressLint("NotifyDataSetChanged")
    private void setShowStyle() {

        if (mainViewModel.logListShowCallsign) {
            binding.logRecyclerView.setAdapter(logCallsignAdapter);
            logCallsignAdapter.notifyDataSetChanged();
        } else {
            binding.logRecyclerView.setAdapter(logQSLAdapter);
            logQSLAdapter.notifyDataSetChanged();
        }

    }

    /**
     * 查询日志
     *
     * @param callsign 呼号
     */
    private void queryByCallsign(String callsign, int offset) {
        loading = true;//开始读数据
        //分两种查询
        if (mainViewModel.logListShowCallsign) {
            if (offset == 0) {//说明是新增记录
                logCallsignAdapter.clearRecords();//清空记录
            }

            mainViewModel.databaseOpr.getQSLCallsignsByCallsign(false, offset, callsign, mainViewModel.queryQRZFilter
                    , mainViewModel.queryCommentFilter, mainViewModel.queryStartDate, mainViewModel.queryEndDate
                    , new OnQueryQSLCallsign() {
                        @Override
                        public void afterQuery(ArrayList<QSLCallsignRecord> records) {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    logCallsignAdapter.setQSLCallsignList(records);
                                    loading = false;
                                }
                            });
                        }
                    });
        } else {
            if (offset == 0) {//说明是新增记录
                logQSLAdapter.clearRecords();
            }
            mainViewModel.databaseOpr.getQSLRecordByCallsign(false, offset, callsign, mainViewModel.queryQRZFilter
                    , mainViewModel.queryCommentFilter, mainViewModel.queryStartDate, mainViewModel.queryEndDate
                    , new OnQueryQSLRecordCallsign() {
                        @Override
                        public void afterQuery(ArrayList<QSLRecordStr> records) {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    logQSLAdapter.setQSLList(records);
                                    loading = false;
                                }
                            });
                        }
                    });

        }
    }


    /**
     * Update the filter indicator to show which filters are active
     */
    private void updateFilterIndicator() {
        ArrayList<String> activeFilters = new ArrayList<>();
        
        // Check comment filter
        if (mainViewModel.queryCommentFilter != null && !mainViewModel.queryCommentFilter.trim().isEmpty()) {
            activeFilters.add(GeneralVariables.getStringFromResource(R.string.filter_indicator_comment));
        }
        
        // Check QRZ filter
        if (mainViewModel.queryQRZFilter == 1) {
            activeFilters.add(GeneralVariables.getStringFromResource(R.string.filter_indicator_qrz_uploaded));
        } else if (mainViewModel.queryQRZFilter == 2) {
            activeFilters.add(GeneralVariables.getStringFromResource(R.string.filter_indicator_qrz_missing));
        }
        
        // Check date range filter
        boolean hasStartDate = mainViewModel.queryStartDate != null && !mainViewModel.queryStartDate.trim().isEmpty();
        boolean hasEndDate = mainViewModel.queryEndDate != null && !mainViewModel.queryEndDate.trim().isEmpty();
        if (hasStartDate || hasEndDate) {
            activeFilters.add(GeneralVariables.getStringFromResource(R.string.filter_indicator_date_range));
        }
        
        // Show or hide indicator based on active filters
        if (!activeFilters.isEmpty()) {
            String filterText = GeneralVariables.getStringFromResource(R.string.filters_active) + " " 
                    + String.join(", ", activeFilters);
            binding.filterIndicatorTextView.setText(filterText);
            binding.filterIndicatorTextView.setVisibility(View.VISIBLE);
        } else {
            binding.filterIndicatorTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Execute the selected action from the Actions spinner
     */
    private void executeAction(ActionsSpinnerAdapter.Action action) {
        try {
            switch (action) {
                case EXPORT:
                    if (getLocalIp() == null) {
                        new HelpDialog(requireContext(), requireActivity()
                                , GeneralVariables.getStringFromResource(R.string.export_null)
                                , false).show();
                    } else {
                        new HelpDialog(requireContext(), requireActivity()
                                , String.format(GeneralVariables.getStringFromResource(R.string.export_info)
                                , getLocalIp(), LogHttpServer.DEFAULT_PORT)
                                , false).show();
                    }
                    break;
                    
                case SHARE_LOGS:
                    buildShareLogs();
                    break;
                    
                case MAP_LOCATION:
                    Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
                    intent.putExtra("qslAll", mainViewModel.queryKey);
                    // Note: GridTrackerMainActivity may need to be updated to use new filters
                    startActivity(intent);
                    break;
                    
                case VIEW_STYLE:
                    mainViewModel.logListShowCallsign = !mainViewModel.logListShowCallsign;
                    setShowStyle();
                    queryByCallsign(binding.inputMycallEdit.getText().toString(), 0);
                    break;
                    
                case UPLOAD_QRZ:
                    uploadUnuploadedQSOsToQRZ();
                    break;
                    
                case FILTER:
                    new FilterDialog(requireContext(), mainViewModel).show();
                    break;
                    
                case STATISTICS:
                    showCountFragment();
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error executing action: " + e.getMessage(), e);
            ToastMessage.show("Error: " + e.getMessage());
        }
    }

    /**
     * 显示统计页面
     */
    private void showCountFragment() {
        //用于Fragment的导航。
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        navHostFragment.getNavController().navigate(R.id.countFragment);
    }

    /**
     * 显示QRZ查询界面
     *
     * @param callsign 呼号
     */
    private void showQrzFragment(String callsign) {
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        Bundle bundle = new Bundle();
        bundle.putString(QRZ_Fragment.CALLSIGN_PARAM, callsign);
        navHostFragment.getNavController().navigate(R.id.QRZ_Fragment, bundle);
    }


    /**
     * 上传未上传到QRZ.com的QSO记录
     * Upload unuploaded QSO records to QRZ.com
     */
    private void uploadUnuploadedQSOsToQRZ() {
        String apiKey = GeneralVariables.getQrzApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.qrz_api_key_not_set));
            return;
        }

        // Show progress message
        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.upload_qrz_uploading));

        // Get all unuploaded QSOs
        mainViewModel.databaseOpr.getUnuploadedQSOs(new OnQueryQSLRecordCallsign() {
            @Override
            public void afterQuery(ArrayList<QSLRecordStr> records) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (records.isEmpty()) {
                            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.upload_qrz_no_unuploaded));
                            return;
                        }

                        // Convert all QSLRecordStr to QSLRecord and upload in bulk
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<QSLRecord> qslRecords = new ArrayList<>();
                                
                                // Convert all records
                                for (QSLRecordStr recordStr : records) {
                                    try {
                                        QSLRecord qslRecord = convertQSLRecordStrToQSLRecord(recordStr);
                                        if (qslRecord != null) {
                                            qslRecords.add(qslRecord);
                                        }
                                    } catch (Exception e) {
                                        android.util.Log.e(TAG, "Error converting QSO: " + e.getMessage());
                                    }
                                }

                                if (qslRecords.isEmpty()) {
                                    requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.upload_qrz_no_unuploaded));
                                        }
                                    });
                                    return;
                                }

                                // Upload all QSOs in a single bulk API call
                                ThirdPartyService.BulkUploadResult result = ThirdPartyService.UploadMultipleToQRZ(qslRecords);

                                // Show result toast using COUNT from API response
                                final int uploadedCount = result.count;
                                final boolean success = result.success;
                                final String errorMsg = result.errorMessage;
                                
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (success) {
                                            // Use COUNT from API response
                                            ToastMessage.show(String.format(
                                                    GeneralVariables.getStringFromResource(R.string.upload_qrz_complete),
                                                    uploadedCount));
                                        } else {
                                            // Show error message
                                            ToastMessage.show(String.format(
                                                    GeneralVariables.getStringFromResource(R.string.upload_qrz_failed),
                                                    errorMsg != null ? errorMsg : "Unknown error"));
                                        }
                                        // Refresh the log list to show updated upload status
                                        queryByCallsign(mainViewModel.queryKey, 0);
                                    }
                                });
                            }
                        }).start();
                    }
                });
            }
        });
    }

    /**
     * Convert QSLRecordStr to QSLRecord for QRZ upload
     */
    private QSLRecord convertQSLRecordStrToQSLRecord(QSLRecordStr recordStr) {
        try {
            // Parse time_on to get date and time separately
            String[] timeOnParts = recordStr.getTime_on().split("-");
            if (timeOnParts.length != 2) {
                return null;
            }
            String qsoDate = timeOnParts[0];
            String timeOn = timeOnParts[1];

            // Parse time_off to get date and time separately
            String[] timeOffParts = recordStr.getTime_off().split("-");
            String qsoDateOff = timeOffParts.length == 2 ? timeOffParts[0] : qsoDate;
            String timeOff = timeOffParts.length == 2 ? timeOffParts[1] : timeOn;

            // Build HashMap for QSLRecord constructor (similar to ADIF import)
            HashMap<String, String> map = new HashMap<>();
            map.put("CALL", recordStr.getCall());
            if (recordStr.getGridsquare() != null && !recordStr.getGridsquare().isEmpty()) {
                map.put("GRIDSQUARE", recordStr.getGridsquare());
            }
            if (recordStr.getMode() != null && !recordStr.getMode().isEmpty()) {
                map.put("MODE", recordStr.getMode());
            } else {
                map.put("MODE", "FT8");
            }
            if (recordStr.getRst_sent() != null && !recordStr.getRst_sent().isEmpty()) {
                map.put("RST_SENT", recordStr.getRst_sent());
            }
            if (recordStr.getRst_rcvd() != null && !recordStr.getRst_rcvd().isEmpty()) {
                map.put("RST_RCVD", recordStr.getRst_rcvd());
            }
            map.put("QSO_DATE", qsoDate);
            map.put("TIME_ON", timeOn);
            map.put("QSO_DATE_OFF", qsoDateOff);
            map.put("TIME_OFF", timeOff);
            if (recordStr.getBand() != null && !recordStr.getBand().isEmpty()) {
                map.put("BAND", recordStr.getBand());
            }
            if (recordStr.getFreq() != null && !recordStr.getFreq().isEmpty()) {
                map.put("FREQ", recordStr.getFreq());
            }
            if (recordStr.getStation_callsign() != null && !recordStr.getStation_callsign().isEmpty()) {
                map.put("STATION_CALLSIGN", recordStr.getStation_callsign());
            } else {
                map.put("STATION_CALLSIGN", GeneralVariables.myCallsign);
            }
            if (recordStr.getMy_gridsquare() != null && !recordStr.getMy_gridsquare().isEmpty()) {
                map.put("MY_GRIDSQUARE", recordStr.getMy_gridsquare());
            }
            if (recordStr.getComment() != null && !recordStr.getComment().isEmpty()) {
                map.put("COMMENT", recordStr.getComment());
            }

            // Create QSLRecord using HashMap constructor
            QSLRecord qslRecord = new QSLRecord(map);

            // Set flags
            qslRecord.isQSL = recordStr.isQSL;
            qslRecord.isLotW_import = recordStr.isLotW_import;
            qslRecord.isLotW_QSL = recordStr.isLotW_QSL;
            qslRecord.isQRZ_uploaded = recordStr.isQRZ_uploaded;

            return qslRecord;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error converting QSLRecordStr to QSLRecord: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取本机IP地址
     *
     * @return IP 地址
     */
    @Nullable
    private String getLocalIp() {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress == 0) {
            return null;
        }
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "." + (ipAddress >> 16 & 0xff)
                + "." + (ipAddress >> 24 & 0xff));
    }

    @Override
    public void onDestroy() {
        //判断生成共享log线程是否还在工作，如果还在工作，要销毁对话框，防止出现not attached to window manager错误
        if (Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue())) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }

        super.onDestroy();
    }
}