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
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Edge-to-edge with transparent bars
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        webView = new WebView(this);
        setContentView(webView);

        // Edge-to-edge WebView
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
                // Inject CSS to handle status bar inset (safe area)
                int statusBarHeight = getStatusBarHeight();
                int navBarHeight = getNavBarHeight();
                String js = String.format(
                    "javascript:(function() {" +
                    "  var style = document.createElement('style');" +
                    "  style.textContent = 'html { padding-top: %dpx !important; padding-bottom: %dpx !important; box-sizing: border-box; }" +
                    "  body { padding-top: env(safe-area-inset-top, %dpx) !important; padding-bottom: env(safe-area-inset-bottom, %dpx) !important; }';" +
                    "  document.head.appendChild(style);" +
                    "})()",
                    statusBarHeight, navBarHeight, statusBarHeight, navBarHeight
                );
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

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getNavBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
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
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
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
