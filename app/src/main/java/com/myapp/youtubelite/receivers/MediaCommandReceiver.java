package com.myapp.youtubelite.receivers;

    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.os.Build;
    import android.util.Log;
    import android.view.KeyEvent;

    import com.myapp.youtubelite.ForegroundService;

    /**
    * FIX BUG #6: MediaCommandReceiver previously had an EMPTY body, making hardware
    * media keys (headphone inline button, Bluetooth controls, lock-screen media keys)
    * completely non-functional.  This implementation decodes the KeyEvent and forwards
    * it to ForegroundService, which then updates the notification and relays the
    * command to the WebView via MainActivity's BroadcastReceiver.
    */
    public class MediaCommandReceiver extends BroadcastReceiver {

      private static final String TAG = "MediaCommandReceiver";

      @Override
      public void onReceive(Context context, Intent intent) {
          if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;

          KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
          // Only act on key-down to avoid double-firing (down + up)
          if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) return;

          int keyCode = event.getKeyCode();
          Log.d(TAG, "Media button pressed: keyCode=" + keyCode);

          String serviceAction = null;
          switch (keyCode) {
              case KeyEvent.KEYCODE_MEDIA_PLAY:
                  serviceAction = ForegroundService.ACTION_PLAY;
                  break;
              case KeyEvent.KEYCODE_MEDIA_PAUSE:
                  serviceAction = ForegroundService.ACTION_PAUSE;
                  break;
              // Single-click on headphone inline button or Bluetooth play/pause key
              case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
              case KeyEvent.KEYCODE_HEADSETHOOK:
                  serviceAction = NotificationActionReceiver.ACTION_PLAY_PAUSE;
                  break;
              case KeyEvent.KEYCODE_MEDIA_NEXT:
              case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                  serviceAction = NotificationActionReceiver.ACTION_NEXT;
                  break;
              case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
              case KeyEvent.KEYCODE_MEDIA_REWIND:
                  serviceAction = NotificationActionReceiver.ACTION_PREVIOUS;
                  break;
              case KeyEvent.KEYCODE_MEDIA_STOP:
                  serviceAction = NotificationActionReceiver.ACTION_STOP;
                  break;
              default:
                  Log.d(TAG, "Unhandled media key: " + keyCode);
                  break;
          }

          if (serviceAction != null) {
              try {
                  Intent serviceIntent = new Intent(context, ForegroundService.class);
                  serviceIntent.setAction(serviceAction);
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                      context.startForegroundService(serviceIntent);
                  } else {
                      context.startService(serviceIntent);
                  }
                  Log.d(TAG, "Forwarded media key to ForegroundService: " + serviceAction);
              } catch (Exception e) {
                  Log.e(TAG, "Failed to forward media key to ForegroundService", e);
              }
          }
      }
    }
    