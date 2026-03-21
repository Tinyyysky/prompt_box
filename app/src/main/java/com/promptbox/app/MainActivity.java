package com.promptbox.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private boolean lastDark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        setContentView(webView);

        // 让内容绘制到系统栏后面
        webView.setFitsSystemWindows(false);

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
                int sb = resDimen("status_bar_height");
                int nb = resDimen("navigation_bar_height");
                // 内容绘制到系统栏后面 → 需要手动加 padding
                // 状态栏高度加到 .hdr，让标题不被遮挡
                String js = "(function(){" +
                    "var s=document.createElement('style');" +
                    "s.textContent='" +
                    ".hdr{padding-top:max(12px," + sb + "px) !important}" +
                    ".search{top:max(0px," + sb + "px) !important}" +
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

        // 首次设置系统栏
        lastDark = isDarkMode();
        applySystemBars(lastDark);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean dark = isDarkMode();
        if (dark != lastDark) {
            lastDark = dark;
            applySystemBars(dark);
        }
    }

    private void applySystemBars(boolean dark) {
        Window w = getWindow();
        View dv = w.getDecorView();

        // 全透明 → app 背景透出，深色/浅色自动适配
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);

        int flags = dv.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (dark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (dark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        dv.setSystemUiVisibility(flags);
    }

    private boolean isDarkMode() {
        int nightFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightFlags == Configuration.UI_MODE_NIGHT_YES;
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean dark = isDarkMode();
        if (dark != lastDark) {
            lastDark = dark;
            applySystemBars(dark);
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
            ((Activity) ctx).runOnUiThread(() -> Toast.makeText(ctx, "\u5DF2\u590D\u5236", Toast.LENGTH_SHORT).show());
        }
    }
}
