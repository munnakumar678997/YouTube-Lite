package com.myapp.youtubelite;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.app.Activity;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import com.myapp.youtubelite.webview.YTProWebView;
import com.myapp.youtubelite.webview.YTProWebViewClient;
import com.myapp.youtubelite.webview.YTProWebChromeClient;
import com.myapp.youtubelite.webview.WebAppInterface;

public class MainActivity extends Activity {

    private YTProWebView webView;
    private BroadcastReceiver playbackActionReceiver;
    private static final String TAG = "MainActivity";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
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
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setLoadsImagesAutomatically(true);

        // FIX BLACK SCREEN (C): Allow YouTube to open its fullscreen player in a new
        // window context. Without this, the video player silently fails → black box.
        webSettings.setSupportMultipleWindows(true);

        // FIX BLACK SCREEN (B): Set a real Chrome Mobile User-Agent.
        // Android WebView's default UA contains "wv" which YouTube detects and uses
        // to serve a restricted, often broken, video experience (black screen).
        // A standard Chrome Mobile UA makes YouTube treat us like a normal browser.
        webSettings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10; Mobile) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.6367.82 Mobile Safari/537.36");

        // NOTE (BLACK SCREEN FIX A): setLayerType(LAYER_TYPE_HARDWARE) intentionally
        // REMOVED. YouTube's HTML5 video player uses a SurfaceView internally, which
        // renders by punching a transparent hole in the view hierarchy. A hardware
        // layer on the WebView blocks that hole, making the video appear as solid black.
        // The default layer type (LAYER_TYPE_NONE) lets the SurfaceView render correctly.

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // BroadcastReceiver built here, registered in onStart (Bug #4 fix)
        playbackActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && webView != null) {
                    Log.d(TAG, "Received broadcast action: " + action);
                    switch (action) {
                        case ForegroundService.ACTION_PLAY_WEBVIEW:
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    webView.evaluateJavascript(
                                        "(function() {" +
                                        "  var v = document.querySelector('video');" +
                                        "  if (v) { v.play(); }" +
                                        "})();", null);
                                }
                            });
                            break;
                        case ForegroundService.ACTION_PAUSE_WEBVIEW:
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    webView.evaluateJavascript(
                                        "(function() {" +
                                        "  var v = document.querySelector('video');" +
                                        "  if (v) {" +
                                        "    window._ytlUserPaused = true;" +
                                        "    v.pause();" +
                                        "  }" +
                                        "})();", null);
                                }
                            });
                            break;
                        case ForegroundService.ACTION_NEXT_WEBVIEW:
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    webView.evaluateJavascript(
                                        "(function() {" +
                                        "  var selectors = ['.ytp-next-button','button.next-button'," +
                                        "    '.ytm-autonav-bar a','a.ytm-next-button'," +
                                        "    '[aria-label=\"Next\"]','[aria-label=\"Next video\"]'," +
                                        "    'button[aria-label=\"Next\"]','.player-controls-next'];" +
                                        "  for (var i = 0; i < selectors.length; i++) {" +
                                        "    var el = document.querySelector(selectors[i]);" +
                                        "    if (el) { el.click(); return; }" +
                                        "  }" +
                                        "  var endScreen = document.querySelector('a.ytp-endscreen-next,.ytm-endscreen-overlay a');" +
                                        "  if (endScreen) { endScreen.click(); return; }" +
                                        "  var related = document.querySelector('ytm-compact-video-renderer a,ytd-compact-video-renderer a');" +
                                        "  if (related) { related.click(); return; }" +
                                        "  var v = document.querySelector('video');" +
                                        "  if (v) { v.currentTime = v.duration; }" +
                                        "})();", null);
                                }
                            });
                            break;
                        case ForegroundService.ACTION_PREVIOUS_WEBVIEW:
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    webView.evaluateJavascript(
                                        "(function() {" +
                                        "  var selectors = ['.ytp-prev-button','button.previous-button'," +
                                        "    '[aria-label=\"Previous\"]','[aria-label=\"Previous video\"]'," +
                                        "    'button[aria-label=\"Previous\"]','.player-controls-previous'];" +
                                        "  for (var i = 0; i < selectors.length; i++) {" +
                                        "    var el = document.querySelector(selectors[i]);" +
                                        "    if (el) { el.click(); return; }" +
                                        "  }" +
                                        "  var v = document.querySelector('video');" +
                                        "  if (v) {" +
                                        "    if (v.currentTime > 3) { v.currentTime = 0; }" +
                                        "    else { window.history.back(); }" +
                                        "  }" +
                                        "})();", null);
                                }
                            });
                            break;
                    }
                }
            }
        };

        webView.setWebViewClient(new YTProWebViewClient(this));
        webView.setWebChromeClient(new YTProWebChromeClient(this));
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.loadUrl("https://m.youtube.com");
        startBackgroundService();
    }

    // FIX BUG #4: BroadcastReceiver moved from onCreate/onDestroy to onStart/onStop.
    @Override
    protected void onStart() {
        super.onStart();
        if (playbackActionReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ForegroundService.ACTION_PLAY_WEBVIEW);
            filter.addAction(ForegroundService.ACTION_PAUSE_WEBVIEW);
            filter.addAction(ForegroundService.ACTION_NEXT_WEBVIEW);
            filter.addAction(ForegroundService.ACTION_PREVIOUS_WEBVIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // FIX BUG #7: RECEIVER_NOT_EXPORTED — no external app can send fake commands
                registerReceiver(playbackActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(playbackActionReceiver, filter);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playbackActionReceiver != null) {
            try {
                unregisterReceiver(playbackActionReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver already unregistered", e);
            }
        }
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
            Log.w(TAG, "Failed to start background service", e);
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
        // Do NOT call webView.onPause() — this allows background audio to keep playing.
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
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
