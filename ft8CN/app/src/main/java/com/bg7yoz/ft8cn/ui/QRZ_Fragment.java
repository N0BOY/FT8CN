package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;

import com.bg7yoz.ft8cn.databinding.FragmentQrzBinding;


public class QRZ_Fragment extends Fragment {
    private FragmentQrzBinding binding;

    public static final String CALLSIGN_PARAM = "callsign";

    private String qrzParam;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            qrzParam = getArguments().getString(CALLSIGN_PARAM);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentQrzBinding.inflate(inflater, container, false);
        binding.qrzWebView.getSettings().setJavaScriptEnabled(true);
        //binding.qrzWebView.getSettings().setDomStorageEnabled(true);       // 这个要加上
        binding.qrzWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        binding.qrzWebView.getSettings().setUseWideViewPort(true);

        binding.qrzWebView.getSettings().setLoadWithOverviewMode(true);
        binding.qrzWebView.getSettings().setSupportZoom(true);
        binding.qrzWebView.getSettings().setBuiltInZoomControls(true);
        String url = String.format("https://www.qrz.com/db/%s",qrzParam);
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                super.shouldOverrideUrlLoading(view, url);
                view.loadUrl(url);
                return true;
            }
        };
        binding.qrzWebView.setWebViewClient(webViewClient);


        binding.qrzWebView.loadUrl(url);
        return binding.getRoot();
    }
}