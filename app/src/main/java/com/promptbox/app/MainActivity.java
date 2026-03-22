package com.promptbox.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static final String TAG = "PromptBox";
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
                String js = "(function(){" +
                    "var s=document.createElement('style');" +
                    "s.textContent='" +
                    ".sb-hd{padding-top:max(50px," + sb + "px) !important}" +
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
        webView.addJavascriptInterface(new ShareBridge(this), "AndroidShare");
        webView.addJavascriptInterface(new StatusBarBridge(this), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");

        applySystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySystemBars();
    }

    private void applySystemBars() {
        Window w = getWindow();
        boolean dark = isDarkMode();

        if (dark) {
            w.setStatusBarColor(Color.parseColor("#171717"));
        } else {
            w.setStatusBarColor(Color.parseColor("#FAFAFE"));
        }

        w.setNavigationBarColor(Color.TRANSPARENT);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        View dv = w.getDecorView();
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

        applyMiuiDarkMode(dark);
    }

    private void applyMiuiDarkMode(boolean dark) {
        try {
            Method method = Window.class.getMethod("setStatusBarDarkMode", boolean.class);
            method.invoke(getWindow(), dark);
            Log.d(TAG, "Xiaomi setStatusBarDarkMode: " + dark);
        } catch (Exception e) {
            Log.d(TAG, "Not Xiaomi device or method unavailable: " + e.getMessage());
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
        applySystemBars();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    class StatusBarBridge {
        private Activity activity;
        StatusBarBridge(Activity a) { activity = a; }

        @JavascriptInterface
        public int getStatusBarHeight() {
            int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
            return id > 0 ? activity.getResources().getDimensionPixelSize(id) : 50;
        }

        @JavascriptInterface
        public void setStatusBarColor(String color) {
            activity.runOnUiThread(() -> {
                try {
                    Window w = activity.getWindow();
                    w.setStatusBarColor(Color.parseColor(color));
                    // Determine if dark based on luminance
                    int c = Color.parseColor(color);
                    double luminance = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255;
                    boolean darkBg = luminance < 0.5;
                    View dv = w.getDecorView();
                    int flags = dv.getSystemUiVisibility();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (darkBg) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    dv.setSystemUiVisibility(flags);
                    try {
                        Method method = Window.class.getMethod("setStatusBarDarkMode", boolean.class);
                        method.invoke(w, darkBg);
                    } catch (Exception e) {}
                } catch (Exception e) {
                    Log.e(TAG, "setStatusBarColor failed", e);
                }
            });
        }
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

    class ShareBridge {
        private Activity activity;
        private Uri lastSavedUri;

        ShareBridge(Activity a) { activity = a; }

        @JavascriptInterface
        public String saveBase64Png(String base64, String filename) {
            // base64 is raw PNG data (no data: prefix)
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            OutputStream os = null;
            Uri uri = null;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PromptBox");
                    uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) return "";
                    os = activity.getContentResolver().openOutputStream(uri);
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PromptBox");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, filename);
                    os = new FileOutputStream(file);
                    uri = Uri.fromFile(file);
                }
                os.write(data);
                os.flush();
                lastSavedUri = uri;
                return uri.toString();
            } catch (Exception e) {
                Log.e(TAG, "saveBase64Png failed", e);
                return "";
            } finally {
                if (os != null) try { os.close(); } catch (Exception e) {}
            }
        }

        @JavascriptInterface
        public void shareImage(String base64, String title) {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            File cacheDir = activity.getCacheDir();
            File shareDir = new File(cacheDir, "share");
            if (!shareDir.exists()) shareDir.mkdirs();
            File file = new File(shareDir, "share_image.png");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    file
                );
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                intent.putExtra(Intent.EXTRA_SUBJECT, title);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.runOnUiThread(() -> {
                    activity.startActivity(Intent.createChooser(intent, "分享到"));
                });
            } catch (Exception e) {
                Log.e(TAG, "shareImage failed", e);
                activity.runOnUiThread(() -> Toast.makeText(activity, "分享失败", Toast.LENGTH_SHORT).show());
            }
        }
    }
}
