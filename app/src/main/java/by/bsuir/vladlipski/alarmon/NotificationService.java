package by.bsuir.vladlipski.alarmon;

import java.util.LinkedList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;


public class NotificationService extends Service {
  public class NoAlarmsException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private enum MediaSingleton {
    INSTANCE;

    private MediaPlayer mediaPlayer = null;
    private Ringtone fallbackSound = null;
    private Vibrator vibrator = null;
    private int systemNotificationVolume = 0;

    MediaSingleton() {
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    }

    private void normalizeVolume(Context c) {
      final AudioManager audio =
        (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
      systemNotificationVolume =
          audio.getStreamVolume(AudioManager.STREAM_ALARM);
      audio.setStreamVolume(AudioManager.STREAM_ALARM,
          audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
      mediaPlayer.setVolume(1, 1);
    }

    private void resetVolume(Context c) {
      final AudioManager audio =
        (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
      audio.setStreamVolume(AudioManager.STREAM_ALARM, systemNotificationVolume,
              0);
    }

    private void useContext(Context c) {
      fallbackSound = RingtoneManager.getRingtone(c, AlarmUtil.getDefaultAlarmUri());
      if (fallbackSound == null) {
        Uri superFallback = RingtoneManager.getValidRingtoneUri(c);
        fallbackSound = RingtoneManager.getRingtone(c, superFallback);
      }
      if (fallbackSound != null) {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
              fallbackSound.setStreamType(AudioManager.STREAM_ALARM);
          } else {
              fallbackSound.setAudioAttributes(new AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_ALARM)
                      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                      .build());
          }
      }

      vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void ensureSound() {
      if (!mediaPlayer.isPlaying() &&
          fallbackSound != null && !fallbackSound.isPlaying()) {
        fallbackSound.play();
      }
    }

    private void vibrate() {
      if (vibrator != null) {
        vibrator.vibrate(new long[] {500, 500}, 0);
      }
    }

    public void play(Context c, Uri tone) {
      mediaPlayer.reset();
      mediaPlayer.setLooping(true);
      try {
        mediaPlayer.setDataSource(c, tone);
        mediaPlayer.prepare();
        mediaPlayer.start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void stop() {
      mediaPlayer.stop();
      if (vibrator != null) {
        vibrator.cancel();
      }
      if (fallbackSound != null) {
        fallbackSound.stop();
      }
    }
  }

  private LinkedList<Long> firingAlarms;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  private NotificationManager manager;
  private PendingIntent notificationActivity;
  private Handler handler;
  private Runnable soundCheck;
  private Runnable autoCancel;

  @Override
  public IBinder onBind(Intent intent) {
    return new NotificationServiceInterfaceStub(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    firingAlarms = new LinkedList<>();
    service = new AlarmClockServiceBinder(getApplicationContext());
    service.bind();
    db = new DbAccessor(getApplicationContext());

    MediaSingleton.INSTANCE.useContext(getApplicationContext());

    manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    Intent intent = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
    notificationActivity = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

    handler = new Handler();
    soundCheck = new Runnable() {
      @Override
      public void run() {
        MediaSingleton.INSTANCE.ensureSound();

        long next = AlarmUtil.millisTillNextInterval(AlarmUtil.Interval.SECOND);
        handler.postDelayed(soundCheck, next);
      }
    };
    autoCancel = new Runnable() {
      @Override
      public void run() {
        try {
          acknowledgeCurrentNotification(0);
        } catch (NoAlarmsException e) {
          return;
        }
        Intent notifyActivity = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
        notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyActivity.putExtra(ActivityAlarmNotification.TIMEOUT_COMMAND, true);
        startActivity(notifyActivity);
      }
    };
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();
    service.unbind();
    try {
      WakeLock.assertNoneHeld();
    } catch (WakeLock.WakeLockException e) {
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    handleStart(intent);
    return START_NOT_STICKY;
  }

  private void handleStart(Intent intent) {
    // startService called from alarm receiver with an alarm id url.
    if (intent != null && intent.getData() != null) {
      long alarmId = AlarmUtil.alarmUriToId(intent.getData());
      try {
        WakeLock.assertHeld(alarmId);
      } catch (WakeLock.WakeLockException e) {
      }
      Intent notifyActivity = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
      notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(notifyActivity);

      boolean firstAlarm = firingAlarms.size() == 0;
      if (!firingAlarms.contains(alarmId)) {
        firingAlarms.add(alarmId);
      }

      if (firstAlarm) {
        soundAlarm(alarmId);
      }
    }
  }

  public long currentAlarmId() throws NoAlarmsException {
    if (firingAlarms.size() == 0) {
      throw new NoAlarmsException();
    }
    return firingAlarms.getFirst();
  }

  public int firingAlarmCount() {
    return firingAlarms.size();
  }

  public void acknowledgeCurrentNotification(int snoozeMinutes) throws NoAlarmsException {
    long alarmId = currentAlarmId();
    if (firingAlarms.contains(alarmId)) {
      firingAlarms.remove(alarmId);
      if (snoozeMinutes <= 0) {
        service.acknowledgeAlarm(alarmId);
      } else {
        service.snoozeAlarmFor(alarmId, snoozeMinutes);
      }
    }
    stopNotifying();

    if (firingAlarms.size() == 0) {
      stopSelf();
    } else {
      soundAlarm(alarmId);
    }
    try {
      WakeLock.release(alarmId);
    } catch (WakeLock.WakeLockException e) {
    }
  }

  private void soundAlarm(long alarmId) {
    AlarmSettings settings = db.readAlarmSettings(alarmId);
    if (settings.getVibrate()) {
      MediaSingleton.INSTANCE.vibrate();
    }

    MediaSingleton.INSTANCE.normalizeVolume(getApplicationContext());
    MediaSingleton.INSTANCE.play(getApplicationContext(), settings.getTone());

    handler.post(soundCheck);
    int timeoutMillis = 60 * 1000 * AppSettings.alarmTimeOutMins(getApplicationContext());
    handler.postDelayed(autoCancel, timeoutMillis);
  }

  private void stopNotifying() {
    handler.removeCallbacks(soundCheck);
    handler.removeCallbacks(autoCancel);

    MediaSingleton.INSTANCE.stop();
    MediaSingleton.INSTANCE.resetVolume(getApplicationContext());
  }
}
