package com.myapp.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.myapp.youtubelite.receivers.NotificationActionReceiver;

public class ForegroundService extends Service {

    public static final String CHANNEL_ID = "Media";
    public static final String ACTION_START_FOREGROUND_SERVICE  = "com.myapp.youtubelite.ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_PLAY_WEBVIEW              = "com.myapp.youtubelite.ACTION_PLAY_WEBVIEW";
    public static final String ACTION_PAUSE_WEBVIEW             = "com.myapp.youtubelite.ACTION_PAUSE_WEBVIEW";
    public static final String ACTION_NEXT_WEBVIEW              = "com.myapp.youtubelite.ACTION_NEXT_WEBVIEW";
    public static final String ACTION_PREVIOUS_WEBVIEW          = "com.myapp.youtubelite.ACTION_PREVIOUS_WEBVIEW";
    // FIX BUG #5: Dedicated play/pause actions — WebAppInterface no longer uses
    // the toggle ACTION_PLAY_PAUSE, which would incorrectly pause on first call.
    public static final String ACTION_PLAY  = "com.myapp.youtubelite.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.myapp.youtubelite.ACTION_PAUSE";

    private MediaSession mediaSession;
    private PlaybackState playbackState;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;
    private static final String TAG = "ForegroundService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // initMediaSession MUST run before buildNotification (which uses the session token)
        initMediaSession();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YouTubeLite::BgPlayback");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create WakeLock", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // FIX BUG #1 & #2: Android 14+ (API 34) requires the foreground service type
            // to be passed to startForeground(); omitting it throws IllegalStateException.
            // The 3-argument overload was added in API 29 — safe to use from API 29+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, buildNotification(true),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(1, buildNotification(true));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && intent.getAction() != null) {
            handleIntent(intent);
        }

        // FIX BUG #3: Acquire WakeLock indefinitely (released explicitly in onDestroy).
        // The previous 10-minute timed acquire caused background audio to stop mid-playback.
        try {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire WakeLock", e);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release WakeLock", e);
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background playback controls");
            channel.setSound(null, null);
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) {
                mgr.createNotificationChannel(channel);
            }
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "YouTubeLiteSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "MediaSession onPlay");
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                updateNotification(true);
                sendActionToMainActivity(ACTION_PLAY_WEBVIEW);
            }
            @Override
            public void onPause() {
                Log.d(TAG, "MediaSession onPause");
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                updateNotification(false);
                sendActionToMainActivity(ACTION_PAUSE_WEBVIEW);
            }
            @Override
            public void onSkipToNext() {
                Log.d(TAG, "MediaSession onSkipToNext");
                sendActionToMainActivity(ACTION_NEXT_WEBVIEW);
            }
            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "MediaSession onSkipToPrevious");
                sendActionToMainActivity(ACTION_PREVIOUS_WEBVIEW);
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_PLAYING);
    }

    private void updatePlaybackState(int state) {
        playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_PLAY_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_NEXT |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 0, 1.0f)
                .build();
        if (mediaSession != null) {
            mediaSession.setPlaybackState(playbackState);
        }
    }

    private Notification buildNotification(boolean isPlaying) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("YouTube Lite")
                .setContentText(isPlaying ? "Playing" : "Background Playback")
                .setSmallIcon(R.drawable.ic_play_button)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (mediaSession != null) {
            Notification.MediaStyle mediaStyle = new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2);
            builder.setStyle(mediaStyle);
        }

        builder.addAction(makeAction(android.R.drawable.ic_media_previous, "Previous",
                NotificationActionReceiver.ACTION_PREVIOUS, 1));
        if (isPlaying) {
            builder.addAction(makeAction(android.R.drawable.ic_media_pause, "Pause",
                    NotificationActionReceiver.ACTION_PLAY_PAUSE, 2));
        } else {
            builder.addAction(makeAction(android.R.drawable.ic_media_play, "Play",
                    NotificationActionReceiver.ACTION_PLAY_PAUSE, 2));
        }
        builder.addAction(makeAction(android.R.drawable.ic_media_next, "Next",
                NotificationActionReceiver.ACTION_NEXT, 3));

        return builder.build();
    }

    private Notification.Action makeAction(int icon, String title, String intentAction,
            int requestCode) {
        Intent intent = new Intent(this, NotificationActionReceiver.class);
        intent.setAction(intentAction);
        intent.setPackage(getPackageName()); // FIX BUG #7: explicit intent for security
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(icon, title, pi).build();
    }

    private void updateNotification(boolean isPlaying) {
        if (notificationManager != null) {
            notificationManager.notify(1, buildNotification(isPlaying));
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        Log.d(TAG, "handleIntent action: " + action);
        switch (action) {
            case NotificationActionReceiver.ACTION_PLAY_PAUSE:
                // FIX BUG #2: Directly update state and broadcast instead of roundtripping
                // through MediaSession transport controls (unreliable on some Android versions).
                if (playbackState != null &&
                        playbackState.getState() == PlaybackState.STATE_PLAYING) {
                    updatePlaybackState(PlaybackState.STATE_PAUSED);
                    updateNotification(false);
                    sendActionToMainActivity(ACTION_PAUSE_WEBVIEW);
                } else {
                    updatePlaybackState(PlaybackState.STATE_PLAYING);
                    updateNotification(true);
                    sendActionToMainActivity(ACTION_PLAY_WEBVIEW);
                }
                break;
            case NotificationActionReceiver.ACTION_NEXT:
                sendActionToMainActivity(ACTION_NEXT_WEBVIEW);
                break;
            case NotificationActionReceiver.ACTION_PREVIOUS:
                sendActionToMainActivity(ACTION_PREVIOUS_WEBVIEW);
                break;
            // FIX BUG #5: WebAppInterface now sends ACTION_PLAY / ACTION_PAUSE directly.
            // These only update the service state; no back-broadcast to WebView needed
            // because the WebView is already in the correct state.
            case ACTION_PLAY:
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                updateNotification(true);
                break;
            case ACTION_PAUSE:
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                updateNotification(false);
                break;
            case NotificationActionReceiver.ACTION_STOP:
                stopSelf();
                break;
            case ACTION_START_FOREGROUND_SERVICE:
                break;
        }
    }

    private void sendActionToMainActivity(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName()); // FIX BUG #7: restrict to own package
        sendBroadcast(intent);
    }
}
