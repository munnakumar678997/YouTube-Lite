package com.myapp.youtubelite.webview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
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
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && visibility != View.INVISIBLE) {
            super.onWindowVisibilityChanged(visibility);
        }
        // Don't call super when GONE/INVISIBLE - this keeps audio playing in background
    }
}
