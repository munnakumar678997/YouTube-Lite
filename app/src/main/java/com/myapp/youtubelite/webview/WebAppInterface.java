package com.myapp.youtubelite.webview;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.myapp.youtubelite.ForegroundService;
import com.myapp.youtubelite.receivers.NotificationActionReceiver;

public class WebAppInterface {

    private static final String TAG = "WebAppInterface";

    // FIX BUG #5 (Memory Leak): Store ApplicationContext, NOT the Activity context.
    // Storing an Activity context here keeps the entire Activity (and its view hierarchy)
    // alive for as long as the WebView lives, causing a classic Android memory leak.
    private final Context mContext;

    public WebAppInterface(Context c) {
        mContext = c.getApplicationContext();
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Called by injected JS when the video starts playing.
     * FIX BUG #5 (Logic): Previously sent ACTION_PLAY_PAUSE (toggle). Since
     * ForegroundService initialises PlaybackState to STATE_PLAYING, the toggle
     * would immediately pause on the very first call. Now sends dedicated ACTION_PLAY.
     */
    @JavascriptInterface
    public void play() {
        startService(ForegroundService.ACTION_PLAY);
    }

    /**
     * Called by injected JS when the video is paused by the user inside YouTube.
     * FIX BUG #5 (Logic): Sends dedicated ACTION_PAUSE instead of ACTION_PLAY_PAUSE.
     */
    @JavascriptInterface
    public void pause() {
        startService(ForegroundService.ACTION_PAUSE);
    }

    @JavascriptInterface
    public void nextTrack() {
        startService(NotificationActionReceiver.ACTION_NEXT);
    }

    @JavascriptInterface
    public void previousTrack() {
        startService(NotificationActionReceiver.ACTION_PREVIOUS);
    }

    private void startService(String action) {
        try {
            Intent serviceIntent = new Intent(mContext, ForegroundService.class);
            serviceIntent.setAction(action);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(serviceIntent);
            } else {
                mContext.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to send action to ForegroundService: " + action, e);
        }
    }
}
