package by.bsuir.vladlipski.alarmon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public final class AppSettings {
    public static final String ALARM_TIMEOUT = "ALARM_TIMEOUT";

    public static int alarmTimeOutMins(Context c) {
        final String[] values = c.getResources().getStringArray(R.array.time_out_values);
        final String ONE_MIN = values[0];
        final String FIVE_MIN = values[1];
        final String TEN_MIN = values[2];
        final String THIRTY_MIN = values[3];
        final String SIXTY_MIN = values[4];

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        final String value = prefs.getString(ALARM_TIMEOUT, TEN_MIN);
        if (value.equals(ONE_MIN)) {
          return 1;
        } else if (value.equals(FIVE_MIN)) {
          return 5;
        } else if (value.equals(TEN_MIN)) {
          return 10;
        } else if (value.equals(THIRTY_MIN)) {
          return 30;
        } else if (value.equals(SIXTY_MIN)) {
          return 60;
        } else {
          return 10;
        }
    }

}
