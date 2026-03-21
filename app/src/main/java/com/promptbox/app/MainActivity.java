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
                int sb = resDimen("status_bar_height");
                int nb = resDimen("navigation_bar_height");
                // 注入 CSS safe-area 修复 + 调试面板
                String js = "(function(){" +
                    // CSS fix
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
                    // Debug overlay
                    "var d=document.createElement('div');" +
                    "d.id='debug-overlay';" +
                    "d.style.cssText='position:fixed;top:0;left:0;right:0;z-index:9999;background:rgba(0,0,0,.85);color:#0f0;font:12px/1.5 monospace;padding:8px 12px;white-space:pre-wrap;';" +
                    "var hdr=document.querySelector('.hdr');" +
                    "var hRect=hdr?hdr.getBoundingClientRect():null;" +
                    "var sRect=document.querySelector('.search')?document.querySelector('.search').getBoundingClientRect():null;" +
                    "d.textContent='DEBUG\\n'" +
                    "  +'statusBarH(real):'+(" + sb + ")+'px\\n'" +
                    "  +'navBarH:'+(" + nb + ")+'px\\n'" +
                    "  +'vh:'+window.innerHeight+'px\\n'" +
                    "  +'dvh:'+getComputedStyle(document.documentElement).getPropertyValue('--sai-top')+'\\n'" +
                    "  +'hdr.top:'+(hRect?hRect.top:'?')+'px\\n'" +
                    "  +'hdr.height:'+(hRect?hRect.height:'?')+'px\\n'" +
                    "  +'hdr.padTop:'+(hdr?getComputedStyle(hdr).paddingTop:'?')+'\\n'" +
                    "  +'search.top:'+(sRect?sRect.top:'?')+'px\\n'" +
                    "  +'app.top:'+(document.querySelector('.app')?document.querySelector('.app').getBoundingClientRect().top:'?')+'px';" +
                    "document.body.appendChild(d);" +
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

    private int resDimen(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到前台时设置状态栏样式
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#F2F2F7"));
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // 深色图标
        }
        decor.setSystemUiVisibility(flags);
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
