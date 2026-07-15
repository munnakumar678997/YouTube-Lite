package com.myapp.youtubelite.webview;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.myapp.youtubelite.ForegroundService;

public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void play() {
        Intent serviceIntent = new Intent(mContext, ForegroundService.class);
        serviceIntent.setAction("com.myapp.youtubelite.ACTION_PLAY_PAUSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
    }

    @JavascriptInterface
    public void pause() {
        Intent serviceIntent = new Intent(mContext, ForegroundService.class);
        serviceIntent.setAction("com.myapp.youtubelite.ACTION_PLAY_PAUSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
    }

    @JavascriptInterface
    public void nextTrack() {
        Intent serviceIntent = new Intent(mContext, ForegroundService.class);
        serviceIntent.setAction("com.myapp.youtubelite.ACTION_NEXT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
    }

    @JavascriptInterface
    public void previousTrack() {
        Intent serviceIntent = new Intent(mContext, ForegroundService.class);
        serviceIntent.setAction("com.myapp.youtubelite.ACTION_PREVIOUS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
    }
}
