package com.bg7yoz.ft8cn.ui;
/**
 * FlexRadio选择对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.flex.FlexRadio;
import com.bg7yoz.ft8cn.flex.FlexRadioFactory;

public class SelectFlexRadioDialog extends Dialog {
    private static final String TAG="SelectFlexRadioDialog";
    private final MainViewModel mainViewModel;
    private RecyclerView flexRecyclerView;
    private ImageView upImage;
    private ImageView downImage;
    private FlexRadioFactory flexRadioFactory;
    private FlexRadioAdapter flexRadioAdapter;
    private ImageButton connectFlexImageButton;
    private EditText inputFlexAddressEdit;



    public SelectFlexRadioDialog(@NonNull Context context, MainViewModel mainViewModel){
        super(context);
        this.mainViewModel = mainViewModel;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_flex_dialog_layout);
        flexRecyclerView=(RecyclerView) findViewById(R.id.flexRadioListRecyclerView);
        flexRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        upImage=(ImageView) findViewById(R.id.flexRadioScrollUpImageView);
        downImage=(ImageView)findViewById(R.id.flexRadioScrollDownImageView);
        inputFlexAddressEdit=(EditText)findViewById(R.id.inputFlexAddressEdit);
        connectFlexImageButton=(ImageButton) findViewById(R.id.connectFlexImageButton);
        connectFlexImageButton.setEnabled(false);
        connectFlexImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastMessage.show(String.format(
                        GeneralVariables.getStringFromResource(R.string.connect_flex_ip)
                        ,inputFlexAddressEdit.getText()));
                FlexRadio flexRadio=new FlexRadio();
                Log.e(TAG, "onClick: "+inputFlexAddressEdit.getText().toString());
                flexRadio.setIp(inputFlexAddressEdit.getText().toString());
                flexRadio.setModel("FlexRadio");
                mainViewModel.connectFlexRadioRig(GeneralVariables.getMainContext(),flexRadio);

                dismiss();
            }
        });

        inputFlexAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                    connectFlexImageButton.setEnabled(!inputFlexAddressEdit.getText().toString().isEmpty());
            }
        });




        flexRadioAdapter=new FlexRadioAdapter();
        flexRecyclerView.setAdapter(flexRadioAdapter);

        flexRadioFactory = FlexRadioFactory.getInstance();


        flexRadioFactory.setOnFlexRadioEvents(new FlexRadioFactory.OnFlexRadioEvents() {

            @Override
            public void OnFlexRadioAdded(FlexRadio flexRadio) {
                flexRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        flexRadioAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void OnFlexRadioInvalid(FlexRadio flexRadio) {
                flexRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        flexRadioAdapter.notifyDataSetChanged();
                    }
                });
            }
        });



        //显示滚动箭头
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setScrollImageVisible();
            }
        }, 1000);
        flexRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        if (flexRecyclerView.canScrollVertically(1)) {
            upImage.setVisibility(View.VISIBLE);
        } else {
            upImage.setVisibility(View.GONE);
        }

        if (flexRecyclerView.canScrollVertically(-1)) {
            downImage.setVisibility(View.VISIBLE);
        } else {
            downImage.setVisibility(View.GONE);
        }
    }

    class FlexRadioAdapter extends RecyclerView.Adapter<FlexRadioAdapter.FlexViewHolder>{


        @NonNull
        @Override
        public FlexViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.flex_device_list_item, parent, false);
            final FlexRadioAdapter.FlexViewHolder holder = new FlexRadioAdapter.FlexViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull FlexViewHolder holder, int position) {
            holder.flexRadio=flexRadioFactory.flexRadios.get(position);
            holder.flexRadioIpTextView.setText(holder.flexRadio.getIp());
            holder.flexRadioSerialNumTextView.setText(holder.flexRadio.getSerial());
            holder.flexRadioNameTextView.setText(holder.flexRadio.getModel());

            holder.flexRadioListConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ToastMessage.show(String.format(
                            GeneralVariables.getStringFromResource(R.string.select_flex_device)
                            ,holder.flexRadio.getModel()));

                    //此处添加连接flex电台的动作
                    //mainViewModel.connectBluetoothRig(GeneralVariables.getMainContext(), holder.device.device);
                    mainViewModel.connectFlexRadioRig(GeneralVariables.getMainContext(),holder.flexRadio);
                    dismiss();
                }
            });

        }

        @Override
        public int getItemCount() {
            return flexRadioFactory.flexRadios.size();
        }



        class FlexViewHolder extends RecyclerView.ViewHolder{
            public FlexRadio flexRadio;
            TextView flexRadioNameTextView,flexRadioIpTextView,flexRadioSerialNumTextView;
            ConstraintLayout flexRadioListConstraintLayout;
            public FlexViewHolder(@NonNull View itemView) {
                super(itemView);
                flexRadioNameTextView=itemView.findViewById(R.id.flexRadioNameTextView);
                flexRadioIpTextView=itemView.findViewById(R.id.flexRadioIpTextView);
                flexRadioSerialNumTextView=itemView.findViewById(R.id.flexRadioSerialNumTextView);
                flexRadioListConstraintLayout=itemView.findViewById(R.id.flexRadioListConstraintLayout);
            }

        }
    }


}
