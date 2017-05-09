package by.bsuir.vladlipski.alarmon;

import java.lang.reflect.Field;

import android.net.Uri;
import android.provider.Settings;

public final class AlarmUtil {
  static public Uri alarmIdToUri(long alarmId) {
    return Uri.parse("alarm_id:" + alarmId);
  }

  public static long alarmUriToId(Uri uri) {
    return Long.parseLong(uri.getSchemeSpecificPart());
  }

  enum Interval {
    SECOND(1000), MINUTE(60 * 1000), HOUR(60 * 60 * 1000);
    private long millis;
    public long millis() { return millis; }
    Interval(long millis) {
      this.millis = millis;
    }
  }

  public static long millisTillNextInterval(Interval interval) {
    long now = System.currentTimeMillis();
    return interval.millis() - now % interval.millis();
  }

  public static long nextIntervalInUTC(Interval interval) {
    long now = System.currentTimeMillis();
    return now + interval.millis() - now % interval.millis();
  }

  public static Uri getDefaultAlarmUri() {
    try {
      Field f = Settings.System.class.getField("DEFAULT_ALARM_ALERT_URI");
      return (Uri) f.get(null);
    } catch (Exception e) {
      return Settings.System.DEFAULT_NOTIFICATION_URI;
    }
  }
}
