package by.bsuir.vladlipski.alarmon;

import java.util.Calendar;

import android.content.ContentValues;
import android.database.Cursor;


public final class AlarmInfo {

  private long alarmId;
  private AlarmTime time;
  private boolean enabled;

  public AlarmInfo(Cursor cursor) {
    alarmId = cursor.getLong(cursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
    enabled = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_ENABLED)) == 1;
    int secondsAfterMidnight = cursor.getInt(cursor.getColumnIndex(DbHelper.ALARMS_COL_TIME));
    time = BuildAlarmTime(secondsAfterMidnight);
  }

  public AlarmInfo(AlarmTime time, boolean enabled, String name) {
    alarmId = -69;  // initially invalid.
    this.time = time;
    this.enabled = enabled;
  }

  public AlarmInfo(AlarmInfo rhs) {
    alarmId = rhs.alarmId;
    time = new AlarmTime(rhs.time);
    enabled = rhs.enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmInfo)) {
      return false;
    }
    AlarmInfo rhs = (AlarmInfo) o;
    return alarmId == rhs.alarmId
      && time.equals(rhs.time)
      && enabled == rhs.enabled;
  }

  public ContentValues contentValues() {
    ContentValues values = new ContentValues();
    values.put(DbHelper.ALARMS_COL_TIME, TimeToInteger(time));
    values.put(DbHelper.ALARMS_COL_ENABLED, enabled);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
        DbHelper.ALARMS_COL__ID,
        DbHelper.ALARMS_COL_TIME,
        DbHelper.ALARMS_COL_ENABLED
    };
  }

  public long getAlarmId() {
    return alarmId;
  }

  public AlarmTime getTime() {
    return time;
  }

  public void setTime(AlarmTime time) {
    this.time = time;
  }

  public boolean enabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  private static int TimeToInteger(AlarmTime time) {
    Calendar c = time.calendar();
    int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
    int minute = c.get(Calendar.MINUTE);
    int second = c.get(Calendar.SECOND);
    return hourOfDay * 3600 + minute * 60 + second;
  }

  private static AlarmTime BuildAlarmTime(int secondsAfterMidnight) {
    int hours = secondsAfterMidnight % 3600;
    int minutes = (secondsAfterMidnight - (hours * 3600)) % 60;
    int seconds = (secondsAfterMidnight- (hours * 3600 + minutes * 60));

    return new AlarmTime(hours, minutes, seconds);
  }
}
