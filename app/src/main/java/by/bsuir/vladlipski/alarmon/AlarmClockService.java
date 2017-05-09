package by.bsuir.vladlipski.alarmon;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

public final class AlarmClockService extends Service {
  public final static String COMMAND_EXTRA = "command";
  public final static int COMMAND_UNKNOWN = 1;
  public final static int COMMAND_DEVICE_BOOT = 3;
  public final static int NOTIFICATION_BAR_ID = 69;

  private DbAccessor db;
  private PendingAlarmList pendingAlarms;

  @Override
  public void onCreate() {
    super.onCreate();

    db = new DbAccessor(getApplicationContext());
    pendingAlarms = new PendingAlarmList(getApplicationContext());

    for (Long alarmId : db.getEnabledAlarms()) {
      if (pendingAlarms.pendingTime(alarmId) != null) {
        continue;
      }

      AlarmTime alarmTime = null;

      AlarmInfo info = db.readAlarmInfo(alarmId);

      if (info != null) {
          alarmTime = info.getTime();
      }

      pendingAlarms.put(alarmId, alarmTime);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    handleStart(intent);
    return START_STICKY;
  }

  private void handleStart(Intent intent) {
    if (intent != null && intent.hasExtra(COMMAND_EXTRA)) {
      Bundle extras = intent.getExtras();
      int command = extras.getInt(COMMAND_EXTRA, COMMAND_UNKNOWN);

      final Handler handler = new Handler();
      final Runnable maybeShutdown = new Runnable() {
        @Override
        public void run() {
          if (pendingAlarms.size() == 0) {
            stopSelf();
          }
        }
      };

      switch (command) {
        case COMMAND_DEVICE_BOOT:
          handler.post(maybeShutdown);
          break;
        default:
          throw new IllegalArgumentException("Unknown service command.");
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    db.closeConnections();

    final NotificationManager manager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(NOTIFICATION_BAR_ID);

  }

  @Override
  public IBinder onBind(Intent intent) {
    return new AlarmClockInterfaceStub(getApplicationContext(), this);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    if (pendingAlarms.size() == 0) {
      stopSelf();
      return false;
    }

    return true;
  }

  public AlarmTime pendingAlarm(long alarmId) {
    return pendingAlarms.pendingTime(alarmId);
  }

  public AlarmTime[] pendingAlarmTimes() {
    return pendingAlarms.pendingTimes();
  }

  public long resurrectAlarm(AlarmTime time, String alarmName, boolean enabled) {
      long alarmId =  db.newAlarm(time, enabled, alarmName);

      if (enabled) {
          scheduleAlarm(alarmId);
      }

      return alarmId;
  }

  public void createAlarm(AlarmTime time) {
    long alarmId = db.newAlarm(time, true, "");
    scheduleAlarm(alarmId);
  }

    public void deleteAlarm(long alarmId) {
        pendingAlarms.remove(alarmId);

        db.deleteAlarm(alarmId);
    }

  public void deleteAllAlarms() {
    for (Long alarmId : db.getAllAlarms()) {
      deleteAlarm(alarmId);
    }
  }

  public void scheduleAlarm(long alarmId) {
    AlarmInfo info = db.readAlarmInfo(alarmId);
    if (info == null) {
      return;
    }
    pendingAlarms.put(alarmId, info.getTime());
    db.enableAlarm(alarmId, true);

    final Intent self = new Intent(getApplicationContext(), AlarmClockService.class);
    startService(self);
  }

  public void acknowledgeAlarm(long alarmId) {
    AlarmInfo info = db.readAlarmInfo(alarmId);
    if (info == null) {
      return;
    }
    pendingAlarms.remove(alarmId);
    db.enableAlarm(alarmId, false);
  }

  public void dismissAlarm(long alarmId) {
    AlarmInfo info = db.readAlarmInfo(alarmId);
    if (info == null) {
      return;
    }
    pendingAlarms.remove(alarmId);
    db.enableAlarm(alarmId, false);
  }

  public void snoozeAlarm(long alarmId) {
    snoozeAlarmFor(alarmId, db.readAlarmSettings(alarmId).getSnoozeMinutes());
  }

  public void snoozeAlarmFor(long alarmId, int minutes) {
    pendingAlarms.remove(alarmId);
    AlarmTime time = AlarmTime.snoozeInMillisUTC(minutes);
    pendingAlarms.put(alarmId, time);
  }
}