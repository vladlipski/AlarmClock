package by.bsuir.vladlipski.alarmon;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wdullaer.materialdatetimepicker.time.*;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;

public final class ActivityAlarmClock extends AppCompatActivity implements
        TimePickerDialog.OnTimeSetListener,
        TimePickerDialog.OnTimeChangedListener {

    public static final int DELETE_CONFIRM = 1;
    public static final int DELETE_ALARM_CONFIRM = 2;

    private TimePickerDialog picker;
    public static ActivityAlarmClock activityAlarmClock;

    private static AlarmClockServiceBinder service;
    private static NotificationServiceBinder notifyService;
    private DbAccessor db;
    private static AlarmAdapter adapter;
    private Cursor cursor;
    private Handler handler;
    private Runnable tickCallback;
    private static RecyclerView alarmList;
    private int mLastFirstVisiblePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.alarm_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        activityAlarmClock = this;

        service = new AlarmClockServiceBinder(getApplicationContext());

        db = new DbAccessor(getApplicationContext());

        handler = new Handler();

        alarmList = (RecyclerView) findViewById(R.id.alarm_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        alarmList.setLayoutManager(layoutManager);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final AlarmInfo alarmInfo = adapter.getAlarmInfos().
                        get(viewHolder.getAdapterPosition());

                final long alarmId = alarmInfo.getAlarmId();

                removeItemFromList(ActivityAlarmClock.this, alarmId,
                        viewHolder.getAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);

        itemTouchHelper.attachToRecyclerView(alarmList);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();

                picker = TimePickerDialog.newInstance(
                        ActivityAlarmClock.this,
                        ActivityAlarmClock.this,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(ActivityAlarmClock.this)
                );

                picker.setThemeDark(true);
                picker.vibrate(true);
                picker.enableSeconds(false);

                AlarmTime time = new AlarmTime(now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE), 0);

                picker.setTitle(time.timeUntilString(ActivityAlarmClock.this));
                picker.show(getFragmentManager(), "TimePickerDialog");
            }
        });

        tickCallback = new Runnable() {
            @Override
            public void run() {
                redraw();

                AlarmUtil.Interval interval = AlarmUtil.Interval.MINUTE;

                long next = AlarmUtil.millisTillNextInterval(interval);

                handler.postDelayed(tickCallback, next);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();

        service.bind();

        handler.post(tickCallback);

        requery();

        alarmList.getLayoutManager().scrollToPosition(mLastFirstVisiblePosition);

        notifyService = new NotificationServiceBinder(getApplicationContext());

        notifyService.bind();

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            @Override
            public void run(NotificationServiceInterface service) {
                int count;

                try {
                    count = service.firingAlarmCount();
                } catch (RemoteException e) {
                    return;
                }

                if (count > 0) {
                    Intent notifyActivity = new Intent(getApplicationContext(),
                            ActivityAlarmNotification.class);

                    notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(notifyActivity);
                }
            }
        });

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

        handler.removeCallbacks(tickCallback);

        service.unbind();

        if (notifyService != null) {
            notifyService.unbind();
        }

        mLastFirstVisiblePosition = ((LinearLayoutManager)
                alarmList.getLayoutManager()).
                findFirstCompletelyVisibleItemPosition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        db.closeConnections();

        activityAlarmClock = null;

        notifyService = null;

        cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        AlarmTime time = new AlarmTime(hourOfDay, minute, second);

        service.createAlarm(time);

        requery();
    }

    @Override
    public void onTimeChanged(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        AlarmTime time = new AlarmTime(hourOfDay, minute, second);

        picker.setTitle(time.timeUntilString(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                showDialogFragment(DELETE_CONFIRM);
                break;
            case R.id.action_default_settings:
                Intent alarm_settings = new Intent(getApplicationContext(),
                        ActivityAlarmSettings.class);

                alarm_settings.putExtra(ActivityAlarmSettings.EXTRAS_ALARM_ID,
                        AlarmSettings.DEFAULT_SETTINGS_ID);

                startActivity(alarm_settings);
                break;
            case R.id.action_app_settings:
                Intent app_settings = new Intent(getApplicationContext(),
                        ActivityAppSettings.class);

                startActivity(app_settings);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDialogFragment(int id) {
        DialogFragment dialog = new ActivityDialogFragment().newInstance(
                id);

        dialog.show(getFragmentManager(), "ActivityDialogFragment");
    }

    private void redraw() {
        adapter.notifyDataSetChanged();

        Calendar now = Calendar.getInstance();

        AlarmTime time = new AlarmTime(now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), 0);

        ((CollapsingToolbarLayout) findViewById(R.id.toolbar_layout)).setTitle(
                time.localizedString(this));
    }

    private void requery() {
        cursor = db.readAlarmInfo();

        ArrayList<AlarmInfo> infos = new ArrayList<>();

        while (cursor.moveToNext()) {
            infos.add(new AlarmInfo(cursor));
        }

        adapter = new AlarmAdapter(infos, service, this);

        alarmList.setAdapter(adapter);

        setEmptyViewIfEmpty(this);
    }

    public static void setEmptyViewIfEmpty(Activity activity) {
        if (adapter.getItemCount() == 0) {
            activity.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);

            alarmList.setVisibility(View.GONE);
        } else {
            activity.findViewById(R.id.empty_view).setVisibility(View.GONE);

            alarmList.setVisibility(View.VISIBLE);
        }
    }

    public static void removeItemFromList(Activity activity, long alarmId, int position) {
        if (adapter.getItemCount() == 1) {
            ((AppBarLayout) activity.findViewById(R.id.app_bar)).
                    setExpanded(true);
        }

        service.deleteAlarm(alarmId);

        adapter.removeAt(position);

        setEmptyViewIfEmpty(activity);
    }

    public static class ActivityDialogFragment extends DialogFragment {

        public ActivityDialogFragment newInstance(int id) {
            ActivityDialogFragment fragment = new ActivityDialogFragment();

            Bundle args = new Bundle();

            args.putInt("id", id);

            fragment.setArguments(args);

            return fragment;
        }

        public ActivityDialogFragment newInstance(int id, AlarmInfo info,
                int position) {
            ActivityDialogFragment fragment = new ActivityDialogFragment();

            Bundle args = new Bundle();

            args.putInt("id", id);

            args.putLong("alarmId", info.getAlarmId());

            args.putInt("position", position);

            fragment.setArguments(args);

            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            switch (getArguments().getInt("id")) {
                case ActivityAlarmClock.DELETE_CONFIRM:
                    final AlertDialog.Builder deleteConfirmBuilder =
                            new AlertDialog.Builder(getActivity());

                    deleteConfirmBuilder.setTitle(R.string.delete_all);

                    deleteConfirmBuilder.setMessage(R.string.confirm_delete);

                    deleteConfirmBuilder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            service.deleteAllAlarms();

                            adapter.removeAll();

                            setEmptyViewIfEmpty(getActivity());

                            dismiss();
                        }
                    });

                    deleteConfirmBuilder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
                    return deleteConfirmBuilder.create();
                case ActivityAlarmClock.DELETE_ALARM_CONFIRM:
                    final AlertDialog.Builder deleteAlarmConfirmBuilder =
                            new AlertDialog.Builder(getActivity());

                    deleteAlarmConfirmBuilder.setTitle(R.string.delete);

                    deleteAlarmConfirmBuilder.setMessage(
                            R.string.confirm_delete);

                    deleteAlarmConfirmBuilder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    removeItemFromList(getActivity(),
                                            getArguments().getLong("alarmId"),
                                            getArguments().getInt("position"));

                                    dismiss();
                                }
                            });

                    deleteAlarmConfirmBuilder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dismiss();
                                }
                            });
                    return deleteAlarmConfirmBuilder.create();
                default:
                    return super.onCreateDialog(savedInstanceState);
            }
        }

    }

}
