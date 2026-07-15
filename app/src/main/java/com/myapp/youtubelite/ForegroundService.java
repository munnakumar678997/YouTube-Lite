package com.myapp.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.myapp.youtubelite.receivers.NotificationActionReceiver;

public class ForegroundService extends Service {

    public static final String CHANNEL_ID = "Media";
    private MediaSession mediaSession;
    private PlaybackState playbackState;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YouTubeLite::BgPlayback");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleIntent(intent);
        }
        startForeground(1, buildNotification(false));
        if (!wakeLock.isHeld()) wakeLock.acquire();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
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
            NotificationManager mgr = getSystemService(NotificationManager.class);
            mgr.createNotificationChannel(channel);
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "YouTubeLiteSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                updateNotification(true);
            }
            @Override
            public void onPause() {
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                updateNotification(false);
            }
            @Override
            public void onSkipToNext() {
                Log.d("ForegroundService", "onSkipToNext");
            }
            @Override
            public void onSkipToPrevious() {
                Log.d("ForegroundService", "onSkipToPrevious");
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_PAUSED);
    }

    private void updatePlaybackState(int state) {
        playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 0, 1.0f)
                .build();
        mediaSession.setPlaybackState(playbackState);
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
                .setContentText("Background Playback")
                .setSmallIcon(R.drawable.ic_play_button)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        // Add media style
        Notification.MediaStyle mediaStyle = new Notification.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);
        builder.setStyle(mediaStyle);

        // Add actions
        builder.addAction(makeAction(android.R.drawable.ic_media_previous, "Previous",
                NotificationActionReceiver.ACTION_PREVIOUS));
        if (isPlaying) {
            builder.addAction(makeAction(android.R.drawable.ic_media_pause, "Pause",
                    NotificationActionReceiver.ACTION_PLAY_PAUSE));
        } else {
            builder.addAction(makeAction(android.R.drawable.ic_media_play, "Play",
                    NotificationActionReceiver.ACTION_PLAY_PAUSE));
        }
        builder.addAction(makeAction(android.R.drawable.ic_media_next, "Next",
                NotificationActionReceiver.ACTION_NEXT));

        return builder.build();
    }

    private Notification.Action makeAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(this, NotificationActionReceiver.class);
        intent.setAction(intentAction);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(icon, title, pi).build();
    }

    private void updateNotification(boolean isPlaying) {
        notificationManager.notify(1, buildNotification(isPlaying));
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case NotificationActionReceiver.ACTION_PLAY_PAUSE:
                if (playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING) {
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
