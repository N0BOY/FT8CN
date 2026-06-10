package com.bg7yoz.ft8cn.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;

public class ShareLogsProgressDialog extends Dialog {
    private static final String TAG = "ShareLogsProgressDialog";

    private final MainViewModel mainViewModel;
    private TextView shareDataInfoTextView,shareProgressTextView;
    private ProgressBar shareFileDataProgressBar;
    private final boolean isImportMode;
    private final Observer<String> shareInfoObserver = new Observer<String>() {
        @Override
        public void onChanged(String s) {
            if (shareDataInfoTextView != null) {
                shareDataInfoTextView.setText(s);
            }
        }
    };
    private final Observer<Integer> sharePositionObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            if (shareFileDataProgressBar != null) {
                shareFileDataProgressBar.setProgress(integer);
            }
        }
    };
    private final Observer<Boolean> importRunningObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            if (!aBoolean) {
                dismiss();
            }
        }
    };
    private final Observer<Boolean> shareRunningObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            if (!aBoolean) {
                dismiss();
            }
        }
    };
    private final Observer<Integer> shareCountObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            if (shareFileDataProgressBar != null) {
                shareFileDataProgressBar.setMax(integer);
            }
        }
    };
    //private final int progressMax;


    public ShareLogsProgressDialog(@NonNull Context context
            , MainViewModel projectsViewModel, boolean isImportMode) {
        super(context, R.style.ShareProgressDialog);
        this.mainViewModel = projectsViewModel;
        this.isImportMode = isImportMode;
        //this.progressMax =progressMax;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_file_progress_dialog);
        setCancelable(false);//禁止点击旁边退出
        shareDataInfoTextView = findViewById(R.id.shareDataInfoTextView);
        shareProgressTextView = findViewById(R.id.shareProgressTextView);
        if (isImportMode){
            shareProgressTextView.setText(R.string.share_import_log_data);
        }else {
            shareProgressTextView.setText(R.string.preparing_log_data);
        }

        shareFileDataProgressBar = findViewById(R.id.shareFileDataProgressBar);
        Button cancelShareButton = findViewById(R.id.cancelShareButton);


        mainViewModel.mutableShareInfo.observeForever(shareInfoObserver);
        mainViewModel.mutableSharePosition.observeForever(sharePositionObserver);
        mainViewModel.mutableImportShareRunning.observeForever(importRunningObserver);
        mainViewModel.mutableShareRunning.observeForever(shareRunningObserver);
        mainViewModel.mutableShareCount.observeForever(shareCountObserver);

        cancelShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isImportMode) {
                    mainViewModel.mutableImportShareRunning.postValue(false);
                } else {
                    mainViewModel.mutableShareRunning.postValue(false);
                }
            }
        });

    }

    @Override
    public void dismiss() {
        mainViewModel.mutableShareInfo.removeObserver(shareInfoObserver);
        mainViewModel.mutableSharePosition.removeObserver(sharePositionObserver);
        mainViewModel.mutableImportShareRunning.removeObserver(importRunningObserver);
        mainViewModel.mutableShareRunning.removeObserver(shareRunningObserver);
        mainViewModel.mutableShareCount.removeObserver(shareCountObserver);
        super.dismiss();
    }
}
