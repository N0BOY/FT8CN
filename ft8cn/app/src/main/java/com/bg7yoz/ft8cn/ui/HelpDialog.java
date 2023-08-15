package com.bg7yoz.ft8cn.ui;
/**
 * 帮助信息的对话框。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.BuildConfig;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class HelpDialog extends Dialog {
    private static final String TAG = "HelpDialog";
    private final Context context;
    private final Activity activity;
    private final String msg;
    private ImageView upImageView;
    private ImageView downImageView;
    private ScrollView scrollView;
    private TextView messageTextView;
    private TextView appNameTextView;
    private TextView buildVersionTextView;
    private final Timer timer=new Timer();
    private TimerTask timeEvent() {
        return new TimerTask() {
            @Override
            public void run() {
                //心跳动作
                //doHeartBeatEvent(onUtcTimer);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setImageVisible();
                    }
                });

            }
        };
    }




    public HelpDialog(@NonNull Context context,Activity activity,String str, boolean fromFile) {
        super(context, R.style.HelpDialog);
        this.context = context;
        this.activity=activity;
        if (fromFile) {
            msg = getTextFromAssets(str);
        } else {
            msg = str;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_dialog_layout);
        messageTextView = (TextView) findViewById(R.id.helpMessage);
        appNameTextView = (TextView) findViewById(R.id.appNameTextView);
        buildVersionTextView = (TextView) findViewById(R.id.buildVersionTextView);
        messageTextView.setText(msg);
        upImageView = (ImageView) findViewById(R.id.scrollUpImageView);
        downImageView = (ImageView) findViewById(R.id.scrollDownImageView);
        scrollView = (ScrollView) findViewById(R.id.helpScrollView);
        upImageView.setVisibility(View.INVISIBLE);
        downImageView.setVisibility(View.INVISIBLE);
        appNameTextView.setText(GeneralVariables.getStringFromResource(R.string.app_name));
        buildVersionTextView.setText(BuildConfig.VERSION_NAME);

        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                Log.d(TAG, String.format("onCreate: getMeasuredHeight:%d, getHeight:%d scroll height:%d"
                        , messageTextView.getMeasuredHeight(), messageTextView.getHeight(),scrollView.getHeight()));


            }
        });

        Button getNewButton=(Button) findViewById(R.id.getNewVersionButton);
        getNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jumpUriToBrowser(context,"https://github.com/N0BOY/FT8CN/releases");
            }
        });

        timer.schedule(timeEvent(), 10, 500);
    }

    private  void jumpUriToBrowser(Context context,String url){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri=Uri.parse(url);
        intent.setData(uri);
        context.startActivity(intent);
    }

    private void setImageVisible(){
        if (scrollView.getScrollY() == 0) {
            upImageView.setVisibility(View.INVISIBLE);
        } else {
            upImageView.setVisibility(View.VISIBLE);
        }

        if (scrollView.getMeasuredHeight() <= messageTextView.getMeasuredHeight()-scrollView.getScrollY()) {
            downImageView.setVisibility(View.VISIBLE);
        } else {
            downImageView.setVisibility(View.INVISIBLE);
        }
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
            params.height = (int) (height * 0.9);
        }else {
            params.width= (int) (width * 0.8);
            params.height = (int) (height * 0.5);
        }
        getWindow().setAttributes(params);

    }



    public String getTextFromAssets(String fileName) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open(fileName);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            return new String(bytes);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
