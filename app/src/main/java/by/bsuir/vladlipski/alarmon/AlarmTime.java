package by.bsuir.vladlipski.alarmon;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;


public final class AlarmTime implements Parcelable, Comparable<AlarmTime> {
  private Calendar calendar;

  public AlarmTime(AlarmTime rhs) {
    calendar = (Calendar) rhs.calendar.clone();
  }

  public AlarmTime(int hourOfDay, int minute, int second) {
    this.calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
    calendar.set(Calendar.MINUTE, minute);
    calendar.set(Calendar.SECOND, second);
    checkNextOccurrence();
  }

  private void checkNextOccurrence() {
    Calendar now = Calendar.getInstance();
    if (calendar.before(now)) {
      calendar.add(Calendar.DATE, 1);
    }
  }

  @Override
  public int compareTo(@NonNull AlarmTime another) {
    return calendar.compareTo(another.calendar);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmTime)) {
      return false;
    }
    AlarmTime rhs = (AlarmTime) o;

      return calendar.get(Calendar.HOUR_OF_DAY) == rhs.calendar().get(Calendar.HOUR_OF_DAY)
              && calendar.get(Calendar.MINUTE) == rhs.calendar().get(Calendar.MINUTE)
              && calendar.get(Calendar.SECOND) == rhs.calendar().get(Calendar.SECOND);
  }

  public String toString() {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm.ss MMMM dd yyyy",
            Locale.US);
    return formatter.format(calendar.getTimeInMillis());
  }

  public String localizedString(Context context) {
    boolean is24HourFormat = DateFormat.is24HourFormat(context);
    String format;
    String second = "";
    if (is24HourFormat) {
      format = "HH:mm";
    } else {
      format = "h:mm" + " aaa";
    }

    SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.US);
    return formatter.format(calendar.getTime());
  }

  public Calendar calendar() {
    return calendar;
  }

  public String timeUntilString(Context c) {
    Calendar now = Calendar.getInstance();
    if (calendar.before(now)) {
      return c.getString(R.string.alarm_has_occurred);
    }
    long now_min = now.getTimeInMillis() / 1000 / 60;
    long then_min = calendar.getTimeInMillis() / 1000 / 60;
    long difference_minutes = then_min - now_min;
    long days = difference_minutes / (60 * 24);
    long hours = difference_minutes % (60 * 24);
    long minutes = hours % 60;
    hours = hours / 60;

    String value = "";
    if (days == 1) {
      value += c.getString(R.string.day, days) + " ";
    } else if (days > 1) {
      value += c.getString(R.string.days, days) + " ";
    }
    if (hours == 1) {
      value += c.getString(R.string.hour, hours) + " ";
    } else if (hours > 1) {
      value += c.getString(R.string.hours, hours) + " ";
    }
    if (minutes == 1) {
      value += c.getString(R.string.minute, minutes) + " ";
    } else if (minutes > 1) {
      value += c.getString(R.string.minutes, minutes) + " ";
    }
    return value;
  }


  public static AlarmTime snoozeInMillisUTC(int minutes) {
    Calendar snooze = Calendar.getInstance();
    snooze.set(Calendar.SECOND, 0);
    snooze.add(Calendar.MINUTE, minutes);
    return new AlarmTime(
        snooze.get(Calendar.HOUR_OF_DAY),
        snooze.get(Calendar.MINUTE),
        snooze.get(Calendar.SECOND));
  }

  private AlarmTime(Parcel source) {
    this.calendar = (Calendar) source.readSerializable();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(calendar);
  }

  public static final Parcelable.Creator<AlarmTime> CREATOR =
    new Parcelable.Creator<AlarmTime>() {
      @Override
      public AlarmTime createFromParcel(Parcel source) {
        return new AlarmTime(source);
      }
      @Override
      public AlarmTime[] newArray(int size) {
        return new AlarmTime[size];
      }
    };

  @Override
  public int describeContents() {
    return 0;
  }
}
