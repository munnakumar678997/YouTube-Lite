package com.myapp.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.myapp.youtubelite.receivers.NotificationActionReceiver;

public class ForegroundService extends Service {

    public static final String CHANNEL_ID = "YouTubeLiteServiceChannel";
    private MediaSession mediaSession;
    private PlaybackState playbackState;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleIntent(intent);
        }
        startForeground(1, buildNotification(false)); // Start with paused state
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Lite Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "YouTubeLiteMediaSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                Log.d("ForegroundService", "onPlay");
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                updateNotification(true);
                // TODO: Send event to WebView to play video
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d("ForegroundService", "onPause");
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                updateNotification(false);
                // TODO: Send event to WebView to pause video
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.d("ForegroundService", "onSkipToNext");
                // TODO: Send event to WebView to play next video
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.d("ForegroundService", "onSkipToPrevious");
                // TODO: Send event to WebView to play previous video
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_PAUSED);
    }

    private void updatePlaybackState(int state) {
        playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 0, 1.0f)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    private Notification buildNotification(boolean isPlaying) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("YouTube Lite")
                .setContentText("Background Playback")
                .setSmallIcon(R.drawable.ic_play_button) // TODO: Create this drawable
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        // Add actions
        if (isPlaying) {
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", NotificationActionReceiver.ACTION_PREVIOUS));
            builder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", NotificationActionReceiver.ACTION_PLAY_PAUSE));
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", NotificationActionReceiver.ACTION_NEXT));
        } else {
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", NotificationActionReceiver.ACTION_PREVIOUS));
            builder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", NotificationActionReceiver.ACTION_PLAY_PAUSE));
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", NotificationActionReceiver.ACTION_NEXT));
        }

        return builder.build();
    }

    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), NotificationActionReceiver.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    private void updateNotification(boolean isPlaying) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, buildNotification(isPlaying));
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case NotificationActionReceiver.ACTION_PLAY_PAUSE:
                if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
                    mediaSession.getController().getTransportControls().pause();
                } else {
                    mediaSession.getController().getTransportControls().play();
                }
                break;
            case NotificationActionReceiver.ACTION_NEXT:
                mediaSession.getController().getTransportControls().skipToNext();
                break;
            case NotificationActionReceiver.ACTION_PREVIOUS:
                mediaSession.getController().getTransportControls().skipToPrevious();
                break;
            case NotificationActionReceiver.ACTION_STOP:
                stopSelf();
                break;
        }
    }
}
