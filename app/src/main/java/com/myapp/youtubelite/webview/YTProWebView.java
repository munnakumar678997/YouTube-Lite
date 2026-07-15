package com.myapp.youtubelite.webview;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class YTProWebView extends WebView {

    public YTProWebView(Context context) {
        super(context);
    }

    public YTProWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YTProWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        if (visibility != GONE) {
            super.onWindowVisibilityChanged(visibility);
        }
    }
}
