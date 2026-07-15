package com.myapp.youtubelite.webview;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

public class YTProWebChromeClient extends WebChromeClient {

    private Activity activity;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;
    private int originalSystemUiVisibility;

    public YTProWebChromeClient(Activity activity) {
        this.activity = activity;
    }

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

        ((FrameLayout) activity.getWindow().getDecorView()).addView(customView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
        activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onHideCustomView() {
        ((FrameLayout) activity.getWindow().getDecorView()).removeView(customView);
        customView = null;
        activity.getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
        activity.setRequestedOrientation(originalOrientation);
        customViewCallback.onCustomViewHidden();
        customViewCallback = null;
    }
}
