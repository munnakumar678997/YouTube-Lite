package com.myapp.youtubelite;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import com.myapp.youtubelite.webview.YTProWebView;
import com.myapp.youtubelite.webview.YTProWebViewClient;
import com.myapp.youtubelite.webview.YTProWebChromeClient;
import com.myapp.youtubelite.webview.WebAppInterface;

public class MainActivity extends Activity {

    private YTProWebView webView;
    private BroadcastReceiver playbackActionReceiver;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = findViewById(R.id.youtube_webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Register BroadcastReceiver for playback actions
        playbackActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && webView != null) {
                    switch (action) {
                        case ForegroundService.ACTION_PLAY_WEBVIEW:
                            webView.evaluateJavascript("document.querySelector('video').play();", null);
                            break;
                        case ForegroundService.ACTION_PAUSE_WEBVIEW:
                            webView.evaluateJavascript("document.querySelector('video').pause();", null);
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ForegroundService.ACTION_PLAY_WEBVIEW);
        filter.addAction(ForegroundService.ACTION_PAUSE_WEBVIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playbackActionReceiver, filter);
        }

        webView.setWebViewClient(new YTProWebViewClient(this));
        webView.setWebChromeClient(new YTProWebChromeClient(this));
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.loadUrl("https://m.youtube.com");

        // Start foreground service to enable background playback
        startBackgroundService();
    }

    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            // Ignore if service fails to start
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Do NOT pause webView - allow background audio playback
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackActionReceiver != null) {
            try {
                unregisterReceiver(playbackActionReceiver);
            } catch (Exception e) {
                // Ignore if already unregistered
            }
        }
        if (webView != null) {
            webView.destroy();
        }
    }
}
