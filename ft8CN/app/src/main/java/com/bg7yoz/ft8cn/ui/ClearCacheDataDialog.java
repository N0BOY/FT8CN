package com.bg7yoz.ft8cn.ui;

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
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.database.OnAfterQueryFollowCallsigns;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ClearCacheDataDialog extends Dialog {
    public static enum CACHE_MODE {FOLLOW_DATA, CALL_LOG}

    private static final String TAG = "HelpDialog";
    private final Context context;
    private final Activity activity;
    private ImageView upImageView;
    private ImageView downImageView;
    private ScrollView scrollView;
    private TextView cacheHelpMessage;
    private TextView appNameTextView;
    private TextView buildVersionTextView;
    private CACHE_MODE cache_mode;
    private DatabaseOpr db;
    private final Timer timer = new Timer();

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


    public ClearCacheDataDialog(@NonNull Context context, Activity activity, DatabaseOpr db, CACHE_MODE cache_mode) {
        super(context, R.style.HelpDialog);
        this.context = context;
        this.activity = activity;
        this.cache_mode = cache_mode;
        this.db=db;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clear_cache_dialog_layout);
        cacheHelpMessage = (TextView) findViewById(R.id.cacheHelpMessage);
        appNameTextView = (TextView) findViewById(R.id.appNameTextView);
        buildVersionTextView = (TextView) findViewById(R.id.buildVersionTextView);
        //cacheHelpMessage.setText(msg);
        upImageView = (ImageView) findViewById(R.id.scrollUpImageView);
        downImageView = (ImageView) findViewById(R.id.scrollDownImageView);
        scrollView = (ScrollView) findViewById(R.id.helpScrollView);
        upImageView.setVisibility(View.INVISIBLE);
        downImageView.setVisibility(View.INVISIBLE);
        appNameTextView.setText(GeneralVariables.getStringFromResource(R.string.app_name));
        buildVersionTextView.setText(BuildConfig.VERSION_NAME);

        Button cancelButton=(Button) findViewById(R.id.cancelClearButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        StringBuilder msg = new StringBuilder();
        if (cache_mode == CACHE_MODE.FOLLOW_DATA) {
            msg.append(GeneralVariables.getStringFromResource(R.string.html_tracking_callsign));
            for (int i = 0; i < GeneralVariables.followCallsign.size(); i++) {
                msg.append("\n" + GeneralVariables.followCallsign.get(i));
            }
            cacheHelpMessage.setText(msg.toString());
        }
        if (cache_mode==CACHE_MODE.CALL_LOG){
            db.getMessageLogTotal(new OnAfterQueryFollowCallsigns() {
                @Override
                public void doOnAfterQueryFollowCallsigns(ArrayList<String> callsigns) {
                    StringBuilder msg=new StringBuilder();
                    msg.append(GeneralVariables.getStringFromResource(R.string.log_statistics_cache));
                    for (int i = 0; i <callsigns.size() ; i++) {
                        msg.append("\n"+callsigns.get(i));
                    }
                    cacheHelpMessage.setText(msg.toString());
                }
            });
        }

        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                Log.d(TAG, String.format("onCreate: getMeasuredHeight:%d, getHeight:%d scroll height:%d"
                        , cacheHelpMessage.getMeasuredHeight(), cacheHelpMessage.getHeight(), scrollView.getHeight()));


            }
        });

        Button getNewButton = (Button) findViewById(R.id.clearCacheButton);
        getNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cache_mode == CACHE_MODE.FOLLOW_DATA) {
                    synchronized (GeneralVariables.followCallsign) {
                        GeneralVariables.followCallsign.clear();
                        db.clearFollowCallsigns();
                    }
                }
                if (cache_mode==CACHE_MODE.CALL_LOG){
                        db.clearLogCacheData();
                }

                dismiss();
            }
        });

        timer.schedule(timeEvent(), 10, 500);
    }

    private void jumpUriToBrowser(Context context, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(url);
        intent.setData(uri);
        context.startActivity(intent);
    }

    private void setImageVisible() {
        if (scrollView.getScrollY() == 0) {
            upImageView.setVisibility(View.INVISIBLE);
        } else {
            upImageView.setVisibility(View.VISIBLE);
        }

        if (scrollView.getMeasuredHeight() <= cacheHelpMessage.getMeasuredHeight() - scrollView.getScrollY()) {
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
        int height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        params.height = (int) (height * 0.6);
        if (width > height) {
            params.width = (int) (width * 0.6);
            params.height = (int) (height * 0.9);
        } else {
            params.width = (int) (width * 0.8);
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
