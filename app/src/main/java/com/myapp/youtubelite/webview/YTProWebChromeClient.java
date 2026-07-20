package com.myapp.youtubelite.webview;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class YTProWebChromeClient extends WebChromeClient {

    private final Activity activity;
    private View customView;
    private CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;
    private int originalOrientation;

    public YTProWebChromeClient(Activity activity) {
        this.activity = activity;
    }

    // ── Fullscreen video (landscape) ─────────────────────────────────────────
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customView = view;
        originalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
        originalOrientation = activity.getRequestedOrientation();
        customViewCallback = callback;

        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        decor.addView(customView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onHideCustomView() {
        if (customView == null) return;
        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        decor.removeView(customView);
        customView = null;
        activity.getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
        activity.setRequestedOrientation(originalOrientation);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    // ── Multi-window support ──────────────────────────────────────────────────
    // FIX BLACK SCREEN: setSupportMultipleWindows(true) was enabled in MainActivity
    // so that YouTube can open its fullscreen video player in a new window context.
    // Without this handler, onCreateWindow returns false (default) and the video
    // player window is silently discarded → black screen where video should appear.
    // We create a child WebView with the same settings and load it in-place.
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog,
            boolean isUserGesture, android.os.Message resultMsg) {
        WebView childWebView = new WebView(activity);
        WebSettings s = childWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setUserAgentString(view.getSettings().getUserAgentString());

        childWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView w) {
                // Remove child from container when YouTube closes the popup
                if (w.getParent() instanceof ViewGroup) {
                    ((ViewGroup) w.getParent()).removeView(w);
                }
            }
        });
        childWebView.setWebViewClient(new android.webkit.WebViewClient());

        // Overlay the child over the main WebView
        FrameLayout container = (FrameLayout) activity.getWindow().getDecorView();
        container.addView(childWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(childWebView);
        resultMsg.sendToTarget();
        return true;
    }
}
