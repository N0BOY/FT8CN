package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.bluetooth.BluetoothConstants;

import java.util.ArrayList;

public class SelectBluetoothDialog extends Dialog {
    class BluetoothDeviceInfo {
        BluetoothDevice device;
        boolean isSPP;
        boolean isHeadSet;

        public BluetoothDeviceInfo(BluetoothDevice device, boolean isSPP,boolean isHeadSet) {
            this.device = device;
            this.isSPP = isSPP;
            this.isHeadSet=isHeadSet;
        }
    }

    private MainViewModel mainViewModel;
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDeviceInfo> devices = new ArrayList<>();
    private RecyclerView devicesRecyclerView;
    private BluetoothDevicesAdapter blueToothListAdapter;

    private ImageView upImage;
    private ImageView downImage;


    public SelectBluetoothDialog(@NonNull Context context, MainViewModel mainViewModel) {
        super(context);
        this.mainViewModel = mainViewModel;

    }



    @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
    private void getBluetoothDevice() {
        devices.clear();
        if (bluetoothAdapter == null) {
            return;
        }
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (BluetoothConstants.checkIsSpp(device)){//spp设备放前面
                devices.add(0,new BluetoothDeviceInfo(device,true,BluetoothConstants.checkIsHeadSet(device)));
                continue;
            }
            if (BluetoothConstants.checkIsHeadSet(device)){//headset设备放后面
                devices.add(new BluetoothDeviceInfo(device, false,BluetoothConstants.checkIsHeadSet(device)));
                continue;
            }
        }

        blueToothListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_bluetooth_dialog_layout);
        devicesRecyclerView = (RecyclerView) findViewById(R.id.bluetoothListRecyclerView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        blueToothListAdapter = new BluetoothDevicesAdapter();
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        devicesRecyclerView.setAdapter(blueToothListAdapter);
        upImage=(ImageView) findViewById(R.id.bluetoothScrollUpImageView);
        downImage=(ImageView)findViewById(R.id.bluetoothScrollDownImageView);
        getBluetoothDevice();

        //显示滚动箭头
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setScrollImageVisible();
            }
        }, 1000);
        devicesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                setScrollImageVisible();
            }
        });

    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //设置对话框的大小，以百分比0.6
        int height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
//        params.height = (int) (height * 0.6);
        if (width > height) {
            params.width = (int) (width * 0.6);
            params.height = (int) (height * 0.6);
        } else {
            params.width = (int) (width * 0.8);
            params.height = (int) (height * 0.5);
        }
        getWindow().setAttributes(params);

    }
    /**
     * 设置界面的上下滚动的图标
     */
    private void setScrollImageVisible() {

        if (devicesRecyclerView.canScrollVertically(1)) {
            upImage.setVisibility(View.VISIBLE);
        } else {
            upImage.setVisibility(View.GONE);
        }

        if (devicesRecyclerView.canScrollVertically(-1)) {
            downImage.setVisibility(View.VISIBLE);
        } else {
            downImage.setVisibility(View.GONE);
        }
    }

    class BluetoothDevicesAdapter extends RecyclerView.Adapter<BluetoothDevicesAdapter.BluetoothHolder> {

        @NonNull
        @Override
        public BluetoothHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.bluetooth_device_list_item, parent, false);
            final BluetoothDevicesAdapter.BluetoothHolder holder = new BluetoothDevicesAdapter.BluetoothHolder(view);
            return holder;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull BluetoothHolder holder, int position) {
            holder.device = devices.get(position);
            holder.bluetoothNameTextView.setText(holder.device.device.getName());
            if (holder.device.isSPP){
                holder.bluetoothNameTextView.setTextColor(getContext().getResources().getColor(
                        R.color.bluetooth_device_enable_color));
            }else {
                holder.bluetoothNameTextView.setTextColor(getContext().getResources().getColor(
                        R.color.bluetooth_device_disable_color));
            }
            if (BluetoothConstants.checkIsHeadSet(holder.device.device)){
                holder.headsetImageView.setVisibility(View.VISIBLE);
            }else {
                holder.headsetImageView.setVisibility(View.GONE);
            }
            if (BluetoothConstants.checkIsSpp(holder.device.device)){
                holder.sppDeviceImageView.setVisibility(View.VISIBLE);
            }else {
                holder.sppDeviceImageView.setVisibility(View.GONE);
            }
            holder.bluetoothAddressTextView.setText(holder.device.device.getAddress());

            holder.bluetoothListConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ToastMessage.show(String.format(
                            GeneralVariables.getStringFromResource(R.string.select_bluetooth_device)
                            ,holder.device.device.getName()));
                    mainViewModel.connectBluetoothRig(GeneralVariables.getMainContext(), holder.device.device);

                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        class BluetoothHolder extends RecyclerView.ViewHolder {
            public BluetoothDeviceInfo device;
            TextView bluetoothNameTextView, bluetoothAddressTextView;
            ConstraintLayout bluetoothListConstraintLayout;
            ImageView headsetImageView,sppDeviceImageView;
            public BluetoothHolder(@NonNull View itemView) {
                super(itemView);
                bluetoothNameTextView = itemView.findViewById(R.id.bluetoothNameTextView);
                bluetoothAddressTextView = itemView.findViewById(R.id.bluetoothAddressTextView);
                bluetoothListConstraintLayout = itemView.findViewById(R.id.bluetoothListConstraintLayout);
                headsetImageView = itemView.findViewById(R.id.headsetImageView);
                sppDeviceImageView = itemView.findViewById(R.id.sppDeviceImageView);
            }
        }
    }

}
