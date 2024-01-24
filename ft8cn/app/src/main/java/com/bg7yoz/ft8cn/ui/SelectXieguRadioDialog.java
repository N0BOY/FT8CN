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
import com.bg7yoz.ft8cn.x6100.X6100Radio;
import com.bg7yoz.ft8cn.x6100.XieguRadioFactory;

public class SelectXieguRadioDialog extends Dialog {
    private static final String TAG="SelectXieguRadioDialog";
    private final MainViewModel mainViewModel;
    private RecyclerView xieguRecyclerView;
    private ImageView upImage;
    private ImageView downImage;
    private XieguRadioFactory xieguRadioFactory;
    private XieguRadioAdapter xieguRadioAdapter;
    private ImageButton connectXieguImageButton;
    private EditText inputXieguAddressEdit;



    public SelectXieguRadioDialog(@NonNull Context context, MainViewModel mainViewModel){
        super(context);
        this.mainViewModel = mainViewModel;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_xiegu_dialog_layout);
        xieguRecyclerView=(RecyclerView) findViewById(R.id.xieguRadioListRecyclerView);
        xieguRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        upImage=(ImageView) findViewById(R.id.xieguRadioScrollUpImageView);
        downImage=(ImageView)findViewById(R.id.xieguRadioScrollDownImageView);
        inputXieguAddressEdit=(EditText)findViewById(R.id.inputXieguAddressEdit);
        connectXieguImageButton=(ImageButton) findViewById(R.id.connectXieguImageButton);
        connectXieguImageButton.setEnabled(false);
        connectXieguImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastMessage.show(String.format(
                        GeneralVariables.getStringFromResource(R.string.select_xiegu_device)
                        ,inputXieguAddressEdit.getText()));
                X6100Radio xieguRadio = new X6100Radio();
                xieguRadio.setRig_ip(inputXieguAddressEdit.getText().toString());
                xieguRadio.setModelName("Xiegu Rig");
                ToastMessage.show(xieguRadio.getRig_ip());
                //此处添加连接6100电台的动作
                mainViewModel.connectXieguRadioRig(GeneralVariables.getMainContext(),xieguRadio);

                dismiss();
            }
        });

        inputXieguAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                    connectXieguImageButton.setEnabled(!inputXieguAddressEdit.getText().toString().isEmpty());
            }
        });




        xieguRadioAdapter=new XieguRadioAdapter();
        xieguRecyclerView.setAdapter(xieguRadioAdapter);

        xieguRadioFactory = XieguRadioFactory.getInstance();


        xieguRadioFactory.setOnXieguRadioEvents(new XieguRadioFactory.OnXieguRadioEvents() {

            @Override
            public void onXieguRadioAdded(X6100Radio xieguRadio) {
                xieguRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        xieguRadioAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onXieguRadioInvalid(X6100Radio flexRadio) {
                xieguRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        xieguRadioAdapter.notifyDataSetChanged();
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
        xieguRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        if (xieguRecyclerView.canScrollVertically(1)) {
            upImage.setVisibility(View.VISIBLE);
        } else {
            upImage.setVisibility(View.GONE);
        }

        if (xieguRecyclerView.canScrollVertically(-1)) {
            downImage.setVisibility(View.VISIBLE);
        } else {
            downImage.setVisibility(View.GONE);
        }
    }

    class XieguRadioAdapter extends RecyclerView.Adapter<XieguRadioAdapter.XieguViewHolder>{


        @NonNull
        @Override
        public XieguViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.xiegu_device_list_item, parent, false);
            final XieguViewHolder holder = new XieguViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull XieguViewHolder holder, int position) {
            holder.xieguRadio=xieguRadioFactory.xieguRadios.get(position);
            holder.xieguRadioIpTextView.setText(holder.xieguRadio.getRig_ip());
            holder.xieguInfoTextView.setText(holder.xieguRadio.getMac());
            holder.xieguRadioNameTextView.setText(holder.xieguRadio.getModelName());

            holder.xieguRadioListConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ToastMessage.show(String.format(
                            GeneralVariables.getStringFromResource(R.string.select_xiegu_device)
                            ,holder.xieguRadio.getModelName()));
                    ToastMessage.show(holder.xieguRadio.getRig_ip());
                    //此处添加连接6100电台的动作
                    mainViewModel.connectXieguRadioRig(GeneralVariables.getMainContext(),holder.xieguRadio);
                    dismiss();
                }
            });

        }

        @Override
        public int getItemCount() {
            return xieguRadioFactory.xieguRadios.size();
        }



        class XieguViewHolder extends RecyclerView.ViewHolder{
            public X6100Radio xieguRadio;
            TextView xieguRadioNameTextView,xieguRadioIpTextView,xieguInfoTextView;
            ConstraintLayout xieguRadioListConstraintLayout;
            public XieguViewHolder(@NonNull View itemView) {
                super(itemView);
                xieguRadioNameTextView=itemView.findViewById(R.id.xieguRadioNameTextView);
                xieguRadioIpTextView=itemView.findViewById(R.id.xieguRadioIpTextView);
                xieguInfoTextView=itemView.findViewById(R.id.xieguInfoTextView);
                xieguRadioListConstraintLayout=itemView.findViewById(R.id.xieguRadioListConstraintLayout);
            }

        }
    }


}
