package by.bsuir.vladlipski.alarmon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;


public final class ActivityAlarmSettings extends AppCompatActivity implements
        TimePickerDialog.OnTimeChangedListener,
        TimePickerDialog.OnTimeSetListener {

  public static final String EXTRAS_ALARM_ID = "alarm_id";
  private static final int MISSING_EXTRAS = -69;
    private static final String SETTINGS_VIBRATE_KEY = "SETTINGS_VIBRATE_KEY";
    private static final String SETTINGS_SNOOZE_KEY = "SETTINGS_SNOOZE_KEY";
    private static final String SETTINGS_TIME_HOUR_OF_DAY_KEY = "SETTINGS_TIME_HOUR_OF_DAY_KEY";
    private static final String SETTINGS_TIME_MINUTE_KEY = "SETTINGS_TIME_MINUTE_KEY";
    private static final String SETTINGS_TIME_SECOND_KEY = "SETTINGS_TIME_SECOND_KEY";

  private enum SettingType {
    TIME,
    SNOOZE,
    VIBRATE
  }
    public static final int SNOOZE_PICKER = 4;
    public static final int DELETE_CONFIRM = 6;
    public static final int PERMISSION_NOT_GRANTED = 8;

    private TimePickerDialog picker;

  private static long alarmId;
  private static AlarmClockServiceBinder service;
  private DbAccessor db;
  private AlarmInfo originalInfo;
  private static AlarmInfo info;
  private static AlarmSettings originalSettings;
  private static AlarmSettings settings;
  static SettingsAdapter settingsAdapter;
  private static ProgressDialog progressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.settings);

      setTitle(getString(R.string.settings));

      ActionBar actionBar = getSupportActionBar();

      if (actionBar != null) {
          actionBar.setDisplayHomeAsUpEnabled(true);
      }

    alarmId = getIntent().getExtras().getLong(EXTRAS_ALARM_ID, MISSING_EXTRAS);
    if (alarmId == MISSING_EXTRAS) {
      throw new IllegalStateException("EXTRAS_ALARM_ID not supplied in intent.");
    }

    service = new AlarmClockServiceBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());

    originalInfo = db.readAlarmInfo(alarmId);
    if (originalInfo != null) {
      info = new AlarmInfo(originalInfo);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SETTINGS_TIME_HOUR_OF_DAY_KEY)
                    && savedInstanceState.containsKey(SETTINGS_TIME_MINUTE_KEY)
                    && savedInstanceState.containsKey(SETTINGS_TIME_SECOND_KEY)) {
                info.setTime(new AlarmTime(
                                savedInstanceState.getInt(SETTINGS_TIME_HOUR_OF_DAY_KEY),
                                savedInstanceState.getInt(SETTINGS_TIME_MINUTE_KEY),
                                savedInstanceState.getInt(SETTINGS_TIME_SECOND_KEY)
                        ));
            }

        }
    }
    originalSettings = db.readAlarmSettings(alarmId);
    settings = new AlarmSettings(originalSettings);

      if (savedInstanceState != null) {
          if (savedInstanceState.containsKey(SETTINGS_SNOOZE_KEY)) {
              settings.setSnoozeMinutes(savedInstanceState.getInt(
                      SETTINGS_SNOOZE_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_VIBRATE_KEY)) {
              settings.setVibrate(savedInstanceState.getBoolean(
                      SETTINGS_VIBRATE_KEY));
          }
      }

    final ArrayList<Setting> settingsObjects =
      new ArrayList<>(SettingType.values().length);
    if (alarmId != AlarmSettings.DEFAULT_SETTINGS_ID) {
      settingsObjects.add(new Setting() {
        @Override
        public String name() { return getString(R.string.time); }
        @Override
        public String value() { return info.getTime().localizedString(getApplicationContext()); }
        @Override
        public SettingType type() { return SettingType.TIME; }
      });
    }
    settingsObjects.add(new Setting() {
      @Override
      public String name() { return getString(R.string.snooze_minutes); }
      @Override
      public String value() { return "" + settings.getSnoozeMinutes(); }
      @Override
      public SettingType type() { return SettingType.SNOOZE; }
    });
    // The vibrator setting for this alarm.
    settingsObjects.add(new Setting() {
      @Override
      public String name() { return getString(R.string.vibrate); }
      @Override
      public String value() { return settings.getVibrate() ?
          getString(R.string.enabled) : getString(R.string.disabled); }
      @Override
      public SettingType type() { return SettingType.VIBRATE; }
    });

    final ListView settingsList = (ListView) findViewById(R.id.settings_list);
    settingsAdapter = new SettingsAdapter(getApplicationContext(),
            settingsObjects);
    settingsList.setAdapter(settingsAdapter);
    settingsList.setOnItemClickListener(new SettingsListClickListener());
  }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (alarmId == AlarmSettings.DEFAULT_SETTINGS_ID) {
            return super.onCreateOptionsMenu(menu);
        } else {
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.alarm_settings_menu, menu);

            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_alarm:
                showDialogFragment(DELETE_CONFIRM);
                return true;
            case android.R.id.home:
                saveAlarmSettings();

                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        saveAlarmSettings();
    }

    @Override
  protected void onResume() {
    super.onResume();
    service.bind();

      TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().
              findFragmentByTag("TimePickerDialog");

      if (tpd != null) {
          picker = tpd;
          tpd.setOnTimeSetListener(this);
          tpd.setOnTimeChangedListener(this);
      }
  }

  @Override
  protected void onPause() {
    super.onPause();
    service.unbind();
  }

  @Override
  protected void onDestroy() {
      super.onDestroy();
      db.closeConnections();
      if (progressDialog != null) {
          progressDialog.dismiss();
      }
      progressDialog = null;
      picker = null;
      settingsAdapter = null;
  }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SETTINGS_VIBRATE_KEY, settings.getVibrate());
        outState.putInt(SETTINGS_SNOOZE_KEY, settings.getSnoozeMinutes());
        if (originalInfo != null && info != null) {
            final AlarmTime infoTime = info.getTime();

            outState.putInt(SETTINGS_TIME_HOUR_OF_DAY_KEY,
                    infoTime.calendar().get(Calendar.HOUR_OF_DAY));

            outState.putInt(SETTINGS_TIME_MINUTE_KEY,
                    infoTime.calendar().get(Calendar.MINUTE));

            outState.putInt(SETTINGS_TIME_SECOND_KEY,
                    infoTime.calendar().get(Calendar.SECOND));
        }
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        info.setTime(new AlarmTime(hourOfDay, minute, second));
        settingsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTimeChanged(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        final AlarmTime time = new AlarmTime(hourOfDay, minute, second);
        picker.setTitle(time.timeUntilString(this));
    }

    private void saveAlarmSettings() {
        if (originalInfo != null && !originalInfo.equals(info)) {
            db.writeAlarmInfo(alarmId, info);
            if (!originalInfo.getTime().equals(info.getTime())) {
                service.scheduleAlarm(alarmId);
            } else if (originalInfo.enabled()) {
                service.scheduleAlarm(alarmId);
            }
        }

        if (!originalSettings.equals(settings)) {
            db.writeAlarmSettings(alarmId, settings);
        }
    }

  private final class SettingsListClickListener implements OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      final SettingsAdapter adapter = (SettingsAdapter) parent.getAdapter();
      SettingType type = adapter.getItem(position).type();
      switch (type) {
        case TIME:
            final AlarmTime time = info.getTime();

            Calendar c = time.calendar();

            picker = TimePickerDialog.newInstance(
                    ActivityAlarmSettings.this,
                    ActivityAlarmSettings.this,
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(ActivityAlarmSettings.this)
            );

            picker.setThemeDark(true);
            picker.vibrate(true);
            picker.enableSeconds(false);
            picker.setTitle(time.timeUntilString(ActivityAlarmSettings.this));
            picker.show(getFragmentManager(), "TimePickerDialog");
          break;

        case SNOOZE:
          showDialogFragment(SNOOZE_PICKER);
          break;
      }
    }
  }

  private abstract class Setting {
    public abstract String name();
    public abstract String value();
    public abstract SettingType type();
  }

    private final class SettingsAdapter extends ArrayAdapter<Setting> {

        List<Setting> settingsObjects;

        public SettingsAdapter(Context context, List<Setting> settingsObjects) {
            super(context, 0, settingsObjects);

            this.settingsObjects = settingsObjects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

            LayoutInflater inflater = getLayoutInflater();

            if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.vibrate))) {
                convertView = inflater.inflate(R.layout.vibrate_settings_item, parent,
                        false);
            } else {
                convertView = inflater.inflate(R.layout.settings_item, parent,
                        false);
            }

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
			holder.populateFrom(settingsObjects.get(position));
			return(convertView);
        }

    }

	private class ViewHolder {
        private View row;
		ViewHolder(View row) {
            this.row = row;
		}
		void populateFrom(Setting setting) {
            ((TextView) row.findViewById(R.id.setting_name)).
                    setText(setting.name());
            if (setting.name().equalsIgnoreCase(getString(R.string.vibrate))) {
                SwitchCompat vibrateSwitch = (SwitchCompat) row.findViewById(
                        R.id.setting_vibrate_sc);

                if (settings.getVibrate()) {
                    vibrateSwitch.setChecked(true);
                } else {
                    vibrateSwitch.setChecked(false);
                }

                vibrateSwitch.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            settings.setVibrate(true);
                        } else {
                            settings.setVibrate(false);
                        }
                    }
                });
            } else {
                ((TextView) row.findViewById(R.id.setting_value)).
                        setText(setting.value());
            }
		}

	}

    private void showDialogFragment(int id) {
        DialogFragment dialog = new ActivityDialogFragment().newInstance(
                id);
        dialog.show(getFragmentManager(), "ActivityDialogFragment");
    }

  public static class ActivityDialogFragment extends DialogFragment {

    public ActivityDialogFragment newInstance(int id) {
      ActivityDialogFragment fragment = new ActivityDialogFragment();
      Bundle args = new Bundle();
      args.putInt("id", id);
      fragment.setArguments(args);
      return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      switch (getArguments().getInt("id")) {
          case SNOOZE_PICKER:
              final CharSequence[] items = new CharSequence[60];
              for (int i = 1; i <= 60; ++i) {
                  items[i-1] = Integer.toString(i);
              }
              final AlertDialog.Builder snoozeBuilder = new AlertDialog.Builder(getActivity());
              snoozeBuilder.setTitle(R.string.snooze_minutes);
              snoozeBuilder.setSingleChoiceItems(items, settings.getSnoozeMinutes() - 1,
                      new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int item) {
                              settings.setSnoozeMinutes(item + 1);
                              settingsAdapter.notifyDataSetChanged();
                              dismiss();
                          }
                      });
              return snoozeBuilder.create();
          case DELETE_CONFIRM:
              final AlertDialog.Builder deleteConfirmBuilder = new AlertDialog.Builder(getActivity());
              deleteConfirmBuilder.setTitle(R.string.delete);
              deleteConfirmBuilder.setMessage(R.string.confirm_delete);
              deleteConfirmBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      service.deleteAlarm(alarmId);
                      dismiss();
                      getActivity().finish();
                  }
              });
              deleteConfirmBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dismiss();
                  }
              });
              return deleteConfirmBuilder.create();
        case PERMISSION_NOT_GRANTED:
            final AlertDialog.Builder permissionBuilder = new AlertDialog.Builder(
                    getActivity());
            permissionBuilder.setTitle(R.string.permission_not_granted_title);
            permissionBuilder.setMessage(R.string.permission_not_granted);
            permissionBuilder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
            return permissionBuilder.create();
        default:
          return super.onCreateDialog(savedInstanceState);
      }
    }

  }
}
