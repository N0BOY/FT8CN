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
import android.graphics.Rect;
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
import com.bg7yoz.ft8cn.databinding.FragmentLogBinding;
import com.bg7yoz.ft8cn.grid_tracker.GridTrackerMainActivity;
import com.bg7yoz.ft8cn.html.LogHttpServer;
import com.bg7yoz.ft8cn.log.LogCallsignAdapter;
import com.bg7yoz.ft8cn.log.LogQSLAdapter;
import com.bg7yoz.ft8cn.log.OnQueryQSLCallsign;
import com.bg7yoz.ft8cn.log.OnQueryQSLRecordCallsign;
import com.bg7yoz.ft8cn.log.QSLCallsignRecord;
import com.bg7yoz.ft8cn.log.QSLRecordStr;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogFragment extends Fragment {
    private static final String TAG = "LogFragment";
    private FragmentLogBinding binding;
    private MainViewModel mainViewModel;

    private LogCallsignAdapter logCallsignAdapter;
    private LogQSLAdapter logQSLAdapter;
    private boolean loading = false;//防止滑动触发多次查询
    private int lastItemPosition;


    public LogFragment() {
        // Required empty public constructor
    }


    public static LogFragment newInstance(String param1, String param2) {
        LogFragment fragment = new LogFragment();

        return fragment;
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


        //设置显示统计页面按钮
        binding.countImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCountFragment();
            }
        });

        binding.inputMycallEdit.setText(mainViewModel.queryKey);
        queryByCallsign(mainViewModel.queryKey, 0);

        mainViewModel.mutableQueryFilter.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                queryByCallsign(mainViewModel.queryKey, 0);
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

        //过滤条件按钮
        binding.filterImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FilterDialog(requireContext(), mainViewModel).show();
            }
        });

        //导出按钮
        binding.exportImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getLocalIp()==null) {
                    new HelpDialog(requireContext(), requireActivity()
                            , GeneralVariables.getStringFromResource(R.string.export_null)
                            ,false).show();
                }else {
                    new HelpDialog(requireContext(), requireActivity()
                            , String.format(GeneralVariables.getStringFromResource(R.string.export_info)
                            , getLocalIp(), LogHttpServer.DEFAULT_PORT)
                            , false).show();
                }

            }
        });

        binding.logViewStyleimageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainViewModel.logListShowCallsign = !mainViewModel.logListShowCallsign;
                setShowStyle();
                queryByCallsign(binding.inputMycallEdit.getText().toString(), 0);//偏移量0，就是重新查询
            }
        });

        //定位按钮的动作
        binding.locationInMapImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
                intent.putExtra("qslAll", mainViewModel.queryKey);
                intent.putExtra("queryFilter", mainViewModel.queryFilter);
                startActivity(intent);
            }
        });

        return binding.getRoot();
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
                case 0:
                    logQSLAdapter.setRecordIsQSL(position, false);
                    logQSLAdapter.notifyItemChanged(position);
                    break;
                case 1:
                    logQSLAdapter.setRecordIsQSL(position, true);
                    logQSLAdapter.notifyItemChanged(position);
                    break;
                case 2:
                    showQrzFragment(logQSLAdapter.getRecord(position).getCall());
                    break;
                case 3:
                    Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
                    //ArrayList<QSLRecordStr> records=new ArrayList<>();
                    //records.add(logQSLAdapter.getRecord(position));
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

    private boolean itemIsOnScreen(View view) {
        if (view != null) {
            int width = view.getWidth();
            int height = view.getHeight();
            Rect rect = new Rect(0, 0, width, height);
            return view.getLocalVisibleRect(rect);
        }
        return false;
    }

    private void loadQueryData(RecyclerView recyclerView) {
//        if ((!loading)&&(recyclerView.computeVerticalScrollRange()
//                - recyclerView.computeVerticalScrollExtent()
//                - recyclerView.computeVerticalScrollOffset() < 100)) {
        if ((!loading)) {
            //ToastMessage.show("查询");
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
                    loadQueryData(recyclerView);

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

//                if (dy>0){//列表向上移动
//                    loadQueryData(recyclerView);
//                }
                //当列表上滑，接近底部时，开始查询
//                if ((!loading)&&(dy>0)&&(recyclerView.computeVerticalScrollRange()
//                        - recyclerView.computeVerticalScrollExtent()
//                        - recyclerView.computeVerticalScrollOffset() < 100)) {
//                    ToastMessage.show("查询");
//                    if (mainViewModel.logListShowCallsign){
//                        queryByCallsign(mainViewModel.queryKey, logCallsignAdapter.getItemCount());
//                    }else {
//                        queryByCallsign(mainViewModel.queryKey, logQSLAdapter.getItemCount());
//                    }
//                }
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
                    if (mainViewModel.logListShowCallsign) {//此时是显示QSL呼号日志
                        //logCallsignAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    } else {
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
                }

                if (direction == ItemTouchHelper.START) {
                    if (mainViewModel.logListShowCallsign) {//修改手工确认
                        //logCallsignAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    } else {
                        logQSLAdapter.setRecordIsQSL(viewHolder.getAdapterPosition()
                                , !logQSLAdapter.getRecord(viewHolder.getAdapterPosition()).isQSL);
                        logQSLAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }
            }

            //判断列表格式，呼号列表
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder) {
                int swipeFlag;
                if (mainViewModel.logListShowCallsign) {
                    swipeFlag = 0;
                } else {
                    swipeFlag = ItemTouchHelper.START | ItemTouchHelper.END;
                }
                return makeMovementFlags(0, swipeFlag);
            }

            //制作删除背景的图标显示
            Drawable delIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.log_item_delete_icon);
            Drawable qslIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_library_add_check_24);
            Drawable background = new ColorDrawable(Color.LTGRAY);

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY
                    , int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                Drawable icon;
                View itemView = viewHolder.itemView;
                if (dX > 0) {
                    icon = delIcon;
                } else {
                    icon = qslIcon;
                }

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
        }).attachToRecyclerView(binding.logRecyclerView);
    }


    /**
     * 设置显示模式。通联的呼号和日志两种表现方式
     */
    @SuppressLint("NotifyDataSetChanged")
    private void setShowStyle() {

        if (mainViewModel.logListShowCallsign) {
            binding.logViewStyleimageButton.setImageResource(R.drawable.ic_baseline_assignment_ind_24);
            binding.logRecyclerView.setAdapter(logCallsignAdapter);
            logCallsignAdapter.notifyDataSetChanged();
            binding.locationInMapImageButton.setVisibility(View.GONE);//隐藏定位按钮
        } else {
            binding.logViewStyleimageButton.setImageResource(R.drawable.ic_baseline_assignment_24);
            binding.logRecyclerView.setAdapter(logQSLAdapter);
            logQSLAdapter.notifyDataSetChanged();
            binding.locationInMapImageButton.setVisibility(View.VISIBLE);//显示定位按钮
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

            mainViewModel.databaseOpr.getQSLCallsignsByCallsign(false, offset, callsign, mainViewModel.queryFilter
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
            mainViewModel.databaseOpr.getQSLRecordByCallsign(false, offset, callsign, mainViewModel.queryFilter
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
     * 显示统计页面
     */
    private void showCountFragment() {
        //用于Fragment的导航。
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        navHostFragment.getNavController().navigate(R.id.countFragment);
        ;

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
}