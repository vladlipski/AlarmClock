package by.bsuir.vladlipski.alarmon;

import android.content.Context;
import android.os.RemoteException;

public final class AlarmClockInterfaceStub extends AlarmClockInterface.Stub {
  private Context context;
  private AlarmClockService service;

  AlarmClockInterfaceStub(Context context, AlarmClockService service) {
    this.context = context;
    this.service = service;
  }

  @Override
  public AlarmTime pendingAlarm(long alarmId) throws RemoteException {
    return service.pendingAlarm(alarmId);
  }

  @Override
  public AlarmTime[] pendingAlarmTimes() throws RemoteException {
    return service.pendingAlarmTimes();
  }

  @Override
  public long resurrectAlarm(AlarmTime time, String alarmName, boolean enabled)
      throws RemoteException {
    return service.resurrectAlarm(time, alarmName, enabled);
  }

  @Override
  public void createAlarm(AlarmTime time) throws RemoteException {
    service.createAlarm(time);
  }

  @Override
  public void deleteAlarm(long alarmId) throws RemoteException {
    service.deleteAlarm(alarmId);
  }

  @Override
  public void deleteAllAlarms() throws RemoteException {
    service.deleteAllAlarms();
  }

  @Override
  public void scheduleAlarm(long alarmId) throws RemoteException {
    service.scheduleAlarm(alarmId);
  }

  @Override
  public void unscheduleAlarm(long alarmId) {
    service.dismissAlarm(alarmId);
  }

  public void acknowledgeAlarm(long alarmId) {
    service.acknowledgeAlarm(alarmId);
  }

  @Override
  public void snoozeAlarm(long alarmId) throws RemoteException {
    service.snoozeAlarm(alarmId);
  }

  @Override
  public void snoozeAlarmFor(long alarmId, int minutes) throws RemoteException {
    service.snoozeAlarmFor(alarmId, minutes);
  }

}
