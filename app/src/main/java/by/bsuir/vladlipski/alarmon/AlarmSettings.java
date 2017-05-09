package by.bsuir.vladlipski.alarmon;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public final class AlarmSettings {
  static public final long DEFAULT_SETTINGS_ID = -1;

  private Uri tone;
  private int snoozeMinutes;
  private boolean vibrate;

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues();
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
    values.put(DbHelper.SETTINGS_COL_SNOOZE, snoozeMinutes);
    values.put(DbHelper.SETTINGS_COL_VIBRATE, vibrate);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
      DbHelper.SETTINGS_COL_ID,
      DbHelper.SETTINGS_COL_SNOOZE,
      DbHelper.SETTINGS_COL_VIBRATE
    };
  }

  public AlarmSettings() {
    tone = AlarmUtil.getDefaultAlarmUri();
    snoozeMinutes = 10;
    vibrate = false;
  }

  public AlarmSettings(AlarmSettings rhs) {
    tone = AlarmUtil.getDefaultAlarmUri();
    snoozeMinutes = rhs.snoozeMinutes;
    vibrate = rhs.vibrate;
  }

  public AlarmSettings(Cursor cursor) {
    cursor.moveToFirst();
    tone = AlarmUtil.getDefaultAlarmUri();
    snoozeMinutes = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SNOOZE));
    vibrate = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VIBRATE)) == 1;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmSettings)) {
      return false;
    }
    AlarmSettings rhs = (AlarmSettings) o;
    return snoozeMinutes == rhs.snoozeMinutes
      && vibrate == rhs.vibrate;
  }

  public Uri getTone() {
    return tone;
  }

  public int getSnoozeMinutes() {
    return snoozeMinutes;
  }

  public void setSnoozeMinutes(int minutes) {
    if (minutes < 1) {
      minutes = 1;
    } else if (minutes > 60) {
      minutes = 60;
    }
    this.snoozeMinutes = minutes;
  }

  public boolean getVibrate() {
    return vibrate;
  }

  public void setVibrate(boolean vibrate) {
    this.vibrate = vibrate;
  }
}
