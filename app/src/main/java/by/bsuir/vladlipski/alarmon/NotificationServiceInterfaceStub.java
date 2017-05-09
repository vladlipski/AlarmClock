package by.bsuir.vladlipski.alarmon;

import by.bsuir.vladlipski.alarmon.NotificationService.NoAlarmsException;

import android.os.RemoteException;


public class NotificationServiceInterfaceStub extends NotificationServiceInterface.Stub {
  private NotificationService service;

  public NotificationServiceInterfaceStub(NotificationService service) {
    this.service = service;
  }

  @Override
  public long currentAlarmId() throws RemoteException {
    try {
      return service.currentAlarmId();
    } catch (NoAlarmsException e) {
      throw new RemoteException();
    }
  }

  public int firingAlarmCount() throws RemoteException {
    return service.firingAlarmCount();
  }

  @Override
  public void acknowledgeCurrentNotification(int snoozeMinutes) throws RemoteException {
    try {
      service.acknowledgeCurrentNotification(snoozeMinutes);
    } catch (NoAlarmsException e) {
      throw new RemoteException();
    }
  }
}
