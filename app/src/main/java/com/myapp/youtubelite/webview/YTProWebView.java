package com.myapp.youtubelite.webview;

import android.content.Context;
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

    /**
     * FIX (Background Audio): Suppress window-gone/invisible signals that Android
     * sends when the screen turns off or the app moves to background. Without this
     * override, the default WebView.onWindowVisibilityChanged(GONE) internally pauses
     * all media playback — cutting off audio within seconds of screen-off.
     *
     * VISIBLE is always passed through so the video surface reinitialises correctly
     * when the user brings the app back to the foreground.
     *
     * Note: setLayerType(LAYER_TYPE_HARDWARE) has been removed from MainActivity.
     * Hardware layers blocked the SurfaceView "punch-through" that HTML5 video needs
     * to render, causing the black-screen bug.
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == View.VISIBLE) {
            super.onWindowVisibilityChanged(visibility);
        }
        // Suppress GONE / INVISIBLE → audio continues while screen is off.
    }

    /**
     * Do NOT call super.onPause() — it triggers WebView's internal media pause.
     * MainActivity.onPause() calls this, and NOT calling super here keeps audio alive.
     */
    @Override
    public void onPause() {
        // Intentionally empty: do not pause audio/video when Activity goes to background.
    }

    /**
     * Resume timers and rendering when Activity comes to foreground.
     * Called from MainActivity.onResume().
     */
    @Override
    public void onResume() {
        super.onResume();
    }
}
