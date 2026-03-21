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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

        // fitsSystemWindows 由系统自动处理顶部/底部间距 → 布局正确
        webView.setFitsSystemWindows(true);

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
                // fitsSystemWindows=true 系统自动处理间距
                // 只处理导航栏底部补偿（小白条）
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

        lastDark = isDarkMode();
        applySystemBars(lastDark);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean dark = isDarkMode();
        applySystemBars(dark);
    }

    private void applySystemBars(boolean dark) {
        Window w = getWindow();
        View dv = w.getDecorView();

        // 状态栏颜色匹配 app 背景
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

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

        // 小米/HyperOS 专用：强制设置状态栏图标暗色模式
        setMiuiStatusBarDarkMode(dark);
    }

    /**
     * 小米 HyperOS / MIUI 专用 API
     * 标准 Android 的 SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 在小米系统上可能不生效
     * 需要调用小米私有 API setStatusBarDarkMode
     */
    private void setMiuiStatusBarDarkMode(boolean dark) {
        try {
            Class<?> clazz = getWindow().getClass();
            Method method = clazz.getMethod("setStatusBarDarkMode", boolean.class);
            method.invoke(getWindow(), !dark);  // true=暗色图标, false=亮色图标
        } catch (Exception e) {
            try {
                // 备选方案：通过 WindowManager.LayoutParams 反射
                Class<?> lpClass = getWindow().getAttributes().getClass();
                Field darkFlag = lpClass.getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                int flags = darkFlag.getInt(getWindow().getAttributes());
                darkFlag.setInt(getWindow().getAttributes(), dark ? (flags & ~0x00000020) : (flags | 0x00000020));
            } catch (Exception e2) {
                // 不是小米设备，忽略
            }
        }
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
