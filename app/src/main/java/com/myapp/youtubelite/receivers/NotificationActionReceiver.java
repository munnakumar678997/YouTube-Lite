package com.myapp.youtubelite.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.myapp.youtubelite.ForegroundService;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_PLAY_PAUSE = "com.myapp.youtubelite.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.myapp.youtubelite.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.myapp.youtubelite.ACTION_PREVIOUS";
    public static final String ACTION_STOP = "com.myapp.youtubelite.ACTION_STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d("NotificationActionReceiver", "Received action: " + action);

            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.setAction(action);
            context.startService(serviceIntent);
        }
    }
}
