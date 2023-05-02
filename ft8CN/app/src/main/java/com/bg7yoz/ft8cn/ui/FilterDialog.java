package com.bg7yoz.ft8cn.ui;
/**
 * 日志查询的过滤对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;

public class FilterDialog extends Dialog {
    private static final String TAG = "FilterDialog";

    private MainViewModel mainViewModel;
    private RadioButton filterAllButton,filterIsQslButton,filterNoneQslButton;

    public FilterDialog(Context  context,MainViewModel mainViewModel) {
        super(context, R.style.HelpDialog);
        this.mainViewModel=mainViewModel;

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_dialog_layout);
        filterAllButton= (RadioButton) findViewById(R.id.filterAllRadioButton);
        filterIsQslButton= (RadioButton) findViewById(R.id.filterIsQSLRadioButton);
        filterNoneQslButton= (RadioButton) findViewById(R.id.filterNoneQSLRadioButton);

        View.OnClickListener onClickListener=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.e(TAG, "onClick: ---------------->" );
                if (filterAllButton.isChecked()){
                    mainViewModel.queryFilter=0;
                    mainViewModel.mutableQueryFilter.postValue(0);
                }
                if (filterIsQslButton.isChecked()){
                    mainViewModel.queryFilter=1;
                    mainViewModel.mutableQueryFilter.postValue(1);
                }
                if (filterNoneQslButton.isChecked()){
                    mainViewModel.queryFilter=2;
                    mainViewModel.mutableQueryFilter.postValue(2);
                }
                FilterDialog.this.dismiss();
            }
        };
        filterAllButton.setOnClickListener(onClickListener);
        filterIsQslButton.setOnClickListener(onClickListener);
        filterNoneQslButton.setOnClickListener(onClickListener);
    }


    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //设置对话框的大小，以百分比0.6
        int height=getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width=getWindow().getWindowManager().getDefaultDisplay().getWidth();
        params.height = (int) (height * 0.6);
        if (width>height) {
            params.width = (int) (width * 0.6);
            params.height = (int) (height * 0.6);
        }else {
            params.width= (int) (width * 0.8);
            params.height = (int) (height * 0.5);
        }
        getWindow().setAttributes(params);
        switch (mainViewModel.queryFilter){
            case 1:
                filterIsQslButton.setChecked(true);//只显示QSL的
                break;
            case 2:
                filterNoneQslButton.setChecked(true);//只显示没有QSL的
                break;
            default:
                filterAllButton.setChecked(true);//显示全部
        }
    }




}
