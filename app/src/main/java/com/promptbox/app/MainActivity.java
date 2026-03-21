package com.promptbox.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.webkit.ValueCallback;
import android.graphics.Color;
import android.view.View;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // fitsSystemWindows=true 已经处理了顶部间距
                // 只需要处理导航栏底部补偿
                int nb = resDimen("navigation_bar_height");
                String js = "(function(){" +
                    "var s=document.createElement('style');" +
                    "s.textContent='" +
                    ".batch-bar{bottom:max(20px," + nb + "px) !important}" +
                    ".m-ft{padding-bottom:max(12px," + nb + "px) !important}" +
                    ".fab{bottom:calc(max(20px," + nb + "px) + 8px) !important}" +
                    ".toast{bottom:max(28px," + nb + "px) !important}" +
                    "';" +
                    "document.head.appendChild(s);" +
                    "})()";
                view.evaluateJavascript(js, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams fc) {
                fileCallback = cb;
                Intent intent = fc.createIntent();
                try {
                    startActivityForResult(intent, 1001);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new ClipboardBridge(this), "AndroidClipboard");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window w = getWindow();
        // 状态栏 = 浅灰色（匹配 app 背景 #F2F2F7）+ 深色图标
        w.setStatusBarColor(Color.parseColor("#F2F2F7"));
        w.setNavigationBarColor(Color.TRANSPARENT);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        View dv = w.getDecorView();
        int flags = dv.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        dv.setSystemUiVisibility(flags);
    }

    private int resDimen(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    class ClipboardBridge {
        private Context ctx;
        ClipboardBridge(Context c) { ctx = c; }

        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("prompt", text));
            runOnUiThread(() -> Toast.makeText(ctx, "\u5DF2\u590D\u5236", Toast.LENGTH_SHORT).show());
        }
    }
}
