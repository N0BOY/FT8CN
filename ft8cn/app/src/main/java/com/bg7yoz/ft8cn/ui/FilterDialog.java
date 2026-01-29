package com.bg7yoz.ft8cn.ui;
/**
 * 日志查询的过滤对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;

public class FilterDialog extends Dialog {
    private static final String TAG = "FilterDialog";

    private MainViewModel mainViewModel;
    private RadioButton filterQRZAllButton, filterQRZUploadedButton, filterQRZMissingButton;
    private EditText startDateEditText, endDateEditText, commentFilterEditText;

    public FilterDialog(Context context, MainViewModel mainViewModel) {
        super(context, R.style.HelpDialog);
        this.mainViewModel = mainViewModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_dialog_layout);
        
        // Comment Text Filter
        commentFilterEditText = findViewById(R.id.commentFilterEditText);
        
        // QRZ Upload Status Filter
        filterQRZAllButton = findViewById(R.id.filterQRZAllRadioButton);
        filterQRZUploadedButton = findViewById(R.id.filterQRZUploadedRadioButton);
        filterQRZMissingButton = findViewById(R.id.filterQRZMissingRadioButton);
        
        // Date Range Filter
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);

        // Comment Text Filter listener
        TextWatcher commentWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String comment = s.toString().trim();
                mainViewModel.queryCommentFilter = comment;
                mainViewModel.mutableQueryCommentFilter.postValue(comment);
            }
        };
        commentFilterEditText.addTextChangedListener(commentWatcher);

        // QRZ Upload Status Filter listeners
        View.OnClickListener qrzFilterListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filterQRZAllButton.isChecked()) {
                    mainViewModel.queryQRZFilter = 0;
                    mainViewModel.mutableQueryQRZFilter.postValue(0);
                } else if (filterQRZUploadedButton.isChecked()) {
                    mainViewModel.queryQRZFilter = 1;
                    mainViewModel.mutableQueryQRZFilter.postValue(1);
                } else if (filterQRZMissingButton.isChecked()) {
                    mainViewModel.queryQRZFilter = 2;
                    mainViewModel.mutableQueryQRZFilter.postValue(2);
                }
            }
        };
        filterQRZAllButton.setOnClickListener(qrzFilterListener);
        filterQRZUploadedButton.setOnClickListener(qrzFilterListener);
        filterQRZMissingButton.setOnClickListener(qrzFilterListener);

        // Date Range Filter listeners
        TextWatcher dateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Update start date
                if (s == startDateEditText.getEditableText()) {
                    String date = s.toString().trim();
                    mainViewModel.queryStartDate = date;
                    mainViewModel.mutableQueryStartDate.postValue(date);
                }
                // Update end date
                else if (s == endDateEditText.getEditableText()) {
                    String date = s.toString().trim();
                    mainViewModel.queryEndDate = date;
                    mainViewModel.mutableQueryEndDate.postValue(date);
                }
            }
        };
        startDateEditText.addTextChangedListener(dateWatcher);
        endDateEditText.addTextChangedListener(dateWatcher);
    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //设置对话框的大小，以百分比0.6
        int height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        params.height = (int) (height * 0.7);
        if (width > height) {
            params.width = (int) (width * 0.6);
            params.height = (int) (height * 0.7);
        } else {
            params.width = (int) (width * 0.9);
            params.height = (int) (height * 0.7);
        }
        getWindow().setAttributes(params);
        
        // Set Comment Text Filter
        if (mainViewModel.queryCommentFilter != null && !mainViewModel.queryCommentFilter.isEmpty()) {
            commentFilterEditText.setText(mainViewModel.queryCommentFilter);
        }
        
        // Set QRZ Upload Status Filter
        switch (mainViewModel.queryQRZFilter) {
            case 1:
                filterQRZUploadedButton.setChecked(true);
                break;
            case 2:
                filterQRZMissingButton.setChecked(true);
                break;
            default:
                filterQRZAllButton.setChecked(true);
        }
        
        // Set Date Range Filter
        if (mainViewModel.queryStartDate != null && !mainViewModel.queryStartDate.isEmpty()) {
            startDateEditText.setText(mainViewModel.queryStartDate);
        }
        if (mainViewModel.queryEndDate != null && !mainViewModel.queryEndDate.isEmpty()) {
            endDateEditText.setText(mainViewModel.queryEndDate);
        }
    }
}
