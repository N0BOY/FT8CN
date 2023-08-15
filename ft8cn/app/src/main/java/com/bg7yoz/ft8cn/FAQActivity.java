package com.bg7yoz.ft8cn;
/**
 * 问题收集的WebView。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class FAQActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faqactivity);


        WebView webView = (WebView) findViewById(R.id.faqWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);       // 这个要加上

        /* 获得 webview url，请注意url单词是product而不是products，products是旧版本的参数，用错地址将不能成功提交 */
        //String url = "https://www.qrz.com/db/BG7YOZ";
        String url = "https://support.qq.com/product/415890";

        /* WebView 内嵌 Client 可以在APP内打开网页而不是跳出到浏览器 */
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                super.shouldOverrideUrlLoading(view, url);
                view.loadUrl(url);
                return true;
            }
        };
        webView.setWebViewClient(webViewClient);

        webView.loadUrl(url);
    }

}